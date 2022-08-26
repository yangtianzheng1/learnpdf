package com.example.pdflibrary.edit;

import static com.example.pdflibrary.edit.PdfEditGraph.Rectangle;

import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.pdflibrary.edit.dealinterface.AnnotationActionInterface;
import com.example.pdflibrary.edit.module.EditGraphData;
import com.example.pdflibrary.edit.module.EditPdfData;
import com.example.pdflibrary.edit.module.EditTextData;
import com.example.pdflibrary.edit.module.RectFData;
import com.example.pdflibrary.util.LogUtils;
import com.example.pdflibrary.util.Size;
import com.example.pdflibrary.util.SizeF;
import com.example.pdflibrary.view.PDFView;
import com.example.pdflibrary.view.PDFViewForeground;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EditHandler extends Handler {

    public static final int MSG_TEXT_PAINT = 13;

    public static final int MSG_GRAPH_PAINT = 14;

    private static final String TAG = EditHandler.class.getName();

    private final PDFView pdfView;
    private final PDFViewForeground pdfViewForeground;

    private volatile boolean running = false;

    public EditHandler(Looper looper, PDFView pdfView, PDFViewForeground pdfViewForeground) {
        super(looper);
        this.pdfView = pdfView;
        this.pdfViewForeground = pdfViewForeground;
    }

    public void addEditTextTask(SelectText startSelectText, SelectText endSelectText, boolean isEnd, PdfEditColor editColor) {
        if (startSelectText == null || endSelectText == null) {
            return;
        }
        removeMessages(MSG_TEXT_PAINT);
        EditTextTask editTextTask = new EditTextTask(startSelectText, endSelectText, editColor);
        editTextTask.isActionLeave = isEnd;
        Message msg = obtainMessage(MSG_TEXT_PAINT, editTextTask);
        sendMessage(msg);
    }

    public void addEditGraphTask(SelectGraph selectGraphStart, SelectGraph selectGraphEnd, boolean isEnd,PdfEditColor editColor){
        if (selectGraphStart == null || selectGraphEnd == null){
            return;
        }
        removeMessages(MSG_GRAPH_PAINT);
        EditGraphTask editGraphTask = new EditGraphTask(selectGraphStart, selectGraphEnd, isEnd, editColor);
        Message message = obtainMessage(MSG_GRAPH_PAINT, editGraphTask);
        sendMessage(message);
    }

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case MSG_TEXT_PAINT:
                EditTextTask task = (EditTextTask) message.obj;
                dealEditTextTask(task);
                break;
            case MSG_GRAPH_PAINT:
                EditGraphTask graphTask = (EditGraphTask) message.obj;
                dealEditGraphTask(graphTask);
                break;
            default:
                break;
        }
    }

    private void dealEditGraphTask(EditGraphTask editGraphTask){
        SelectGraph start = editGraphTask.selectGraphStart;
        SelectGraph end = editGraphTask.selectGraphEnd;
        if (start == null || end == null){
            return;
        }
        if (start.page == end.page){
            EditGraphData editGraphData = new EditGraphData();
            editGraphData.color = editGraphTask.pdfEditColor;
            editGraphData.page = start.page + 1;
            editGraphData.editGraph = start.pdfEditGraph;

            RectF rectF = new RectF();
            rectF.left = Math.min(start.canvasX,  end.canvasX);
            rectF.top = Math.min(start.canvasY, end.canvasY);
            rectF.right = Math.max(start.canvasX,  end.canvasX);
            rectF.bottom = Math.max(start.canvasY, end.canvasY);

            RectFData rectFData = new RectFData(rectF, start.zoom);

            rectFData.left = Math.min(start.pageX, end.pageX);
            rectFData.right = Math.max(start.pageX, end.pageX);
            rectFData.top = Math.max(start.pageY, end.pageY);
            rectFData.bottom = Math.min(start.pageY, end.pageY);

            editGraphData.rectFDataList =  new ArrayList<>();
            editGraphData.rectFDataList.add(rectFData);

            if (editGraphTask.isActionLeave){
                editGraphData.viewRect = new Rect();
                editGraphData.viewRect.left = (int) Math.min(start.viewX, end.viewX);
                editGraphData.viewRect.right = (int) Math.max(start.viewX, end.viewX);
                editGraphData.viewRect.top = (int) Math.min(start.viewY, end.viewY);
                editGraphData.viewRect.bottom = (int) Math.max(start.viewY, end.viewY);
            }
            doUiTask(editGraphTask.isActionLeave, editGraphData, editGraphData.rectFDataList);
        }
    }

    private void doUiTask(boolean isEnd, EditPdfData editPdfData, List<RectFData> rectFList){
        if (pdfView == null){
            return;
        }
        pdfView.post(new Runnable() {
            @Override
            public void run() {
                if (isEnd){
                    pdfViewForeground.addEditPdfData(editPdfData, new AnnotationActionInterface() {
                        @Override
                        public void createUpdateDeleteCallBack(int status) {
                            if (status == 0 && pdfView != null && pdfViewForeground != null){
                                pdfViewForeground.clearDrawData();
                                pdfViewForeground.addOrUpdateData(editPdfData.page, editPdfData.key, editPdfData);
                                pdfView.invalidate();
                            }
                        }
                    });
                }else {
                    pdfViewForeground.clearDrawData();
                    if (editPdfData instanceof EditTextData){
                        PdfAnnotationDrawData pdfAnnotationDrawData = new PdfAnnotationDrawData(true, true, rectFList);
                        pdfViewForeground.addDrawData(pdfAnnotationDrawData);
                    }else if (editPdfData instanceof EditGraphData){
                        PdfAnnotationDrawData pdfAnnotationDrawData = new PdfAnnotationDrawData(false, ((EditGraphData) editPdfData).editGraph == Rectangle, rectFList);
                        pdfViewForeground.addDrawData(pdfAnnotationDrawData);
                    }
                    pdfView.invalidate();
                }
            }
        });
    }

    private void dealEditTextTask(EditTextTask editTextTask) {
        SelectText start = editTextTask.startSelectText;
        SelectText end = editTextTask.endSelectText;
        if (start.page != -1 && start.page == end.page) {
            if (start.pageText != null && start.charIdx >= 0 && start.charIdx < start.pageText.length() && end.charIdx >= 0 && end.charIdx < end.pageText.length()) {
                int startCharIdx = start.charIdx;
                int endCharIdx = end.charIdx;
                if (startCharIdx == endCharIdx) {
                    return;
                }
                if (startCharIdx > endCharIdx) {
                    int temp = startCharIdx;
                    startCharIdx = endCharIdx;
                    endCharIdx = temp;
                }
                String result = start.pageText.substring(startCharIdx, endCharIdx);
                String[] lines = result.split("\\r?\\n");
                int startIndex = startCharIdx;
                int endIndex;
                LinkedList<RectFData> rectFS = new LinkedList<RectFData>();
                SizeF pageSize = pdfView.pdfFile.getScaledPageSize(start.page, start.zoom);
                float pageMainOffset = start.pageMainOffset;
                float pageSecondaryOffset = start.pageSecondaryOffset;
                for (String string : lines) {
                    int length = string.length();
                    endIndex = startIndex + length;
                    if (endIndex > endCharIdx) {
                        break;
                    }
                    ArrayList<RectF> subRectFS = new ArrayList<>();
                    if (length < 4) {
                        pdfView.pdfFile.getTextRects(start.page, 0, 0, pageSize.toSize(), subRectFS, start.textPtr, startIndex, length + 1);
                    } else {
                        int count = 4;
                        while (subRectFS.size() == 0 && count >= 2) {
                            pdfView.pdfFile.getTextRects(start.page, 0, 0, pageSize.toSize(), subRectFS, start.textPtr, startIndex, count);
                            count--;
                        }
                        if (subRectFS.size() == 0) {
                            LogUtils.logD(TAG, " error  subRectFS.size() == 0", true);
                            continue;
                        }
                        count = 2;
                        ArrayList<RectF> rightRectFs = new ArrayList<>();
                        while (rightRectFs.size() == 0 && count >= 1) {
                            pdfView.pdfFile.getTextRects(start.page, 0, 0, pageSize.toSize(), rightRectFs, start.textPtr, endIndex - count, count + 1);
                            count--;
                        }
                        if (rightRectFs.size() == 0) {
                            LogUtils.logD(TAG, " error  rightRectFs.size() == 0", true);
                            continue;
                        }
                        subRectFS.addAll(rightRectFs);
                    }
                    if (subRectFS.size() > 0) {
                        RectF left = subRectFS.get(0);
                        if (subRectFS.size() > 1) {
                            RectF right = subRectFS.get(subRectFS.size() - 1);
                            left.right = right.right;
                            if (left.top > right.top) {
                                left.top = right.top;
                            }
                            if (left.bottom < right.bottom) {
                                left.bottom = right.bottom;
                            }
                        }
                        left.top -= 10;
                        left.bottom += 25;
                        left.left -= 5;
                        left.right += 20;
                        if (left.left < 0){
                            left.left = 0;
                        }
                        if (left.top < 0){
                            left.top = 0;
                        }
                        if (left.right > pageSize.getWidth()){
                            left.right = pageSize.getWidth();
                        }
                        if (left.bottom > pageSize.getHeight()){
                            left.bottom = pageSize.getHeight();
                        }

                        RectFData rectFData = new RectFData(left, start.zoom);
                        Size originalPageSize= pdfView.pdfFile.getOriginalPageSize(start.page);
                        int originalWidth = originalPageSize.getWidth();
                        int originalHeight = originalPageSize.getHeight();
                        float originalLeft = (left.left/pageSize.getWidth())*originalWidth;
                        float originalRight = (left.right/pageSize.getWidth())*originalWidth;
                        float originalTop = (1 - left.top/pageSize.getHeight())*originalHeight;
                        float originalBottom = (1 - left.bottom/pageSize.getHeight())*originalHeight;
                        rectFData.left = Math.min(originalLeft, originalRight);
                        rectFData.right = Math.max(originalLeft, originalRight);
                        rectFData.top = Math.max(originalTop, originalBottom);
                        rectFData.bottom = Math.min(originalTop, originalBottom);

                        if (start.isVertical){
                            left.offset(pageSecondaryOffset, pageMainOffset);
                        }else {
                            left.offset(pageMainOffset, pageSecondaryOffset);
                        }

                        rectFS.add(rectFData);
                    }
                    startIndex = endIndex + 2;
                }
                if (rectFS.size() > 0 ) {
                    boolean isActionLeave = editTextTask.isActionLeave;
                    EditTextData editTextData = new EditTextData();
                    editTextData.color = editTextTask.pdfEditColor;
                    editTextData.rectFDataList = rectFS;
                    editTextData.page = start.page + 1;
                    editTextData.text =  start.pageText.substring(startCharIdx, endCharIdx);
                    doUiTask(isActionLeave, editTextData, rectFS);
                }
            }
        }
    }

    public void stop() {
        running = false;
    }

    public void start() {
        running = true;
    }

    private static class EditTextTask {

        SelectText startSelectText;
        SelectText endSelectText;
        boolean isActionLeave = false;
        PdfEditColor pdfEditColor = null;

        public EditTextTask(SelectText startSelectText, SelectText endSelectText, PdfEditColor color) {
            this.startSelectText = startSelectText;
            this.endSelectText = endSelectText;
            pdfEditColor = color;
        }
    }

    private static class EditGraphTask{
        SelectGraph selectGraphStart;
        SelectGraph selectGraphEnd;
        boolean isActionLeave = false;
        PdfEditColor pdfEditColor = null;

        public EditGraphTask(SelectGraph selectGraphStart, SelectGraph selectGraphEnd, boolean isActionLeave, PdfEditColor pdfEditColor) {
            this.selectGraphStart = selectGraphStart;
            this.selectGraphEnd = selectGraphEnd;
            this.isActionLeave = isActionLeave;
            this.pdfEditColor = pdfEditColor;
        }
    }


    public ArrayList<RectF> mergeLineRects(List<RectF> selRects, RectF box, SelectText selectText, SizeF pageSize) {
        RectF tmp = new RectF();
        ArrayList<RectF> selLineRects = new ArrayList<>(selRects.size());
        RectF currentLineRect = null;
        for (RectF rI : selRects) {
            //CMN.Log("RectF rI:selRects", rI);
            if (currentLineRect != null && Math.abs((currentLineRect.top + currentLineRect.bottom) - (rI.top + rI.bottom)) < currentLineRect.bottom - currentLineRect.top) {
                currentLineRect.left = Math.min(currentLineRect.left, rI.left);
                currentLineRect.right = Math.max(currentLineRect.right, rI.right);
                currentLineRect.top = Math.min(currentLineRect.top, rI.top);
                currentLineRect.bottom = Math.max(currentLineRect.bottom, rI.bottom);
            } else {
                currentLineRect = new RectF();
                currentLineRect.set(rI);
                selLineRects.add(currentLineRect);
                int cid = getCharIdxAtPos(rI.left + 1, rI.top + rI.height() / 2, selectText, pageSize);
                if (cid > 0) {
                    getCharLoosePos(tmp, cid, selectText, pageSize);
                    currentLineRect.left = Math.min(currentLineRect.left, tmp.left);
                    currentLineRect.right = Math.max(currentLineRect.right, tmp.right);
                    currentLineRect.top = Math.min(currentLineRect.top, tmp.top);
                    currentLineRect.bottom = Math.max(currentLineRect.bottom, tmp.bottom);
                }
            }
            if (box != null) {
                box.left = Math.min(box.left, currentLineRect.left);
                box.right = Math.max(box.right, currentLineRect.right);
                box.top = Math.min(box.top, currentLineRect.top);
                box.bottom = Math.max(box.bottom, currentLineRect.bottom);
            }
        }
        return selLineRects;
    }

    /**
     * Get the char index at a page position
     *
     * @param posX position X in the page coordinate<br/>
     * @param posY position Y in the page coordinate<br/>
     */
    public int getCharIdxAtPos(float posX, float posY, SelectText selectText, SizeF pageSize) {
        return pdfView.pdfFile.getCharIndexAtCoord(selectText.page, pageSize.getWidth(), pageSize.getHeight(), selectText.textPtr, posX, posY, 100, 100);
    }

    public void getCharLoosePos(RectF pos, int index, SelectText selectText, SizeF pageSize) {
        pdfView.pdfFile.nativeGetMixedLooseCharPos(selectText.page, 0, 0, (int) pageSize.getWidth(), (int) pageSize.getHeight(), pos, selectText.textPtr, index, true);
    }

}

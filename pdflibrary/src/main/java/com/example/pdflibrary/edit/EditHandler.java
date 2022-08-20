package com.example.pdflibrary.edit;

import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.example.pdflibrary.edit.module.EditTextData;
import com.example.pdflibrary.util.LogUtils;
import com.example.pdflibrary.util.SizeF;
import com.example.pdflibrary.view.PDFView;
import com.example.pdflibrary.view.PDFViewForeground;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class EditHandler extends Handler {

    public static final int MSG_TEXT_PAINT = 13;

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

    @Override
    public void handleMessage(Message message) {
        switch (message.what) {
            case MSG_TEXT_PAINT:
                EditTextTask task = (EditTextTask) message.obj;
                dealEditTextTask(task);
                break;
            default:
                break;
        }
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
                LogUtils.logD(TAG, " result " + result, true);
                LogUtils.logD(TAG, " length " + result.length(), true);
                String[] lines = result.split("\\r?\\n");
                int startIndex = startCharIdx;
                int endIndex;
                LinkedList<RectF> rectFS = new LinkedList<RectF>();
                SizeF pageSize = pdfView.pdfFile.getScaledPageSize(start.page, pdfView.getZoom());
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
                        LogUtils.logD(TAG, " rectF  left" + left, false);
                        if (subRectFS.size() > 1) {
                            RectF right = subRectFS.get(subRectFS.size() - 1);
                            LogUtils.logD(TAG, " rectF  right " + right, false);
                            left.right = right.right;
                            if (left.top > right.top) {
                                left.top = right.top;
                            }
                            if (left.bottom < right.bottom) {
                                left.bottom = right.bottom;
                            }
                        }
                        left.top -= 10;
                        left.bottom += 15;
                        left.left -= 5;
                        left.right += 25;
                        left.offset(pageSecondaryOffset, pageMainOffset);
                        rectFS.add(left);
                    }
                    startIndex = endIndex + 2;
                }
                if (rectFS.size() > 0 ) {
                    boolean isActionLeave = editTextTask.isActionLeave;
                    EditTextData editTextData = new EditTextData();
                    editTextData.color = editTextTask.pdfEditColor;
                    editTextData.rectFList = rectFS;
                    editTextData.page = start.page;
                    editTextData.pageText = start.pageText;
                    editTextData.startIndexText = startCharIdx;
                    editTextData.endIndexText = endCharIdx;

                    pdfView.post(new Runnable() {
                        @Override
                        public void run() {
                            pdfViewForeground.clear();
                            if (isActionLeave){
                                pdfViewForeground.addEditTextData(editTextData);
                            }else {
                                pdfViewForeground.addEditTextRect(rectFS);
                            }
                            pdfView.invalidate();
                        }
                    });
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

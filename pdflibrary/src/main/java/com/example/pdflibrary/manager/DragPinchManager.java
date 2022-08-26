package com.example.pdflibrary.manager;

import static com.example.pdflibrary.Constants.Pinch.MAXIMUM_ZOOM;
import static com.example.pdflibrary.Constants.Pinch.MINIMUM_ZOOM;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.PopupWindow;

import com.example.pdflibrary.Constants;
import com.example.pdflibrary.PdfFile;
import com.example.pdflibrary.edit.EditHandler;
import com.example.pdflibrary.edit.PdfEditColor;
import com.example.pdflibrary.edit.PdfEditMode;
import com.example.pdflibrary.edit.SelectGraph;
import com.example.pdflibrary.edit.SelectPdf;
import com.example.pdflibrary.edit.SelectText;
import com.example.pdflibrary.edit.dealinterface.AnnotationActionInterface;
import com.example.pdflibrary.edit.dealinterface.EditTextInterface;
import com.example.pdflibrary.edit.dealinterface.EditTextInterfaceWidget;
import com.example.pdflibrary.edit.dealinterface.SelectColorInterface;
import com.example.pdflibrary.edit.dealinterface.SelectColorInterfaceWidget;
import com.example.pdflibrary.edit.module.EditPdfData;
import com.example.pdflibrary.edit.module.PopupWindowUtil;
import com.example.pdflibrary.edit.module.RectFData;
import com.example.pdflibrary.element.Link;
import com.example.pdflibrary.element.LinkTapEvent;
import com.example.pdflibrary.scroll.ScrollHandle;
import com.example.pdflibrary.util.LogUtils;
import com.example.pdflibrary.util.Size;
import com.example.pdflibrary.util.SizeF;
import com.example.pdflibrary.view.PDFView;
import com.example.pdflibrary.view.PDFViewForeground;

import java.util.List;

public class DragPinchManager implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    private final String TAG = "DragPinchManager";

    private final PDFView pdfView;
    private final AnimationManager animationManager;

    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleGestureDetector;

    private boolean scrolling = false;
    private boolean scaling = false;
    private boolean enabled = false;

    private boolean isTextEditMode = false;
    private boolean isGraphEditMode = false;
    private boolean isEraserMode = false;

    private SelectText selectTextStart;
    private SelectGraph selectGraphStart;

    private final PDFViewForeground pdfViewForeground;
    private EditHandler editHandler;

    public DragPinchManager(PDFView pdfView, AnimationManager animationManager, PDFViewForeground pdfViewForeground) {
        this.pdfView = pdfView;
        this.animationManager = animationManager;
        this.pdfViewForeground = pdfViewForeground;
        gestureDetector = new GestureDetector(pdfView.getContext(), this);
        scaleGestureDetector = new ScaleGestureDetector(pdfView.getContext(), this);
        pdfView.setOnTouchListener(this);
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

    public void disableLongpress() {
        gestureDetector.setIsLongpressEnabled(false);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        boolean onTapHandled = pdfView.callbacks.callOnTap(e);
        if (!isMultiPointer()){
            dealSingleTap(e);
        }
        if (!onTapHandled) {
            ScrollHandle ps = pdfView.getScrollHandle();
            if (ps != null && !pdfView.documentFitsView()) {
                if (!ps.shown()) {
                    ps.show();
                } else {
                    ps.hide();
                }
            }
        }
        pdfView.performClick();
        return true;
    }

    private void dealSingleTap(MotionEvent e){
        if (isTextEditMode || isGraphEditMode){
            EditPdfData editPdfData = null;
            SelectText selectText = getTextByActionTap(e.getX(), e.getY(), false, false);
            if (isTextEditMode){
                editPdfData = searchClickItem(selectText);
            }
            if (editPdfData == null && isGraphEditMode){
                editPdfData = searchClickItem(selectText);
            }
            showMenuPopupWindow(editPdfData, e.getX(), e.getY());
        }else if (isEraserMode){
            SelectText selectText = getTextByActionTap(e.getX(), e.getY(), false, false);
            EditPdfData editPdfData = searchClickItem(selectText);
            deleteAnnotation(editPdfData);
        }
    }

    private void deleteAnnotation(EditPdfData editPdfData){
        if (editPdfData != null){
            pdfView.getBusinessInterface().deletePdfAnnotation(editPdfData, new AnnotationActionInterface() {
                @Override
                public void createUpdateDeleteCallBack(int status) {
                    if (status == 2){
                        if (pdfViewForeground != null && pdfView != null){
                            pdfViewForeground.deleteData(editPdfData.page, editPdfData.key);
                            pdfView.invalidate();
                        }
                    }
                }
            });
        }
    }

    private EditPdfData searchClickItem(SelectText selectText){
        if (selectText != null){
            List<String> keys = pdfViewForeground.editPdfKeyMap.get(selectText.page + 1);
            if (keys != null && keys.size() > 0){
                for (String key : keys){
                    EditPdfData pdfData = pdfViewForeground.editPdfDataMap.get(key);
                    if (pdfData != null && pdfData.rectFDataList != null && pdfData.rectFDataList.size() > 0 ){
                        for (RectFData rectFData : pdfData.rectFDataList){
                            if (selectText.pageX >= rectFData.left && selectText.pageX <= rectFData.right
                             && selectText.pageY >= rectFData.bottom && selectText.pageY <= rectFData.top){
                                return pdfData;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private void showMenuPopupWindow(EditPdfData editPdfData, float viewX, float viewY){
        if (editPdfData == null){
            return;
        }
        pdfViewForeground.selectEditData(editPdfData);
        EditTextInterfaceWidget widget = pdfView.getBusinessInterface().getEditTextWidget();
        if (widget == null){
            return;
        }
        PopupWindow popupWindow = PopupWindowUtil.showEditTextPopupWindow(widget, pdfView, viewX, viewY, new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                pdfViewForeground.selectEditData(null);
            }
        });
        widget.setEditTextInterface(new EditTextInterface() {
            @Override
            public void openColorWindow() {
                popupWindow.dismiss();
                openColorSelectWindow(editPdfData, viewX, viewY);
            }

            @Override
            public void copyQuote() {
                popupWindow.dismiss();
                pdfView.getBusinessInterface().copyQuote(editPdfData);
            }

            @Override
            public void copyLabel() {
                popupWindow.dismiss();
                pdfView.getBusinessInterface().copyLabel(editPdfData);
            }

            @Override
            public void copyLink() {
                popupWindow.dismiss();
                pdfView.getBusinessInterface().copyLink(editPdfData);
            }

            @Override
            public void cleanColor() {
                popupWindow.dismiss();
                deleteAnnotation(editPdfData);
            }
        });
        pdfView.invalidate();
    }

    private void openColorSelectWindow(EditPdfData editPdfData, float viewX, float viewY){
        SelectColorInterfaceWidget widget = pdfView.getBusinessInterface().getSelectColorWidget(editPdfData.color);
        if (widget == null){
            return;
        }
        PopupWindow popupWindow = PopupWindowUtil.showSelectColorPopupWindow(widget, pdfView, viewX, viewY, false, false, null);
        widget.setSelectColorInterface(new SelectColorInterface() {
            @Override
            public void selectColorCallBack(PdfEditColor pdfEditColor) {
                popupWindow.dismiss();
                editPdfData.color = pdfEditColor;
                pdfView.getBusinessInterface().updatePdfAnnotation(editPdfData, new AnnotationActionInterface() {
                    @Override
                    public void createUpdateDeleteCallBack(int status) {
                        if (status != -1){
                            pdfView.invalidate();
                        }
                    }
                });
            }
        });
    }

    private void startPageFling(MotionEvent downEvent, MotionEvent ev, float velocityX, float velocityY) {
        if (!checkDoPageFling(velocityX, velocityY)) {
            return;
        }

        int direction;
        if (pdfView.isSwipeVertical()) {
            direction = velocityY > 0 ? -1 : 1;
        } else {
            direction = velocityX > 0 ? -1 : 1;
        }
        // get the focused page during the down event to ensure only a single page is changed
        float delta = pdfView.isSwipeVertical() ? ev.getY() - downEvent.getY() : ev.getX() - downEvent.getX();
        float offsetX = pdfView.getCurrentXOffset() - delta * pdfView.getZoom();
        float offsetY = pdfView.getCurrentYOffset() - delta * pdfView.getZoom();
        int startingPage = pdfView.findFocusPage(offsetX, offsetY);
        int targetPage = Math.max(0, Math.min(pdfView.getPageCount() - 1, startingPage + direction));

        Constants.SnapEdge edge = pdfView.findSnapEdge(targetPage);
        float offset = pdfView.snapOffsetForPage(targetPage, edge);
        animationManager.startPageFlingAnimation(-offset);
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (!pdfView.isDoubletapEnabled()) {
            return false;
        }

        if (pdfView.getZoom() < pdfView.getMidZoom()) {
            pdfView.zoomWithAnimation(e.getX(), e.getY(), pdfView.getMidZoom());
        } else if (pdfView.getZoom() < pdfView.getMaxZoom()) {
            pdfView.zoomWithAnimation(e.getX(), e.getY(), pdfView.getMaxZoom());
        } else {
            pdfView.resetZoomWithAnimation();
        }
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        animationManager.stopFling();
        isTextEditMode = false;
        isGraphEditMode = false;
        isEraserMode = false;
        if (pdfView.getCurrentMode() == PdfEditMode.TEXT) {
            isTextEditMode = true;
            if (!isMultiPointer()){
                selectTextStart = getTextByActionTap(e.getX(), e.getY(), false, true);
            }
        } else if (pdfView.getCurrentMode() == PdfEditMode.GRAPH) {
            isGraphEditMode = true;
            if (!isMultiPointer()){
                selectGraphStart = getSelectGraph(e.getX(), e.getY());
            }
        } else if (pdfView.getCurrentMode() == PdfEditMode.ERASER) {
            isEraserMode = true;
        }
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    private SelectText getTextByActionTap(float x, float y, boolean isMoveOrUp, boolean needText) {
        SelectText selectText = new SelectText();
        selectText.zoom = pdfView.getZoom();
        selectText.canvasX = x - pdfView.getCurrentXOffset();
        selectText.canvasY = y - pdfView.getCurrentYOffset();

        int page = pdfView.pdfFile.getPageAtOffset(pdfView.isSwipeVertical()? selectText.canvasY : selectText.canvasX, pdfView.getZoom());
        selectText.page = page;

        if (isMoveOrUp){
            if (selectTextStart != null){
                if (selectTextStart.page != page){
                    return null;
                }else {
                    selectText.pageMainOffset = selectTextStart.pageMainOffset;
                    selectText.pageSecondaryOffset = selectTextStart.pageSecondaryOffset;
                    selectText.textPtr = selectTextStart.textPtr;
                    selectText.pageText = selectTextStart.pageText;
                    SizeF pageSize = pdfView.pdfFile.getScaledPageSize(page, pdfView.getZoom());
                    double[] result = dealPageXY(selectText, page, pageSize);
                    if (needText){
                        selectText.charIdx = pdfView.pdfFile.getCharIndexAtCoord(page, pageSize.getWidth(), pageSize.getHeight(), selectText.textPtr, result[0], result[1], 10, 10);
                    }
                    return selectText;
                }
            }
        }

        SizeF pageSize = pdfView.pdfFile.getScaledPageSize(page, pdfView.getZoom());
        selectText.isVertical = pdfView.isSwipeVertical();
        double[] result = dealPageXY(selectText, page, pageSize);
        if (needText){
            long textId = pdfView.pdfFile.getPageTextId(page);
            selectText.textPtr = textId;
            selectText.pageText = pdfView.pdfFile.getTextFromTextPtr(textId);
            selectText.charIdx = pdfView.pdfFile.getCharIndexAtCoord(page, pageSize.getWidth(), pageSize.getHeight(), textId, result[0], result[1], 10, 10);
        }
        return selectText;
    }

    private double[] dealPageXY(SelectPdf selectPdf, int page, SizeF pageSize){
        float offsetX = 0;
        float offsetY = 0;
        if (selectPdf.isVertical){
            offsetY = pdfView.pdfFile.getPageOffset(page, pdfView.getZoom());
            offsetX = pdfView.pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());
            selectPdf.pageMainOffset = offsetY;
            selectPdf.pageSecondaryOffset = offsetX;
        }else {
            offsetX = pdfView.pdfFile.getPageOffset(page, pdfView.getZoom());
            offsetY = pdfView.pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());
            selectPdf.pageMainOffset = offsetX;
            selectPdf.pageSecondaryOffset = offsetY;
        }
        double dx = selectPdf.canvasX - offsetX;
        double dy = selectPdf.canvasY - offsetY;
        Size originalSize = pdfView.pdfFile.getOriginalPageSize(page);
        selectPdf.pageX = (float) (dx/pageSize.getWidth() * originalSize.getWidth());
        selectPdf.pageY = (float) ((1 - dy/pageSize.getHeight()) * originalSize.getHeight());
        if (selectPdf.pageX < 0 ){
            selectPdf.pageX = 0;
        }else if (selectPdf.pageX > originalSize.getWidth()){
            selectPdf.pageX = originalSize.getWidth();
        }
        if (selectPdf.pageY < 0 ){
            selectPdf.pageY = 0;
        }else if (selectPdf.pageY > originalSize.getHeight()){
            selectPdf.pageY = originalSize.getHeight();
        }
        return new double[]{dx, dy};
    }

    private SelectGraph getSelectGraph(float x, float y){
        SelectGraph selectGraph = new SelectGraph();
        selectGraph.zoom = pdfView.getZoom();
        selectGraph.viewX = x;
        selectGraph.viewY = y;

        selectGraph.pdfEditGraph = pdfView.getPdfEditGraph();
        selectGraph.canvasX = x - pdfView.getCurrentXOffset();
        selectGraph.canvasY = y - pdfView.getCurrentYOffset();

        int page = pdfView.pdfFile.getPageAtOffset(pdfView.isSwipeVertical()?selectGraph.canvasY: selectGraph.canvasX, pdfView.getZoom());
        selectGraph.page = page;

        SizeF pageSize = pdfView.pdfFile.getScaledPageSize(page, pdfView.getZoom());
        selectGraph.isVertical = pdfView.isSwipeVertical();
        dealPageXY(selectGraph, page, pageSize);
        return selectGraph;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        LogUtils.logD(TAG, " onScroll  e2 " + e2.toString(),true);
        scrolling = true;
        if (!isMultiPointer()){
            if (isTextEditMode) {
                textEdit(e2.getX(), e2.getY(), false);
                return true;
            } else if (isGraphEditMode) {
                graphEdit(e2.getX(), e2.getY(), false);
                return true;
            }
        }else {
            selectTextStart = null;
            selectGraphStart = null;
        }
        if (pdfView.isZooming() || pdfView.isSwipeEnabled()) {
            pdfView.moveRelativeTo(-distanceX, -distanceY);
        }
        if (!scaling || pdfView.doRenderDuringScale()) {
            pdfView.loadPageByOffset();
        }
        return true;
    }

    private void onScrollEnd(MotionEvent event) {
        pdfView.loadPages();
        hideHandle();
        if (!animationManager.isFlinging()) {
            pdfView.performPageSnap();
        }
    }

    @Override
    public void onLongPress(MotionEvent e) {
        pdfView.callbacks.callOnLongPress(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        LogUtils.logD(TAG, " onFling  e1 " + e1.toString() + " e2 " + e1.toString());
        if (!pdfView.isSwipeEnabled()) {
            return false;
        }
        if (pdfView.isPageFlingEnabled()) {
            if (pdfView.pageFillsScreen()) {
                onBoundedFling(velocityX, velocityY);
            } else {
                startPageFling(e1, e2, velocityX, velocityY);
            }
            return true;
        }

        int xOffset = (int) pdfView.getCurrentXOffset();
        int yOffset = (int) pdfView.getCurrentYOffset();

        float minX, minY;
        PdfFile pdfFile = pdfView.pdfFile;
        if (pdfView.isSwipeVertical()) {
            minX = -(pdfView.toCurrentScale(pdfFile.getMaxPageWidth()) - pdfView.getWidth());
            minY = -(pdfFile.getDocLen(pdfView.getZoom()) - pdfView.getHeight());
        } else {
            minX = -(pdfFile.getDocLen(pdfView.getZoom()) - pdfView.getWidth());
            minY = -(pdfView.toCurrentScale(pdfFile.getMaxPageHeight()) - pdfView.getHeight());
        }

        animationManager.startFlingAnimation(xOffset, yOffset, (int) (velocityX), (int) (velocityY),
                (int) minX, 0, (int) minY, 0);
        return true;
    }

    private void onBoundedFling(float velocityX, float velocityY) {
        int xOffset = (int) pdfView.getCurrentXOffset();
        int yOffset = (int) pdfView.getCurrentYOffset();

        PdfFile pdfFile = pdfView.pdfFile;

        float pageStart = -pdfFile.getPageOffset(pdfView.getCurrentPage(), pdfView.getZoom());
        float pageEnd = pageStart - pdfFile.getPageLength(pdfView.getCurrentPage(), pdfView.getZoom());
        float minX, minY, maxX, maxY;
        if (pdfView.isSwipeVertical()) {
            minX = -(pdfView.toCurrentScale(pdfFile.getMaxPageWidth()) - pdfView.getWidth());
            minY = pageEnd + pdfView.getHeight();
            maxX = 0;
            maxY = pageStart;
        } else {
            minX = pageEnd + pdfView.getWidth();
            minY = -(pdfView.toCurrentScale(pdfFile.getMaxPageHeight()) - pdfView.getHeight());
            maxX = pageStart;
            maxY = 0;
        }

        animationManager.startFlingAnimation(xOffset, yOffset, (int) (velocityX), (int) (velocityY),
                (int) minX, (int) maxX, (int) minY, (int) maxY);
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float dr = detector.getScaleFactor();
        float wantedZoom = pdfView.getZoom() * dr;
        float minZoom = Math.min(MINIMUM_ZOOM, pdfView.getMinZoom());
        float maxZoom = Math.min(MAXIMUM_ZOOM, pdfView.getMaxZoom());
        if (wantedZoom < minZoom) {
            dr = minZoom / pdfView.getZoom();
        } else if (wantedZoom > maxZoom) {
            dr = maxZoom / pdfView.getZoom();
        }
        pdfView.zoomCenteredRelativeTo(dr, new PointF(detector.getFocusX(), detector.getFocusY()));
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        scaling = true;
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        pdfView.loadPages();
        hideHandle();
        scaling = false;
    }

    private int pointerCount;

    private boolean isMultiPointer(){
        if (scaling){
            return true;
        }
        return pointerCount > 0;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!enabled) {
            return false;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK){
            case MotionEvent.ACTION_POINTER_DOWN:
                pointerCount++;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                pointerCount--;
                break;
            default:
                break;
        }
        LogUtils.logD(TAG, " pointerCount " + pointerCount, true);
        boolean retVal = scaleGestureDetector.onTouchEvent(event);
        retVal = gestureDetector.onTouchEvent(event) || retVal;
        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (scrolling) {
                scrolling = false;
                if (!isMultiPointer()){
                    if (isTextEditMode) {
                        textEdit(event.getX(), event.getY(), true);
                        return true;
                    } else if (isGraphEditMode) {
                        graphEdit(event.getX(), event.getY(), true);
                        return true;
                    }
                }
                onScrollEnd(event);
            }
        }
        return retVal;
    }

    private int step = 0;

    private void textEdit(float x, float y, boolean isEnd) {
        step++;
        if (step == 3 || isEnd){
            step = 0;
        }else {
            return;
        }
        if (editHandler != null && selectTextStart != null){
            editHandler.addEditTextTask(selectTextStart, getTextByActionTap(x, y, true, true), isEnd, pdfView.getEditColor());
        }
    }

    private void graphEdit(float x, float y, boolean isEnd) {
        step++;
        if (step == 3 || isEnd){
            step = 0;
        }else {
            return;
        }
        if (editHandler != null && selectGraphStart != null){
            editHandler.addEditGraphTask(selectGraphStart, getSelectGraph(x, y ), isEnd, pdfView.getEditColor());
        }
    }

    private void hideHandle() {
        ScrollHandle scrollHandle = pdfView.getScrollHandle();
        if (scrollHandle != null && scrollHandle.shown()) {
            scrollHandle.hideDelayed();
        }
    }

    private boolean checkDoPageFling(float velocityX, float velocityY) {
        float absX = Math.abs(velocityX);
        float absY = Math.abs(velocityY);
        return pdfView.isSwipeVertical() ? absY > absX : absX > absY;
    }

    public void setEditHandler(EditHandler editHandler) {
        this.editHandler = editHandler;
    }
}


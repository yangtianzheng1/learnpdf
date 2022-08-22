package com.example.pdflibrary.manager;

import static com.example.pdflibrary.Constants.Pinch.MAXIMUM_ZOOM;
import static com.example.pdflibrary.Constants.Pinch.MINIMUM_ZOOM;

import android.graphics.PointF;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.PopupWindow;

import com.example.pdflibrary.Constants;
import com.example.pdflibrary.PdfFile;
import com.example.pdflibrary.edit.EditDataManager;
import com.example.pdflibrary.edit.EditHandler;
import com.example.pdflibrary.edit.PdfEditColor;
import com.example.pdflibrary.edit.PdfEditGraph;
import com.example.pdflibrary.edit.PdfEditMode;
import com.example.pdflibrary.edit.SelectText;
import com.example.pdflibrary.edit.dealinterface.EditTextInterface;
import com.example.pdflibrary.edit.dealinterface.EditTextInterfaceWidget;
import com.example.pdflibrary.edit.dealinterface.SelectColorInterface;
import com.example.pdflibrary.edit.dealinterface.SelectColorInterfaceWidget;
import com.example.pdflibrary.edit.dealinterface.SelectGraphInterface;
import com.example.pdflibrary.edit.dealinterface.SelectGraphInterfaceWidget;
import com.example.pdflibrary.edit.module.EditTextData;
import com.example.pdflibrary.edit.module.PopupWindowUtil;
import com.example.pdflibrary.element.Link;
import com.example.pdflibrary.element.LinkTapEvent;
import com.example.pdflibrary.scroll.ScrollHandle;
import com.example.pdflibrary.util.LogUtils;
import com.example.pdflibrary.util.ResultCoordinate;
import com.example.pdflibrary.util.Size;
import com.example.pdflibrary.util.SizeF;
import com.example.pdflibrary.util.ViewCanvasPageCoordinateUtil;
import com.example.pdflibrary.view.PDFView;
import com.example.pdflibrary.view.PDFViewForeground;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DragPinchManager implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    private String TAG = "DragPinchManager";

    private PDFView pdfView;
    private AnimationManager animationManager;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private boolean scrolling = false;
    private boolean scaling = false;
    private boolean enabled = false;

    private boolean isTextEditMode = false;
    private boolean isGraphEditMode = false;
    private boolean isEraserMode = false;

    private SelectText selectTextStart;

    private PDFViewForeground pdfViewForeground;
    private EditHandler editHandler;
    private EditDataManager editDataManager;

    public DragPinchManager(PDFView pdfView, AnimationManager animationManager, PDFViewForeground pdfViewForeground) {
        this.pdfView = pdfView;
        this.animationManager = animationManager;
        this.pdfViewForeground = pdfViewForeground;
        gestureDetector = new GestureDetector(pdfView.getContext(), this);
        scaleGestureDetector = new ScaleGestureDetector(pdfView.getContext(), this);
        editDataManager = new EditDataManager();
        pdfViewForeground.setEditDataManager(editDataManager);
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
//        boolean linkTapped = checkLinkTapped(e.getX(), e.getY());
        dealSingleTap(e);
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
        if (isTextEditMode){
            searchClickItem(getTextByActionTap(e.getX(), e.getY(), false), e.getX(), e.getY());
        }else if (isGraphEditMode){

        }else if (isEraserMode){

        }
    }

    private void searchClickItem(SelectText selectText, float viewX, float viewY){
        if (selectText != null){
            EditTextData editTextData = editDataManager.findEditTextData(selectText.page, selectText.charIdx);
            if (editTextData != null){
                pdfViewForeground.selectEditTextRectFs(editTextData);
                EditTextInterfaceWidget widget = pdfView.getBusinessInterface().getEditTextWidget();
                if (widget == null){
                    return;
                }
                PopupWindow popupWindow = PopupWindowUtil.showEditTextPopupWindow(widget, pdfView, viewX, viewY, editTextData, null);
                widget.setEditTextInterface(new EditTextInterface() {
                    @Override
                    public void openColorWindow() {
                        popupWindow.dismiss();
                        openColorSelectWindow(editTextData, viewX, viewY);
                    }

                    @Override
                    public void copyQuote() {

                    }

                    @Override
                    public void copyLabel() {

                    }

                    @Override
                    public void copyLink() {

                    }

                    @Override
                    public void cleanColor() {
                        openGraphSelectWindow(viewX, viewY);
                    }
                });
                pdfView.invalidate();
            }
        }
    }

    private void openColorSelectWindow(EditTextData editTextData, float viewX, float viewY){
        SelectColorInterfaceWidget widget = pdfView.getBusinessInterface().getSelectColorWidget();
        if (widget == null){
            return;
        }
        widget.setSelectColorInterface(new SelectColorInterface() {
            @Override
            public void selectColorCallBack(PdfEditColor pdfEditColor) {

            }
        });
        PopupWindow popupWindow = PopupWindowUtil.showSelectColorPopupWindow(widget, pdfView, viewX, viewY, editTextData, false, false, null);
    }

    private void openGraphSelectWindow(float viewX, float viewY){
        SelectGraphInterfaceWidget widget = pdfView.getBusinessInterface().getSelectGraphWidget();
        if (widget == null){
            return;
        }
        widget.setSelectGraphInterface(new SelectGraphInterface() {
            @Override
            public void selectGraphCallBack(PdfEditGraph pdfEditGraph) {

            }
        });
        PopupWindow popupWindow = PopupWindowUtil.showSelectGraphPopupWindow(widget, pdfView, viewX, viewY, null);
    }

    private boolean checkLinkTapped(float x, float y) {
        PdfFile pdfFile = pdfView.pdfFile;
        if (pdfFile == null) {
            return false;
        }
        float mappedX = -pdfView.getCurrentXOffset() + x;
        float mappedY = -pdfView.getCurrentYOffset() + y;
        int page = pdfFile.getPageAtOffset(pdfView.isSwipeVertical() ? mappedY : mappedX, pdfView.getZoom());
        SizeF pageSize = pdfFile.getScaledPageSize(page, pdfView.getZoom());
        int pageX, pageY;
        if (pdfView.isSwipeVertical()) {
            pageX = (int) pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());
            pageY = (int) pdfFile.getPageOffset(page, pdfView.getZoom());
        } else {
            pageY = (int) pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());
            pageX = (int) pdfFile.getPageOffset(page, pdfView.getZoom());
        }
        for (Link link : pdfFile.getPageLinks(page)) {
            RectF mapped = pdfFile.mapRectToDevice(page, pageX, pageY, (int) pageSize.getWidth(),
                    (int) pageSize.getHeight(), link.getBounds());
            mapped.sort();
            if (mapped.contains(mappedX, mappedY)) {
                pdfView.callbacks.callLinkHandler(new LinkTapEvent(x, y, mappedX, mappedY, mapped, link));
                return true;
            }
        }
        return false;
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
        LogUtils.logD(TAG, " onDown  X " + e.getX() + " Y " + e.getY());
        isTextEditMode = false;
        isGraphEditMode = false;
        isEraserMode = false;
        if (pdfView.getCurrentMode() == PdfEditMode.TEXT) {
            isTextEditMode = true;
            selectTextStart = getTextByActionTap(e.getX(), e.getY(), false);
        } else if (pdfView.getCurrentMode() == PdfEditMode.GRAPH) {
            isGraphEditMode = true;
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

    private SelectText getTextByActionTap(float x, float y, boolean isMoveOrUp) {
        SelectText selectText = new SelectText();
        selectText.scale = pdfView.getZoom();
        ResultCoordinate resultCoordinate = ViewCanvasPageCoordinateUtil.viewCoordinateToCanvas(x, y,
                pdfView.getCurrentXOffset(), pdfView.getCurrentYOffset());
        selectText.canvasX = resultCoordinate.x;
        selectText.canvasY = resultCoordinate.y;

        int page = ViewCanvasPageCoordinateUtil.getPageIndexByCanvasXY(resultCoordinate.x.longValue(), resultCoordinate.y.longValue(), pdfView.getZoom(),
                pdfView.isSwipeVertical(), pdfView.pdfFile);
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
                    double dx = resultCoordinate.x - selectTextStart.pageSecondaryOffset;
                    double dy = resultCoordinate.y - selectTextStart.pageMainOffset;
                    Size originalSize = pdfView.pdfFile.getOriginalPageSize(page);
                    selectText.pageX = (float) (dx/pageSize.getWidth() * originalSize.getWidth());
                    selectText.pageY = (float) ((1 - dy/pageSize.getHeight()) * originalSize.getHeight());
                    selectText.charIdx = pdfView.pdfFile.getCharIndexAtCoord(page, pageSize.getWidth(), pageSize.getHeight(), selectText.textPtr, dx, dy, 10, 10);
                    return selectText;
                }
            }
        }
        SizeF pageSize = pdfView.pdfFile.getScaledPageSize(page, pdfView.getZoom());
        float offsetY = pdfView.pdfFile.getPageOffset(page, pdfView.getZoom());
        float offsetX = pdfView.pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());
        selectText.pageMainOffset = offsetY;
        selectText.pageSecondaryOffset = offsetX;
        double dx = resultCoordinate.x - offsetX;
        double dy = resultCoordinate.y - offsetY;
        Size originalSize = pdfView.pdfFile.getOriginalPageSize(page);
        selectText.pageX = (float) (dx/pageSize.getWidth() * originalSize.getWidth());
        selectText.pageY = (float) ((1 - dy/pageSize.getHeight()) * originalSize.getHeight());
        long textId = pdfView.pdfFile.getPageTextId(page);
        selectText.textPtr = textId;
        selectText.pageText = pdfView.pdfFile.getTextFromTextPtr(textId);
        selectText.charIdx = pdfView.pdfFile.getCharIndexAtCoord(page, pageSize.getWidth(), pageSize.getHeight(), textId, dx, dy, 10, 10);
        return selectText;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        LogUtils.logD(TAG, " onScroll  X " + e1.getX() + " Y " + e1.getY() + "  X  " + e2.getX() + " Y " + e2.getY());
        scrolling = true;
        if (isTextEditMode) {
            return true;
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
        LogUtils.logD(TAG, " onFling  X " + e1.getX() + " Y " + e1.getY() + "  X  " + e2.getX() + " Y " + e2.getY());
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!enabled) {
            return false;
        }
        boolean actionMove = event.getAction() == MotionEvent.ACTION_MOVE;
        boolean actionUp = event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL;
        if (actionMove || actionUp) {
            if (isTextEditMode) {
                textEdit(event.getX(), event.getY(), actionUp);
            } else if (isGraphEditMode) {
                graphEdit(event.getX(), event.getY());
            } else if (isEraserMode) {
                eraser(event.getX(), event.getY());
            }
        }

        boolean retVal = scaleGestureDetector.onTouchEvent(event);
        retVal = gestureDetector.onTouchEvent(event) || retVal;

        if (actionUp) {
            if (scrolling) {
                scrolling = false;
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
        long start = System.currentTimeMillis();
        if (editHandler != null){
            editHandler.addEditTextTask(selectTextStart, getTextByActionTap(x, y, true), isEnd, pdfView.getEditColor());
        }
        LogUtils.logD(TAG, " costTime " + (System.currentTimeMillis() - start), true);
    }

    private void graphEdit(float x, float y) {

    }

    private void eraser(float x, float y) {

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


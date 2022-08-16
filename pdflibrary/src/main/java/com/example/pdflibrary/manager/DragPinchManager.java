package com.example.pdflibrary.manager;

import static com.example.pdflibrary.Constants.Pinch.MAXIMUM_ZOOM;
import static com.example.pdflibrary.Constants.Pinch.MINIMUM_ZOOM;

import android.graphics.PointF;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import com.example.pdflibrary.Constants;
import com.example.pdflibrary.PdfFile;
import com.example.pdflibrary.element.Link;
import com.example.pdflibrary.element.LinkTapEvent;
import com.example.pdflibrary.scroll.ScrollHandle;
import com.example.pdflibrary.util.LogUtils;
import com.example.pdflibrary.util.ResultCoordinate;
import com.example.pdflibrary.util.SizeF;
import com.example.pdflibrary.util.ViewCanvasPageCoordinateUtil;
import com.example.pdflibrary.view.PDFView;
import com.example.pdflibrary.view.PDFViewForeground;

import java.util.ArrayList;

public class DragPinchManager implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener, View.OnTouchListener {

    private String TAG = "DragPinchManager";

    private PDFView pdfView;
    private AnimationManager animationManager;

    private GestureDetector gestureDetector;
    private ScaleGestureDetector scaleGestureDetector;

    private boolean scrolling = false;
    private boolean scaling = false;
    private boolean enabled = false;
    private boolean isLongPress = false;
    private float longPressStartX = 0;
    private float longPressStartY = 0;

    private PDFViewForeground pdfViewForeground;

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

    public  void disableLongpress(){
        gestureDetector.setIsLongpressEnabled(false);
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        boolean onTapHandled = pdfView.callbacks.callOnTap(e);
        boolean linkTapped = checkLinkTapped(e.getX(), e.getY());
        if (!onTapHandled && !linkTapped) {
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
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        LogUtils.logD(TAG, " onShowPress  X " + e.getX() + " Y " + e.getY());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        LogUtils.logD(TAG, " onSingleTapUp  X " + e.getX() + " Y " + e.getY());
//        String tapText = getTextByActionTap(e);
//        LogUtils.logD(TAG, " tapText " + tapText);
        return false;
    }

    private String getTextByActionTap(MotionEvent e){
        ResultCoordinate resultCoordinate = ViewCanvasPageCoordinateUtil.viewCoordinateToCanvas(e.getX(), e.getY(),
                pdfView.getCurrentXOffset(), pdfView.getCurrentYOffset());
        int page = ViewCanvasPageCoordinateUtil.getPageIndexByCanvasXY(resultCoordinate.x.longValue(), resultCoordinate.y.longValue(), pdfView.getZoom(),
                pdfView.isSwipeVertical(), pdfView.pdfFile);
        SizeF pageSize = pdfView.pdfFile.getScaledPageSize(page, pdfView.getZoom());
        LogUtils.logD(TAG, " pageSize " + pageSize);
        float offsetY = pdfView.pdfFile.getPageOffset(page, pdfView.getZoom());
        float offsetX = pdfView.pdfFile.getSecondaryPageOffset(page, pdfView.getZoom());
        double dx = resultCoordinate.x - offsetX;
        double dy = resultCoordinate.y - offsetY;
        long textId = pdfView.pdfFile.getPageTextId(page);
        String pageText = pdfView.pdfFile.getTextFromTextPtr(textId);
        int charIdx = pdfView.pdfFile.getCharIndexAtCoord(page, pageSize.getWidth(), pageSize.getHeight(), textId, dx, dy, 10, 10 );
        String result = "";
        if (charIdx >= 0 && charIdx < pageText.length()){
            result = pageText.substring(charIdx, charIdx + 1);
            LogUtils.logD(TAG, " tapText " + result);
            ArrayList<RectF> rectFS = new ArrayList<>();
            pdfView.pdfFile.getTextRects(page, 0, 0, pageSize.toSize(), rectFS, textId, charIdx, 15);
            for (int i = 0; i < rectFS.size(); i++){
                LogUtils.logD(TAG, " rect " + rectFS.get(i).toString());
                RectF rectF = rectFS.get(i);
                ResultCoordinate temp = new ResultCoordinate();
                ResultCoordinate temp1 = new ResultCoordinate();
                temp.x = Double.valueOf(rectF.left + offsetX);
                temp.y = Double.valueOf(rectF.top + offsetY);
                temp1.x = Double.valueOf(rectF.right + offsetX);
                temp1.y = Double.valueOf(rectF.bottom + offsetY);
                pdfViewForeground.drawRect(temp.x, temp.y, temp1.x, temp1.y);
            }
            pdfView.postInvalidate();
        }
        return result;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        LogUtils.logD(TAG, " onScroll  X " + e1.getX() + " Y " + e1.getY() + "  X  " + e2.getX() + " Y " + e2.getY());
        scrolling = true;
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
        LogUtils.logD(TAG, " onLongPress  X " + e.getX() + " Y " + e.getY());
        longPressStartX = e.getX();
        longPressStartY = e.getY();
        isLongPress = false;
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
        if (isLongPress){
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL){
                isLongPress = false;
                longPressStartX = 0;
                longPressStartY = 0;
                LogUtils.logD(TAG, " action " + event.getAction());
            }else if (event.getAction() == MotionEvent.ACTION_MOVE){
                LogUtils.logD(TAG, " ACTION_MOVE  X " + event.getX() + " Y " + event.getY());
                ResultCoordinate startCoordinate = ViewCanvasPageCoordinateUtil.viewCoordinateToCanvas(longPressStartX,
                        longPressStartY, pdfView.getCurrentXOffset(), pdfView.getCurrentYOffset());
                ResultCoordinate coordinate = ViewCanvasPageCoordinateUtil.viewCoordinateToCanvas(event.getX(),
                        event.getY(), pdfView.getCurrentXOffset(), pdfView.getCurrentYOffset());
                int pageIndexByCanvasXY = ViewCanvasPageCoordinateUtil.getPageIndexByCanvasXY(coordinate.x.longValue(), coordinate.y.longValue(), pdfView.getZoom(), pdfView.isSwipeVertical(), pdfView.pdfFile);
                LogUtils.logD(TAG, "  pageIndexByCanvasXY  " + pageIndexByCanvasXY);
                pdfViewForeground.drawRect(startCoordinate.x, startCoordinate.y, coordinate.x, coordinate.y);
                pdfView.postInvalidate();
            }
        }

        boolean retVal = scaleGestureDetector.onTouchEvent(event);
        retVal = gestureDetector.onTouchEvent(event) || retVal;

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (scrolling) {
                scrolling = false;
                onScrollEnd(event);
            }
        }
        return retVal;
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
}


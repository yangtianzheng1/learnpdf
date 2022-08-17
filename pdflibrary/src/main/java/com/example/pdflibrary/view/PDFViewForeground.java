package com.example.pdflibrary.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pdflibrary.util.LogUtils;

import java.util.LinkedList;
import java.util.List;

public class PDFViewForeground {

    private String TAG =  "PDFViewForeground";

    private List<RectF> rectFS = new LinkedList<>();
    private Paint paint;

    public PDFViewForeground(PDFView pdfView) {
        paint = new Paint();
        paint.setColor(pdfView.getEditColor());
    }

    public void onDraw(Canvas canvas){
        if (rectFS.size() > 0){
            for (RectF rectF : rectFS){
                canvas.drawRect(rectF, paint);
                LogUtils.logD(TAG, rectFS.toString());
            }
        }
    }

    public void drawRect(Float left, Float top, Float right, Float bottom){
        RectF rectF = new RectF(left,  top, right , bottom);
        rectFS.add(rectF);
    }

    public void drawRect(RectF rectF){
        rectFS.add(rectF);
    }

    public void clear(){
        rectFS.clear();
    }

    public void zoom(float zoom){
//        if (rectF != null){
//            scaleRectF = new RectF(rectF.left * zoom, rectF.top * zoom, rectF.right * zoom, rectF.bottom * zoom);
//        }
    }

}

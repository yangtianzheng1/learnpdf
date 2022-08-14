package com.example.pdflibrary.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pdflibrary.util.LogUtils;

public class PDFViewForeground {

    private String TAG =  "PDFViewForeground";

    private RectF rectF;
    private RectF scaleRectF;
    private Paint paint;

    public PDFViewForeground() {
        paint = new Paint();
        paint.setColor(0X55996633);
    }

    public void onDraw(Canvas canvas){
        if (scaleRectF != null){
            canvas.drawRect(scaleRectF, paint);
            return;
        }
        if (rectF != null){
            canvas.drawRect(rectF, paint);
            LogUtils.logD(TAG, rectF.toString());
        }
    }

    public void drawRect(Double left, Double top, Double right, Double bottom){
        rectF = new RectF( left.floatValue(),  top.floatValue(), right.floatValue() , bottom.floatValue());
    }

    public void zoom(float zoom){
        if (rectF != null){
            scaleRectF = new RectF(rectF.left * zoom, rectF.top * zoom, rectF.right * zoom, rectF.bottom * zoom);
        }
    }

}

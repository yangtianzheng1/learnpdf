package com.example.pdflibrary.view;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pdflibrary.util.LogUtils;

import java.util.LinkedList;
import java.util.List;

public class PDFViewForeground {

    private String TAG =  "PDFViewForeground";

    private List<RectF> editTextRectFs = new LinkedList<>();
    private Paint paint;

    public PDFViewForeground(PDFView pdfView) {
        paint = new Paint();
        paint.setColor(pdfView.getEditColor());
    }

    public void onDraw(Canvas canvas){
        if (editTextRectFs.size() > 0){
            for (RectF rectF : editTextRectFs){
                canvas.drawRect(rectF, paint);
            }
        }
    }

    public void clear(){
        editTextRectFs.clear();
    }

    public void addEditTextRect(List<RectF> rectFList){
        editTextRectFs.addAll(rectFList);
    }

    public void zoom(float zoom){
//        if (rectF != null){
//            scaleRectF = new RectF(rectF.left * zoom, rectF.top * zoom, rectF.right * zoom, rectF.bottom * zoom);
//        }
    }

}

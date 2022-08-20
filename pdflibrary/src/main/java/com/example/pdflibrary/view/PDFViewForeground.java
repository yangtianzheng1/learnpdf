package com.example.pdflibrary.view;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pdflibrary.edit.EditDataManager;
import com.example.pdflibrary.edit.PdfEditColor;
import com.example.pdflibrary.edit.module.EditTextData;
import com.example.pdflibrary.util.LogUtils;

import java.util.LinkedList;
import java.util.List;

public class PDFViewForeground {

    private String TAG =  "PDFViewForeground";

    private List<RectF> editTextRectFs = new LinkedList<>();
    private List<RectF> selectEditTextRectFs = new LinkedList<>();
    private Paint paint;
    private Paint selectPaint;
    private PDFView pdfView;

    public void setEditDataManager(EditDataManager editDataManager) {
        this.editDataManager = editDataManager;
    }

    private EditDataManager editDataManager;

    public PDFViewForeground(PDFView pdfView) {
        paint = new Paint();
        this.pdfView = pdfView;
        selectPaint = new Paint();
        selectPaint.setStyle(Paint.Style.STROKE);
        selectPaint.setStrokeWidth(2);
        selectPaint.setPathEffect(new DashPathEffect(new float[]{9, 5}, 0));
    }

    public void onDraw(Canvas canvas){
        paint.setColor(pdfView.getEditColor().getColor());
        selectPaint.setColor(pdfView.getEditColor().getColor());
        if (editTextRectFs.size() > 0){
            for (RectF rectF : editTextRectFs){
                canvas.drawRect(rectF, paint);
            }
        }

        if (selectEditTextRectFs != null && selectEditTextRectFs.size() > 0 ){
            drawSelectEditTextRectFs(selectEditTextRectFs, canvas);
        }

        int currentPage = pdfView.getCurrentPage();

        drawPage(currentPage, canvas);
        drawPage(currentPage - 1, canvas);
        drawPage(currentPage + 1, canvas);
    }

    private void drawPage(int page, Canvas canvas){
        List<EditTextData> editTextDataList = editDataManager.findEditTextDataList(page);
        if (editTextDataList != null){
            for (EditTextData editTextData : editTextDataList){
                if (editTextData.rectFList != null){
                    paint.setColor(editTextData.color.getColor());
                    for (RectF rectF : editTextData.rectFList){
                        canvas.drawRect(rectF, paint);
                    }
                }
            }
        }
    }

    private void drawSelectEditTextRectFs(List<RectF> rectFList, Canvas canvas){
        for (RectF rectF : rectFList){
            canvas.drawRect(rectF, selectPaint);
        }
    }

    public void clear(){
        editTextRectFs.clear();
    }

    public void addEditTextRect(List<RectF> rectFList){
        editTextRectFs.addAll(rectFList);
    }

    public void addEditTextData(EditTextData editTextData){
        if (editDataManager != null){
            editDataManager.putEditTextData(editTextData);
        }
    }

    public void selectEditTextRectFs(EditTextData editTextData){
        selectEditTextRectFs = editTextData.rectFList;
    }

    public void cleanSelectEditTextRectFs(){
        selectEditTextRectFs = null;
    }

    public void zoom(float zoom){
//        if (rectF != null){
//            scaleRectF = new RectF(rectF.left * zoom, rectF.top * zoom, rectF.right * zoom, rectF.bottom * zoom);
//        }
    }

}

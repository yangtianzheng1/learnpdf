package com.example.pdflibrary.view;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pdflibrary.edit.PdfEditGraph;
import com.example.pdflibrary.edit.module.EditGraphData;
import com.example.pdflibrary.edit.module.EditPdfData;
import com.example.pdflibrary.edit.module.EditTextData;
import com.example.pdflibrary.edit.module.RectFData;
import com.example.pdflibrary.util.Size;
import com.example.pdflibrary.util.SizeF;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class PDFViewForeground {

    private String TAG =  "PDFViewForeground";

    private List<RectFData> editTextRectFs = new LinkedList<>();
    private List<RectFData> selectEditTextRectFs = new LinkedList<>();

    private EditGraphData editGraphData = null;

    private EditGraphData selectGraphData = null;

    public Map<Integer, List<String>>  editPdfKeyMap = new TreeMap<>();

    public Map<String, EditPdfData>  editPdfDataMap = new HashMap<>();

    private Paint paint;
    private Paint selectPaint;
    private Paint graphPaint;

    private PDFView pdfView;

    public void setEditDataManager() {
    }

    public PDFViewForeground(PDFView pdfView) {
        paint = new Paint();
        graphPaint = new Paint();
        this.pdfView = pdfView;
        selectPaint = new Paint();
        selectPaint.setStyle(Paint.Style.STROKE);
        selectPaint.setStrokeWidth(2);
        selectPaint.setPathEffect(new DashPathEffect(new float[]{9, 5}, 0));
    }

    public void onDraw(Canvas canvas){

        paint.setColor(pdfView.getEditColor().getColor());
        graphPaint.setColor(pdfView.getEditColor().getColor());
        selectPaint.setColor(pdfView.getEditColor().getColor());

        if (editTextRectFs.size() > 0){
            for (RectFData rectFData : editTextRectFs){
                canvas.drawRect(rectFData.rectF, paint);
            }
        }

        if (editGraphData != null){
            if (editGraphData.editGraph == PdfEditGraph.Circle){
                if (editGraphData.rectFDataList != null && editGraphData.rectFDataList.size() > 0 ){
                    canvas.drawOval(editGraphData.rectFDataList.get(0).rectF, graphPaint);
                }
            }else {
                if (editGraphData.rectFDataList != null && editGraphData.rectFDataList.size() > 0 ){
                    canvas.drawRect(editGraphData.rectFDataList.get(0).rectF, graphPaint);
                }
            }
        }

        if (selectEditTextRectFs != null && selectEditTextRectFs.size() > 0 ){
            for (RectFData rectFData : selectEditTextRectFs){
                canvas.drawRect(rectFData.rectF, selectPaint);
            }
        }

        int currentPage = pdfView.getCurrentPage();

        drawPage(currentPage, canvas);
        drawPage(currentPage - 1, canvas);
        drawPage(currentPage + 1, canvas);
    }

    private void drawRectFData(int page, RectFData rectFData, Canvas canvas, Paint paint, boolean isOval){
        if (rectFData.rectF == null){
            Size originalSize = pdfView.pdfFile.getOriginalPageSize(page);
            SizeF sizeF = pdfView.pdfFile.getPageSize(page);
            float left = rectFData.left / originalSize.getWidth() * sizeF.getWidth();
            float right = rectFData.right/originalSize.getWidth() * sizeF.getWidth();
            float top = (1 - rectFData.top/originalSize.getHeight()) * sizeF.getHeight();
            float bottom = (1 - rectFData.bottom/originalSize.getHeight()) * sizeF.getHeight();
            RectF rectF = new RectF(left, top, right, bottom);
            float mainOffset = pdfView.pdfFile.getPageOffset(page, 1);
            float secondOffset = pdfView.pdfFile.getSecondaryPageOffset(page, 1);
            rectF.offset(secondOffset, mainOffset);
            rectFData.rectF = rectF;
        }
        if (isOval){
            canvas.drawOval(rectFData.rectF, paint);
        }else {
            canvas.drawRect(rectFData.rectF, paint);
        }
    }

    private void drawPage(int page, Canvas canvas){
        List<String> blockIds = editPdfKeyMap.get(page);
        if (blockIds != null && blockIds.size() > 0){
            for (String item : blockIds){
                EditPdfData editPdfData = editPdfDataMap.get(item);
                if (editPdfData != null){
                    paint.setColor(editPdfData.color.getColor());
                    if (editPdfData instanceof EditTextData){
                        for (RectFData rectFData : editPdfData.rectFDataList){
                            drawRectFData(page, rectFData, canvas, paint, false);
                        }
                    }else if (editPdfData instanceof EditGraphData){
                        if (((EditGraphData)editPdfData).editGraph == PdfEditGraph.Circle){
                            for (RectFData rectFData : editPdfData.rectFDataList){
                                drawRectFData(page, rectFData, canvas, graphPaint, false);
                            }
                        }else {
                            for (RectFData rectFData : editPdfData.rectFDataList){
                                drawRectFData(page, rectFData, canvas, graphPaint, true);
                            }
                        }
                    }
                }
            }
        }
    }

    public void clearEditText(){
        editTextRectFs.clear();
    }

    public void clearEditGraph(){
        editGraphData = null;
    }

    public void setEditGraphRectF(EditGraphData editGraphData){
        this.editGraphData = editGraphData;
    }

    public void addEditGraphData(EditGraphData editGraphData){
        pdfView.getBusinessInterface().createPdfAnnotation(editGraphData);
    }

    public void addEditTextRect(List<RectFData> rectFList){
        editTextRectFs.addAll(rectFList);
    }

    public void addEditTextData(EditTextData editTextData){
        pdfView.getBusinessInterface().createPdfAnnotation(editTextData);
    }

    public void addOrUpdateData(int page, String uuid, EditPdfData data){
        List<String> uuids = editPdfKeyMap.get(page);
        if (uuids == null){
            uuids = new ArrayList<>();
            uuids.add(uuid);
            editPdfKeyMap.put(page, uuids);
        }else {
            if (!uuids.contains(uuid)){
                uuids.add(uuid);
            }
        }
        editPdfDataMap.put(uuid, data);
    }

    public void clearAllData(){
        editPdfKeyMap.clear();
        editPdfDataMap.clear();
    }

    public void deleteData(int page, String uuid){
        List<String> uuids = editPdfKeyMap.get(page);
        if (uuids != null){
            uuids.remove(uuid);
        }
        editPdfDataMap.remove(uuid);
    }

    public void selectEditData(EditPdfData editPdfData){
        if (editPdfData instanceof EditTextData){
            selectEditTextRectFs = editPdfData.rectFDataList;
        }else if (editPdfData instanceof EditGraphData){
            this.selectGraphData = (EditGraphData) editPdfData;
        }
    }

    public void cleanSelectEditTextRectFs(){
        selectEditTextRectFs = null;
    }

    public void zoom(float zoom){

    }

}

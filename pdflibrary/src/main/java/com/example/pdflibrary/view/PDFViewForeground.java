package com.example.pdflibrary.view;

import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pdflibrary.edit.PdfAnnotationDrawData;
import com.example.pdflibrary.edit.PdfEditGraph;
import com.example.pdflibrary.edit.dealinterface.AnnotationActionInterface;
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

    private String TAG = "PDFViewForeground";
    private PdfAnnotationDrawData annotationDrawData;

    private EditPdfData selectPdfData;

    public Map<Integer, List<String>> editPdfKeyMap = new TreeMap<>();

    public Map<String, EditPdfData> editPdfDataMap = new HashMap<>();

    private Paint textPaint;
    private Paint selectPaint;
    private Paint graphPaint;

    private PDFView pdfView;

    public PDFViewForeground(PDFView pdfView) {
        textPaint = new Paint();
        graphPaint = new Paint();
        this.pdfView = pdfView;
        selectPaint = new Paint();
        selectPaint.setStyle(Paint.Style.STROKE);
        selectPaint.setStrokeWidth(2);
        selectPaint.setPathEffect(new DashPathEffect(new float[]{9, 5}, 0));
    }

    private float zoom = 0;

    public void onDraw(Canvas canvas) {
        zoom = pdfView.getZoom();

        drawCurrentDraw(canvas);

        drawSelectData(canvas);

        int currentPage = pdfView.getCurrentPage();

        drawPage(currentPage, canvas);
        drawPage(currentPage - 1, canvas);
        drawPage(currentPage + 1, canvas);
    }

    private void drawCurrentDraw(Canvas canvas){
        if (annotationDrawData != null && annotationDrawData.rectFDataList != null){
            if (annotationDrawData.isText){
                textPaint.setColor(pdfView.getEditColor().getColor());
                for (RectFData rectFData : annotationDrawData.rectFDataList) {
                    if (!rectFData.needInit()){
                        canvas.drawRect(rectFData.getCurrentZoomRectF(zoom), textPaint);
                    }
                }
            }else {
                graphPaint.setColor(pdfView.getEditColor().getColor());
                for (RectFData rectFData : annotationDrawData.rectFDataList) {
                    if (!rectFData.needInit()){
                        if (annotationDrawData.isRect){
                            canvas.drawRect(rectFData.getCurrentZoomRectF(zoom), graphPaint);
                        }else {
                            canvas.drawOval(rectFData.getCurrentZoomRectF(zoom), graphPaint);
                        }
                    }
                }
            }
        }
    }

    private void drawSelectData(Canvas canvas){
        if (selectPdfData != null && selectPdfData.rectFDataList != null){
            selectPaint.setColor(selectPdfData.color.getColor());
            for (RectFData rectFData : selectPdfData.rectFDataList) {
                if (!rectFData.needInit()){
                    canvas.drawRect(rectFData.getCurrentZoomRectF(zoom), selectPaint);
                }
            }
        }
    }

    private void drawRectFData(int page, RectFData rectFData, Canvas canvas, Paint paint, boolean isOval) {
        if (rectFData.needInit()) {
            Size originalSize = pdfView.pdfFile.getOriginalPageSize(page);
            SizeF sizeF = pdfView.pdfFile.getPageSize(page);
            float left = rectFData.left / originalSize.getWidth() * sizeF.getWidth();
            float right = rectFData.right / originalSize.getWidth() * sizeF.getWidth();
            float top = (1 - rectFData.top / originalSize.getHeight()) * sizeF.getHeight();
            float bottom = (1 - rectFData.bottom / originalSize.getHeight()) * sizeF.getHeight();
            RectF rectF = new RectF(left, top, right, bottom);
            float mainOffset = pdfView.pdfFile.getPageOffset(page, 1);
            float secondOffset = pdfView.pdfFile.getSecondaryPageOffset(page, 1);
            if (pdfView.isSwipeVertical()){
                rectF.offset(secondOffset, mainOffset);
            }else {
                rectF.offset(mainOffset, secondOffset);
            }
            rectFData.init(rectF, 1f);
        }
        if (isOval) {
            canvas.drawOval(rectFData.getCurrentZoomRectF(zoom), paint);
        } else {
            canvas.drawRect(rectFData.getCurrentZoomRectF(zoom), paint);
        }
    }

    private void drawPage(int page, Canvas canvas) {
        List<String> blockIds = editPdfKeyMap.get(page + 1);
        if (blockIds != null && blockIds.size() > 0) {
            for (String item : blockIds) {
                EditPdfData editPdfData = editPdfDataMap.get(item);
                if (editPdfData != null) {
                    if (editPdfData instanceof EditTextData) {
                        textPaint.setColor(editPdfData.color.getColor());
                        for (RectFData rectFData : editPdfData.rectFDataList) {
                            drawRectFData(page, rectFData, canvas, textPaint, false);
                        }
                    } else if (editPdfData instanceof EditGraphData) {
                        graphPaint.setColor(editPdfData.color.getColor());
                        if (((EditGraphData) editPdfData).editGraph == PdfEditGraph.Rectangle) {
                            for (RectFData rectFData : editPdfData.rectFDataList) {
                                drawRectFData(page, rectFData, canvas, graphPaint, false);
                            }
                        } else if (((EditGraphData) editPdfData).editGraph == PdfEditGraph.Circle){
                            for (RectFData rectFData : editPdfData.rectFDataList) {
                                drawRectFData(page, rectFData, canvas, graphPaint, true);
                            }
                        }
                    }
                }
            }
        }
    }

    public void clearDrawData() {
        this.annotationDrawData = null;
    }

    public void addDrawData(PdfAnnotationDrawData data) {
        this.annotationDrawData = data;
    }

    public void addEditPdfData(EditPdfData editPdfData, AnnotationActionInterface actionInterface) {
        pdfView.getBusinessInterface().createPdfAnnotation(editPdfData, actionInterface);
    }

    public void addOrUpdateData(int page, String uuid, EditPdfData data) {
        List<String> uuids = editPdfKeyMap.get(page);
        if (uuids == null) {
            uuids = new ArrayList<>();
            uuids.add(uuid);
            editPdfKeyMap.put(page, uuids);
        } else {
            if (!uuids.contains(uuid)) {
                uuids.add(uuid);
            }
        }
        editPdfDataMap.put(uuid, data);
    }

    public void clearAllData() {
        editPdfKeyMap.clear();
        editPdfDataMap.clear();
    }

    public void deleteData(int page, String uuid) {
        List<String> uuids = editPdfKeyMap.get(page);
        if (uuids != null) {
            uuids.remove(uuid);
        }
        editPdfDataMap.remove(uuid);
    }

    public void selectEditData(EditPdfData editPdfData) {
        this.selectPdfData = editPdfData;
    }

    public List<EditPdfData> getSortData() {
        List<EditPdfData> editPdfDataList = new LinkedList<>();
        for (List<String> value : editPdfKeyMap.values()) {
            if (value != null) {
                for (String key : value) {
                    EditPdfData editPdfData = editPdfDataMap.get(key);
                    if (editPdfData != null) {
                        editPdfDataList.add(editPdfData);
                    }
                }
            }
        }
        return editPdfDataList;
    }

    public void cleanSelectEditTextRectFs() {
        selectPdfData = null;
    }

}

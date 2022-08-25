package com.example.pdflibrary.edit;

import com.example.pdflibrary.edit.module.RectFData;

import java.util.List;

public class PdfAnnotationDrawData {
    public boolean isText;
    public boolean isRect;
    public List<RectFData> rectFDataList;

    public PdfAnnotationDrawData(boolean isText, boolean isRect, List<RectFData> rectFDataList) {
        this.isText = isText;
        this.isRect = isRect;
        this.rectFDataList = rectFDataList;
    }
}

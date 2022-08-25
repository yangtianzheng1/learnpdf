package com.example.pdflibrary.edit.module;

import com.example.pdflibrary.edit.PdfEditColor;

import java.util.List;

public abstract class EditPdfData {

    public String key;
    //pdf 展示的page 下标从1开始
    public int page;
    public PdfEditColor color = null;
    public List<RectFData> rectFDataList;
}

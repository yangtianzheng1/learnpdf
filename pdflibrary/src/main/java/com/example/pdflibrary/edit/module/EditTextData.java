package com.example.pdflibrary.edit.module;

import android.graphics.RectF;

import com.example.pdflibrary.edit.PdfEditColor;

import java.util.List;

public class EditTextData implements Comparable{
    public int page;
    public String pageText;
    public int startIndexText;
    public int endIndexText;
    public PdfEditColor color = PdfEditColor.YELLOW;
    public List<RectF> rectFList;

    @Override
    public int compareTo(Object o) {
        if ( o instanceof EditTextData){
            EditTextData other = ((EditTextData) o);
            if (this.startIndexText > other.startIndexText){
                return 1;
            }else if (this.startIndexText < other.startIndexText){
                return -1;
            }
        }
        return 0;
    }
}

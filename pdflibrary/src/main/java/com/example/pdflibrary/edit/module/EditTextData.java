package com.example.pdflibrary.edit.module;

import android.graphics.RectF;

import java.util.List;

public class EditTextData extends EditPdfData implements Comparable{
    public String pageText;
    public int startIndexText;
    public int endIndexText;

    public String text;

    public float scale;

    // 当前触控点在Pdf 页面上的坐标
    public float pageX;
    public float pageY;

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

package com.example.pdflibrary.edit;

import android.graphics.RectF;

import com.example.pdflibrary.edit.module.EditTextData;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class EditDataManager {

    public Map<Integer, List<EditTextData>> editTextDataMap = new TreeMap<>();

    public void putEditTextData(EditTextData editTextData){
        int page = editTextData.page;
        List<EditTextData> list = editTextDataMap.get(page);
        if (list == null){
            list = new LinkedList<>();
            editTextDataMap.put(page, list);
        }
        list.add(editTextData);
        Collections.sort(list);
    }

    public EditTextData findEditTextData(int page, int charIndex){
        List<EditTextData> editTextDataList = editTextDataMap.get(page);
        if (editTextDataList != null){
            for (EditTextData editTextData : editTextDataList){
                if (editTextData.startIndexText <= charIndex && editTextData.endIndexText >= charIndex){
                    return editTextData;
                }
            }
        }
        return null;
    }

    public List<EditTextData> findEditTextDataList(int page){
        return editTextDataMap.get(page);
    }
}

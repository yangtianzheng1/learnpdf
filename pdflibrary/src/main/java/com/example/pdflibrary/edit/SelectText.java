package com.example.pdflibrary.edit;

public class SelectText extends SelectPdf{

    // 当前页面对应的 文本指针
    public long textPtr;
    // 当前页面对应的文本
    public String pageText;
    // 点击文字在文本中位置
    public int charIdx;
}

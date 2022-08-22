package com.example.pdflibrary.edit;

public class SelectText {

    // 当前触控点在画布上的坐标
    public float canvasX;
    public float canvasY;

    public int page;

    // 当前页面左上点 相对于画布原点的便宜点 分为横屏和竖屏模式
    public float pageMainOffset;
    public float pageSecondaryOffset;

    // 当前页面对应的 文本指针
    public long textPtr;
    // 当前页面对应的文本
    public String pageText;
    // 点击文字在文本中位置
    public int charIdx;

    public float scale;

    // 当前触控点在Pdf 页面上的坐标
    public float pageX;
    public float pageY;
}

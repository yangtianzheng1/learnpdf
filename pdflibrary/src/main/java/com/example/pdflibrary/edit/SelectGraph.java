package com.example.pdflibrary.edit;

public class SelectGraph {

    // 当前触控点在画布上的坐标
    public float canvasX;
    public float canvasY;

    public float viewX;
    public float viewY;

    public float pageX;
    public float pageY;

    public int page;

    // 当前页面左上点 相对于画布原点的便宜点 分为横屏和竖屏模式
    public float pageMainOffset;
    public float pageSecondaryOffset;

    public PdfEditGraph pdfEditGraph;

}

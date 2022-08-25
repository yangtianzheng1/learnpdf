package com.example.pdflibrary.edit;

public abstract class SelectPdf {
    // 选点时页面缩放
    public float zoom;
    // 当前触控点在画布上的坐标
    public float canvasX;
    public float canvasY;

    //pdf 数据接口上的page 下标从 0 开始
    public int page;

    // 当前页面左上点 相对于画布原点的便宜点 分为横屏和竖屏模式
    public float pageMainOffset;
    public float pageSecondaryOffset;
    public boolean isVertical = true;

    // 当前触控点在Pdf 页面上的坐标
    public float pageX;
    public float pageY;
}

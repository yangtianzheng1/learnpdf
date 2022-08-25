package com.example.pdflibrary.edit.module;

import android.graphics.RectF;

public class RectFData {

    public RectFData(){
        this(null, 0);
    }

    public RectFData(RectF rectF, float zoom) {
        this.rectF = rectF;
        this.zoom = zoom;
        cacheRectF = rectF;
        cacheZoom = zoom;
    }

    public boolean needInit(){
        return rectF == null;
    }

    public void init(RectF rectF, float zoom){
        this.rectF = rectF;
        this.zoom = zoom;
        cacheRectF = rectF;
        cacheZoom = zoom;
    }

    // scale 不同 绘图区域会有相应的变化
    private RectF rectF;
    private float zoom;

    private RectF cacheRectF;
    private float cacheZoom;

    public RectF getCurrentZoomRectF(float zoom){
        if (Math.abs(cacheZoom - zoom) < 0.0001){
            return cacheRectF;
        }else {
            float scale = zoom/this.zoom;
            cacheZoom = zoom;
            cacheRectF = new RectF(rectF.left * scale, rectF.top * scale,
                    rectF.right*scale, rectF.bottom * scale);
            return cacheRectF;
        }
    }

    // page 上的坐标点
    public float left;
    public float top;
    public float right;
    public float bottom;
}

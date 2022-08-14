package com.example.pdflibrary.util;

import com.example.pdflibrary.PdfFile;

public class ViewCanvasPageCoordinateUtil {

    public static ResultCoordinate viewCoordinateToCanvas(float viewX, float viewY, float canvasXOffset, float canvasYOffset){
        ResultCoordinate resultCoordinate = new ResultCoordinate();
        resultCoordinate.x = (double) (viewX - canvasXOffset);
        resultCoordinate.y = (double) (viewY - canvasYOffset);
        return resultCoordinate;
    }

    public static int getPageIndexByCanvasXY(long canvasX, long canvasY, float zoom, boolean isVertical, PdfFile pdfFile){
        if (pdfFile == null){
            return -1;
        }
        return pdfFile.getPageAtOffset(isVertical ? canvasY : canvasX, zoom);
    }

//    public static ResultCoordinate getScalePageCoordinateByCanvasXY(long canvasX, long canvasY){
//
//    }

}

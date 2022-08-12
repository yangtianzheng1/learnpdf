package com.example.pdflibrary.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class CustomImageView extends AppCompatImageView {

    private Paint mPaint = new Paint();


    public CustomImageView(@NonNull Context context) {
        super(context);
    }

    public CustomImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (rectF != null){
            canvas.drawRect(rectF, mPaint);
        }
    }

    private  RectF rectF;

    public void setRect(RectF rect){
        rectF = rect;
        mPaint.setColor(Color.BLUE);       //设置画笔颜色
        mPaint.setStyle(Paint.Style.FILL);
        postInvalidate();
    }
}

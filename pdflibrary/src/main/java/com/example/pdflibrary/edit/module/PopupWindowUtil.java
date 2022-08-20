package com.example.pdflibrary.edit.module;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;

import com.example.pdflibrary.edit.SelectText;
import com.example.pdflibrary.edit.dealinterface.EditTextInterface;
import com.example.pdflibrary.edit.dealinterface.EditTextInterfaceWidget;
import com.example.pdflibrary.edit.dealinterface.SelectColorInterfaceWidget;
import com.example.pdflibrary.edit.dealinterface.SelectGraphInterfaceWidget;
import com.example.pdflibrary.util.LogUtils;

public class PopupWindowUtil {

    public static PopupWindow showEditTextPopupWindow(EditTextInterfaceWidget contentView, View parentView, float viewX, float viewY, EditTextData editTextData){
        if (contentView != null){
            View childView = (View) contentView;
            int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            childView.measure(width, height);

            int childWidth = childView.getMeasuredWidth();
            int childHeight = childView.getMeasuredHeight();

            PopupWindow popupWindow = new PopupWindow(childView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setTouchable(true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setElevation(0.7f);
            if (childHeight + viewY > parentView.getHeight()){
                popupWindow.showAsDropDown(parentView, (int) viewX, (int) (viewY - parentView.getHeight() - childHeight));
            }else {
                popupWindow.showAsDropDown(parentView, (int) viewX, (int) (viewY - parentView.getHeight()));
            }
            return popupWindow;
        }
        return null;
    }

    public static PopupWindow showSelectColorPopupWindow(SelectColorInterfaceWidget contentView, View parentView, float viewX, float viewY, EditTextData editTextData){
        if (contentView != null){
            View childView = (View) contentView;
            int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            childView.measure(width, height);

            int childWidth = childView.getMeasuredWidth();
            int childHeight = childView.getMeasuredHeight();

            PopupWindow popupWindow = new PopupWindow(childView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setTouchable(true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setElevation(0.7f);
            if (childHeight + viewY > parentView.getHeight()){
                popupWindow.showAsDropDown(parentView, (int) viewX, (int) (viewY - parentView.getHeight() - childHeight));
            }else {
                popupWindow.showAsDropDown(parentView, (int) viewX, (int) (viewY - parentView.getHeight()));
            }
            return popupWindow;
        }
        return null;
    }

    public static PopupWindow showSelectGraphPopupWindow(SelectGraphInterfaceWidget contentView, View parentView, float viewX, float viewY){
        if (contentView != null){
            View childView = (View) contentView;
            int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            childView.measure(width, height);

            int childWidth = childView.getMeasuredWidth();
            int childHeight = childView.getMeasuredHeight();

            PopupWindow popupWindow = new PopupWindow(childView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setTouchable(true);
            popupWindow.setOutsideTouchable(true);
            popupWindow.setElevation(0.7f);
            if (childHeight + viewY > parentView.getHeight()){
                popupWindow.showAsDropDown(parentView, (int) viewX, (int) (viewY - parentView.getHeight() - childHeight));
            }else {
                popupWindow.showAsDropDown(parentView, (int) viewX, (int) (viewY - parentView.getHeight()));
            }
            return popupWindow;
        }
        return null;
    }


}

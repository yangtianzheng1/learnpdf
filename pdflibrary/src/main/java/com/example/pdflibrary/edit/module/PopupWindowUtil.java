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
import com.example.pdflibrary.util.DPTOPXUtil;
import com.example.pdflibrary.util.LogUtils;

public class PopupWindowUtil {

    public static PopupWindow showEditTextPopupWindow(EditTextInterfaceWidget contentView, View parentView, float viewX, float viewY, EditTextData editTextData, PopupWindow.OnDismissListener dismissListener){
        if (contentView != null){
            return showWindow((View) contentView, parentView, viewX, viewY, false, false, dismissListener);
        }
        return null;
    }

    public static PopupWindow showSelectColorPopupWindow(SelectColorInterfaceWidget contentView, View parentView, float viewX, float viewY, EditTextData editTextData,
                                                         boolean needCenter, boolean ignoreY, PopupWindow.OnDismissListener dismissListener){
        if (contentView != null){
            return showWindow((View) contentView, parentView, viewX, viewY, needCenter, ignoreY, dismissListener);
        }
        return null;
    }

    public static PopupWindow showSelectGraphPopupWindow(SelectGraphInterfaceWidget contentView, View parentView, float viewX, float viewY, PopupWindow.OnDismissListener dismissListener){
        if (contentView != null){
            return showWindow((View) contentView, parentView, viewX, viewY, true, true, dismissListener);
        }
        return null;
    }

    private static PopupWindow showWindow(View childView, View anchor, float x, float y, boolean needCenter, boolean ignoreY, PopupWindow.OnDismissListener dismissListener){
        int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        childView.measure(width, height);

        int childWidth = childView.getMeasuredWidth();
        int childHeight = childView.getMeasuredHeight();

        PopupWindow popupWindow = new PopupWindow(childView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindow.setTouchable(true);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(0.7f);
        if (dismissListener != null){
            popupWindow.setOnDismissListener(dismissListener);
        }
        if (needCenter){
            x = - anchor.getX()/2 - width/2;
        }
        if (ignoreY){
            y = DPTOPXUtil.getPX(anchor.getContext(), 13);
        }else {
            if (childHeight + y > anchor.getHeight()){
                y = y - anchor.getHeight() - childHeight;
            }else {
                y = y - anchor.getHeight();
            }
        }
        popupWindow.showAsDropDown(anchor, (int) x, (int) y);
        return popupWindow;
    }


}

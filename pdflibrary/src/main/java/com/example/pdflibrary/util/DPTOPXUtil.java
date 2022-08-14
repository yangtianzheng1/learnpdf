package com.example.pdflibrary.util;

import android.content.Context;
import android.util.TypedValue;

public class DPTOPXUtil {
    public static int getDP(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

}

package com.example.pdflibrary.util;

import android.util.Log;

public class LogUtils {

    private static final boolean OPEN_LOG = true;

    private LogUtils(){
        throw new RuntimeException("not init LogUtils");
    }

    public static void logD(String tag, String content){
        if (OPEN_LOG){
            Log.d(tag, content);
        }
    }

    public static void logW(String tag, String content){
        if (OPEN_LOG){
            Log.w(tag, content);
        }
    }

    public static void logE(String tag, String content){
        if (OPEN_LOG){
            Log.e(tag, content);
        }
    }
}

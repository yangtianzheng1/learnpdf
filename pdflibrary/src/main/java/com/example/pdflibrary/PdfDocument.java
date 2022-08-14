package com.example.pdflibrary;

import android.os.ParcelFileDescriptor;
import android.util.ArrayMap;

import java.util.Map;

public class PdfDocument {

    long mNativeDocPtr;
    ParcelFileDescriptor parcelFileDescriptor;

    protected final Map<Integer, Long> mNativePagesPtr = new ArrayMap<>();
    protected final Map<Integer, Long> mNativeTextPagesPtr = new ArrayMap<>();
    protected final Map<Integer, Long> mNativeSearchHandlePtr = new ArrayMap<>();
    private ParcelFileDescriptor mFileDescriptor;

    PdfDocument() {
    }

    public boolean hasPage(int index) {
        return this.mNativePagesPtr.containsKey(Integer.valueOf(index));
    }
}

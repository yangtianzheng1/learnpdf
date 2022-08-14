package com.example.pdflibrary.source;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import com.example.pdflibrary.PdfDocument;
import com.example.pdflibrary.PdfiumCore;
import com.example.pdflibrary.util.FileUtils;

import java.io.File;
import java.io.IOException;

public class AssetSource implements DocumentSource {

    private final String assetName;

    public AssetSource(String assetName) {
        this.assetName = assetName;
    }

    @Override
    public PdfDocument createDocument(Context context, PdfiumCore core, String password) throws IOException {
        File f = FileUtils.fileFromAsset(context, assetName);
        ParcelFileDescriptor pfd = ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
        return core.newDocument(pfd, password);
    }
}

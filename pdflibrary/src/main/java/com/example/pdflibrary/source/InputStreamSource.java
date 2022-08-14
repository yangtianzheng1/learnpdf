package com.example.pdflibrary.source;

import android.content.Context;

import com.example.pdflibrary.PdfDocument;
import com.example.pdflibrary.PdfiumCore;
import com.example.pdflibrary.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;

public class InputStreamSource implements DocumentSource {

    private InputStream inputStream;

    public InputStreamSource(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public PdfDocument createDocument(Context context, PdfiumCore core, String password) throws IOException {
        return core.newDocument(FileUtils.toByteArray(inputStream), password);
    }
}
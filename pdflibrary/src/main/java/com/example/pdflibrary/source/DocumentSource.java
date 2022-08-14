package com.example.pdflibrary.source;

import android.content.Context;

import com.example.pdflibrary.PdfDocument;
import com.example.pdflibrary.PdfiumCore;

import java.io.IOException;

public interface DocumentSource {
    PdfDocument createDocument(Context context, PdfiumCore core, String password) throws IOException;
}

package com.example.pdflibrary.edit.dealinterface;

import com.example.pdflibrary.edit.PdfEditColor;
import com.example.pdflibrary.edit.module.EditPdfData;

public interface BusinessInterface {

    EditTextInterfaceWidget getEditTextWidget();

    SelectColorInterfaceWidget getSelectColorWidget(PdfEditColor color);

    SelectGraphInterfaceWidget getSelectGraphWidget();

    void createPdfAnnotation(EditPdfData data);

    void updatePdfAnnotation(EditPdfData data);

    void deletePdfAnnotation(EditPdfData data);
}

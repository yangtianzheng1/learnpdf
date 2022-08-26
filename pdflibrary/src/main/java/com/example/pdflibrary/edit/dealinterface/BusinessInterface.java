package com.example.pdflibrary.edit.dealinterface;

import com.example.pdflibrary.edit.PdfEditColor;
import com.example.pdflibrary.edit.module.EditPdfData;

public interface BusinessInterface {

    EditTextInterfaceWidget getEditTextWidget();

    SelectColorInterfaceWidget getSelectColorWidget(PdfEditColor color);

    SelectGraphInterfaceWidget getSelectGraphWidget();

    void createPdfAnnotation(EditPdfData data, AnnotationActionInterface annotationActionInterface);

    void updatePdfAnnotation(EditPdfData data, AnnotationActionInterface annotationActionInterface);

    void deletePdfAnnotation(EditPdfData data, AnnotationActionInterface annotationActionInterface);

    // 引用
    void copyQuote(EditPdfData editPdfData);

    // 内容
    void copyLabel(EditPdfData editPdfData);

    // 链接
    void copyLink(EditPdfData editPdfData);
}

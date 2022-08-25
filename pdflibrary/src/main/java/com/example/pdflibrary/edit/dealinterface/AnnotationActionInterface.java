package com.example.pdflibrary.edit.dealinterface;

public interface AnnotationActionInterface {

    // status -1 失败  0 创建成功 1 更新成功 2 删除成功
    void createUpdateDeleteCallBack(int status);
}

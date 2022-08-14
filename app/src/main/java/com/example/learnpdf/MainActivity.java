package com.example.learnpdf;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.learnpdf.databinding.ActivityMainBinding;
import com.example.pdflibrary.PdfDocument;
import com.example.pdflibrary.PdfiumCore;
import com.example.pdflibrary.element.Meta;
import com.example.pdflibrary.listener.OnLoadCompleteListener;
import com.example.pdflibrary.listener.OnPageChangeListener;
import com.example.pdflibrary.listener.OnPageErrorListener;
import com.example.pdflibrary.util.LogUtils;
import com.example.pdflibrary.util.Size;
import com.example.pdflibrary.view.DefaultScrollHandle;
import com.example.pdflibrary.view.PDFView;

import java.io.File;

public class MainActivity extends AppCompatActivity implements OnPageChangeListener, OnLoadCompleteListener, OnPageErrorListener {

    private ActivityMainBinding binding;
    private PDFView pdfView;

    private String TAG = "MainActivity";

    private String filePath = "/storage/emulated/0/阿里巴巴Java开发手册终极版v1.3.0.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        pdfView = binding.pdfView;
        binding.button1.setOnClickListener(view -> {
            pickFile();
        });
        requestPermission();
        loadFile(filePath);
    }

    private void loadFile(String path){
        if (TextUtils.isEmpty(path)){
            return;
        }
        filePath = path;
        try {
            File file = new File(filePath);
            if (file.exists() && file.length() > 0){
                pdfView.fromFile(file)
                        .defaultPage(0)
                        .onPageChange(this)
                        .swipeHorizontal(false)
                        .enableAnnotationRendering(true)
                        .onLoad(this)
                        .scrollHandle(new DefaultScrollHandle(this))
                        .spacing(10) // in dp
                        .onPageError(this)
                        .fitEachPage(true)
                        .load();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        LogUtils.logD(TAG, " onPageChanged  page " + page + " pageCount " + pageCount);
    }

    @Override
    public void loadComplete(int nbPages) {
        LogUtils.logD(TAG, " loadComplete  nbPages " + nbPages);
    }

    @Override
    public void onPageError(int page, Throwable t) {
        LogUtils.logD(TAG, " onPageError  page " + page + " t " + t.getCause());
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Please grante write storage permission", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Please grante write storage permission", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }
    }

    void pickFile() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                READ_EXTERNAL_STORAGE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{READ_EXTERNAL_STORAGE},
                    101
            );

            return;
        }

        launchPicker();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            Uri uri = data.getData();
            String filePath = uri.getPath();
            LogUtils.logD(TAG, "filePath " + filePath);
            loadFile(filePath);
        }
    }

    void launchPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        try {
            startActivityForResult(intent, 100);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    private int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    private int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }
}
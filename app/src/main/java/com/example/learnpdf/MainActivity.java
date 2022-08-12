package com.example.learnpdf;

import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.os.ParcelFileDescriptor.MODE_READ_ONLY;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;

import com.example.pdflibrary.Meta;
import com.example.pdflibrary.PdfiumSDK;
import com.example.pdflibrary.util.BreakIteratorHelper;
import com.example.pdflibrary.util.Size;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.learnpdf.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PdfiumSDK pdfiumSDK;

    private BreakIteratorHelper breakIteratorHelper;

    private String TAG = "yang";

    private String filePath = "/storage/emulated/0/阿里巴巴Java开发手册终极版v1.3.0.pdf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pdfiumSDK = new PdfiumSDK();
        breakIteratorHelper = new BreakIteratorHelper();

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.button1.setOnClickListener(view -> {
            pickFile();
        });
        requestPermission();
        loadFile(filePath);
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
            Log.d(TAG, "filePath " + filePath);
            loadFile(filePath);
        }
    }

    private void loadFile(String path){
        if (TextUtils.isEmpty(path)){
            return;
        }
        filePath = path;
        try {
            File file = new File(filePath);
            if (file.exists() && file.length() > 0){
                ParcelFileDescriptor fileDescriptor = null;
                fileDescriptor = ParcelFileDescriptor.open(new File(filePath), MODE_READ_ONLY);
                pdfiumSDK.newDocument(fileDescriptor);
                Meta meta = pdfiumSDK.getDocumentMeta();
                Log.d(TAG,  "meta" + meta.toString());

                int page = 1;

                long pagePtr = pdfiumSDK.openPage(page);
                long textPtr = pdfiumSDK.nativeLoadTextPage(pdfiumSDK.getNativeDocPtr(), page);
                Size size = pdfiumSDK.getPageSize(page);
                Log.d(TAG, "Page size: " + size.toString());

                int width = (int) (size.getWidth() * 1.5);
                int height = (int) (size.getHeight() * 1.5);

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                pdfiumSDK.renderPageBitmap(bitmap, page, 0 ,  0 , width, height, true);

                long lnkPtr = pdfiumSDK.nativeGetLinkAtCoord(pagePtr, width, height, 481 * 1.5, 710 * 1.5);
                if(lnkPtr!=0) {
                    String uri = pdfiumSDK.nativeGetLinkURI(pdfiumSDK.getNativeDocPtr(), lnkPtr);
                    Log.d(TAG, " uri " + uri);
                    RectF rect = pdfiumSDK.nativeGetLinkRect(lnkPtr);
                    Log.d(TAG, " rect " + rect.toString());
                }

                long textId = pdfiumSDK.prepareTextInfo(page);
                String text = pdfiumSDK.nativeGetText(textId);
                breakIteratorHelper.setText(text);

                binding.ivShow.setImageBitmap(bitmap);
                binding.ivShow.setOnTouchListener((view, motionEvent) -> {
                    int charIdx = pdfiumSDK.nativeGetCharIndexAtCoord(pagePtr, width, height, textPtr, motionEvent.getX(),
                            motionEvent.getY(), 20, 20);
                    if (charIdx >= 0 ){
                        int ed= breakIteratorHelper.following(charIdx);
                        int st= breakIteratorHelper.previous();
                        try {
                            String temp = text.substring(st, ed);
                            Log.d(TAG, " select text " + temp);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        int rectCount = pdfiumSDK.nativeCountRects(textPtr, charIdx, 4);
                        if (rectCount > 0){
                            RectF[] rects = new RectF[rectCount];
                            for (int i = 0; i < rectCount; i++){
                                RectF rI = new RectF();
                                pdfiumSDK.nativeGetRect(pagePtr,0, 0, width, height, textPtr, rI, i);
                                rects[i] = new RectF(rI.left - 20, rI.top - 20, rI.right + 20, rI.bottom + 20);
                                Log.d(TAG, " rect " + rects[i]);
                                binding.ivShow.setRect(rects[i]);
                            }
                        }
                    }
                    return false;
                });

            }
        } catch (Exception e) {
            e.printStackTrace();
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
package com.example.qrcodereader;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.camera2.interop.Camera2Interop;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.camera2.CaptureRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Range;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private QrCodeReader qrCodeReader;
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        qrCodeReader = new QrCodeReader(onSuccess);
        textView = findViewById(R.id.text_view);

        previewView = findViewById(R.id.camera_preview);
        startCamera();
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider processCameraProvider = cameraProviderFuture.get();
                bindPreview(processCameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(ProcessCameraProvider processCameraProvider) {
        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        ImageAnalysis imageAnalysis = getImageAnalysis();
        imageAnalysis.setAnalyzer(AsyncTask.SERIAL_EXECUTOR, analyzer);
        Camera camera = processCameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageAnalysis);
    }

    private ImageAnalysis getImageAnalysis() {
        ImageAnalysis.Builder builder = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9);

        return builder.build();
    }

    private final ImageAnalysis.Analyzer analyzer = image -> {
        qrCodeReader.scan(image);
    };

    private final QrCodeReader.OnSuccess onSuccess = data -> {
        if (data.size() == 0) {
            return;
        }
        MainActivity.this.runOnUiThread(() -> {
            textView.setText("");
            for (String string: data) {
                textView.append(string + " ");
            }
        });
    };

    public static Intent newIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }
}
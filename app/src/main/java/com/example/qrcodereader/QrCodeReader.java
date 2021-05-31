package com.example.qrcodereader;

import android.annotation.SuppressLint;
import android.graphics.Point;
import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.List;

public class QrCodeReader {

    private BarcodeScanner scanner;
    private OnSuccess onSuccess;

    public interface OnSuccess {
        void onScanSuccess(List<String> data);
    }

    public QrCodeReader(OnSuccess onSuccess) {
        scanner = BarcodeScanning.getClient(getOptions());
        this.onSuccess = onSuccess;
    }

    private BarcodeScannerOptions getOptions() {
        return new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_AZTEC)
                        .build();
    }

    @SuppressLint("UnsafeOptInUsageError")
    public void scan(ImageProxy imageProxy) {
        Image image = imageProxy.getImage();

        if (image != null) {
            InputImage inputImage = InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());
            Task<List<Barcode>> result = scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        List<String> data = new ArrayList<>();
                        for (Barcode barcode: barcodes) {
                            Rect bounds = barcode.getBoundingBox();
                            Point[] corners = barcode.getCornerPoints();

                            String rawValue = barcode.getRawValue();
                            data.add(rawValue);

                        }
                        onSuccess.onScanSuccess(data);
                    })
                    .addOnFailureListener(e -> {

                    }).addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        }

    }
}

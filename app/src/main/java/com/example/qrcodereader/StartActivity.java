package com.example.qrcodereader;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.List;

public class StartActivity extends AppCompatActivity {

    private static final String TAG = "StartActivity";
    private static final String[] PERMISSIONS = {
            Manifest.permission.CAMERA,
    };
    private static final int REQUEST_CODE_PERMISSIONS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate() called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_activity);

        if (!hasPermissions()) {
            askPermissions();
        } else {
            handlePermissionsGranted();
        }
    }

    protected void handlePermissionsDenied(List<String> permissions) {
        Log.d(TAG, "handlePermissionsDenied() called");
        finish();
    }

    protected void handlePermissionsGranted() {
        Log.d(TAG, "handlePermissionsGranted() called");
        openMainActivity();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d(TAG, "onRequestPermissionResult() called");

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            List<String> deniedPermissions = new ArrayList<>();
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    deniedPermissions.add(permissions[i]);
                }
            }
            if (deniedPermissions.size() == 0) {
                handlePermissionsGranted();
            } else {
                handlePermissionsDenied(deniedPermissions);
            }
        }
    }

    private boolean hasPermissions() {
        Log.d(TAG, "hasPermissions() called");
        for (String permission: PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void askPermissions() {
        Log.d(TAG, "askPermissions() called");
        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_CODE_PERMISSIONS);
    }

    private void openMainActivity() {
        Log.d(TAG, "openMainActivity() called");
        startActivity(MainActivity.newIntent(this));
        finish();
    }

}
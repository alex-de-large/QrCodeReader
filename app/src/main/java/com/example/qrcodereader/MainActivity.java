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
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CaptureRequest;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private SensorEventListener onRecieveAccListener;
    private SensorEventListener onRecieveGyroListener;
    private SensorEventListener onRecieveMagnetometrListener;
    private Sensor mOrientationTest;
    private Sensor mAcceleration;
    private Sensor mGyroscope;
    private Sensor mMagnetometr;
    private SensorManager mSensorManager;
    private ImageView pointer;
    private float mCurrentDegree = 0f;
    private int test_case = 0;
    private MadgwickAHRS mMadgwickAHRS = new MadgwickAHRS(0.01f, 0.00001f);
    private float ax, ay, az, gx, gy, gz, mx, my, mz;



    private QrCodeReader qrCodeReader;
    private TextView textView;
    private ArrayList<Float> testCoord = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        qrCodeReader = new QrCodeReader(onSuccess);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        pointer = findViewById(R.id.image_view);
        testCoord.add(0.0f);
        testCoord.add(30.0f);
        testCoord.add(60.0f);
        testCoord.add(90.0f);
        testCoord.add(120.0f);
        testCoord.add(150.0f);
        testCoord.add(180.0f);
        testCoord.add(210.0f);
        textView = findViewById(R.id.text_view);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mOrientationTest = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mMagnetometr = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        onRecieveAccListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    ax = event.values[0];
                    ay = event.values[1];
                    az = event.values[2];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        onRecieveGyroListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                    gx = event.values[0];
                    gy = event.values[1];
                    gz = event.values[2];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        onRecieveMagnetometrListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    mx = event.values[0]; //Плоскость XY
                    my = event.values[1]; //Плоскость XZ
                    mz = event.values[2]; //Плоскость ZY
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };
        Timer mTimer;
        GuiTimer guiTimer;
        mTimer = new Timer();
        guiTimer = new GuiTimer();
        mTimer.schedule(guiTimer, 1000, 10); // 100Hz
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

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        ImageAnalysis imageAnalysis = getImageAnalysis();
        imageAnalysis.setAnalyzer(AsyncTask.SERIAL_EXECUTOR, analyzer);
        Camera camera = processCameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, imageAnalysis);
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
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(sensorEventListener, mOrientationTest, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(onRecieveMagnetometrListener,
                mMagnetometr,
                SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(onRecieveAccListener,
                mAcceleration,
                SensorManager.SENSOR_DELAY_FASTEST); // 200Hz, 5 ms delay
        mSensorManager.registerListener(onRecieveGyroListener,
                mGyroscope,
                SensorManager.SENSOR_DELAY_FASTEST);

    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(onRecieveAccListener);
        mSensorManager.unregisterListener(onRecieveGyroListener);
        mSensorManager.unregisterListener(onRecieveMagnetometrListener);
        mSensorManager.unregisterListener(sensorEventListener, mOrientationTest);
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
//            if (mLastAccelerometerSet && mLastMagnetometerSet) {
//                SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
//                SensorManager.getOrientation(mR, mOrientation);
//                float azimuthInRadians = mOrientation[0];
//                float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
//
//                textView.setText(azimuthInDegress + "");
//                RotateAnimation ra = new RotateAnimation(
//                        mCurrentDegree,
//                        -azimuthInDegress,
//                        Animation.RELATIVE_TO_SELF, 0.5f,
//                        Animation.RELATIVE_TO_SELF,
//                        0.5f);
//
//                ra.setDuration(250);
//
//                ra.setFillAfter(true);
//
//                pointer.startAnimation(ra);
//                mCurrentDegree = -azimuthInDegress;
//            }
//                float[] eulerAngles = mMadgwickAHRS.getEulerAngles();
//
//                Log.d("OSI", String.format("%f",testCoord.get(test_case) - eulerAngles[2]));
//                textView.setText(-(testCoord.get(test_case) - eulerAngles[2]) + "");
//                RotateAnimation ra = new RotateAnimation(
//                        mCurrentDegree,
//                        -(testCoord.get(test_case) - eulerAngles[2]),
//                        Animation.RELATIVE_TO_SELF, 0.5f,
//                        Animation.RELATIVE_TO_SELF,
//                        0.5f);
//                ra.setDuration(250);
//                ra.setFillAfter(true);
//                pointer.startAnimation(ra);
//                mCurrentDegree = -(testCoord.get(test_case) - eulerAngles[2]);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    public void changeDegree(View view) {
        test_case++;
    }

    class GuiTimer extends TimerTask {

        @Override
        public void run() {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mMadgwickAHRS.update(gx, gy, gz, ax, ay, az);

                    float[] eulerAngles = mMadgwickAHRS.getEulerAngles();

                    Log.d("OSI", String.format("%f",testCoord.get(test_case) - eulerAngles[0]));
                    textView.setText(-(testCoord.get(test_case) - eulerAngles[0]) + "");
                    RotateAnimation ra = new RotateAnimation(
                            mCurrentDegree,
                            eulerAngles[0],
                            Animation.RELATIVE_TO_SELF, 0.5f,
                            Animation.RELATIVE_TO_SELF,
                            0.5f);
                    ra.setDuration(250);
                    ra.setFillAfter(true);
                    pointer.startAnimation(ra);
                    mCurrentDegree = eulerAngles[0];

                }
            });
        }
    }
}
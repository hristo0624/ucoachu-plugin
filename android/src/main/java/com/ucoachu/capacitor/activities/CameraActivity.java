package com.ucoachu.capacitor.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.ucoachu.capacitor.adapters.OnboardingAdapter;
import com.ucoachu.capacitor.components.AutoFitTextureView;
import com.ucoachu.capacitor.components.DonutProgress;
import com.ucoachu.capacitor.R;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


public class CameraActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener,
        TextureView.SurfaceTextureListener {
    static final String TAG = "ksc.log";

    private Boolean isLandScapeMode = true;
    private static final int CAMERA_TYPE = 0;  // 0: back, 1: front

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
    };

    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();


    private static final int REQUEST_VIDEO_PERMISSIONS = 1;

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    private AutoFitTextureView mTextureView;
    private CameraDevice mCameraDevice;
    private CameraCaptureSession mPreviewSession;

    private Size mPreviewSize;
    private Size mVideoSize;
    private MediaRecorder mMediaRecorder;
    private Integer mSensorOrientation;
    private String mNextVideoAbsolutePath;
    private CaptureRequest.Builder mPreviewBuilder;

    private boolean mIsRecordingVideo = false;

    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    private CameraDevice.StateCallback mStateCallback;

    private ImageView mImageButtonInfo;
    private ImageView mImageButtonClose;
    private ImageView mImageButtonRecord;
    private TextView mTextButtonSelectedTime;

    private RelativeLayout mDlgTimeSelection;
    private RelativeLayout mLayoutButtonTime25;
    private RelativeLayout mLayoutButtonTime20;
    private RelativeLayout mLayoutButtonTime15;
    private RelativeLayout mLayoutButtonTime10;
    private RelativeLayout mLayoutButtonTime03;

    private RelativeLayout mLayoutButtonSelectionTime25;
    private RelativeLayout mLayoutButtonSelectionTime20;
    private RelativeLayout mLayoutButtonSelectionTime15;
    private RelativeLayout mLayoutButtonSelectionTime10;
    private RelativeLayout mLayoutButtonSelectionTime03;

    private int mSelectedTime = 25;

    private SensorManager mSensorManager;
    private Sensor mMagnetometer;
    private Sensor mAccelerometer;
    private float[] mGravityValues = new float[3];
    private float[] mAccelerationValues = new float[3];
    private float[] mRotationMatrix = new float[9];
    private float mLastDirectionInDegrees = 0f;
    private RelativeLayout mLayoutMagBar;

    private CountDownTimer mCountDownTimer;
    private DonutProgress mCountDownProgress;

    private ViewPager mViewPager;
    private OnboardingAdapter mOnboardingAdapter;
    private RelativeLayout mLayoutOnboarding;
    private ImageView mImageButtonOnboardingClose;
    private TabLayout mTabLayoutDot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        isLandScapeMode = getIntent().getStringExtra("mode").contentEquals("landscape");
        Log.e(TAG, isLandScapeMode ? "landscape mode" : "portrait mode");

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mLayoutMagBar = (RelativeLayout) findViewById(R.id.layout_mag_bar);

        mImageButtonInfo = (ImageView) findViewById(R.id.image_button_info);
        mImageButtonInfo.setOnClickListener(this);
        mImageButtonClose = (ImageView) findViewById(R.id.image_button_close);
        mImageButtonClose.setOnClickListener(this);
        mTextButtonSelectedTime = (TextView) findViewById(R.id.text_button_selected_time);
        mTextButtonSelectedTime.setOnClickListener(this);

        mDlgTimeSelection = (RelativeLayout) findViewById(R.id.layout_dlg_time_selection);
        mDlgTimeSelection.setVisibility(View.GONE);

        mLayoutButtonTime25 = (RelativeLayout) findViewById(R.id.layout_button_time_25);
        mLayoutButtonTime25.setOnClickListener(this);
        mLayoutButtonTime20 = (RelativeLayout) findViewById(R.id.layout_button_time_20);
        mLayoutButtonTime20.setOnClickListener(this);
        mLayoutButtonTime15 = (RelativeLayout) findViewById(R.id.layout_button_time_15);
        mLayoutButtonTime15.setOnClickListener(this);
        mLayoutButtonTime10 = (RelativeLayout) findViewById(R.id.layout_button_time_10);
        mLayoutButtonTime10.setOnClickListener(this);
        mLayoutButtonTime03 = (RelativeLayout) findViewById(R.id.layout_button_time_03);
        mLayoutButtonTime03.setOnClickListener(this);

        mLayoutButtonSelectionTime25 = (RelativeLayout) findViewById(R.id.layout_selection_time_25);
        mLayoutButtonSelectionTime20 = (RelativeLayout) findViewById(R.id.layout_selection_time_20);
        mLayoutButtonSelectionTime15 = (RelativeLayout) findViewById(R.id.layout_selection_time_15);
        mLayoutButtonSelectionTime10 = (RelativeLayout) findViewById(R.id.layout_selection_time_10);
        mLayoutButtonSelectionTime03 = (RelativeLayout) findViewById(R.id.layout_selection_time_03);

        mCountDownProgress = (DonutProgress) findViewById(R.id.countdown_progress);
        mImageButtonRecord = (ImageView) findViewById(R.id.image_button_record);
        mImageButtonRecord.setOnClickListener(this);
        mTextureView = (AutoFitTextureView) findViewById(R.id.texture);

        timerInitialize(mSelectedTime);

        mStateCallback = new CameraDevice.StateCallback() {

            @Override
            public void onOpened(CameraDevice cameraDevice) {
                mCameraDevice = cameraDevice;
                startPreview();
                mCameraOpenCloseLock.release();
                if (null != mTextureView) {
                    configureTransform(mTextureView.getWidth(), mTextureView.getHeight());
                }
            }

            @Override
            public void onDisconnected(CameraDevice cameraDevice) {
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;
            }

            @Override
            public void onError(CameraDevice cameraDevice, int error) {
                mCameraOpenCloseLock.release();
                cameraDevice.close();
                mCameraDevice = null;

                finish();
            }
        };

        mViewPager = (ViewPager) findViewById(R.id.onboarding_view_pager);
        mImageButtonOnboardingClose = (ImageView) findViewById(R.id.image_button_onboarding_close);
        mImageButtonOnboardingClose.setOnClickListener(this);
        mLayoutOnboarding = (RelativeLayout) findViewById(R.id.layout_onboarding);
        mTabLayoutDot = (TabLayout) findViewById(R.id.tab_layout_dot);

        mLayoutOnboarding.setVisibility(View.GONE);

        mOnboardingAdapter = new OnboardingAdapter(this);
        mViewPager.setAdapter(mOnboardingAdapter);
        mTabLayoutDot.setupWithViewPager(mViewPager);
    }

    public void onClick(View v) {
        if (v.getId() == R.id.image_button_info) {
            mLayoutOnboarding.setVisibility(View.VISIBLE);
        } else if (v.getId() == R.id.image_button_onboarding_close) {
            mLayoutOnboarding.setVisibility(View.GONE);
        } else if (v.getId() == R.id.image_button_close) {
            onBackPressed();
        } else if (v.getId() == R.id.image_button_record) {
            if (!mIsRecordingVideo) {
                mCountDownProgress.setVisibility(View.VISIBLE);
                mCountDownTimer.start();

                startRecordingVideo();
            } else {
                mCountDownProgress.setVisibility(View.GONE);

                mCountDownTimer.cancel();
                mCountDownProgress.setProgress(mSelectedTime);
                mCountDownProgress.setMax(mSelectedTime);

                stopRecordingVideo();
            }
        } else if (v.getId() == R.id.text_button_selected_time) {
            if (mIsRecordingVideo) {
                Log.e(TAG, "video is in recording now...");
                return;
            }

            mDlgTimeSelection.setVisibility(View.VISIBLE);
        } else if (v.getId() == R.id.layout_button_time_25) {
            mDlgTimeSelection.setVisibility(View.GONE);

            mLayoutButtonSelectionTime25.setVisibility(View.VISIBLE);
            mLayoutButtonSelectionTime20.setVisibility(View.GONE);
            mLayoutButtonSelectionTime15.setVisibility(View.GONE);
            mLayoutButtonSelectionTime10.setVisibility(View.GONE);
            mLayoutButtonSelectionTime03.setVisibility(View.GONE);

            mTextButtonSelectedTime.setText("25 sec");
            mSelectedTime = 25;
            timerInitialize(mSelectedTime);
        } else if (v.getId() == R.id.layout_button_time_20) {
            mDlgTimeSelection.setVisibility(View.GONE);

            mLayoutButtonSelectionTime25.setVisibility(View.GONE);
            mLayoutButtonSelectionTime20.setVisibility(View.VISIBLE);
            mLayoutButtonSelectionTime15.setVisibility(View.GONE);
            mLayoutButtonSelectionTime10.setVisibility(View.GONE);
            mLayoutButtonSelectionTime03.setVisibility(View.GONE);

            mTextButtonSelectedTime.setText("20 sec");
            mSelectedTime = 20;
            timerInitialize(mSelectedTime);
        } else if (v.getId() == R.id.layout_button_time_15) {
            mDlgTimeSelection.setVisibility(View.GONE);

            mLayoutButtonSelectionTime25.setVisibility(View.GONE);
            mLayoutButtonSelectionTime20.setVisibility(View.GONE);
            mLayoutButtonSelectionTime15.setVisibility(View.VISIBLE);
            mLayoutButtonSelectionTime10.setVisibility(View.GONE);
            mLayoutButtonSelectionTime03.setVisibility(View.GONE);

            mTextButtonSelectedTime.setText("15 sec");
            mSelectedTime = 15;
            timerInitialize(mSelectedTime);
        } else if (v.getId() == R.id.layout_button_time_10) {
            mDlgTimeSelection.setVisibility(View.GONE);

            mLayoutButtonSelectionTime25.setVisibility(View.GONE);
            mLayoutButtonSelectionTime20.setVisibility(View.GONE);
            mLayoutButtonSelectionTime15.setVisibility(View.GONE);
            mLayoutButtonSelectionTime10.setVisibility(View.VISIBLE);
            mLayoutButtonSelectionTime03.setVisibility(View.GONE);

            mTextButtonSelectedTime.setText("10 sec");
            mSelectedTime = 10;
            timerInitialize(mSelectedTime);
        } else if (v.getId() == R.id.layout_button_time_03) {
            mDlgTimeSelection.setVisibility(View.GONE);

            mLayoutButtonSelectionTime25.setVisibility(View.GONE);
            mLayoutButtonSelectionTime20.setVisibility(View.GONE);
            mLayoutButtonSelectionTime15.setVisibility(View.GONE);
            mLayoutButtonSelectionTime10.setVisibility(View.GONE);
            mLayoutButtonSelectionTime03.setVisibility(View.VISIBLE);

            mTextButtonSelectedTime.setText("3 sec");
            mSelectedTime = 3;
            timerInitialize(mSelectedTime);
        }
    }

    public void timerInitialize(int selectedTime) {
        mCountDownProgress.setVisibility(View.GONE);

        mCountDownProgress.setProgress(mSelectedTime);
        mCountDownProgress.setMax(mSelectedTime);

        mCountDownTimer = new CountDownTimer(selectedTime * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                mCountDownProgress.setProgress((int) millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                mCountDownProgress.setVisibility(View.GONE);
                mCountDownProgress.setProgress(0);

                goNextActivity();
            }
        };
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }

        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<Size>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w &&
                    option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }

        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void requestVideoPermissions() {
        ActivityCompat.requestPermissions(this, VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_VIDEO_PERMISSIONS:
                if (grantResults.length == VIDEO_PERMISSIONS.length) {
                    for (int result : grantResults) {
                        if (result != PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, "Permission not allowed");
                            break;
                        }
                    }
                } else {
                    Log.d(TAG, "Permission not allowed");
                }
                break;
        }
    }

    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestVideoPermissions();
            return;
        }

        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);

        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);

            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            mVideoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            mPreviewSize = new Size(width, height);

            /*int orientation = getResources().getConfiguration().orientation;
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            } else {
                mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
            }*/

            configureTransform(width, height);
            mMediaRecorder = new MediaRecorder();
            //manager.openCamera(cameraId, mStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(this, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            finish();
        } catch (NullPointerException e) {
            Toast.makeText(this, "This device doesn't support Camera2 API.", Toast.LENGTH_SHORT).show();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }


    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            if (null != mMediaRecorder) {
                mMediaRecorder.release();
                mMediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    private void startPreview() {
        if (null == mCameraDevice || !mTextureView.isAvailable() || null == mPreviewSize) {
            return;
        }

        try {
            setUpMediaRecorder();
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;

            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            List<Surface> surfaces = new ArrayList<>();

            // Set up Surface for the camera preview
            Surface previewSurface = new Surface(texture);
            surfaces.add(previewSurface);
            mPreviewBuilder.addTarget(previewSurface);

            // Set up Surface for the MediaRecorder
            Surface recorderSurface = mMediaRecorder.getSurface();
            surfaces.add(recorderSurface);
            mPreviewBuilder.addTarget(recorderSurface);

            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            mPreviewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Log.d(TAG, "onConfigureFailed.");
                        }
                    }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == mCameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private void setUpMediaRecorder() throws IOException {
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

        if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
            mNextVideoAbsolutePath = getVideoFilePath(this);
        }

        mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
        mMediaRecorder.setVideoEncodingBitRate(10000000);
        mMediaRecorder.setVideoFrameRate(30);
        mMediaRecorder.setVideoSize(mVideoSize.getWidth(), mVideoSize.getHeight());
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        switch (mSensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mMediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mMediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mMediaRecorder.prepare();
    }

    private String getVideoFilePath(Context context) {
        final File dir = context.getExternalFilesDir(null);
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }

    private void startRecordingVideo() {
        try {
            mImageButtonRecord.setImageResource(R.drawable.icon_record_stop);
            mIsRecordingVideo = true;

            // Start recording
            mMediaRecorder.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }


    private void stopRecordingVideo() {
        mIsRecordingVideo = false;
        mImageButtonRecord.setImageResource(R.drawable.icon_record_start);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Write whatever to want to do after delay specified (1 sec)
                // Stop recording
                if (mMediaRecorder.getMaxAmplitude() > 0) {
                    mMediaRecorder.stop();
                    mMediaRecorder.reset();

                    Log.d(TAG, "Video saved: " + mNextVideoAbsolutePath);

                    mNextVideoAbsolutePath = null;
                    startPreview();
                }
            }
        }, 1000);
    }

    private void goNextActivity() {
        //stopRecordingVideo();

        finish();

        Intent myIntent = new Intent(CameraActivity.this, PlayerActivity.class);
        myIntent.putExtra("videoUrl", mNextVideoAbsolutePath);
        startActivity(myIntent);

        mNextVideoAbsolutePath = null;
    }

    @Override
    protected void onResume() {
        super.onResume();

        mSensorManager.registerListener(this, mMagnetometer,
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);

        startBackgroundThread();
        if (mTextureView.isAvailable()) {
            openCamera(mTextureView.getWidth(), mTextureView.getHeight());
        } else {
            mTextureView.setSurfaceTextureListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        closeCamera();
        stopBackgroundThread();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mSensorManager.unregisterListener(this);
        mCountDownTimer.cancel();
    }

    private void calculateCompassDirection(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mAccelerationValues = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGravityValues = event.values.clone();
                break;
        }
        boolean success = SensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerationValues, mGravityValues);
        if (success) {
            float[] orientationValues = new float[3];
            SensorManager.getOrientation(mRotationMatrix, orientationValues);
            float roll = (float) Math.toDegrees(-orientationValues[2]);
            RotateAnimation rotateAnimation = new RotateAnimation(mLastDirectionInDegrees, roll,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(5000);
            rotateAnimation.setInterpolator(new LinearInterpolator());
            mLayoutMagBar.startAnimation(rotateAnimation);
            mLastDirectionInDegrees = roll;
        }
    }

    //callback functions
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                          int width, int height) {
        openCamera(width, height);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                            int width, int height) {
        configureTransform(width, height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        calculateCompassDirection(sensorEvent);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    class CompareSizesByArea implements Comparator<Size> {
        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}
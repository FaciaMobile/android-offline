package com.facia.faciasdk.Singleton;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.SensorManager;

import androidx.fragment.app.FragmentManager;

import com.facia.faciasdk.Activity.ActivityListener;
import com.facia.faciasdk.ApiModels.ApiClient;
import com.facia.faciasdk.ApiModels.ApiInterface;
import com.facia.faciasdk.Camera.CameraXHelpers.CameraListeners;
import com.facia.faciasdk.databinding.FragmentCameraBinding;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class SingletonData {
    private static SingletonData data;
    private JsonArray qlFrameList = new JsonArray();
    private FragmentCameraBinding fragmentCameraBinding;
    private Activity activity;
    private Boolean isSdkStarted = false;
    private Context context;
    private long current, previous, currentTimeSmallOval, previousTimeSmallOval, blinkCurrent, blinkPrevious;
    private Application application;
    private String referenceId = "", detectionState = "", appInfo = "";
//            qlVideoPath = "";
    private int blinkCount = 0, resultApiCount = 0, increasedFrameCount = 0, quickLivenessFrameCount = 0;
    private final ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
    private Boolean isVideoRecording = false, isOvalSizeIncreased = false, isFaceFinalized = false, isSizeIncreasing = false,
            isDetectionTimerOn = false, isProcessingStarted = false, quickLiveness = true, isFaceDetectedFirstTime = false, isBlinkDetectionTimerOn = false,
            isCameraBackPressed = false, isQuickRequestInProcess = false, isSmallOvalSteadyTimerOn = false, isBlinkTimerOn = false, isQlReqInProcess = false;
    private CameraListeners cameraListeners;
    private Boolean isCameraProcessing = false;
    private FragmentManager fragmentManager;
    private JsonObject cardJson = new JsonObject();
    private float maxOpenDist = 0.0f, currentLightValue = 0.0f;
    private ActivityListener activityListener;

    private SingletonData() {
    }

    /**
     * making single instance of Singleton (SetAndGetData) class
     *
     * @return  returning instance
     */
    public static SingletonData getInstance() {
        if (data == null) {
            data = new SingletonData();
        }
        return data;
    }

    public FragmentCameraBinding getFragmentCameraBinding() {
        return fragmentCameraBinding;
    }

    public void setFragmentCameraBinding(FragmentCameraBinding fragmentCameraBinding) {
        this.fragmentCameraBinding = fragmentCameraBinding;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public int getBlinkCount() {
        return blinkCount;
    }

    public void setBlinkCount(int blinkCount) {
        this.blinkCount = blinkCount;
    }

    public CameraListeners getCameraListeners() {
        return cameraListeners;
    }

    public void setCameraListeners(CameraListeners cameraListeners) {
        this.cameraListeners = cameraListeners;
    }

    public Boolean isOvalSizeIncreased() {
        return isOvalSizeIncreased;
    }

    public void setOvalSizeIncreased(Boolean ovalSizeIncreased) {
        isOvalSizeIncreased = ovalSizeIncreased;
    }

    public Boolean isVideoRecording() {
        return isVideoRecording;
    }

    public void setVideoRecording(Boolean videoRecording) {
        isVideoRecording = videoRecording;
    }

    public Boolean isFaceFinalized() {
        return isFaceFinalized;
    }

    public void setFaceFinalized(Boolean faceFinalized) {
        isFaceFinalized = faceFinalized;
    }

    public ApiInterface getApiInterface() {
        return apiInterface;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Boolean isProcessingStarted() {
        return isProcessingStarted;
    }

    public void setProcessingStarted(Boolean processingStarted) {
        isProcessingStarted = processingStarted;
    }

    public int getResultApiCount() {
        return resultApiCount;
    }

    public void setResultApiCount(int resultApiCount) {
        this.resultApiCount = resultApiCount;
    }

    public int getIncreasedFrameCount() {
        return increasedFrameCount;
    }

    public void setIncreasedFrameCount(int increasedFrameCount) {
        this.increasedFrameCount = increasedFrameCount;
    }

    public Boolean isCameraProcessing() {
        return isCameraProcessing;
    }

    public void setCameraProcessing(Boolean cameraProcessing) {
        isCameraProcessing = cameraProcessing;
    }

    public int getQuickLivenessFrameCount() {
        return quickLivenessFrameCount;
    }

    public void setQuickLivenessFrameCount(int quickLivenessFrameCount) {
        this.quickLivenessFrameCount = quickLivenessFrameCount;
    }

    public Boolean isQuickLiveness() {
        return quickLiveness;
    }

    public void setQuickLiveness(Boolean quickLiveness) {
        this.quickLiveness = quickLiveness;
    }

    public Boolean isCameraBackPressed() {
        return isCameraBackPressed;
    }

    public void setCameraBackPressed(Boolean cameraBackPressed) {
        isCameraBackPressed = cameraBackPressed;
    }

    public long getCurrent() {
        return current;
    }

    public void setCurrent(long current) {
        this.current = current;
    }

    public long getPrevious() {
        return previous;
    }

    public void setPrevious(long previous) {
        this.previous = previous;
    }

    public Boolean isDetectionTimerOn() {
        return isDetectionTimerOn;
    }

    public void setDetectionTimerOn(Boolean detectionTimerOn) {
        isDetectionTimerOn = detectionTimerOn;
    }

    public Boolean isQuickRequestInProcess() {
        return isQuickRequestInProcess;
    }

    public void setQuickRequestInProcess(Boolean quickRequestInProcess) {
        isQuickRequestInProcess = quickRequestInProcess;
    }

    public Boolean isSdkStarted() {
        return isSdkStarted;
    }

    public void setSdkStarted(Boolean sdkStarted) {
        isSdkStarted = sdkStarted;
    }

    public String getDetectionState() {
        return detectionState;
    }

    public void setDetectionState(String detectionState) {
        this.detectionState = detectionState;
    }

    public Boolean isFaceDetectedFirstTime() {
        return isFaceDetectedFirstTime;
    }

    public void setFaceDetectedFirstTime(Boolean faceDetectedFirstTime) {
        isFaceDetectedFirstTime = faceDetectedFirstTime;
    }

    public Boolean isSizeIncreasing() {
        return isSizeIncreasing;
    }

    public void setSizeIncreasing(Boolean sizeIncreasing) {
        isSizeIncreasing = sizeIncreasing;
    }

    public FragmentManager getFragmentManager() {
        return fragmentManager;
    }

    public void setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
    }

    public JsonObject getCardJson() {
        return cardJson;
    }

    public void setCardJson(JsonObject cardJson) {
        this.cardJson = cardJson;
    }

    public long getCurrentTimeSmallOval() {
        return currentTimeSmallOval;
    }

    public void setCurrentTimeSmallOval(long currentTimeSmallOval) {
        this.currentTimeSmallOval = currentTimeSmallOval;
    }

    public long getPreviousTimeSmallOval() {
        return previousTimeSmallOval;
    }

    public void setPreviousTimeSmallOval(long previousTimeSmallOval) {
        this.previousTimeSmallOval = previousTimeSmallOval;
    }

    public Boolean isSmallOvalSteadyTimerOn() {
        return isSmallOvalSteadyTimerOn;
    }

    public void setSmallOvalSteadyTimerOn(Boolean smallOvalSteadyTimerOn) {
        isSmallOvalSteadyTimerOn = smallOvalSteadyTimerOn;
    }

    public long getBlinkCurrent() {
        return blinkCurrent;
    }

    public void setBlinkCurrent(long blinkCurrent) {
        this.blinkCurrent = blinkCurrent;
    }

    public long getBlinkPrevious() {
        return blinkPrevious;
    }

    public void setBlinkPrevious(long blinkPrevious) {
        this.blinkPrevious = blinkPrevious;
    }

    public Boolean isBlinkTimerOn() {
        return isBlinkTimerOn;
    }

    public void setBlinkTimerOn(Boolean blinkTimerOn) {
        isBlinkTimerOn = blinkTimerOn;
    }

    public float getMaxOpenDist() {
        return maxOpenDist;
    }

    public void setMaxOpenDist(float maxOpenDist) {
        this.maxOpenDist = maxOpenDist;
    }

    public float getCurrentLightValue() {
        return currentLightValue;
    }

    public void setCurrentLightValue(float currentLightValue) {
        this.currentLightValue = currentLightValue;
    }

    public ActivityListener getActivityListener() {
        return activityListener;
    }

    public void setActivityListener(ActivityListener activityListener) {
        this.activityListener = activityListener;
    }

    public Boolean isBlinkDetectionTimerOn() {
        return isBlinkDetectionTimerOn;
    }

    public void setBlinkDetectionTimerOn(Boolean blinkDetectionTimerOn) {
        isBlinkDetectionTimerOn = blinkDetectionTimerOn;
    }

    public JsonArray getQlFrameList() {
        return qlFrameList;
    }

    public void setQlFrameList(JsonArray qlFrameList) {
        this.qlFrameList = qlFrameList;
    }

    public String getAppInfo() {
        return appInfo;
    }

    public void setAppInfo(String appInfo) {
        this.appInfo = appInfo;
    }

    public Boolean isQlReqInProcess() {
        return isQlReqInProcess;
    }

    public void setQlReqInProcess(Boolean qlReqInProcess) {
        isQlReqInProcess = qlReqInProcess;
    }

    //    public String getQlVideoPath() {
//        return qlVideoPath;
//    }
//
//    public void setQlVideoPath(String qlVideoPath) {
//        this.qlVideoPath = qlVideoPath;
//    }
}

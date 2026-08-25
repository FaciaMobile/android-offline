package com.facia.faciasdk.Camera.CameraXHelpers;

import android.graphics.Bitmap;
import android.webkit.WebView;

public interface CameraListeners {
    void frameProcessed();

    void startVideoRecording();

    void stopVideoRecording();

    void processQuickLiveness(Bitmap bitmap);

    void cameraTimeOut();

    void fileUploaded();

    void setAnimationViewsAndText(WebView animationView, String animationName, String text,
                                  String resultStatus, double similarityScore, Boolean gotResult, String declinedReason , String errorStr);

    void setUi(int cameraLayoutVisibility, int faceLayoutVisibility, int matchIdLayoutVisibility, int docLivenessLayoutVisibility, int quickLivenessInstVisibility,
               int qlResultInstructionScrVisibility, int resultLayoutVisibility, int imgPreviewLayoutVisibility);

    void convertBitmapToBase64(Bitmap bitmap);
}

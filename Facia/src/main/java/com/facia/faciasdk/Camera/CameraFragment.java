package com.facia.faciasdk.Camera;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import com.facia.faciasdk.Utils.FaciaLogger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.content.ContextCompat;
import androidx.core.util.Consumer;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.facia.faciasdk.Activity.Helpers.Enums.DocumentType;
import com.facia.faciasdk.Activity.Helpers.Enums.FaceDetectionThreshold;
import com.facia.faciasdk.Activity.Helpers.Enums.ServiceType;
import com.facia.faciasdk.Activity.Helpers.IntentHelper;
import com.facia.faciasdk.Activity.Helpers.Enums.FaceLivenessType;
import com.facia.faciasdk.Activity.Helpers.Enums.OvalSize;
import com.facia.faciasdk.Activity.Helpers.RequestModel;
import com.facia.faciasdk.Utils.AppColors;
import com.facia.faciasdk.Camera.ApiHelpers.LivenessApiHelper;
import com.facia.faciasdk.Camera.ApiHelpers.SimilarityApiHelper;
import com.facia.faciasdk.Camera.CameraXHelpers.CameraListeners;
import com.facia.faciasdk.Camera.CameraXHelpers.CameraXViewModel;
//import com.facia.faciasdk.Camera.CameraXHelpers.QualityPreferences;
import com.facia.faciasdk.Camera.CardDetection.CardDetectionTflite;
import com.facia.faciasdk.Camera.FaceDetection.FaceDetectionHelper;
import com.facia.faciasdk.DocumentType.DocTypeFragment;
import com.facia.faciasdk.DocumentType.KycAddressTypeFragment;
import com.facia.faciasdk.DocumentType.KycDocTypeFragment;
import com.facia.faciasdk.DocumentType.KycDocTwoTypeFragment;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Constants.ThresholdConstants;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.facia.faciasdk.databinding.FragmentCameraBinding;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class CameraFragment extends Fragment implements CameraListeners, View.OnClickListener {
    //    private QualityPreferences qualityPreferences;
    private final Handler docBtnHandler = new Handler();
    @Nullable
    protected ProcessCameraProvider cameraProvider;
    @Nullable
    protected String storagePath = "";
    protected Boolean isQuickLiveness = true, defaultAndNotRooted = true, isCamInstShown = false, isBgChanging = false,
            backPressed = false, isCardDetectionInProcess = false, isCamStopped = false, isDocCaptured = false, matchIdLiveness;
    protected int fileUploadCount = 0;
    protected Dialog dialog;
    protected HashMap<String, String> requestResponseObj;
    protected int recursiveQlCounter = 0, frameCounter = 0;
    // document auto-capture stability accumulator (see ThresholdConstants.CARD_STABILITY_*)
    protected int stableFrameCounter = 0;
    protected long firstStableFrameTime = 0L;
    protected String requestType = "", currentState = "";
    protected BottomSheetDialog bottomSheetDialog;
    protected View bottomSheetView;
    private BottomSheetDialog docGuidelinesDialog;
    @Nullable
    protected Preview previewUseCase;
    private long qlListCurrent, qlListPrevious;
    private FaceDetectionHelper faceDetectionHelper;
    //    private Bitmap detectedFaceFrame;
    private FragmentCameraBinding fragmentCameraBinding;
    private final Runnable docBtnRunnable = new Runnable() {
        @Override
        public void run() {
            if (docGuidelinesDialog != null && docGuidelinesDialog.isShowing()) {
                docBtnHandler.postDelayed(this, 500);
                return;
            }
            if (fragmentCameraBinding.imagePreviewParentLayout.getVisibility() != View.VISIBLE) {
                fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);
            }
        }
    };
    private VideoCapture<Recorder> videoCapture;
    private Recording currentRecording;
    private CameraSelector cameraSelector;
    private File videoFile;
    private LivenessApiHelper livenessApiHelper;
    private SimilarityApiHelper similarityApiHelper;
    private RequestModel requestModel;
    private AppColors appColors;
    private Camera cameraX;
    private HelperFunctions helperFunctions;
    private List<Bitmap> framesList = new ArrayList<>();
    private String serviceType;
    private String livenessType;
    private String clientReference;
    private Boolean docVerification;
    private boolean docVerificationEnabled;
    private boolean docTwoVerificationEnabled;
    private boolean addressVerificationEnabled;
    private static final String TAG = "FACIA_FLOW";

//    private Quality videoQuality;
//    private int imageQuality;

    /**
     * method to handle screen's back press click
     * resetting values
     * unbinding camera
     * calling previous screen
     */
    private final OnBackPressedCallback backPressCallBack = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            try {
                if (fragmentCameraBinding.cameraParentLayout.getVisibility() == View.VISIBLE) {
                    if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.DOCUMENT_LIVENESS &&
                            requestModel.getConfigObject().getBoolean("showDocumentType")) {
                        showDocTypeFragment();
                    } else if (fragmentCameraBinding.ovalTickAnimation.getVisibility() != View.VISIBLE &&
                            !fragmentCameraBinding.faceDetectInst.getText().equals(R.string.perfect)) {
                        helperFunctions.exitDialog();
                    }
                }
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "CameraFragment/handleOnBackPressed");
            }
        }
    };
    private CardDetectionTflite cardDetectionTflite;
    /**
     * CaptureEvent listener
     * on start recording and stop, will set a variable
     * on stop recording, will check if processing is completed
     * then it will unbind camera and will call next screen to upload & show result
     */
    private final Consumer<VideoRecordEvent> videoCallback = new Consumer<androidx.camera.video.VideoRecordEvent>() {
        @Override
        public void accept(androidx.camera.video.VideoRecordEvent videoRecordEvent) {
            try {
                if (videoRecordEvent instanceof VideoRecordEvent.Start) {
                    SingletonData.getInstance().setVideoRecording(true);
                    if (SingletonData.getInstance().isFaceFinalized()) {
                        Webhooks.testingValues("CameraFragment/videoCallback-Video STARTED, while face is detected.");
                    }
                } else if (videoRecordEvent instanceof VideoRecordEvent.Finalize) {
                    SingletonData.getInstance().setVideoRecording(false);
                    if (SingletonData.getInstance().isFaceFinalized()) {
                        SingletonData.getInstance().setFaceFinalized(false);
                        initRequests(storagePath, null);
                        try {
                            cameraProvider.unbindAll();
                        } catch (Exception e) {
                            Webhooks.exceptionReport(e, "CameraFragment/videoCallback-inner");
                        }
                    } else {
//                        if (currentState.equals("QL")) {
//                            SingletonData.getInstance().setQlVideoPath(storagePath);
//                        }else {
                        videoFile.delete();
//                        }
                    }
                }
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "CameraFragment/videoCallback");
            }
        }
    };

    @SuppressLint("RestrictedApi")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        fragmentCameraBinding = FragmentCameraBinding.inflate(inflater, container, false);
        initialization();
        return fragmentCameraBinding.getRoot();
    }

    /**
     * Method to initialize variables
     * and setting camera related values
     * invoking further method to bind camera use cases
     */
    @SuppressLint("RestrictedApi")
    private void initialization() {
        try {
            requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);

            try {
                JSONObject configObject = requestModel.getConfigObject();
                boolean docVerification = configObject.optBoolean("doc_verification", false);
                docVerificationEnabled = requestModel.getConfigObject().optBoolean("doc_verification", false);
                docTwoVerificationEnabled = requestModel.getConfigObject().optBoolean("doc_two_verification", false);
                addressVerificationEnabled = docVerificationEnabled && requestModel.getConfigObject().optBoolean("address_verification", false);
                String serviceTypeStr = configObject.optString("serviceType");
                ServiceType currentServiceType = ServiceType.valueOf(serviceTypeStr);
                boolean isMatchToPhotoId = currentServiceType == ServiceType.MATCH_TO_PHOTO_ID;
                if (docVerification && isMatchToPhotoId) {
                    // Check and force the setting if it's currently true or missing (defaults to true)
                    if (configObject.optBoolean("matchIdLiveness", true)) {
                        configObject.put("matchIdLiveness", false);
//                        Webhooks.testingValues("Config adjusted: doc_verification enabled with MATCH_TO_PHOTO_ID forces matchIdLiveness to false.");
                    }
                }
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "CameraFragment/initialization-KYCConfigAdjustment");
            }

            // 2. Apply Colors
            appColors = new AppColors(requestModel.getConfigObject(), getContext());
            applyAppColors();
            try {
                boolean docVerification = requestModel.getConfigObject().optBoolean("doc_verification", false);
                boolean isMatchToPhotoId = ServiceType.valueOf(
                        requestModel.getConfigObject().getString("serviceType")
                ) == ServiceType.MATCH_TO_PHOTO_ID;

                if (isMatchToPhotoId && docVerification) {
                    fragmentCameraBinding.tryUploadPicTxt.setVisibility(View.VISIBLE);
                    fragmentCameraBinding.tryUploadPicTxt.setOnClickListener(this);
                } else {
                    fragmentCameraBinding.tryUploadPicTxt.setVisibility(View.GONE);
                }
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "CameraFragment/initialization-tryUploadPicTxt");
                if (fragmentCameraBinding.tryUploadPicTxt != null) {
                    fragmentCameraBinding.tryUploadPicTxt.setVisibility(View.GONE);
                }
            }

            // 3. Setup Singleton and Helpers
            SingletonData.getInstance().setDetectionTimerOn(false);
            dialog = new Dialog(SingletonData.getInstance().getContext());
            SingletonData.getInstance().setFragmentCameraBinding(fragmentCameraBinding);
            SingletonData.getInstance().setCameraListeners(this);
            SingletonData.getInstance().setQlReqInProcess(false);
            SingletonData.getInstance().setCameraBackPressed(false);
            initHelperClasses();
            // Only reset skip flags at the very start of a fresh flow.
            // When we navigate out (e.g., to KYC type screens) and come back, we must preserve skips.
            if (requestModel.getFaceImage() == null) {
                requestModel.setIdBackSkipped(false);
                requestModel.setDocTwoBackSkipped(false);
                requestModel.setAddressBackSkipped(false);
            }

            // 4. Determine Initial Flow
            if (requestModel.isSimilarity()) {
                requestType = "checkSimilarity";
                currentState = "similarityRequest";
                helperFunctions.handleCheckSimilarity(similarityApiHelper, false);
            } else {
                if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.MATCH_TO_PHOTO_ID) {
                    requestType = "photoIdMatch";
                    if (docVerificationEnabled && requestModel.getConfigObject().optBoolean("perform_full_kyc", false)
                            && requestModel.getFaceImage() != null && requestModel.getConfigObject().has("addressDocType")
                            && addressVerificationEnabled && requestModel.getAddressImage() == null) {
                        transitionToAddressFrontCapture();
                    } else if (docVerificationEnabled && requestModel.getConfigObject().optBoolean("perform_full_kyc", false) && requestModel.getFaceImage() != null) {
                        // If doc one front is already captured, resume the flow from where it left off
                        // (e.g. coming back from KycDocTwoTypeFragment — should not re-show doc one guidelines)
                        if (requestModel.getIdImage() != null) {
                            uploadCapturedImg();
                        } else {
                            matchIdByCapture();
                        }
                    } else {
                        helperFunctions.setQlUi(false);
                        initCamera(true);
                    }
                } else if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.DOCUMENT_LIVENESS) {
                    if (DocumentType.valueOf(requestModel.getConfigObject().getString("documentType")) == DocumentType.ID_CARD) {
                        matchIdByCapture();
                    } else {
                        currentState = "docDetection";
                        initCamera(false);
                        setUi(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE);
                    }
                } else {
                    helperFunctions.setLivenessUi();
                    initCamera(true);
                }
            }
            // 5. Final Setup
            helperFunctions.initClickListeners();
            requireActivity().getOnBackPressedDispatcher().addCallback(this.requireActivity(), backPressCallBack);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/initialization");
        }
    }

    /**
     * Applies configured colors to all relevant UI elements in the fragment.
     */
    private void applyAppColors() {
        try {
            fragmentCameraBinding.faceDetectInst.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.tryPassportTxt.setTextColor(appColors.getDarkTextColor());
            if (fragmentCameraBinding.tryUploadPicTxt != null) {
                fragmentCameraBinding.tryUploadPicTxt.setTextColor(appColors.getDarkTextColor());
            }
            fragmentCameraBinding.faceDocDetectInst.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.docLivenessInstTxt.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.quickLivenessInstTxt.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.retakeBtn.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.retakeBtn.setBackgroundTintList(ColorStateList.valueOf(appColors.getButtonBgColor()));
            fragmentCameraBinding.imgContinueBtn.setBackgroundTintList(ColorStateList.valueOf(appColors.getButtonBgColor()));
            fragmentCameraBinding.imgContinueBtn.setTextColor(appColors.getButtonTextColor());
            fragmentCameraBinding.resultTxt.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.resultContinueBtn.setBackgroundTintList(ColorStateList.valueOf(appColors.getButtonBgColor()));
            fragmentCameraBinding.resultContinueBtn.setTextColor(appColors.getButtonTextColor());
            fragmentCameraBinding.instHeadingTxt.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.instText1.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.instText2.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.instText3.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.instText4.setTextColor(appColors.getDarkTextColor());
            fragmentCameraBinding.instText5.setTextColor(appColors.getDarkTextColor());
//            fragmentCameraBinding.CardDetectInst.setTextColor(appColors.getDarkTextColor());
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/applyAppColors");
        }
    }

    protected void checkCamInst() {
        try {
            new Handler().postDelayed(() -> {
                try {
                    if (fragmentCameraBinding.cameraInstParentLayout.getVisibility() == View.GONE) {
                        Webhooks.testingValues("Inst issue found: " +
                                SingletonData.getInstance().isQuickRequestInProcess() + " : " +
                                SingletonData.getInstance().isCameraBackPressed() + " : " +
                                isCamInstShown + " : " + dialog.isShowing() + " : " +
                                (fragmentCameraBinding.previewView.getBitmap() == null));
                    }
                } catch (Exception e) {
                    Webhooks.exceptionReport(e, "CameraFragment/checkCamInst/Handler");
                }
            }, 1500);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/checkCamInst-inner");
        }
    }

    private void initHelperClasses() {
        try {
            if (requestModel.isSimilarity()) {
                similarityApiHelper = new SimilarityApiHelper(fragmentCameraBinding, requestModel.getToken(), appColors);
            } else {
                if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.MATCH_TO_PHOTO_ID) {
                    similarityApiHelper = new SimilarityApiHelper(fragmentCameraBinding, requestModel.getToken(), appColors);
                }
                livenessApiHelper = new LivenessApiHelper(fragmentCameraBinding, requestModel.getToken(), requestModel.getConfigObject().getBoolean("showDeclineReason"), appColors);
            }
            helperFunctions = new HelperFunctions(fragmentCameraBinding, this, requestModel, appColors);
            faceDetectionHelper = new FaceDetectionHelper(SingletonData.getInstance().getContext(),
                    requestModel.getConfigObject().getBoolean("dlEyesBlinkDetectionTimeout"),
                    FaceDetectionThreshold.valueOf(requestModel.getConfigObject().getString("faceDetectionThreshold")),
                    OvalSize.valueOf(requestModel.getConfigObject().getString("ovalSize")));
            helperFunctions.initialiseBottomSheet();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/initHelperClasses");
        }
    }

    private void showDocTypeFragment() {
        try {
            backPressed = true;
            helperFunctions.stopCamera();
            Fragment docTypeFragment = new DocTypeFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, docTypeFragment, docTypeFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/showDocTypeFragment");
        }
    }

    protected void initCamera(Boolean isLiveness) {
        try {
            cameraSelector = new CameraSelector.Builder().requireLensFacing(isLiveness ?
                    CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK).build();
            new ViewModelProvider(this, (ViewModelProvider.Factory)
                    ViewModelProvider.AndroidViewModelFactory.getInstance(SingletonData.getInstance().getApplication()))
                    .get(CameraXViewModel.class)
                    .getProcessCameraProvider()
                    .observe(getViewLifecycleOwner(),
                            provider -> {
                                cameraProvider = provider;
                                bindAllCameraUseCases(isLiveness);
                            });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/initCamera");
        }
    }

    /**
     * method to init binding camera use cases
     * will check camera provider, will unbind previous use cases
     * and will call further method to bind camera use cases
     */
    private void bindAllCameraUseCases(Boolean isLiveness) {
        try {
            if (cameraProvider != null) {
                cameraProvider.unbindAll();
                if (cameraProvider == null) {
                    return;
                }
                if (previewUseCase != null) {
                    cameraProvider.unbind(previewUseCase);
                }
                bindCameraUseCases(isLiveness);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/bindAllCameraUseCases");
        }
    }

    /**
     * method to bind camera use cases
     * will get camera provider instance
     * and will bind preview and video use case
     * invoking further method to set FaceMesh detection
     */
    private void bindCameraUseCases(Boolean isLiveness) {
        try {
            Preview.Builder builder = new Preview.Builder();
//            @SuppressLint("RestrictedApi") Preview.Builder builder = new Preview.Builder().setMaxResolution(new Size(1920,1080));
            previewUseCase = builder.build();
            previewUseCase.setSurfaceProvider(fragmentCameraBinding.previewView.getSurfaceProvider());
            Recorder recorder = new Recorder.Builder()
//                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.HIGHEST))).build();
//                    .setQualitySelector(QualitySelector.from(videoQuality, FallbackStrategy.higherQualityOrLowerThan(videoQuality))).build();
                    .setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD))).build();
            videoCapture = VideoCapture.withOutput(recorder);
            new ViewModelProvider(this, (ViewModelProvider.Factory)
                    ViewModelProvider.AndroidViewModelFactory.getInstance(getActivity().getApplication()))
                    .get(CameraXViewModel.class)
                    .getProcessCameraProvider()
                    .observe(
                            getViewLifecycleOwner(),
                            provider -> {
                                cameraProvider = provider;
                                try {
                                    cameraProvider.unbindAll();
                                    try {
                                        cameraX = cameraProvider.bindToLifecycle(getViewLifecycleOwner(), cameraSelector,
                                                previewUseCase, videoCapture);
                                        helperFunctions.tapToFocus(cameraX.getCameraControl());
                                    } catch (Exception e) {
                                        Webhooks.exceptionReport(e, "CameraFragment/bindCameraUseCases/ViewModelProvider-inner");
                                    }
                                } catch (Exception e) {
                                    Webhooks.exceptionReport(e, "CameraFragment/bindCameraUseCases/ViewModelProvider-inner");
                                }
                            });
            if (isLiveness) {
//                helperFunctions.setOvalBorder();
            } else if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.MATCH_TO_PHOTO_ID ||
                    ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.DOCUMENT_LIVENESS &&
                            DocumentType.valueOf(requestModel.getConfigObject().getString("documentType")) == DocumentType.ID_CARD) {
                new TestPreview().execute();//android 14
                SingletonData.getInstance().getActivity().runOnUiThread(() ->
                        new Handler().postDelayed(() -> detectCard(), 1000));
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/bindCameraUseCases");
        }
    }

    /**
     * callback to process next frame
     */
    @Override
    public void frameProcessed() {
        try {
//            helperFunctions.focusInCenter(cameraX.getCameraControl());
            if (!SingletonData.getInstance().isQuickRequestInProcess() && !SingletonData.getInstance().isCameraBackPressed()
                    && !isCamInstShown) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        if (!dialog.isShowing()) {
                            SingletonData.getInstance().setCameraProcessing(true);
                            Bitmap bitmap;
                            if (framesList.size() == 0) {
                                bitmap = fragmentCameraBinding.previewView.getBitmap();
                            } else {
                                bitmap = framesList.get(framesList.size() - 1);
                                framesList.remove(framesList.size() - 1);
                            }
                            frameCounter++;
                            if (bitmap != null && frameCounter > 1) {
                                try {
                                    if ((Color.alpha(bitmap.getPixel(0, 0)) != 0 || fragmentCameraBinding.previewView.getPreviewStreamState()
                                            .getValue().toString().equalsIgnoreCase("STREAMING")) &&
                                            fragmentCameraBinding.cameraInstParentLayout.getVisibility() == View.GONE) {
                                        fragmentCameraBinding.hidePreviewView.setVisibility(View.GONE);//android 14
                                        fragmentCameraBinding.cameraInstParentLayout.setVisibility(View.VISIBLE);
                                    }
                                } catch (Exception e) {
                                    try {
                                        Webhooks.exceptionReport(e, "Camera/frameProcessed-inner1");
                                        new Handler().postDelayed(() -> fragmentCameraBinding.cameraInstParentLayout.setVisibility(View.VISIBLE),
                                                500);
                                    } catch (Exception ex) {
                                        Webhooks.exceptionReport(e, "Camera/frameProcessed-inner2");
                                        fragmentCameraBinding.cameraInstParentLayout.setVisibility(View.VISIBLE);
                                    }
                                }

//                                if (SingletonData.getInstance().isQuickLiveness()) {
//                                    detectedFaceFrame = bitmap;
//                                }
                                detectFaceInFrame(bitmap);
                            } else {
                                frameProcessed();
                            }
                        }
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "CameraFragment/frameProcessed/Handler");
                        frameProcessed();
                    }
                });
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/frameProcessed");
            frameProcessed();
        }
    }

    private void detectFace(Bitmap frame) {
        try {
            InputImage image = InputImage.fromBitmap(frame, 0);
            FaceDetectorOptions faceDetectorOptions =
                    new FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                            .build();
            FaceDetector detector = FaceDetection.getClient(faceDetectorOptions);
            detector.process(image)
                    .addOnSuccessListener(
                            faces -> {
                                try {
                                    if (faces.size() > 0) {
                                        int index;
                                        if (faces.size() > 1) {
                                            index = 0;
                                            int height = faces.get(0).getBoundingBox().height();
                                            for (int i = 1; i < faces.size(); i++) {
                                                if (faces.get(i).getBoundingBox().height() > height) {
                                                    height = faces.get(i).getBoundingBox().height();
                                                    index = i;
                                                }
                                            }
                                        } else {
                                            index = 0;
                                        }
//                                        new DetectLight(frame, faces.get(index)).execute();
//                                        if (helperFunctions.isImageDark(frame, 50)) {
                                        if (requestModel.getConfigObject().getBoolean("dimLightDetection")) {
                                            if (SingletonData.getInstance().getCurrentLightValue() < 1.0f) {
                                                faceDetectionHelper.setFaceMeshResult(null, frame, true);
                                            } else {
                                                faceDetectionHelper.setFaceMeshResult(faces.get(index), frame, false);
                                            }
                                        } else {
                                            faceDetectionHelper.setFaceMeshResult(faces.get(index), frame, false);
                                        }
                                    } else {
                                        faceDetectionHelper.setFaceMeshResult(null, frame, false);
                                    }
                                } catch (Exception e) {
                                    Webhooks.exceptionReport(e, "CameraFragment/detectFaceInFrame/faceCallback");
                                }
                            })
                    .addOnFailureListener(
                            e -> {
                                Webhooks.testingValues("Face detection failure: " + e.getMessage());
                                faceDetectionHelper.setFaceMeshResult(null, frame, false);
                            });
            framesList.add(fragmentCameraBinding.previewView.getBitmap());
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/detectFace");
        }
    }

    /**
     * method to detect faces in a frame
     * handle QL & DL timeout
     * handling further flow
     *
     * @param frame single frame from camera stream
     */
    private void detectFaceInFrame(Bitmap frame) {
        try {
            //timer
            if (SingletonData.getInstance().isQuickLiveness() ? requestModel.getConfigObject().getBoolean("qlFaceDetectionTimeout") :
                    requestModel.getConfigObject().getBoolean("dlFaceDetectionTimeout")) {
                if (SingletonData.getInstance().getDetectionState().equals("smallOval")) {
                    SingletonData.getInstance().setCurrent(System.currentTimeMillis());
                    if (!SingletonData.getInstance().isDetectionTimerOn()) {
                        SingletonData.getInstance().setDetectionTimerOn(true);
                        SingletonData.getInstance().setPrevious(System.currentTimeMillis());
                        detectFace(frame);
                    } else if (SingletonData.getInstance().getCurrent() - SingletonData.getInstance().getPrevious() >=
                            TimeConstants.QL_AND_DL_TIMEOUT) {
                        SingletonData.getInstance().getCameraListeners().cameraTimeOut();
                    } else {
                        detectFace(frame);
                    }
                } else if (SingletonData.getInstance().getDetectionState().equals("bigOval")) {
                    SingletonData.getInstance().setCurrent(System.currentTimeMillis());
                    if (!SingletonData.getInstance().isDetectionTimerOn()) {
                        SingletonData.getInstance().setDetectionTimerOn(true);
                        SingletonData.getInstance().setPrevious(System.currentTimeMillis());
                        detectFace(frame);
                    } else if (SingletonData.getInstance().getCurrent() - SingletonData.getInstance().getPrevious() >=
                            TimeConstants.QL_AND_DL_TIMEOUT) {
                        SingletonData.getInstance().getCameraListeners().cameraTimeOut();
                    } else {
                        detectFace(frame);
                    }
                } else {
                    detectFace(frame);
                }
            } else {
                detectFace(frame);
            }
            //timer
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/detectFaceInFrame");
        }
    }

    /**
     * method will be called whenever activity will be resumed
     * if camera was working and activity was paused and after that activity resumed
     * it will restart frame processing
     */
    @Override
    public void onResume() {
        super.onResume();
//        rivtd
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
//                Settings.System.canWrite(SingletonData.getInstance().getActivity())) {
//            HelperFunctions.applyMaxBrightness();
//        }

        isCamStopped = false;
        if (SingletonData.getInstance().isCameraProcessing()) {
            frameProcessed();
        } else if (isCardDetectionInProcess || fragmentCameraBinding.docParentLayout.getVisibility() == View.VISIBLE) {
            detectCard();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        isCamStopped = true;
    }

    /**
     * callback to start video recording
     */
    @Override
    public void startVideoRecording() {
        startRecording();
    }

    /**
     * callback to stop video recording
     */
    @SuppressLint("RestrictedApi")
    @Override
    public void stopVideoRecording() {
        try {
            currentRecording.stop();
        } catch (Exception e) {
//            if (SingletonData.getInstance().isFaceFinalized()) {
//                Webhooks.exceptionReport(e, "CameraFragment/stopVideoRecording-catch");
//            }
        }
    }

    /**
     * method to start video recording
     * creating temp file in cache
     * saving video to that file's path
     */
    @SuppressLint({"MissingPermission", "RestrictedApi"})
    private void startRecording() {
        try {
            long timeStamp = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timeStamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");

            File directory = requireContext().getCacheDir();
            videoFile = null;
            try {
                videoFile = File.createTempFile(
                        "facia_recording",
                        ".mp4",
                        directory
                );
            } catch (IOException e) {
                FaciaLogger.e(e);
            }
            storagePath = videoFile.getPath();
            FileOutputOptions fileOutputOptions = new FileOutputOptions.Builder(videoFile).build();
            currentRecording = videoCapture.getOutput().prepareRecording(requireActivity(), fileOutputOptions).start(
//                    getExecutor(), videoCallback);
                    getExecutor(), videoCallback);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/startRecording");
        }
    }

    /**
     * method to return main executor
     *
     * @return main executor
     */
    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(SingletonData.getInstance().getContext());
    }

    @Override
    public void processQuickLiveness(Bitmap bitmap) {
//        stopVideoRecording();
        fragmentCameraBinding.showInstBtn.setEnabled(false);
        SingletonData.getInstance().setQlReqInProcess(true);
        SingletonData.getInstance().setQuickRequestInProcess(true);
        try {
            cameraProvider.unbindAll();

//            cameraProvider.unbind(previewUseCase);
//            cameraProvider = null;
//            fragmentCameraBinding.previewView.removeAllViews();
//            fragmentCameraBinding.previewView.setBackgroundColor(Color.TRANSPARENT);
////            fragmentCameraBinding.previewView.setAlpha(0);
//            previewUseCase.setSurfaceProvider(null);
//            previewUseCase = new Preview.Builder().build();
//            fragmentCameraBinding.previewView.destroyDrawingCache();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/processQuickLiveness-inner");
        }
        SingletonData.getInstance().getActivity().runOnUiThread(() -> initRequests("", bitmap));
    }

    @Override
    public void cameraTimeOut() {
        try {
            setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
            SingletonData.getInstance().setDetectionTimerOn(false);
            SingletonData.getInstance().setCameraBackPressed(true);
            cameraProvider.unbindAll();
            SingletonData.getInstance().setReferenceId("");
            setAnimationViewsAndText(fragmentCameraBinding.animationView, "facia_failure.svg",
                    SingletonData.getInstance().getActivity().getString(R.string.request_timed_out), "request.timeout", 0.0, true, "", "");
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/cameraTimeOut");
        }
    }

    /**
     * method to initialize instances & values
     * invoking further method to set animation views
     * handling click of continue button (exiting from sdk)
     */
//    private void initRequests(String filePath, Bitmap bitmap) {
//        try {
//            SingletonData.getInstance().getActivityListener().unregisterSensors();
//            isCardDetectionInProcess = false;
//            isCamStopped = false;
//            SingletonData.getInstance().setCameraProcessing(false);
//            SingletonData.getInstance().setVideoRecording(false);
//            helperFunctions.showOvalTickAnimation();
//            new Handler().postDelayed(() -> {
//                try {
//                    if (!backPressed) {
//                        try {
//                            fragmentCameraBinding.showInstBtn.setEnabled(true);
//                            isCamInstShown = false;
//                            bottomSheetDialog.cancel();
//
////                            fragmentCameraBinding.previewView.removeAllViews();//android 14
//                            fragmentCameraBinding.hidePreviewView.setVisibility(View.VISIBLE);//android 14
//
////                        cameraProvider.unbind(previewUseCase);
////                        cameraProvider = null;
////                        fragmentCameraBinding.cameraInstParentLayout.setVisibility(View.GONE);
////                        fragmentCameraBinding.previewView.removeAllViews();
////                        fragmentCameraBinding.previewView.setBackgroundColor(Color.TRANSPARENT);
////                        fragmentCameraBinding.previewView.setAlpha(0);
////                        previewUseCase.setSurfaceProvider(null);
////                        previewUseCase = new Preview.Builder().build();
////                        fragmentCameraBinding.previewView.destroyDrawingCache();
//                        } catch (Exception e) {
//                            Webhooks.exceptionReport(e, "CameraFragment/initRequests/Handler/!backPressed");
//                        }
//
//                        fragmentCameraBinding.ovalTickAnimation.setVisibility(View.GONE);
//                        setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
//                        if (SingletonData.getInstance().isQuickLiveness()) {
//                            setAnimationViewsAndText(fragmentCameraBinding.animationView, "facia_loader.svg", "", "", 0.0, false);
//                            new ConvertBitmapToFileAndProcess(bitmap, true).execute();
//                        } else {
//                            setAnimationViewsAndText(fragmentCameraBinding.animationViewUpload, "facia_uploading.svg",
//                                    SingletonData.getInstance().getActivity().getString(R.string.uploading), "", 0.0, false);
//                            if (FaceLivenessType.valueOf(requestModel.getConfigObject().getString("livenessType")) ==
//                                    FaceLivenessType.ADDITIONAL_CHECK_LIVENESS && !isQuickLiveness) {
//                                livenessApiHelper.livenessRequestObject(new File(filePath));
//                            } else {
//                                livenessApiHelper.createRequestJsonObject(new File(filePath), "dl", currentState);
//                            }
//                        }
//                    }
//                } catch (JSONException e) {
//                    Webhooks.exceptionReport(e, "CameraFragment/initRequests/Handler");
//                }
//            }, 1400);
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "ResultFragment/initRequests");
//        }
//    }
    private void initRequests(String filePath, Bitmap bitmap) {
        try {
            SingletonData.getInstance().getActivityListener().unregisterSensors();
            isCardDetectionInProcess = false;
            isCamStopped = false;
            SingletonData.getInstance().setCameraProcessing(false);
            SingletonData.getInstance().setVideoRecording(false);
            helperFunctions.showOvalTickAnimation();
            fragmentCameraBinding.ovalTickAnimation.setVisibility(View.VISIBLE);

            matchIdLiveness = requestModel.getConfigObject().getBoolean("matchIdLiveness");
            serviceType = requestModel.getConfigObject().getString("serviceType");
            livenessType = requestModel.getConfigObject().getString("livenessType");
            clientReference = requestModel.getConfigObject().getString("clientReference");
            docVerification = requestModel.getConfigObject().optBoolean("doc_verification", false);


            new Handler().postDelayed(() -> {
                if (!backPressed) {
                    try {
                        fragmentCameraBinding.showInstBtn.setEnabled(true);
                        isCamInstShown = false;
                        bottomSheetDialog.cancel();
                        if (FaceLivenessType.valueOf(livenessType) != FaceLivenessType.DETAILED_LIVENESS_ONLY || ServiceType.valueOf(serviceType) == ServiceType.MATCH_TO_PHOTO_ID) {
                            if (!(!matchIdLiveness && ServiceType.valueOf(serviceType) == ServiceType.MATCH_TO_PHOTO_ID)) {
                                fragmentCameraBinding.hidePreviewView.setVisibility(View.VISIBLE);
                                if (!defaultAndNotRooted && FaceLivenessType.valueOf(livenessType) == FaceLivenessType.DEFAULT_LIVENESS) {
                                    fragmentCameraBinding.hidePreviewView.setVisibility(View.GONE);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "CameraFragment/initRequests/Handler/!backPressed");
                    }

                    if (!matchIdLiveness && ServiceType.valueOf(serviceType) == ServiceType.MATCH_TO_PHOTO_ID) {
                        new ConvertBitmapToFileAndProcess(bitmap, true).execute();
                    } else {
                        fragmentCameraBinding.ovalTickAnimation.setVisibility(View.GONE);
                        setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
                        if (SingletonData.getInstance().isQuickLiveness()) {
                            setAnimationViewsAndText(fragmentCameraBinding.animationView, "facia_loader.svg", "", "", 0.0, false, "", "");
                            new ConvertBitmapToFileAndProcess(bitmap, true).execute();
                        } else {
                            setAnimationViewsAndText(fragmentCameraBinding.animationViewUpload, "facia_uploading.svg",
                                    SingletonData.getInstance().getActivity().getString(R.string.uploading), "", 0.0, false, "", "");

//                                if (FaceLivenessType.valueOf(requestModel.getConfigObject().getString("livenessType")) ==
//                                        FaceLivenessType.ADDITIONAL_CHECK_LIVENESS && !isQuickLiveness) {
//                                    livenessApiHelper.livenessRequestObject(new File(filePath));
//                                } else
                            livenessApiHelper.createRequestJsonObject(new File(filePath), "dl", currentState, clientReference, serviceType);

                        }
                    }
                }
            }, 1400);

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ResultFragment/initRequests");
        }
    }


//    private File convertBitmapToFile(Bitmap bmp, Boolean isDocDetected) {
//        File file = null;
//        try {
//            file = new File(SingletonData.getInstance().getActivity().getCacheDir(),
//                    isDocDetected ? "detectedDoc.jpeg" : "detectedFace.jpeg");
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            ByteArrayOutputStream bos = new ByteArrayOutputStream();

    /// /            bmp.compress(Bitmap.CompressFormat.JPEG, imageQuality /*ignored for PNG*/, bos);
//            bmp.compress(Bitmap.CompressFormat.JPEG, 70 /*ignored for PNG*/, bos);
//            byte[] bitmapData = bos.toByteArray();
//            try {
//                FileOutputStream fos = new FileOutputStream(file);
//                fos.write(bitmapData);
//                fos.flush();
//                fos.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "CameraFragment/convertBitmapToFile");
//        }
//        return file;
//    }
    private File convertBitmapToFile(Bitmap bmp, Boolean isDocDetected) {
        File file = null;
        try {
            // Generate unique filenames for documents
            String fileName;
            if (isDocDetected) {
                // Check if we're processing front or back document
                boolean isMatchToPhotoId = ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.MATCH_TO_PHOTO_ID;
                boolean docVerification = isMatchToPhotoId && requestModel.getConfigObject().optBoolean("doc_verification", false);

                if (docVerification) {
                    if (requestModel.getIdImage() == null) {
                        fileName = "id_front_" + System.currentTimeMillis() + ".jpeg";
                    } else {
                        fileName = "id_back_" + System.currentTimeMillis() + ".jpeg";
                    }
                } else {
                    fileName = "detectedDoc_" + System.currentTimeMillis() + ".jpeg";
                }
            } else {
                fileName = "detectedFace_" + System.currentTimeMillis() + ".jpeg";
            }

            file = new File(SingletonData.getInstance().getActivity().getCacheDir(), fileName);

            try {
                file.createNewFile();
            } catch (IOException e) {
                FaciaLogger.e(e);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos);
            byte[] bitmapData = bos.toByteArray();

            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bitmapData);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                FaciaLogger.e(e);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/convertBitmapToFile");
        }
        return file;
    }

    @Override
    public void fileUploaded() {
        try {
            fileUploadCount = fileUploadCount + 1;
            if (fileUploadCount == 2) {
                setAnimationViewsAndText(fragmentCameraBinding.animationViewUpload, "facia_loader.svg",
                        "", "", 0.0, false, "", "");
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/fileUploaded");
        }
    }

    /**
     * method to set UI
     */
    @Override
    public void setUi(int cameraLayoutVisibility, int faceLayoutVisibility, int matchIdLayoutVisibility,
                      int docLivenessLayoutVisibility, int quickLivenessInstVisibility, int qlResultInstructionScrVisibility, int resultLayoutVisibility, int imgPreviewLayoutVisibility) {
        try {
            SingletonData.getInstance().getActivity().runOnUiThread(() -> {
                try {
//                    if (currentState.equals("QL")){
//                        fragmentCameraBinding.showInstBtn.setVisibility(View.GONE);
//                    }else if (currentState.equals("DL")){
                    fragmentCameraBinding.showInstBtn.setVisibility(View.VISIBLE);
//                    }
                    fragmentCameraBinding.cameraParentLayout.setVisibility(cameraLayoutVisibility);
                    fragmentCameraBinding.faceParentLayout.setVisibility(faceLayoutVisibility);
                    fragmentCameraBinding.docParentLayout.setVisibility(matchIdLayoutVisibility);
                    fragmentCameraBinding.docLivenessParentLayout.setVisibility(docLivenessLayoutVisibility);
                    fragmentCameraBinding.resultParentLayout.setVisibility(resultLayoutVisibility);
                    fragmentCameraBinding.quickLivenessInst.setVisibility(View.GONE);
//                    fragmentCameraBinding.quickLivenessInst.setVisibility(quickLivenessInstVisibility);
                    fragmentCameraBinding.imagePreviewParentLayout.setVisibility(imgPreviewLayoutVisibility);
                    fragmentCameraBinding.resultInstScrParentLayout.setVisibility(qlResultInstructionScrVisibility);

                    if (resultLayoutVisibility == View.VISIBLE || imgPreviewLayoutVisibility == View.VISIBLE ||
                            qlResultInstructionScrVisibility == View.VISIBLE) {
                        if (helperFunctions.isAppLogoExists()) {
                            int imageResource = getResources().getIdentifier(
                                    ApiConstants.MERCHANT_APP_LOGO, "drawable", requestModel.getParentActivity().getPackageName());
                            fragmentCameraBinding.footer.appLogo.setImageResource(imageResource);
                            fragmentCameraBinding.footer.appLogo.setVisibility(View.VISIBLE);
                        } else {
                            fragmentCameraBinding.footer.appLogo.setVisibility(View.GONE);
                        }
                    }
                } catch (Exception e) {
                    Webhooks.exceptionReport(e, "CameraFragment/setUi-inner");
                }
            });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/setUi");
        }
    }

    @Override
    public void convertBitmapToBase64(Bitmap bitmap) {
        new BitmapToBase64(bitmap).execute();
    }

    /**
     * method to set animation view
     *
     * @param animationView  view to be set
     * @param animationName  animation to be load
     * @param declinedReason
     */
    @Override
    public void setAnimationViewsAndText(WebView animationView, String animationName, String text,
                                         String requestStatus, double similarityScore, Boolean gotResult, String declinedReason, String errorStr) {
        try {
            SingletonData.getInstance().getActivity().runOnUiThread(() -> {
                try {
                    fragmentCameraBinding.resultContinueBtn.setVisibility(View.GONE);
                    if (!gotResult) {
                        //show loader/uploader
                        helperFunctions.loadAnimation(animationView, animationName);
                        if (text.equals(SingletonData.getInstance().getActivity().getString(R.string.uploading))) {
                            helperFunctions.showResultText(text, animationView, declinedReason);
                        } else {
                            fragmentCameraBinding.resultTxt.setVisibility(View.GONE);
                        }
                    } else if (requestType.equals("additionalCheckLiveness") && currentState.equals("QL") &&
                            requestStatus.equals("liveness.unverified")) {
                        SingletonData.getInstance().setQuickRequestInProcess(false);
                        if (requestModel.getConfigObject().getBoolean("livenessRetryFlow")) {
                            if (recursiveQlCounter <= requestModel.getConfigObject().getInt("livenessRetryCount")) {
                                //re open QL
                                if (requestModel.getConfigObject().getBoolean("showMiddleInstructions")) {
                                    setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE);
                                    //auto time reset key should be implement here
//                                    new Handler().postDelayed(() -> helperFunctions.setQlUi(), TimeConstants.REMOVE_QL_INSTRUCTIONS_SCR);
                                } else {
                                    helperFunctions.setQlUi(true);
                                }
                            } else {
                                //QL failed in case of additional, go to DL
                                helperFunctions.setAdditionalLivenessUi(animationView);
                            }
                        } else {
                            //QL failed in case of additional, go to DL
                            helperFunctions.setAdditionalLivenessUi(animationView);
                        }
                    } else if (requestType.equals("photoIdMatch") && currentState.equals("QL")) {
                        if (requestStatus.equals("liveness.verified")) {
                            if (docVerificationEnabled && requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                                showKycDocTypeFragment();
                            } else {
                                matchIdByCapture();
                            }
//                            //ql detected, show result
//                            helperFunctions.loadAnimation(animationView, animationName);
//                            helperFunctions.showResultText(text);
//                            // go to doc detection after delay
//                            new Handler().postDelayed(this::matchIdByCapture, TimeConstants.REMOVE_RESULT_SCREEN);
                        } else if (requestStatus.equals("liveness.unverified") &&
                                requestModel.getConfigObject().getBoolean("documentRetryFlow")) {
                            if (recursiveQlCounter <= requestModel.getConfigObject().getInt("documentRetryCount")) {
                                //re open QL
                                if (requestModel.getConfigObject().getBoolean("showMiddleInstructions")) {
                                    setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE);
                                    //auto time reset key should be implement here
//                                    new Handler().postDelayed(() -> helperFunctions.setQlUi(), TimeConstants.REMOVE_QL_INSTRUCTIONS_SCR);
                                } else {
                                    helperFunctions.setQlUi(true);
                                }
                            } else if (requestModel.getConfigObject().getBoolean("showResult")) {
                                helperFunctions.showResult(animationView, animationName, text, requestStatus, similarityScore, declinedReason, errorStr);
                            } else {
                                helperFunctions.notToShowResult(requestStatus, similarityScore, errorStr);
                            }
                        } else {
                            if (requestModel.getConfigObject().getBoolean("showResult")) {
                                helperFunctions.showResult(animationView, animationName, text, requestStatus, similarityScore, declinedReason, errorStr);
                            } else {
                                helperFunctions.notToShowResult(requestStatus, similarityScore, errorStr);
                            }
                        }
                    } else if (requestType.equals("quickLivenessOnly") && requestStatus.equals("liveness.unverified")) {
                        if (requestModel.getConfigObject().getBoolean("livenessRetryFlow")) {
                            if (recursiveQlCounter <= requestModel.getConfigObject().getInt("livenessRetryCount")) {
                                //re open QL
                                if (requestModel.getConfigObject().getBoolean("showMiddleInstructions")) {
                                    setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE);
                                    //auto time reset key should be implement here
//                                    new Handler().postDelayed(() -> helperFunctions.setQlUi(), TimeConstants.REMOVE_QL_INSTRUCTIONS_SCR);
                                } else {
                                    helperFunctions.setQlUi(true);
                                }
                            } else {
                                if (requestModel.getConfigObject().getBoolean("showResult")) {
                                    helperFunctions.showResult(animationView, animationName, text, requestStatus, similarityScore, declinedReason, errorStr);
                                } else if (!requestModel.getConfigObject().getBoolean("showResult")) {
                                    helperFunctions.notToShowResult(requestStatus, similarityScore, errorStr);
                                }
                            }
                        } else {
                            if (requestModel.getConfigObject().getBoolean("showResult")) {
                                helperFunctions.showResult(animationView, animationName, text, requestStatus, similarityScore, declinedReason, errorStr);
                            } else if (!requestModel.getConfigObject().getBoolean("showResult")) {
                                helperFunctions.notToShowResult(requestStatus, similarityScore, errorStr);
                            }
                        }
                    } else if (requestModel.getConfigObject().getBoolean("showResult")) {
                        helperFunctions.showResult(animationView, animationName, text, requestStatus, similarityScore, declinedReason, errorStr);
                    } else if (!requestModel.getConfigObject().getBoolean("showResult")) {
                        helperFunctions.notToShowResult(requestStatus, similarityScore, errorStr);
                    }
                } catch (Exception e) {
                    Webhooks.exceptionReport(e, "CameraFragment/setAnimationViewsAndText-inner");
                }
            });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/setAnimationViewsAndText");
        }
    }

    @Override
    public void onClick(View view) {
        synchronized (view) {
            view.setEnabled(false);
            int id = view.getId();
            if (id == R.id.resultContinueBtn) {
                helperFunctions.feedbackScr();
            } else if (id == R.id.tryUploadPicTxt) {
                handleUploadFromGallery();
            } else if (id == R.id.imgContinueBtn) {
                fragmentCameraBinding.retakeBtn.setEnabled(false);
                new Handler().postDelayed(() -> fragmentCameraBinding.retakeBtn.setEnabled(true),
                        TimeConstants.SYNCHRONIZED_CONSTANT);
                isDocCaptured = false;
                uploadCapturedImg();
            } else if (id == R.id.retakeBtn) {
                fragmentCameraBinding.imgContinueBtn.setEnabled(false);
                new Handler().postDelayed(() -> fragmentCameraBinding.imgContinueBtn.setEnabled(true),
                        TimeConstants.SYNCHRONIZED_CONSTANT);
                isDocCaptured = false;
                retakeBtnHandling();
            } else if (id == R.id.captureDocBtn) {
                if (fragmentCameraBinding.previewView.getPreviewStreamState()
                        .getValue().toString().equalsIgnoreCase("STREAMING")) {
                    isDocCaptured = true;
                    Bitmap bitmap = fragmentCameraBinding.previewView.getBitmap();
                    captureSound();
                    convertAndDisplayCapturedCard(helperFunctions.cropFrame(bitmap));
                }
            } else if (id == R.id.captureDocLivenessBtn) {
                Bitmap bitmap = fragmentCameraBinding.previewView.getBitmap();
                fragmentCameraBinding.detectedFrame.setImageBitmap(bitmap);
                setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE);
            } else if (id == R.id.instRetryBtn) {
                helperFunctions.setQlUi(true);
            } else if (id == R.id.showInstBtn) {
                helperFunctions.showBottomSheet();
            } else if (id == R.id.showDocInstBtn) {
                // Show doc two guidelines when in doc two states, else show doc one guidelines
                if (currentState.equals("docTwoDetection") || currentState.equals("docTwoDetectionBack")) {
                    showDocTwoGuidelinesBottomSheet();
                } else {
                    showDocGuidelinesBottomSheet();
                }
            }
        }
        new Handler().postDelayed(() -> view.setEnabled(true), TimeConstants.SYNCHRONIZED_CONSTANT);
    }

//    private void uploadCapturedImg() {
//        try {
//            if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.DOCUMENT_LIVENESS) {

    /// /                Toast.makeText(SingletonData.getInstance().getContext(), "Please Go Back", Toast.LENGTH_SHORT).show();
//            } else {
//                helperFunctions.handleCheckSimilarity(similarityApiHelper, true);
//            }
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "CameraFragment/uploadCapturedImg");
//        }
//    }
    private void uploadCapturedImg() {
        try {
            if (!docVerificationEnabled) {
                helperFunctions.handleCheckSimilarity(similarityApiHelper, true);
                return;
            }

            // 1. ID Front (Required)
            if (requestModel.getIdImage() == null) {
                matchIdByCapture();
                return;
            }

            // 2. ID Back (Optional - Check Image OR Skip Flag)
            if (requestModel.getIdBackImage() == null && !requestModel.isIdBackSkipped()) {
                try {
                    if (requestModel.getConfigObject().optBoolean("perform_full_kyc", false) &&
                            DocumentType.valueOf(requestModel.getConfigObject().getString("documentType")) == DocumentType.PASSPORT) {
                        requestModel.setIdBackSkipped(true);
                    } else {
                        transitionToIdBackCapture();
                        return;
                    }
                } catch (Exception e) {
                    transitionToIdBackCapture();
                    return;
                }
            }

            // 3. Document Two Front (Required if doc_two_verification is ON)
            if (docTwoVerificationEnabled && requestModel.getDocTwoImage() == null) {
                transitionToDocTwoFrontCapture();
                return;
            }

            // 4. Document Two Back (Optional - Check Image OR Skip Flag)
            if (docTwoVerificationEnabled && requestModel.getDocTwoBackImage() == null && !requestModel.isDocTwoBackSkipped()) {
                transitionToDocTwoBackCapture();
                return;
            }

            // 5. Address Front (Required if Address Verification is ON)
            if (addressVerificationEnabled && requestModel.getAddressImage() == null) {
                if (requestModel.getConfigObject().optBoolean("perform_full_kyc", false)
                        && !requestModel.getConfigObject().has("addressDocType")) {
                    showKycAddressTypeFragment();
                    return;
                }
                transitionToAddressFrontCapture();
                return;
            }

            // 6. Address Back (Optional - Check Image OR Skip Flag)
            if (addressVerificationEnabled && requestModel.getAddressBackImage() == null && !requestModel.isAddressBackSkipped()) {
                if (requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                    requestModel.setAddressBackSkipped(true);
                } else {
                    transitionToAddressBackCapture();
                    return;
                }
            }

            // 7. Submit
            finishVerificationAndSubmit();

        } catch (Exception e) {
            FaciaLogger.e(e);
        }
    }

    private void transitionToIdBackCapture() {
        currentState = "docDetectionBack";
        setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
            if (requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                try {
                    DocumentType docType = DocumentType.valueOf(requestModel.getConfigObject().getString("documentType"));
                    if (docType == DocumentType.DRIVING_LICENSE) {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_license_back_in_box);
                    } else {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_id_card_back_in_box);
                    }
                } catch (Exception e) {
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_card_back_in_box);
                }
                fragmentCameraBinding.showDocInstBtn.setVisibility(View.VISIBLE);
            } else {
                fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_card_back_in_box);
            }
            fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);

            // Clear any previous listeners and set new one
            fragmentCameraBinding.skipBtn.setOnClickListener(null);
            fragmentCameraBinding.skipBtn.setVisibility(View.VISIBLE);
            fragmentCameraBinding.skipBtn.setOnClickListener(v -> {
                requestModel.setIdBackSkipped(true);
                requestModel.setIdBackImage(null);
                fragmentCameraBinding.skipBtn.setVisibility(View.GONE);
                uploadCapturedImg();
            });
            detectCard();
        }, 300);
    }

    //    private void retakeBtnHandling() {
//        try {
//            if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.MATCH_TO_PHOTO_ID) {
//                fragmentCameraBinding.captureDocBtn.setVisibility(View.GONE);
//                setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
//                docBtnHandler.postDelayed(docBtnRunnable, TimeConstants.SHOW_CAPTURE_DOC_BTN_DELAY);
//                detectCard();
//            } else {
//                if (DocumentType.valueOf(requestModel.getConfigObject().getString("documentType")) == DocumentType.ID_CARD) {
//                    fragmentCameraBinding.captureDocBtn.setVisibility(View.GONE);
//                    setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
//                    docBtnHandler.postDelayed(docBtnRunnable, TimeConstants.SHOW_CAPTURE_DOC_BTN_DELAY);
//                    detectCard();
//                } else {
//                    setUi(View.VISIBLE, View.GONE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE);
//                }
//            }
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "CameraFragment/retakeBtnHandling");
//        }
//    }
    private void retakeBtnHandling() {
        try {
            // KILL background processes immediately
            isCardDetectionInProcess = false;
            docBtnHandler.removeCallbacks(docBtnRunnable);

            if (!docVerificationEnabled) {
                requestModel.setIdImage(null);
                currentState = "docDetection";
            } else {
                switch (currentState) {
                    case "addressDetectionBack":
                        requestModel.setAddressBackImage(null);
                        requestModel.setAddressBackSkipped(false);
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_address_back_in_box);
                        break;
                    case "addressDetection":
                        requestModel.setAddressImage(null);
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_address_in_box);
                        break;
                    case "docDetectionBack":
                        requestModel.setIdBackImage(null);
                        requestModel.setIdBackSkipped(false);
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_card_back_in_box);
                        break;
                    case "docTwoDetectionBack":
                        requestModel.setDocTwoBackImage(null);
                        requestModel.setDocTwoBackSkipped(false);
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_doc_two_back_in_box);
                        break;
                    case "docTwoDetection":
                        requestModel.setDocTwoImage(null);
                        currentState = "docTwoDetection";
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_doc_two_front_in_box);
                        break;
                    case "docDetection":
                    default:
                        requestModel.setIdImage(null);
                        currentState = "docDetection";
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_card_in_box);
                        break;
                }
            }

            isDocCaptured = false;
            // Reset PDF preview state on retake
            fragmentCameraBinding.pdfPreviewScrollView.setVisibility(View.GONE);
            fragmentCameraBinding.pdfPagesContainer.removeAllViews();
            fragmentCameraBinding.detectedFrame.setVisibility(View.VISIBLE);

            // Restore imgContinueBtn top constraint back to detectedFrame
            ConstraintLayout parentLayout = (ConstraintLayout) fragmentCameraBinding.imgContinueBtn.getParent();
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(parentLayout);
            constraintSet.connect(
                    R.id.imgContinueBtn,
                    ConstraintSet.TOP,
                    R.id.detectedFrame,
                    ConstraintSet.BOTTOM
            );
            constraintSet.applyTo(parentLayout);

            fragmentCameraBinding.captureDocBtn.setVisibility(View.GONE);

            // Handle Skip button visibility correctly on retake
            if (currentState.equals("docDetectionBack") || currentState.equals("addressDetectionBack")
                    || currentState.equals("docTwoDetectionBack")) {
                fragmentCameraBinding.skipBtn.setVisibility(View.VISIBLE);
            } else {
                fragmentCameraBinding.skipBtn.setVisibility(View.GONE);
            }

            setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
            docBtnHandler.postDelayed(docBtnRunnable, TimeConstants.SHOW_CAPTURE_DOC_BTN_DELAY);
            detectCard();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/retakeBtnHandling");
        }
    }

//    private void handleUploadFromGallery() {
//        try {
//            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//            intent.setType("image/*");
//
//            int requestCode = 1000;  // Default fallback
//
//            if (docVerificationEnabled) {
//                switch (currentState) {
//                    case "docDetection":
//                        requestCode = 5001;  // ID Front
//                        break;
//                    case "docDetectionBack":
//                        requestCode = 5002;  // ID Back
//                        break;
//                    case "docTwoDetection":
//                        requestCode = 5005;  // Doc Two Front
//                        break;
//                    case "docTwoDetectionBack":
//                        requestCode = 5006;  // Doc Two Back
//                        break;
//                    case "addressDetection":
//                        requestCode = 5003;  // Address Front
//                        break;
//                    case "addressDetectionBack":
//                        requestCode = 5004;  // Address Back
//                        break;
//                    default:
//                        requestCode = 5001;  // Default to ID Front
//                        break;
//                }
//            } else {
//                requestCode = 5001;  // Default to ID Front
//            }
//
//            Log.d(TAG, "handleUploadFromGallery: Starting gallery picker with requestCode=" + requestCode);
//            startActivityForResult(intent, requestCode);
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "CameraFragment/handleUploadFromGallery");
//        }
//    }


    private void handleUploadFromGallery() {
        try {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "application/pdf"});

            int requestCode = 1000;  // Default fallback

            if (docVerificationEnabled) {
                switch (currentState) {
                    case "docDetection":
                        requestCode = 5001;  // ID Front
                        break;
                    case "docDetectionBack":
                        requestCode = 5002;  // ID Back
                        break;
                    case "docTwoDetection":
                        requestCode = 5005;  // Doc Two Front
                        break;
                    case "docTwoDetectionBack":
                        requestCode = 5006;  // Doc Two Back
                        break;
                    case "addressDetection":
                        requestCode = 5003;  // Address Front
                        break;
                    case "addressDetectionBack":
                        requestCode = 5004;  // Address Back
                        break;
                    default:
                        requestCode = 5001;  // Default to ID Front
                        break;
                }
            } else {
                requestCode = 5001;  // Default to ID Front
            }

            FaciaLogger.d(TAG, "handleUploadFromGallery: Starting gallery picker with requestCode=" + requestCode);
            startActivityForResult(intent, requestCode);

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/handleUploadFromGallery");
        }
    }

    /**
     * Renders the first page of a PDF as a Bitmap for preview thumbnail.
     */


    /**
     * Renders ALL pages of a PDF as a list of Bitmaps for scrollable preview.
     */
    private List<Bitmap> renderAllPdfPages(Context context, Uri uri) {
        List<Bitmap> pages = new ArrayList<>();
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
            if (pfd == null) return pages;

            PdfRenderer renderer = new PdfRenderer(pfd);
            int pageCount = renderer.getPageCount();

            for (int i = 0; i < pageCount; i++) {
                PdfRenderer.Page page = renderer.openPage(i);

                // Scale factor for better readability (2x native resolution)
                int scaleFactor = 2;
                Bitmap bitmap = Bitmap.createBitmap(
                        page.getWidth() * scaleFactor,
                        page.getHeight() * scaleFactor,
                        Bitmap.Config.ARGB_8888
                );

                Canvas canvas = new Canvas(bitmap);
                canvas.drawColor(Color.WHITE);

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                page.close();

                pages.add(bitmap);
            }

            renderer.close();
            pfd.close();
        } catch (Exception e) {
            FaciaLogger.e(TAG, "renderAllPdfPages error: " + e.getMessage());
        }
        return pages;
    }

    /**
     * Copies the raw PDF bytes from a content URI to a cache file.
     * This preserves the original PDF data for proper base64 encoding.
     */
    private File savePdfToFile(Uri uri, String fileName) throws IOException {
        File file = new File(SingletonData.getInstance().getActivity().getCacheDir(), fileName);
        InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
        if (inputStream == null) throw new IOException("Failed to open PDF input stream");

        FileOutputStream fos = new FileOutputStream(file);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        fos.flush();
        fos.close();
        inputStream.close();

        return file;
    }

    /**
     * Shows a scrollable PDF preview with all pages rendered as bitmaps.
     * Hides the image preview ImageView and shows the PDF ScrollView instead.
     * Re-anchors imgContinueBtn's top constraint to pdfPreviewScrollView
     * so the button layout matches image preview positioning.
     */
    private void showPdfPreview(List<Bitmap> pages) {
        try {
            // Hide image preview, show PDF scroll preview
            fragmentCameraBinding.detectedFrame.setVisibility(View.GONE);
            fragmentCameraBinding.pdfPreviewScrollView.setVisibility(View.VISIBLE);

            // Re-anchor imgContinueBtn top constraint from detectedFrame to pdfPreviewScrollView
            // so Continue button stays correctly positioned above Retake/Cancel button
            ConstraintLayout parentLayout = (ConstraintLayout) fragmentCameraBinding.imgContinueBtn.getParent();
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(parentLayout);
            constraintSet.connect(
                    R.id.imgContinueBtn,
                    ConstraintSet.TOP,
                    R.id.pdfPreviewScrollView,
                    ConstraintSet.BOTTOM
            );
            constraintSet.applyTo(parentLayout);

            LinearLayout container = fragmentCameraBinding.pdfPagesContainer;
            container.removeAllViews();

            for (int i = 0; i < pages.size(); i++) {
                Bitmap pageBitmap = pages.get(i);

                ImageView pageView = new ImageView(requireContext());
                pageView.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                pageView.setAdjustViewBounds(true);
                pageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                pageView.setImageBitmap(pageBitmap);

                container.addView(pageView);

                // Add divider between pages (except after last page)
                if (i < pages.size() - 1) {
                    View divider = new View(requireContext());
                    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 4);
                    dividerParams.setMargins(0, 8, 0, 8);
                    divider.setLayoutParams(dividerParams);
                    divider.setBackgroundColor(Color.LTGRAY);
                    container.addView(divider);
                }
            }
        } catch (Exception e) {
            FaciaLogger.e(TAG, "showPdfPreview error: " + e.getMessage());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) return;

        try {
            Uri uri = data.getData();
            long size = getFileSizeFromUri(uri);

            if (size > 3 * 1024 * 1024) {
                showError("File must be less than 3MB");
                return;
            }

            assert uri != null;
            String mimeType = requireContext().getContentResolver().getType(uri);
            boolean isPdf = "application/pdf".equals(mimeType);

            if (isPdf) {
                // ===== PDF FLOW =====
                handlePdfSelection(uri, requestCode);
            } else {
                // ===== IMAGE FLOW (unchanged) =====
                handleImageSelection(uri, requestCode);
            }

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/onActivityResult");
        }
    }

    /**
     * Handles PDF file selection from gallery.
     * Saves the raw PDF to cache (preserving original bytes for base64),
     * renders all pages for scrollable preview, and sets the file on RequestModel.
     */
    private void handlePdfSelection(Uri uri, int requestCode) {
        try {
            String fileName;
            File file;
            String suffix = "_" + System.currentTimeMillis() + ".pdf";

            switch (requestCode) {
                case 5001: // ID Front
                    fileName = "uploaded_id_front" + suffix;
                    file = savePdfToFile(uri, fileName);
                    requestModel.setIdImage(file);
                    currentState = "docDetection";
                    break;

                case 5002: // ID Back
                    fileName = "uploaded_id_back" + suffix;
                    file = savePdfToFile(uri, fileName);
                    requestModel.setIdBackImage(file);
                    requestModel.setIdBackSkipped(false);
                    currentState = "docDetectionBack";
                    break;

                case 5003: // Address Front
                    fileName = "uploaded_address_front" + suffix;
                    file = savePdfToFile(uri, fileName);
                    requestModel.setAddressImage(file);
                    currentState = "addressDetection";
                    break;

                case 5004: // Address Back
                    fileName = "uploaded_address_back" + suffix;
                    file = savePdfToFile(uri, fileName);
                    requestModel.setAddressBackImage(file);
                    requestModel.setAddressBackSkipped(false);
                    currentState = "addressDetectionBack";
                    break;

                case 5005: // Doc Two Front
                    fileName = "uploaded_doc_two_front" + suffix;
                    file = savePdfToFile(uri, fileName);
                    requestModel.setDocTwoImage(file);
                    currentState = "docTwoDetection";
                    break;

                case 5006: // Doc Two Back
                    fileName = "uploaded_doc_two_back" + suffix;
                    file = savePdfToFile(uri, fileName);
                    requestModel.setDocTwoBackImage(file);
                    requestModel.setDocTwoBackSkipped(false);
                    currentState = "docTwoDetectionBack";
                    break;

                default:
                    return;
            }

            // Render all PDF pages for scrollable preview
            List<Bitmap> pdfPages = renderAllPdfPages(requireContext(), uri);
            if (pdfPages.isEmpty()) {
                showError("Failed to render PDF");
                return;
            }

            showPdfPreview(pdfPages);
            isDocCaptured = true;
            setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE);

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/handlePdfSelection");
        }
    }

    /**
     * Handles image file selection from gallery (existing image flow, unchanged).
     */
    private void handleImageSelection(Uri uri, int requestCode) {
        try {
            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(
                    requireContext().getContentResolver(), uri);

            if (originalBitmap == null) {
                showError("Failed to load image");
                return;
            }

            Bitmap processedBitmap = originalBitmap;
            if (originalBitmap.getWidth() > 1080 || originalBitmap.getHeight() > 1080) {
                processedBitmap = Bitmap.createScaledBitmap(originalBitmap, 1080, 1080, true);
            }

            String fileName;
            File file;

            switch (requestCode) {
                case 5001: // ID Front
                    fileName = "uploaded_id_front_" + System.currentTimeMillis() + ".jpeg";
                    file = saveBitmapToFile(processedBitmap, fileName);
                    requestModel.setIdImage(file);
                    currentState = "docDetection";
                    break;

                case 5002: // ID Back
                    fileName = "uploaded_id_back_" + System.currentTimeMillis() + ".jpeg";
                    file = saveBitmapToFile(processedBitmap, fileName);
                    requestModel.setIdBackImage(file);
                    requestModel.setIdBackSkipped(false);
                    currentState = "docDetectionBack";
                    break;

                case 5003: // Address Front
                    fileName = "uploaded_address_front_" + System.currentTimeMillis() + ".jpeg";
                    file = saveBitmapToFile(processedBitmap, fileName);
                    requestModel.setAddressImage(file);
                    currentState = "addressDetection";
                    break;

                case 5004: // Address Back
                    fileName = "uploaded_address_back_" + System.currentTimeMillis() + ".jpeg";
                    file = saveBitmapToFile(processedBitmap, fileName);
                    requestModel.setAddressBackImage(file);
                    requestModel.setAddressBackSkipped(false);
                    currentState = "addressDetectionBack";
                    break;

                case 5005: // Doc Two Front
                    fileName = "uploaded_doc_two_front_" + System.currentTimeMillis() + ".jpeg";
                    file = saveBitmapToFile(processedBitmap, fileName);
                    requestModel.setDocTwoImage(file);
                    currentState = "docTwoDetection";
                    break;

                case 5006: // Doc Two Back
                    fileName = "uploaded_doc_two_back_" + System.currentTimeMillis() + ".jpeg";
                    file = saveBitmapToFile(processedBitmap, fileName);
                    requestModel.setDocTwoBackImage(file);
                    requestModel.setDocTwoBackSkipped(false);
                    currentState = "docTwoDetectionBack";
                    break;
            }

            setImageToPreview(processedBitmap);
            isDocCaptured = true;
            setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE);

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/handleImageSelection");
        }
    }

    private File saveBitmapToFile(Bitmap bitmap, String fileName) throws IOException {
        File file = new File(SingletonData.getInstance().getActivity().getCacheDir(), fileName);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, bos);
        byte[] bitmapData = bos.toByteArray();

        FileOutputStream fos = new FileOutputStream(file);
        fos.write(bitmapData);
        fos.flush();
        fos.close();

        return file;
    }

    private void setImageToPreview(Bitmap bitmap) {
        try {
            // Reset PDF preview state - ensure image preview is active
            fragmentCameraBinding.pdfPreviewScrollView.setVisibility(View.GONE);
            fragmentCameraBinding.pdfPagesContainer.removeAllViews();
            fragmentCameraBinding.detectedFrame.setVisibility(View.VISIBLE);

            // Restore imgContinueBtn top constraint back to detectedFrame
            ConstraintLayout parentLayout = (ConstraintLayout) fragmentCameraBinding.imgContinueBtn.getParent();
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(parentLayout);
            constraintSet.connect(
                    R.id.imgContinueBtn,
                    ConstraintSet.TOP,
                    R.id.detectedFrame,
                    ConstraintSet.BOTTOM
            );
            constraintSet.applyTo(parentLayout);

            // Get the ImageView dimensions
            int targetWidth = fragmentCameraBinding.detectedFrame.getWidth();
            int targetHeight = fragmentCameraBinding.detectedFrame.getHeight();

            // If dimensions are not available yet, use a default size
            if (targetWidth <= 0 || targetHeight <= 0) {
                targetWidth = 1080;
                targetHeight = 1920;
            }

            // Calculate the scale to fit within ImageView while maintaining aspect ratio
            float scale = Math.min(
                    (float) targetWidth / bitmap.getWidth(),
                    (float) targetHeight / bitmap.getHeight()
            );

            int newWidth = (int) (bitmap.getWidth() * scale);
            int newHeight = (int) (bitmap.getHeight() * scale);

            // Create scaled bitmap
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

            // Set the scaled bitmap to ImageView
            fragmentCameraBinding.detectedFrame.setImageBitmap(scaledBitmap);
            fragmentCameraBinding.detectedFrame.setScaleType(ImageView.ScaleType.FIT_CENTER);

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/setImageToPreview");
            // Fallback: set original bitmap
            fragmentCameraBinding.detectedFrame.setImageBitmap(bitmap);
            fragmentCameraBinding.detectedFrame.setScaleType(ImageView.ScaleType.FIT_CENTER);
        }
    }

    private long getFileSizeFromUri(Uri uri) {
        try {
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            cursor.moveToFirst();
            @SuppressLint("Range") long size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE));
            cursor.close();
            return size;
        } catch (Exception e) {
            return 0;
        }
    }

    private void showError(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    private void showKycDocTypeFragment() {
        try {
            Fragment kycDocTypeFragment = new KycDocTypeFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, kycDocTypeFragment, kycDocTypeFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/showKycDocTypeFragment");
        }
    }

    private void showKycDocTwoTypeFragment() {
        try {
            Fragment kycDocTwoTypeFragment = new KycDocTwoTypeFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, kycDocTwoTypeFragment, kycDocTwoTypeFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/showKycDocTwoTypeFragment");
        }
    }

    private void showKycAddressTypeFragment() {
        try {
            Fragment kycAddressTypeFragment = new KycAddressTypeFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, kycAddressTypeFragment, kycAddressTypeFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/showKycAddressTypeFragment");
        }
    }

    private void initDocGuidelinesBottomSheet() {
        try {
            docGuidelinesDialog = new BottomSheetDialog(SingletonData.getInstance().getActivity(), R.style.BottomSheetDialogTheme);
            View sheetView = LayoutInflater.from(SingletonData.getInstance().getActivity()).inflate(R.layout.doc_guidelines_bottom_sheet, null);
            docGuidelinesDialog.setContentView(sheetView);
            docGuidelinesDialog.setCanceledOnTouchOutside(true);
            ImageView crossImg = sheetView.findViewById(R.id.docGuidelinesCrossImg);
            if (crossImg != null) {
                crossImg.setOnClickListener(v -> docGuidelinesDialog.dismiss());
            }
            docGuidelinesDialog.setOnDismissListener(dialog -> {
                docBtnHandler.removeCallbacks(docBtnRunnable);
                docBtnHandler.postDelayed(docBtnRunnable, TimeConstants.SHOW_CAPTURE_DOC_BTN_DELAY);
            });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/initDocGuidelinesBottomSheet");
        }
    }

    private void showDocGuidelinesBottomSheet() {
        try {
            if (docGuidelinesDialog == null) {
                initDocGuidelinesBottomSheet();
            }
            docGuidelinesDialog.show();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/showDocGuidelinesBottomSheet");
        }
    }

    /**
     * Shows a guidelines bottom sheet for Document Two with fully updated text.
     * Title and all do's/don'ts items say "document two" instead of "document".
     * Dismiss just resumes docBtnRunnable so the capture button re-appears.
     */
    private void showDocTwoGuidelinesBottomSheet() {
        try {
            BottomSheetDialog docTwoGuidelinesDialog = new BottomSheetDialog(
                    SingletonData.getInstance().getContext(),
                    com.google.android.material.R.style.Theme_Design_BottomSheetDialog);

            View sheetView = LayoutInflater.from(SingletonData.getInstance().getContext())
                    .inflate(R.layout.doc_guidelines_bottom_sheet, null);
            docTwoGuidelinesDialog.setContentView(sheetView);

            // Override title
            TextView titleTxt = sheetView.findViewById(R.id.docGuidelinesTitle);
            if (titleTxt != null) {
                titleTxt.setText(R.string.doc_two_guidelines_heading);
            }

            // Override do's item 1: "All 4 corners of the document two should be visible"
            android.widget.LinearLayout dosItem1 = sheetView.findViewById(R.id.dosItem1Layout);
            if (dosItem1 != null && dosItem1.getChildCount() > 1
                    && dosItem1.getChildAt(1) instanceof TextView) {
                ((TextView) dosItem1.getChildAt(1)).setText(R.string.doc_two_guideline_clean_cam);
            }

            // Override do's item 2: "Make sure you are in a well lit room"
            android.widget.LinearLayout dosItem2 = sheetView.findViewById(R.id.dosItem2Layout);
            if (dosItem2 != null && dosItem2.getChildCount() > 1
                    && dosItem2.getChildAt(1) instanceof TextView) {
                ((TextView) dosItem2.getChildAt(1)).setText(R.string.doc_two_guideline_hold_steady);
            }

            // Override don'ts item 1: "Don't capture document two in glare or shadow"
            android.widget.LinearLayout dontsItem1 = sheetView.findViewById(R.id.dontsItem1Layout);
            if (dontsItem1 != null && dontsItem1.getChildCount() > 1
                    && dontsItem1.getChildAt(1) instanceof TextView) {
                ((TextView) dontsItem1.getChildAt(1)).setText(R.string.doc_two_guideline_no_glare);
            }

            // Override don'ts item 2: "Don't crop/cut off any part of document two"
            android.widget.LinearLayout dontsItem2 = sheetView.findViewById(R.id.dontsItem2Layout);
            if (dontsItem2 != null && dontsItem2.getChildCount() > 1
                    && dontsItem2.getChildAt(1) instanceof TextView) {
                ((TextView) dontsItem2.getChildAt(1)).setText(R.string.doc_two_guideline_no_crop);
            }

            // Cross button dismisses dialog
            ImageView crossImg = sheetView.findViewById(R.id.docGuidelinesCrossImg);
            if (crossImg != null) {
                crossImg.setOnClickListener(v -> docTwoGuidelinesDialog.dismiss());
            }

            docTwoGuidelinesDialog.setOnDismissListener(dialog -> {
                // Guidelines dismissed — resume docBtnRunnable so capture button appears
                docBtnHandler.removeCallbacks(docBtnRunnable);
                docBtnHandler.postDelayed(docBtnRunnable, TimeConstants.SHOW_CAPTURE_DOC_BTN_DELAY);
            });

            docTwoGuidelinesDialog.show();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/showDocTwoGuidelinesBottomSheet");
        }
    }

    /**
     * invoking further method to change UI
     * and to open camera to detect card
     */
    private void matchIdByCapture() {
        try {
            currentState = "docDetection";
            fragmentCameraBinding.animationView.loadUrl("");
            docBtnHandler.postDelayed(docBtnRunnable, TimeConstants.SHOW_CAPTURE_DOC_BTN_DELAY);
            cardDetectionTflite = new CardDetectionTflite();
            if (cameraX != null) {
                helperFunctions.tapToFocus(cameraX.getCameraControl());
            }
            initCamera(false);

            // FORCE show instruction layout immediately
            fragmentCameraBinding.hidePreviewView.setVisibility(View.VISIBLE);
            fragmentCameraBinding.docInstParentLayout.setVisibility(View.VISIBLE);

            if (docVerificationEnabled && requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                try {
                    DocumentType docType = DocumentType.valueOf(requestModel.getConfigObject().getString("documentType"));
                    if (docType == DocumentType.PASSPORT) {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_passport_in_box);
                    } else if (docType == DocumentType.DRIVING_LICENSE) {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_license_in_box);
                    } else {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_id_card_in_box);
                    }
                } catch (Exception e) {
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_card_in_box);
                }
            } else if (docVerificationEnabled) {
                fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_card_in_box);
            } else {
                fragmentCameraBinding.faceDocDetectInst.setText(R.string.doc_in_front_of_cam);
            }

            // Reset upload label back to doc one label
            fragmentCameraBinding.tryUploadPicTxt.setText(R.string.upload_pic_id);

            setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);

            if (!requestModel.getConfigObject().getBoolean("card_autocapture")) {
                fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);
                docBtnHandler.removeCallbacks(docBtnRunnable);
            }

            // Optional: small delay if needed on some devices
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                fragmentCameraBinding.docInstParentLayout.setVisibility(View.VISIBLE);
            }, 300);

            if (docVerificationEnabled && requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                fragmentCameraBinding.showDocInstBtn.setVisibility(View.VISIBLE);
                docBtnHandler.removeCallbacks(docBtnRunnable);
                new Handler(Looper.getMainLooper()).postDelayed(this::showDocGuidelinesBottomSheet, 500);
            }

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/matchIdByCapture");
        }
    }

    /**
     * Transition to capture the front of the second document.
     * When perform_full_kyc is ON and no type selected yet → type selection screen.
     * Otherwise → camera opens directly, guidelines shown inside startDocTwoCameraCapture.
     */
    private void transitionToDocTwoFrontCapture() {
        try {
            currentState = "docTwoDetection";

            if (requestModel.getConfigObject().optBoolean("perform_full_kyc", false)
                    && !requestModel.getConfigObject().has("documentTwoType")) {
                // Type not yet selected: go to type selection screen first
                showKycDocTwoTypeFragment();
                return;
            }

            // Type already set (or non-full-kyc): open camera (guidelines will show on open)
            startDocTwoCameraCapture();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/transitionToDocTwoFrontCapture");
        }
    }

    /**
     * Sets up the camera for doc two front capture.
     * Called directly (non-full-kyc) or after type selection (full-kyc).
     * Shows guidelines when camera opens — identical timing to matchIdByCapture.
     */
    private void startDocTwoCameraCapture() {
        try {
            currentState = "docTwoDetection";
            fragmentCameraBinding.animationView.loadUrl("");
            docBtnHandler.postDelayed(docBtnRunnable, TimeConstants.SHOW_CAPTURE_DOC_BTN_DELAY);
            cardDetectionTflite = new CardDetectionTflite();
            if (cameraX != null) {
                helperFunctions.tapToFocus(cameraX.getCameraControl());
            }
            initCamera(false);

            fragmentCameraBinding.hidePreviewView.setVisibility(View.VISIBLE);
            fragmentCameraBinding.docInstParentLayout.setVisibility(View.VISIBLE);

            // Instruction text: use documentTwoType if full-kyc, else generic
            if (requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                try {
                    DocumentType docTwoType = DocumentType.valueOf(
                            requestModel.getConfigObject().getString("documentTwoType"));
                    if (docTwoType == DocumentType.PASSPORT) {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_passport_in_box);
                    } else if (docTwoType == DocumentType.DRIVING_LICENSE) {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_license_in_box);
                    } else {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_id_card_in_box);
                    }
                } catch (Exception e) {
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_doc_two_front_in_box);
                }
            } else {
                fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_doc_two_front_in_box);
            }

            // Upload label reflects doc two context
            fragmentCameraBinding.tryUploadPicTxt.setText(R.string.upload_doc_two);

            setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
            fragmentCameraBinding.skipBtn.setVisibility(View.GONE);

            if (!requestModel.getConfigObject().getBoolean("card_autocapture")) {
                fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);
                docBtnHandler.removeCallbacks(docBtnRunnable);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() ->
                    fragmentCameraBinding.docInstParentLayout.setVisibility(View.VISIBLE), 300);

            // Show guidelines when camera opens — same timing as doc one (matchIdByCapture)
            if (requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                fragmentCameraBinding.showDocInstBtn.setVisibility(View.VISIBLE);
                docBtnHandler.removeCallbacks(docBtnRunnable);
                new Handler(Looper.getMainLooper()).postDelayed(this::showDocTwoGuidelinesBottomSheet, 500);
            }

            detectCard();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/startDocTwoCameraCapture");
        }
    }

    /**
     * Transition to capture the back of the second document.
     * Reuses the same camera/card-detection infrastructure, only state and UI text differ.
     */
    private void transitionToDocTwoBackCapture() {
        currentState = "docTwoDetectionBack";
        setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);

            // Use documentTwoType for back instruction if available
            if (requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                try {
                    DocumentType docTwoType = DocumentType.valueOf(
                            requestModel.getConfigObject().getString("documentTwoType"));
                    if (docTwoType == DocumentType.DRIVING_LICENSE) {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_license_back_in_box);
                    } else {
                        fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_id_card_back_in_box);
                    }
                } catch (Exception e) {
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_doc_two_back_in_box);
                }
            } else {
                fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_doc_two_back_in_box);
            }

            // Update upload button label to reflect doc two context
            fragmentCameraBinding.tryUploadPicTxt.setText(R.string.upload_doc_two);

            fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);
            fragmentCameraBinding.skipBtn.setVisibility(View.VISIBLE);
            fragmentCameraBinding.skipBtn.setOnClickListener(v -> {
                requestModel.setDocTwoBackSkipped(true);
                requestModel.setDocTwoBackImage(null);
                fragmentCameraBinding.skipBtn.setVisibility(View.GONE);
                uploadCapturedImg();
            });
            detectCard();
        }, 300);
    }

    protected void detectCard() {
        try {
            if (!requestModel.getConfigObject().getBoolean("card_autocapture")) {
                fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);
                docBtnHandler.removeCallbacks(docBtnRunnable);
                return;
            }
            if (docGuidelinesDialog != null && docGuidelinesDialog.isShowing()) {
                new Handler().postDelayed(this::detectCard, 500);
                return;
            }
            if (!dialog.isShowing() && fragmentCameraBinding.imagePreviewParentLayout.getVisibility() != View.VISIBLE && !backPressed && !isCamStopped) {
                isCardDetectionInProcess = true;
                Bitmap bitmap = fragmentCameraBinding.previewView.getBitmap();
                if (bitmap != null) {
                    if ((Color.alpha(bitmap.getPixel(0, 0)) != 0 || fragmentCameraBinding.previewView.getPreviewStreamState()
                            .getValue().toString().equalsIgnoreCase("STREAMING")) &&
                            fragmentCameraBinding.docInstParentLayout.getVisibility() == View.GONE) {
                        fragmentCameraBinding.hidePreviewView.setVisibility(View.GONE);//android 14
                        fragmentCameraBinding.docInstParentLayout.setVisibility(View.VISIBLE);
                    }
                    new CameraFragment.DetectCardInBg(bitmap).execute();
                } else {
//                    if (isResumed){
                    new Handler().postDelayed(this::detectCard, 500);
                }
//                } else {
//                    detectCard(false);
//                }
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/detectCard");
        }
    }

    /**
     * Registers one document frame that passed all auto-capture checks (confident, inside the
     * guide box, large enough). Returns true only once the document has stayed valid for at least
     * CARD_STABILITY_MIN_FRAMES consecutive frames AND CARD_STABILITY_MIN_DURATION_MS, so the
     * camera/document has time to stabilize before capture (reduces blurry captures).
     */
    protected boolean registerStableCardFrame() {
        long now = System.currentTimeMillis();
        if (stableFrameCounter == 0) {
            firstStableFrameTime = now;
        }
        stableFrameCounter++;
        boolean enoughFrames = stableFrameCounter >= ThresholdConstants.CARD_STABILITY_MIN_FRAMES;
        boolean enoughTime = (now - firstStableFrameTime) >= ThresholdConstants.CARD_STABILITY_MIN_DURATION_MS;
        return enoughFrames && enoughTime;
    }

    /**
     * Resets the document auto-capture stability accumulator. Called whenever the document is no
     * longer valid (drifts out of the box / too small / not detected) and after a capture, so each
     * detection cycle starts fresh.
     */
    protected void resetCardStability() {
        stableFrameCounter = 0;
        firstStableFrameTime = 0L;
    }

    protected void convertAndDisplayCapturedCard(Bitmap bitmap) {
        resetCardStability();
        docBtnHandler.removeCallbacks(docBtnRunnable);
        new CameraFragment.ConvertBitmapToFileAndProcess(bitmap, false).execute();
        fragmentCameraBinding.detectedFrame.setImageBitmap(bitmap);
        setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE);
    }

    /**
     * method for the capture' sound
     */
    private void captureSound() {
        MediaActionSound sound = new MediaActionSound();
        sound.play(MediaActionSound.SHUTTER_CLICK);
    }


    private Boolean isImgGlow(Bitmap image) {
        int brightPixelCounter = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int pixel : pixels) {
            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);
            double pixelBrightness = (red + green + blue) / 3.0;
            if (pixelBrightness > 240) {
//            if (blue > 235 && red > 245 && green > 245) {
//            double pixelBrightness = (0.2126 * red) + (0.7152 * green) + (0.0722 * blue);
//            if (pixelBrightness > 225) {
                brightPixelCounter++;
                double percentage = 1.5;
                double expectedValue = (percentage / 100) * pixels.length;
                if (brightPixelCounter >= expectedValue) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isImgGlowAndReflected(Bitmap image, List<FaceContour> faceContourList) {
        int brightPixelCounter = 0;
        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int x = i % width;
            int y = i / width;
            int pixel = pixels[i];

            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);
            double pixelBrightness = (red + green + blue) / 3.0;
            double reflectance = Math.max(Math.max(red, green), blue) - Math.min(Math.min(red, green), blue);

            if ((pixelBrightness < (getFaceRect(faceContourList).contains(x, y) ? 10 : 40))) {
//            if ((pixelBrightness > (getFaceRect(faceContourList).contains(x, y) ? 245 : 230)) || reflectance > 7) {
                brightPixelCounter++;
                double percentage = 1.05;
                double expectedValue = (percentage / 100) * pixels.length;
                if (brightPixelCounter >= expectedValue) {
                    return true;
                }
            }
        }
        return false;
    }

    private Boolean isGlow(Bitmap grayscaleImage) {
        final int BOX_SIZE = 75; // Size of each box (120x120 pixels)
        int connectedBoxCounter = 0;
        List<String> rightArr = new ArrayList<>();
        List<String> bottomArr = new ArrayList<>();
//        Bitmap canvasBmp = grayscaleImage.copy(Bitmap.Config.ARGB_8888, true);
//        Canvas canvas = new Canvas(canvasBmp);

        int imageWidth = grayscaleImage.getWidth();
        int imageHeight = grayscaleImage.getHeight();

        int boxCountX = (imageWidth + BOX_SIZE - 1) / BOX_SIZE; // Number of boxes in x-axis
        int boxCountY = (imageHeight + BOX_SIZE - 1) / BOX_SIZE; // Number of boxes in y-axis

        for (int boxY = 0; boxY < boxCountY; boxY++) {
            for (int boxX = 0; boxX < boxCountX; boxX++) {
                // Calculate the starting pixel coordinates of the current box
                int startX = boxX * BOX_SIZE;
                int startY = boxY * BOX_SIZE;
                // Calculate the ending pixel coordinates of the current box
                int endX = Math.min(startX + BOX_SIZE, imageWidth);
                int endY = Math.min(startY + BOX_SIZE, imageHeight);
                // Perform function within the current box
                double totalBrightness = 0;
                for (int y = startY; y < endY; y++) {
                    for (int x = startX; x < endX; x++) {
                        int pixel = grayscaleImage.getPixel(x, y);
                        int red = Color.red(pixel);
                        int green = Color.green(pixel);
                        int blue = Color.blue(pixel);
                        double pixelBrightness = (red + green + blue) / 3.0;
                        totalBrightness = totalBrightness + pixelBrightness;
                    }
                }
                double boxAvgBrightness = totalBrightness / (BOX_SIZE * BOX_SIZE);
                if (boxAvgBrightness > 225) {
//                    Paint paint = new Paint();
//                    paint.setColor(Color.GREEN);
//                    paint.setStrokeWidth(8);
//                    paint.setStyle(Paint.Style.STROKE);
//                    canvas.drawRect(startX, startY, endX - 1, endY - 1, paint);
                    if (rightArr.contains((startX) + "," + startY + "," +
                            (startX) + "," + (endY)) ||
                            bottomArr.contains(startX + "," + (startY) + "," +
                                    (endX) + "," + (startY))) {
                        //box is connected with another
                        connectedBoxCounter++;
                        if (connectedBoxCounter > 0) {
                            //Glow/Light detected
                            return true;
                        }
                    } else {
                        rightArr.add((endX) + "," + startY + "," +
                                (endX) + "," + (endY));
                        bottomArr.add(startX + "," + endY + "," +
                                endX + "," + endY);
                    }
                }
            }
        }
        return false;
    }

    private Bitmap toGrayscale(Bitmap image) {
        int width = image.getWidth();
        int height = image.getHeight();

        Bitmap grayscaleImage = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(grayscaleImage);
        Paint paint = new Paint();
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(filter);
        canvas.drawBitmap(image, 0, 0, paint);

        return grayscaleImage;
    }

    /**
     * method to get Rect of face
     */
    private Rect getFaceRect(List<FaceContour> faceContourList) {
        return new Rect((int) (faceContourList.get(0).getPoints().get(28).x),
                (int) (faceContourList.get(0).getPoints().get(0).y),
                (int) (faceContourList.get(0).getPoints().get(8).x),
                (int) (faceContourList.get(0).getPoints().get(18).y));
    }

    /**
     * method to change bg color
     */
    protected void changeBgColor() {
        try {
            isBgChanging = true;
            int startColor, endColor;
            int backgroundColor = ((ColorDrawable) fragmentCameraBinding.faceViewTop.getBackground()).getColor();
            if (backgroundColor == getResources().getColor(R.color.facia_white_color)) {
                startColor = SingletonData.getInstance().getActivity().getResources().getColor(R.color.facia_white_color);
                endColor = SingletonData.getInstance().getActivity().getResources().getColor(R.color.facia_black_color);
            } else {
                startColor = SingletonData.getInstance().getActivity().getResources().getColor(R.color.facia_black_color);
                endColor = SingletonData.getInstance().getActivity().getResources().getColor(R.color.facia_white_color);
            }
            fragmentCameraBinding.faceViewTop.setBackgroundColor(startColor);

            ValueAnimator colorAnimator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);
            colorAnimator.setDuration(700);

            colorAnimator.addUpdateListener(animator -> {
                int animatedValue = (int) animator.getAnimatedValue();
                fragmentCameraBinding.faceViewTop.setBackgroundColor(animatedValue);
            });
            colorAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    isBgChanging = false;
                }
            });
            colorAnimator.start();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/HelperFunction/changeBgColor");
        }
    }

    private void transitionToAddressFrontCapture() {
        FaciaLogger.i(TAG, "transitionToAddressFrontCapture: Setting UI");
        currentState = "addressDetection";

        boolean isFullKyc = requestModel.getConfigObject().optBoolean("perform_full_kyc", false);

        if (cameraX == null) {
            cardDetectionTflite = new CardDetectionTflite();
            initCamera(false);
        }

        if (isFullKyc) {
            docBtnHandler.postDelayed(docBtnRunnable, TimeConstants.SHOW_CAPTURE_DOC_BTN_DELAY);

            fragmentCameraBinding.hidePreviewView.setVisibility(View.VISIBLE);
            fragmentCameraBinding.docInstParentLayout.setVisibility(View.VISIBLE);

            String addrType = requestModel.getConfigObject().optString("addressDocType", "");
            switch (addrType) {
                case "utility_bill":
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_utility_bill_in_box);
                    break;
                case "bank_statement":
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_bank_statement_in_box);
                    break;
                case "govt_letter":
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_govt_letter_in_box);
                    break;
                case "council_tax":
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_council_tax_in_box);
                    break;
                default:
                    fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_address_in_box);
                    break;
            }

            setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
            fragmentCameraBinding.skipBtn.setVisibility(View.GONE);
            fragmentCameraBinding.showDocInstBtn.setVisibility(View.VISIBLE);

            if (!requestModel.getConfigObject().optBoolean("card_autocapture", true)) {
                fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);
                docBtnHandler.removeCallbacks(docBtnRunnable);
            }

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                fragmentCameraBinding.docInstParentLayout.setVisibility(View.VISIBLE);
            }, 300);

            docBtnHandler.removeCallbacks(docBtnRunnable);
            new Handler(Looper.getMainLooper()).postDelayed(this::showDocGuidelinesBottomSheet, 500);

            detectCard();
        } else {
            setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
                fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_address_in_box);
                fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);
                fragmentCameraBinding.skipBtn.setVisibility(View.GONE);
                detectCard();
            }, 300);
        }
    }

    private void transitionToAddressBackCapture() {
        currentState = "addressDetectionBack";
        setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            setUi(View.VISIBLE, View.GONE, View.VISIBLE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE);
            fragmentCameraBinding.faceDocDetectInst.setText(R.string.fit_address_back_in_box);
            fragmentCameraBinding.captureDocBtn.setVisibility(View.VISIBLE);
            fragmentCameraBinding.skipBtn.setVisibility(View.VISIBLE);
            fragmentCameraBinding.skipBtn.setOnClickListener(v -> {
                requestModel.setAddressBackSkipped(true);
                requestModel.setAddressBackImage(null);
                uploadCapturedImg();
            });
            detectCard();
        }, 300);
    }

    private void finishVerificationAndSubmit() {
        currentState = "docVerificationComplete";
        fragmentCameraBinding.skipBtn.setVisibility(View.GONE);
        helperFunctions.handleCheckSimilarityWithDocVerification(similarityApiHelper);
    }

    /**
     * Async method to convert bitmap to file
     */
    private class ConvertBitmapToFileAndProcess {
        Bitmap bitmap = null;
        Boolean isQl = true;

        public ConvertBitmapToFileAndProcess(Bitmap bitmap, Boolean isQl) {
            this.bitmap = bitmap;
            this.isQl = isQl;
        }

        public void execute() {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                final File faceFileToProcess = doInBackground();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> onPostExecute(faceFileToProcess));
            });
        }

        protected File doInBackground() {
            if (isQl) {
                return convertBitmapToFile(bitmap, false);
            } else {
                return convertBitmapToFile(bitmap, true);
            }
        }

//        protected void onPostExecute(File fileToProcess) {
//            if (!isQl) {
//                requestModel.setIdImage(fileToProcess);
//            } else {
//                try {
//                    if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.MATCH_TO_PHOTO_ID) {
//                        requestModel.setFaceImage(fileToProcess);
//                    }
//                } catch (Exception ignored) {
//                }
//                try {
//                    livenessApiHelper.createRequestJsonObject(fileToProcess, "ql", currentState);
//                } catch (Exception e) {
//                    livenessApiHelper.createRequestJsonObject(fileToProcess, "ql", currentState);
//                }
//            }
//        }
//protected void onPostExecute(File fileToProcess) {
//    if (!isQl) {
//        requestModel.setIdImage(fileToProcess);
//    } else {
//        try {
//            if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.MATCH_TO_PHOTO_ID) {
//                requestModel.setFaceImage(fileToProcess);
//
//                matchIdLiveness = requestModel.getConfigObject().getBoolean("matchIdLiveness");
//
//                if (!matchIdLiveness) {
//                    matchIdByCapture();
//                    return;
//                }
//            }
//        } catch (Exception ignored) {
//        }
//        try {
//            livenessApiHelper.createRequestJsonObject(fileToProcess, "ql", currentState , clientReference , serviceType);
//        } catch (Exception e) {
//            livenessApiHelper.createRequestJsonObject(fileToProcess, "ql", currentState , clientReference , serviceType);
//        }
//    }
//
//}

        protected void onPostExecute(File fileToProcess) {
            if (!isAdded() || getContext() == null) return;
            try {
                if (isQl) {
                    if (ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) == ServiceType.MATCH_TO_PHOTO_ID) {
                        requestModel.setFaceImage(fileToProcess);
                        if (!requestModel.getConfigObject().getBoolean("matchIdLiveness")) {
                            if (docVerificationEnabled && requestModel.getConfigObject().optBoolean("perform_full_kyc", false)) {
                                showKycDocTypeFragment();
                            } else {
                                matchIdByCapture();
                            }
                            return;
                        }
                    }
                    livenessApiHelper.createRequestJsonObject(fileToProcess, "ql", currentState, clientReference, serviceType);
                    return;
                }

                if (docVerificationEnabled) {
                    switch (currentState) {
                        case "addressDetection":
                            requestModel.setAddressImage(fileToProcess);
                            break;
                        case "addressDetectionBack":
                            requestModel.setAddressBackImage(fileToProcess);
                            break;
                        case "docDetectionBack":
                            requestModel.setIdBackImage(fileToProcess);
                            break;
                        case "docTwoDetection":
                            requestModel.setDocTwoImage(fileToProcess);
                            break;
                        case "docTwoDetectionBack":
                            requestModel.setDocTwoBackImage(fileToProcess);
                            break;
                        case "docDetection":
                        default:
                            if (currentState.equals("docDetection")) {
                                requestModel.setIdImage(fileToProcess);
                            }
                            break;
                    }
                } else {
                    requestModel.setIdImage(fileToProcess);
                }

                isDocCaptured = true;
                setImageToPreview(this.bitmap);

                SingletonData.getInstance().getActivity().runOnUiThread(() ->
                        setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE)
                );

            } catch (Exception e) {
                FaciaLogger.e(TAG, "ON_POST_EXECUTE Error: " + e.getMessage());
            }
        }
    }

    /**
     * Async method to convert bitmap to base64 and to add in the list
     */
    private class BitmapToBase64 {
        Bitmap bitmap;

        public BitmapToBase64(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public void execute() {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                final String base64 = doInBackground();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> onPostExecute(base64));
            });
        }

        protected String doInBackground() {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        }

        protected void onPostExecute(String base64img) {
            //334 ms delay
            qlListCurrent = System.currentTimeMillis();
            if (SingletonData.getInstance().getQlFrameList().size() == 0) {
                qlListPrevious = System.currentTimeMillis();
                SingletonData.getInstance().getQlFrameList().add(base64img);
            } else if (qlListCurrent - qlListPrevious >= TimeConstants.ADD_IN_QL_LIST_DELAY &&
                    SingletonData.getInstance().getQlFrameList().size() < 9) {
                SingletonData.getInstance().getQlFrameList().add(base64img);
                qlListPrevious = System.currentTimeMillis();
            }
        }
    }

    /**
     * Async method to process frame in bg to detect card through TFLite model
     */
    private class DetectCardInBg {
        Bitmap bitmap;

        public DetectCardInBg(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        public void execute() {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                final Bitmap resultedBitmap = doInBackground();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> onPostExecute(resultedBitmap));
            });
        }

        protected Bitmap doInBackground() {
            return cardDetectionTflite.run(bitmap);
        }

        protected void onPostExecute(Bitmap resultedBitmap) {
            if (resultedBitmap == null) {
                detectCard();
            } else {
                helperFunctions.handleCardDetection(resultedBitmap);
            }
        }
    }

    private class TestPreview {
        public void execute() {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                final Boolean isDone = doInBackground();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> onPostExecute(isDone));
            });
        }

        protected Boolean doInBackground() {
            return fragmentCameraBinding.previewView.getPreviewStreamState()
                    .getValue().toString().equalsIgnoreCase("STREAMING");
        }

        protected void onPostExecute(Boolean isDone) {
            if (isDone) {
                fragmentCameraBinding.hidePreviewView.setVisibility(View.GONE);
            } else {
                new TestPreview().execute();
            }
        }
    }


    private class DetectLight {
        Bitmap bitmap;
        Face face;

        public DetectLight(Bitmap bitmap, Face face) {
            this.bitmap = bitmap;
            this.face = face;
        }

        public void execute() {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                final Boolean isGlow = doInBackground();
                Handler mainHandler = new Handler(Looper.getMainLooper());
                mainHandler.post(() -> onPostExecute(isGlow));
            });
        }

        protected Boolean doInBackground() {
//            return isGlow(toGrayscale(bitmap));
//            return isImgGlow(toGrayscale(bitmap));
            return isImgGlowAndReflected(toGrayscale(bitmap), face.getAllContours());
        }

        protected void onPostExecute(Boolean isGlow) {
//            if (currentState.equals("QL")) {
            if (isGlow) {
//                SingletonData.getInstance().setQuickLivenessFrameCount(0);
//                fragmentCameraBinding.quickLivenessInstTxt.setText(getResources().getString(R.string.improper_lighting));
//                fragmentCameraBinding.quickLivenessInst.setVisibility(View.VISIBLE);
//                frameProcessed();

                SingletonData.getInstance().setQuickLivenessFrameCount(0);
//                SingletonData.getInstance().getFragmentCameraBinding().faceBorder.setBackgroundResource(
//                        R.drawable.face_border_bg_red);
                SingletonData.getInstance().getFragmentCameraBinding().faceDetectInst.setText(R.string.improper_lighting);
                frameProcessed();
            } else {
//                fragmentCameraBinding.quickLivenessInstTxt.setText("No Light Detected");
                faceDetectionHelper.setFaceMeshResult(face, bitmap, false);
            }
//            }else {
//                if (!isBgChanging) {
//                    changeBgColor();
//                }
//                faceDetectionHelper.setFaceMeshResult(face);
//            }
        }
    }
}
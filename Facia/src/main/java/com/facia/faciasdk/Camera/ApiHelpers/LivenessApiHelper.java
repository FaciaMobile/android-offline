package com.facia.faciasdk.Camera.ApiHelpers;

import com.facia.faciasdk.Utils.FaciaLogger;

import android.os.Handler;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.facia.faciasdk.Activity.Helpers.Enums.ServiceType;
import com.facia.faciasdk.Activity.Helpers.IntentHelper;
import com.facia.faciasdk.Activity.Helpers.RequestModel;
import com.facia.faciasdk.ApiModels.CreateTransaction.CreateTransaction;
import com.facia.faciasdk.ApiModels.LivenessRequest.LivenessRequest;
import com.facia.faciasdk.ApiModels.Result.Result;
import com.facia.faciasdk.ApiModels.UploadQlVideo.UploadQlVideo;
import com.facia.faciasdk.Utils.AppColors;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.TrialEnd.TrialEndFragment;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.facia.faciasdk.Utils.Utilities;
import com.facia.faciasdk.databinding.FragmentCameraBinding;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class LivenessApiHelper {
    private final FragmentCameraBinding fragmentCameraBinding;
    private final String token;
    private final AppColors appColors;

    private boolean showDeclineReason;


    public LivenessApiHelper(FragmentCameraBinding fragmentCameraBinding, String token , boolean showDeclineReason, AppColors appColors) {
        this.fragmentCameraBinding = fragmentCameraBinding;
        this.token = token;
        this.showDeclineReason = showDeclineReason;
        this.appColors = appColors;
    }

    /**
     * method to set request object to upload video
     * for liveness request
     */
    public void createRequestJsonObject(File detectedFaceFile, String type, String currentState , String clientReference , String serviceType) {
        try {
            ServiceType currentService = ServiceType.valueOf(serviceType);
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            builder.addFormDataPart("type", "liveness")
                    .addFormDataPart("file_type", type.equals("dl") ? "video" : "photo")
                    .addFormDataPart("is_proof_captured", "1");
            if (clientReference != null) {
                clientReference = clientReference.trim();
                if (!clientReference.isEmpty() && !clientReference.matches(" ") && currentService != ServiceType.MATCH_TO_PHOTO_ID) {
                    builder.addFormDataPart("client_reference", clientReference);
                }
            }
            builder.addFormDataPart("device_id", Utilities.SimilarMethods.getDeviceId())
                    .addFormDataPart("file", detectedFaceFile.getName(), RequestBody.create(MultipartBody.FORM, detectedFaceFile));
            RequestBody requestBody = builder.build();
            if (Utilities.SimilarMethods.isConnected()) {
                createRequestApi(requestBody, detectedFaceFile, type, currentState);
            } else {
                Utilities.SimilarMethods.internetDialog(appColors);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Result/ApiHelper/createRequestJsonObject");
        }
    }

    /**
     * method to call api to upload video
     *
     * @param requestBody request body to be uploaded
     */
    private void createRequestApi(RequestBody requestBody, File detectedFaceFile, String type, String currentState) {
        try {
            RequestModel requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
            SingletonData.getInstance().getApiInterface().createTransaction(Utilities.SimilarMethods.bearerToken(token),
                            Utilities.SimilarMethods.getDeviceDetails(), "android", requestBody)
                    .enqueue(new retrofit2.Callback<CreateTransaction>() {
                        @Override
                        public void onResponse(retrofit2.Call<CreateTransaction> call, Response<CreateTransaction> response) {
                            FaciaLogger.d("FaciaSDK-API", "createTransaction onResponse: code=" + response.code() + ", isSuccessful=" + response.isSuccessful() + ", body=" + FaciaLogger.responseBody(response));
                            if (response.isSuccessful()) {
                                try {
                                    if (!(ServiceType.valueOf(requestModel.getConfigObject().getString("serviceType")) ==
                                            ServiceType.MATCH_TO_PHOTO_ID || currentState.equals("QL"))) {
                                        detectedFaceFile.delete();
                                    }
                                    if(response.body().getResult().getData().getReferenceId() == null ||
                                    response.body().getResult().getData().getReferenceId().isEmpty()){
                                        Webhooks.testingValues("Reference ID is null or empty and response.isSuccessful = " +response.isSuccessful());
                                    }
                                    SingletonData.getInstance().setReferenceId(
                                            response.body().getResult().getData().getReferenceId());
                                    handleCreateRequestSuccess(type);
                                } catch (Exception e) {
                                    Webhooks.exceptionReport(e, "Camera/LivenessApiHelper/createRequestApi/inner-success");
                                    handleCreateRequestSuccess(type);
                                }
                            } else {
                                detectedFaceFile.delete();
                                String errorStr = "";
                                // full error response body, forwarded as-is to the webhook
                                String fullResponse = "";
                                try {
                                    String errorBody = response.errorBody().string();
                                    fullResponse = errorBody;
                                    JSONObject jsonObject = new JSONObject(errorBody);
                                    // if the response carries a "message", use it as the displayed error
                                    String errorMessage = jsonObject.optString("message", "");

                                    if (!errorMessage.isEmpty() && jsonObject.has("errors")) {
                                        JSONObject errorsObject = jsonObject.getJSONObject("errors");
                                        if (errorsObject.has("client_reference")) {
                                            JSONArray clientRefErrors = errorsObject.getJSONArray("client_reference");
                                            if (clientRefErrors.length() > 0) {
                                                errorStr = errorMessage + ": " + clientRefErrors.getString(0);
                                            } else {
                                                errorStr = errorMessage;
                                            }
                                        } else {
                                            errorStr = errorMessage;
                                        }
                                    } else {
                                        errorStr = errorMessage;
                                    }

                                    handleCreateRequestError(errorStr.contains("limit"), errorStr);
                                } catch (Exception e) {
                                    handleCreateRequestError(false, "");
                                }
                                try {
                                    // send the full response body to the webhook (falls back to the parsed message)
                                    Webhooks.apiReport("Create Transaction, Unsuccessful: " + response.code(), fullResponse.isEmpty() ? errorStr : fullResponse, ApiConstants.LIVENESS);
                                } catch (Exception e) {
                                    Webhooks.apiReport("Create Transaction, Unsuccessful: " + response.code(), "Response not successful", ApiConstants.LIVENESS);
                                }
                            }
                            SingletonData.getInstance().setQuickRequestInProcess(false);
                        }

                        @Override
                        public void onFailure(retrofit2.Call<CreateTransaction> call, Throwable t) {
                            FaciaLogger.e("FaciaSDK-API", "createTransaction onFailure: " + (t != null ? t.getMessage() : "null"), t);
                            detectedFaceFile.delete();
                            try {
                                handleCreateRequestFailure(t.getMessage());
                            }catch (Exception e){
                                handleCreateRequestFailure("");
                            }
                        }
                    });
        } catch (Exception e) {
            detectedFaceFile.delete();
            Webhooks.exceptionReport(e, "Camera/LivenessApiHelper/createRequestApi");
        }
    }

    private void uploadFrameListApi(JsonObject jsonObject) {
        try {
            SingletonData.getInstance().getApiInterface().uploadFrameList(Utilities.SimilarMethods.bearerToken(token),
                            Utilities.SimilarMethods.getDeviceDetails(), "android", jsonObject)
                    .enqueue(new retrofit2.Callback<UploadQlVideo>() {
                        @Override
                        public void onResponse(retrofit2.Call<UploadQlVideo> call, Response<UploadQlVideo> response) {
                            FaciaLogger.d("FaciaSDK-API", "uploadFrameList onResponse: code=" + response.code() + ", isSuccessful=" + response.isSuccessful() + ", body=" + FaciaLogger.responseBody(response));
                            try {
                                if (!response.isSuccessful()) {
                                    Webhooks.apiReport("LivenessApiHelper/uploadFrameListApi (upload frames), Unsuccessful: " + response.code(), response.errorBody().string(), ApiConstants.UPLOAD_FRAMES);
                                }
                            } catch (Exception ignored) {

                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<UploadQlVideo> call, Throwable t) {
                            FaciaLogger.e("FaciaSDK-API", "uploadFrameList onFailure: " + (t != null ? t.getMessage() : "null"), t);
                            try {
                                Webhooks.apiReport("Create Transaction (upload frames), Failure", t.getMessage(), ApiConstants.LIVENESS);
                            } catch (Exception ignored) {

                            }
                        }
                    });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/LivenessApiHelper/uploadFrameListApi");
        }
    }

    /**
     * method to set request object to upload video
     * for liveness request
     *
     * @param videoFile video file to be uploaded
     */
    public void livenessRequestObject(File videoFile) {
        try {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            builder.addFormDataPart("reference_id", SingletonData.getInstance().getReferenceId())
                    .addFormDataPart("merchant_redirect_url", "www.google.com")
                    .addFormDataPart("file", videoFile.getName(), RequestBody.create(MultipartBody.FORM, videoFile));
            RequestBody requestBody = builder.build();
            if (Utilities.SimilarMethods.isConnected()) {
                livenessRequestApi(requestBody, videoFile);
            } else {
                Utilities.SimilarMethods.internetDialog(appColors);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/LivenessApiHelper/livenessRequestRequestObject");
        }
    }

    /**
     * method to call api to upload video
     *
     * @param requestBody request body to be uploaded
     * @param videoFile   file to be uploaded (getting instance to delete file later)
     */
    private void livenessRequestApi(RequestBody requestBody, File videoFile) {
        try {
            SingletonData.getInstance().getApiInterface().livenessRequest(Utilities.SimilarMethods.bearerToken(token),
                    Utilities.SimilarMethods.getDeviceDetails(), "android", requestBody).enqueue(new retrofit2.Callback<LivenessRequest>() {
                @Override
                public void onResponse(retrofit2.Call<LivenessRequest> call, Response<LivenessRequest> response) {
                    FaciaLogger.d("FaciaSDK-API", "livenessRequest onResponse: code=" + response.code() + ", isSuccessful=" + response.isSuccessful() + ", body=" + FaciaLogger.responseBody(response));
                    videoFile.delete();
                    if (response.isSuccessful()) {
                        livenessRequestSuccess();
                    } else {
                        // read the error body once: used both to display the message and to report to the webhook
                        String fullResponse = "";
                        String errorStr = "";
                        try {
                            fullResponse = response.errorBody().string();
                            errorStr = new JSONObject(fullResponse).optString("message", "");
                        } catch (Exception ignored) {
                        }
                        livenessRequestError(errorStr);
                        Webhooks.apiReport("Check Liveness, Unsuccessful: " + response.code(), fullResponse.isEmpty() ? "Response not successful" : fullResponse, ApiConstants.CHECK_LIVENESS);
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<LivenessRequest> call, Throwable t) {
                    FaciaLogger.e("FaciaSDK-API", "livenessRequest onFailure: " + (t != null ? t.getMessage() : "null"), t);
                    videoFile.delete();
                    try {
                        livenessRequestFailure(t.getMessage());
                    }catch (Exception e){
                        livenessRequestFailure("");
                    }
                }
            });
        } catch (Exception e) {
            videoFile.delete();
            Webhooks.exceptionReport(e, "LiveDetection/RequestApiHelper/livenessRequestApi");
        }
    }

    /**
     * method to set json object to call API to get result
     */
    private void getResultJsonObject() {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("reference_id", SingletonData.getInstance().getReferenceId());
            if (Utilities.SimilarMethods.isConnected()) {
                SingletonData.getInstance().setResultApiCount(0);
                getResultApi(jsonObject);
            } else {
                Utilities.SimilarMethods.internetDialog(appColors);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Result/ApiHelper/getResultJsonObject");
        }
    }

    /**
     * method to call api to get result of video uploaded (liveness verified or not)
     *
     * @param jsonObject json object to call API
     */
    private void getResultApi(JsonObject jsonObject) {
        try {
            SingletonData.getInstance().setResultApiCount(SingletonData.getInstance().getResultApiCount() + 1);
            SingletonData.getInstance().getApiInterface().getResult(Utilities.SimilarMethods.bearerToken(
                            token), Utilities.SimilarMethods.getDeviceDetails(), "android", jsonObject)
                    .enqueue(new retrofit2.Callback<Result>() {
                        @Override
                        public void onResponse(retrofit2.Call<Result> call, Response<Result> response) {
                            FaciaLogger.d("FaciaSDK-API", "getResult onResponse: code=" + response.code() + ", isSuccessful=" + response.isSuccessful() + ", body=" + FaciaLogger.responseBody(response));
                            if (!SingletonData.getInstance().isCameraBackPressed()) {
                                if (response.isSuccessful()) {
                                    if (SingletonData.getInstance().isQuickLiveness()) {
                                        handleQLResponse(response, jsonObject);
                                    } else {
                                        handleDLResponse(response, jsonObject);
                                    }
                                } else {
                                    // read the error body once: used both to display the message and to report to the webhook
                                    String fullResponse = "";
                                    String errorStr = "";
                                    try {
                                        fullResponse = response.errorBody().string();
                                        errorStr = new JSONObject(fullResponse).optString("message", "");
                                    } catch (Exception ignored) {
                                    }
                                    // show the API error message when present, otherwise the generic fallback
                                    String displayText = (errorStr != null && !errorStr.trim().isEmpty())
                                            ? errorStr
                                            : SingletonData.getInstance().getActivity().getString(R.string.went_wrong);
                                    SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(fragmentCameraBinding.animationView,
                                            "facia_failure.svg", displayText, "error.occurred", 0.0, true, "", errorStr);
                                    // send the full response body to the webhook
                                    Webhooks.apiReport("Liveness Result, Unsuccessful: " + response.code(), fullResponse.isEmpty() ? "Response not successful" : fullResponse, ApiConstants.LIVENESS_RESULT);
                                }
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<Result> call, Throwable t) {
                            FaciaLogger.e("FaciaSDK-API", "getResult onFailure: " + (t != null ? t.getMessage() : "null"), t);
                            if (!SingletonData.getInstance().isCameraBackPressed()) {
                                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(fragmentCameraBinding.animationView,
                                        "facia_failure.svg", SingletonData.getInstance().getActivity().getString(R.string.went_wrong), "error.occurred", 0.0, true, "", "");
                                Webhooks.apiReport("Liveness Result, Failure", t.getMessage(), ApiConstants.LIVENESS_RESULT);
                            }
                        }
                    });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Result/ApiHelper/getResultApi");
        }
    }

    /**
     * method to handle quick liveness response
     *
     * @param response   response object got from API result
     * @param jsonObject object to send to API to get result
     */
    private void handleQLResponse(Response<Result> response, JsonObject jsonObject) {
        try {
            if (response.body().getResult().getData().getQuickLivenessResponse() != null) {
                if (response.body().getResult().getData().getQuickLivenessResponse() == 1) {
                    SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                            fragmentCameraBinding.animationView, "facia_success.svg",
                            SingletonData.getInstance().getActivity().getString(R.string.verified), "liveness.verified", 0.0, true, "","");
                } else {
                    String declineReason = response.body().getResult().getData().getDeclineReason();
                    if(declineReason == null || declineReason.isEmpty()){
                        Webhooks.testingValues("Decline Reason Value in QL : "+ declineReason);
                    }
                    qlNotVerified(declineReason);}
            } else {
                if (SingletonData.getInstance().getResultApiCount() == 30) {
                    SingletonData.getInstance().setResultApiCount(0);
                    Webhooks.apiReport("Passive Liveness Result: " + response.code(), "Response not found, API called 30 times.", ApiConstants.LIVENESS_RESULT);
                }
                getResultApi(jsonObject);
//                else {

//                }
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleQLSuccessResponse");
        }
    }

    /**
     * method to handle UI if liveness not verified in case of QL
     */
//    private void qlNotVerified(String declineReason) {
//        try {
//            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
//                    fragmentCameraBinding.animationView, "facia_failure.svg",
//                    SingletonData.getInstance().getActivity().getString(R.string.unverified), "liveness.unverified", 0.0, true);
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "Camera/ApiHelper/qlNotVerified");
//        }
//    }
    private void qlNotVerified(String declineReason) {
        try {
            String messageToShow;
            if (showDeclineReason && declineReason != null && !declineReason.isEmpty()) {
                messageToShow = declineReason;
            } else {
                messageToShow = "";
            }

            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                    fragmentCameraBinding.animationView, "facia_failure.svg",
                    SingletonData.getInstance().getActivity().getString(R.string.unverified) , "liveness.unverified", 0.0, true, messageToShow,"");
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/qlNotVerified");
        }
    }

    /**
     * method to handle detail liveness response
     *
     * @param response   response object got from API result
     * @param jsonObject object to send to API to get result
     */
    private void handleDLResponse(Response<Result> response, JsonObject jsonObject) {
        try {
            if (response.body().getResult().getData().getIsRequestOriginal() != null) {
                if (response.body().getResult().getData().getIsRequestOriginal() == 1) {
                    SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(fragmentCameraBinding.animationView,
                            "facia_success.svg", SingletonData.getInstance().getActivity().getString(R.string.verified), "liveness.verified", 0.0, true, "","");
                } else if (response.body().getResult().getData().getIsRequestOriginal() == 0) {
                    String declineReason = response.body().getResult().getData().getDeclineReason();
                    if(declineReason == null || declineReason.isEmpty()){
                        Webhooks.testingValues("Decline Reason Value in DL : "+ declineReason);}
                    dlNotVerified(declineReason);
                }
                Webhooks.testingValues("Got Result on api called: " + SingletonData.getInstance().getResultApiCount());
            } else {
                if (SingletonData.getInstance().getResultApiCount() == 30) {
//                    SingletonData.getInstance().setResultApiCount(0);
                    Webhooks.apiReport("Active Liveness Result: " + response.code(), "Response not found, API called 30 times.", ApiConstants.LIVENESS_RESULT);
                }
                getResultApi(jsonObject);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleDLSuccessResponse");
        }
    }

    /**
     * method to handle UI if liveness not verified in case of DL
     */
//    private void dlNotVerified() {
//        try {
//            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(fragmentCameraBinding.animationView,
//                    "facia_failure.svg", SingletonData.getInstance().getActivity().getString(R.string.unverified), "liveness.unverified", 0.0, true);
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "Camera/ApiHelper/dlNotVerified");
//        }
//    }

    private void dlNotVerified(String declineReason ) {
        try {
            String messageToShow;
            if (showDeclineReason && declineReason != null && !declineReason.isEmpty()) {
                messageToShow = declineReason;
            } else {
                messageToShow = "";
            }
            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                    fragmentCameraBinding.animationView, "facia_failure.svg",
                    SingletonData.getInstance().getActivity().getString(R.string.unverified) , "liveness.unverified", 0.0, true,messageToShow,"");
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/qlNotVerified");
        }
    }

    /**
     * method to handle the success response of Create Request API
     *
     * @param type is QL, DL or checkSimilarity flow' QL
     */
    private void handleCreateRequestSuccess(String type) {
        try {
            if (type.equals("dl")) {
                new Handler().postDelayed(() -> {
                    SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                            View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
                    SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                            fragmentCameraBinding.animationViewUpload, "facia_loader.svg", "", "", 0.0, false, "","");
                }, TimeConstants.UPLOADER_GONE_EXTRA_TIME);
                new Handler().postDelayed(this::getResultJsonObject, TimeConstants.RESULT_API_DELAY_ONCE);
            } else {
                //bg video
                new uploadFrameListInBg().execute();
                getResultJsonObject();
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleCreateRequestSuccess");
        }
    }

    //    private class uploadVideoInBg {
//        public void execute() {
//            Executor executor = Executors.newSingleThreadExecutor();
//            executor.execute(this::doInBackground);
//        }
//
//        protected void doInBackground() {
//            File qlVideoFile = new File(SingletonData.getInstance().getQlVideoPath());
//            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
//            builder.addFormDataPart("reference_id", SingletonData.getInstance().getReferenceId())
//                    .addFormDataPart("file_type", "ref_video")
//                    .addFormDataPart("file", qlVideoFile.getName(), RequestBody.create(MultipartBody.FORM, qlVideoFile));
//            RequestBody requestBody = builder.build();
//            uploadQlVideo(requestBody, qlVideoFile);
//        }
//    }
    private class uploadFrameListInBg {
        public void execute() {
            Executor executor = Executors.newSingleThreadExecutor();
            executor.execute(this::doInBackground);
        }

        protected void doInBackground() {
            try {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("reference_id", SingletonData.getInstance().getReferenceId());
//                jsonObject.addProperty("file_type", "proofs");
                jsonObject.addProperty("proof_type","video_frames");
                jsonObject.addProperty("device_id", Utilities.SimilarMethods.getDeviceId());
//                jsonObject.add("file", SingletonData.getInstance().getQlFrameList());
                jsonObject.add("proof_frames",SingletonData.getInstance().getQlFrameList());
                uploadFrameListApi(jsonObject);
            } catch (Exception e) {
                Webhooks.exceptionReport(e, "LivenessApiHelper/uploadFrameListInBg");
            }
        }
    }


    /**
     * method to handle the un success response of Create Request API
     */
    private void handleCreateRequestError(Boolean isLimitExceeded, String errorStr) {
        try {
            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                    View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
            if (isLimitExceeded) {
                RequestModel requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
                if (requestModel.getConfigObject().getBoolean("isDemoApp")){
                    navigateToTrialEnd();
                }else {
                    SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                            fragmentCameraBinding.animationView, "facia_failure.svg",
                            errorStr, "error.occurred", 0.0, true, "","");
                }
            } else {
                // show the API error message when present, otherwise the generic fallback
                String displayText = (errorStr != null && !errorStr.trim().isEmpty())
                        ? errorStr
                        : SingletonData.getInstance().getActivity().getString(R.string.went_wrong);
                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                        fragmentCameraBinding.animationView, "facia_failure.svg",
                        displayText, "error.occurred", 0.0, true, "","");
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleCreateRequestError");
        }
    }

    /**
     * method to navigate to trial end screen
     */
    private void navigateToTrialEnd() {
        try {
            Fragment trialEndFragment = new TrialEndFragment();
            SingletonData.getInstance().getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.enter_from_right, R.anim.exit_from_left)
                    .replace(R.id.nav_host_fragment, trialEndFragment, trialEndFragment.getClass().getSimpleName())
                    .addToBackStack(null)
                    .commitAllowingStateLoss();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "GreetingFragment/navigateToCameraScreen");
        }
    }

    /**
     * method to handle the failure of Create Request API
     */
    private void handleCreateRequestFailure(String error) {
        try {
            SingletonData.getInstance().setQuickRequestInProcess(false);
            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                    View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                    fragmentCameraBinding.animationView, "facia_failure.svg",
                    SingletonData.getInstance().getActivity().getString(R.string.went_wrong), "error.occurred", 0.0, true, "","");
            Webhooks.apiReport("Create Transaction, Failure" , error, ApiConstants.LIVENESS);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleCreateRequestFailure");
        }
    }

    /**
     * method to handle the success response of Liveness Request API
     */
    private void livenessRequestSuccess() {
        try {
            new Handler().postDelayed(() -> {
                SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                        View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                        fragmentCameraBinding.animationViewUpload, "facia_loader.svg", "", "", 0.0, false, "","");
            }, TimeConstants.UPLOADER_GONE_EXTRA_TIME);
            new Handler().postDelayed(this::getResultJsonObject, TimeConstants.RESULT_API_DELAY_ONCE);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/livenessRequestSuccess");
        }
    }

    /**
     * method to handle the un success response of Liveness Request API
     */
    private void livenessRequestError(String errorStr) {
        try {
            SingletonData.getInstance().setQuickRequestInProcess(false);
            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                    View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
            // show the API error message when present, otherwise the generic fallback
            String displayText = (errorStr != null && !errorStr.trim().isEmpty())
                    ? errorStr
                    : SingletonData.getInstance().getActivity().getString(R.string.went_wrong);
            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                    fragmentCameraBinding.animationView, "facia_failure.svg",
                    displayText, "error.occurred", 0.0, true, "", errorStr == null ? "" : errorStr);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/livenessRequestError");
        }
    }

    /**
     * method to handle the failure of Liveness Request API
     */
    private void livenessRequestFailure(String error) {
        try {
            SingletonData.getInstance().setQuickRequestInProcess(false);
            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                    View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                    fragmentCameraBinding.animationView, "facia_failure.svg",
                    SingletonData.getInstance().getActivity().getString(R.string.went_wrong), "error.occurred", 0.0, true, "","");
            Webhooks.apiReport("Check Liveness, Failure" , error, ApiConstants.CHECK_LIVENESS);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/livenessRequestFailure");
        }
    }
}
package com.facia.faciasdk.Camera.ApiHelpers;

import android.util.Log;

import com.facia.faciasdk.Utils.FaciaLogger;
import android.view.View;

import androidx.fragment.app.Fragment;

import com.facia.faciasdk.Activity.Helpers.IntentHelper;
import com.facia.faciasdk.Activity.Helpers.RequestModel;
import com.facia.faciasdk.ApiModels.CheckSimilarity.CheckSimilarity;
import com.facia.faciasdk.ApiModels.PerformKYC.KycData;
import com.facia.faciasdk.ApiModels.PerformKYC.KycResponse;
import com.facia.faciasdk.ApiModels.UploadProgress.CountingRequestBody;
import com.facia.faciasdk.Utils.AppColors;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.TrialEnd.TrialEndFragment;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Utilities;
import com.facia.faciasdk.databinding.FragmentCameraBinding;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class SimilarityApiHelper {
    private final FragmentCameraBinding fragmentCameraBinding;
    private final AppColors appColors;
    private final String token;

    public SimilarityApiHelper(FragmentCameraBinding fragmentCameraBinding, String token, AppColors appColors) {
        this.fragmentCameraBinding = fragmentCameraBinding;
        this.token = token;
        this.appColors = appColors;
    }

    /**
     * method to set request object to upload files to check similarity
     * for liveness request
     */
    public void checkSimilarityJsonObject(File faceImage, File idImage, float similarityScore, Boolean isCaptured , String clientReference) {
        try {
            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            RequestBody faceImageRequestBody = new CountingRequestBody(RequestBody.create(MediaType.parse("image/*"), faceImage),
                    (bytesUploaded, totalBytes) -> {
                        if (bytesUploaded == totalBytes) {
                            SingletonData.getInstance().getCameraListeners().fileUploaded();
                        }
                    });
            RequestBody idImageRequestBody = new CountingRequestBody(RequestBody.create(MediaType.parse("image/*"), idImage),
                    (bytesUploaded, totalBytes) -> {
                        if (bytesUploaded == totalBytes) {
                            SingletonData.getInstance().getCameraListeners().fileUploaded();
                        }
                    });
            builder.addFormDataPart("type", "photo_id_match")
                    .addFormDataPart("face_frame", faceImage.getName(), faceImageRequestBody);
            if (clientReference != null) {
                clientReference = clientReference.trim();
                if (!clientReference.isEmpty() && !clientReference.matches(" ")) {
                    builder.addFormDataPart("client_reference", clientReference);
                }
            }
            builder.addFormDataPart("device_id", Utilities.SimilarMethods.getDeviceId())
                    .addFormDataPart("id_frame", idImage.getName(), idImageRequestBody);
            RequestBody requestBody = builder.build();
            if (Utilities.SimilarMethods.isConnected()) {
                checkSimilarityApi(requestBody, similarityScore, faceImage, idImage, isCaptured);
            } else {
                Utilities.SimilarMethods.internetDialog(appColors);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Result/ApiHelper/checkSimilarityJsonObject");
        }
    }

    /**
     * method to call api to upload video
     *
     * @param requestBody request body to be uploaded
     */
    private void checkSimilarityApi(RequestBody requestBody, float similarityScore, File faceFile, File docFile, Boolean isCaptured) {
        try {
            RequestModel requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
            SingletonData.getInstance().getApiInterface().checkSimilarity(Utilities.SimilarMethods.bearerToken(token),
                            Utilities.SimilarMethods.getDeviceDetails(), "android", requestBody)
                    .enqueue(new retrofit2.Callback<CheckSimilarity>() {
                        @Override
                        public void onResponse(retrofit2.Call<CheckSimilarity> call, Response<CheckSimilarity> response) {
                            FaciaLogger.d("FaciaSDK-API", "checkSimilarity onResponse: code=" + response.code() + ", isSuccessful=" + response.isSuccessful() + ", body=" + FaciaLogger.responseBody(response));
                            if (response.isSuccessful()) {
                                handleSimilaritySuccess(response, similarityScore, isCaptured);
                            } else {
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
                                    handleSimilarityError(response.code() == 400 && errorStr.contains("limit") , errorStr);
                                } catch (Exception e) {
                                    handleSimilarityError(false , errorStr);
                                }
                                try {
                                    // send the full response body to the webhook (falls back to the parsed message)
                                    Webhooks.apiReport("Create Transaction (photo_id_match), Unsuccessful: " + response.code(), fullResponse.isEmpty() ? errorStr : fullResponse, ApiConstants.FACE_MATCH);
                                } catch (Exception e) {
                                    Webhooks.apiReport("Create Transaction (photo_id_match), Unsuccessful: " + response.code(), "Response not successful", ApiConstants.FACE_MATCH);
                                }
                            }
                            SingletonData.getInstance().setQuickRequestInProcess(false);
                        }

                        @Override
                        public void onFailure(retrofit2.Call<CheckSimilarity> call, Throwable t) {
                            FaciaLogger.e("FaciaSDK-API", "checkSimilarity onFailure: " + (t != null ? t.getMessage() : "null"), t);
                            try {
                                handleSimilarityFailure(t.getMessage());
                            }catch (Exception e){
                                handleSimilarityFailure("");
                            }
                        }
                    });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Result/ApiHelper/checkSimilarityApi");
        }
    }

    /**
     * method to handle if check similarity' API's response is successful
     *
     * @param response        response of the API
     * @param similarityScore pre defined similarity score
     */
    private void handleSimilaritySuccess(Response<CheckSimilarity> response, float similarityScore, Boolean isCaptured) {
        try {
            SingletonData.getInstance().setReferenceId(
                    response.body().getResult().getData().getReferenceId());
            if(response.body().getResult().getData().getReferenceId() == null ||
            response.body().getResult().getData().getReferenceId().isEmpty()){
                Webhooks.testingValues("Reference ID is null or empty and response.isSuccessful = " +response.isSuccessful());
            }
            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                    View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);

            if (isCaptured) {
                if (response.body().getResult().getData().getSimilarityStatus() == 1) {
                    SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                            fragmentCameraBinding.animationView, "facia_success.svg",
                            SingletonData.getInstance().getActivity().getString(R.string.photo_id_matched), "photo_id.match_success",
                            response.body().getResult().getData().getSimilarityScore(), true, "","");
                } else {
                    SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                            fragmentCameraBinding.animationView, "facia_failure.svg",
                            SingletonData.getInstance().getActivity().getString(R.string.photo_id_not_matched), "photo_id.match_failure",
                            response.body().getResult().getData().getSimilarityScore(), true, "","");
                }
            } else if (response.body().getResult().getData().getSimilarityScore() >= similarityScore) {
                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                        fragmentCameraBinding.animationView, "facia_success.svg",
                        SingletonData.getInstance().getActivity().getString(R.string.photo_id_matched), "photo_id.match_success",
                        response.body().getResult().getData().getSimilarityScore(), true, "","");
            }
//            if (response.body().getResult().getData().getSimilarityScore() >= similarityScore ||
//                    (response.body().getStatus() == true && isCaptured)) {
//                SingletonData.getInstance().getFrameProcessing().setAnimationViewsAndText(
//                        fragmentCameraBinding.animationView, "facia_success.svg",
//                        SingletonData.getInstance().getActivity().getString(R.string.photo_id_matched), "photo_id.match_success",
//                        response.body().getResult().getData().getSimilarityScore());
//            }
            else {
                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                        fragmentCameraBinding.animationView, "facia_failure.svg",
                        SingletonData.getInstance().getActivity().getString(R.string.photo_id_not_matched), "photo_id.match_failure",
                        response.body().getResult().getData().getSimilarityScore(), true, "","");
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleSimilaritySuccess");
        }
    }

    /**
     * method to handle if failed to call check similarity' API successfully
     */
    private void handleSimilarityFailure(String error) {
        try {
            SingletonData.getInstance().setQuickRequestInProcess(false);
            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(fragmentCameraBinding.animationView,
                    "facia_failure.svg", SingletonData.getInstance().getActivity().getString(R.string.went_wrong),
                    "error.occurred", 0.0, true, "","");
            Webhooks.apiReport("Create Transaction (photo_id_match), Failure", error, ApiConstants.FACE_MATCH);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleSimilarityFailure");
        }
    }

    /**
     * method to handle if check similarity' API's response is not successful
     */
    private void handleSimilarityError(Boolean isLimitExceeded , String errorStr) {
        try {
            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                    View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);
            if (isLimitExceeded) {
                navigateToTrialEnd();
            }else {
                // show the API error message when present, otherwise the generic fallback
                String displayText = (errorStr != null && !errorStr.trim().isEmpty())
                        ? errorStr
                        : SingletonData.getInstance().getActivity().getString(R.string.went_wrong);
                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                        fragmentCameraBinding.animationView, "facia_failure.svg",
                        displayText, "error.occurred", 0.0, true, "" , errorStr);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleSimilarityError");
        }
    }

    /**
     * Returns the base64 data URI string with the correct MIME type prefix.
     * For PDF files: data:application/pdf;base64,...
     * For image files: data:image/jpeg;base64,...
     */
    private String getBase64DataUri(File file) throws IOException {
        String base64 = Utilities.SimilarMethods.fileToBase64(file);
        if (file.getName().toLowerCase().endsWith(".pdf")) {
            return "data:application/pdf;base64," + base64;
        }
        return "data:image/jpeg;base64," + base64;
    }

    /**
     * method to set request object for KYC with document verification
     */
    public void performKycJsonObject(File faceImage, File idFrontImage, File idBackImage,
                                     File docTwoFrontImage, File docTwoBackImage,
                                     File addressFrontImage, File addressBackImage,
                                     float similarityScore, String clientReference) {
        try {
            RequestModel requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);
            String faceBase64 = "data:image/jpeg;base64," + Utilities.SimilarMethods.fileToBase64(faceImage);
            String idFrontBase64 = getBase64DataUri(idFrontImage);

            String idBackBase64 = null;
            if (idBackImage != null) {
                idBackBase64 = getBase64DataUri(idBackImage);
            }

            // Convert doc two images if they exist
            String docTwoFrontBase64 = null;
            String docTwoBackBase64 = null;
            boolean hasDocTwoFront = docTwoFrontImage != null && docTwoFrontImage.exists();
            boolean hasDocTwoBack = docTwoBackImage != null && docTwoBackImage.exists();

            if (hasDocTwoFront) {
                docTwoFrontBase64 = getBase64DataUri(docTwoFrontImage);
            }
            if (hasDocTwoBack) {
                docTwoBackBase64 = getBase64DataUri(docTwoBackImage);
            }

            // Convert address images if they exist
            String addressFrontBase64 = null;
            String addressBackBase64 = null;
            boolean hasAddressFront = addressFrontImage != null && addressFrontImage.exists();
            boolean hasAddressBack = addressBackImage != null && addressBackImage.exists();

            if (hasAddressFront) {
                addressFrontBase64 = getBase64DataUri(addressFrontImage);
            }
            if (hasAddressBack) {
                addressBackBase64 = getBase64DataUri(addressBackImage);
            }

            boolean isDocAmlEnabled = requestModel.getConfigObject().optBoolean("doc_AML", false);

            JSONObject mainObject = new JSONObject();

            JSONArray servicesArray = new JSONArray();
            servicesArray.put("quick_liveness");
            servicesArray.put("document_ocr");
            servicesArray.put("document_verification");
            servicesArray.put("face_match");

            if (isDocAmlEnabled) {
                servicesArray.put("aml");
            }

            // Check if doc two verification is needed
           boolean docTwoVerificationEnabled = hasDocTwoFront || hasDocTwoBack;
//            if (docTwoVerificationEnabled) {
//                servicesArray.put("document_two");
//            }

            // Check if address verification is enabled
            boolean addressVerificationEnabled = (hasAddressFront || hasAddressBack);

            if (addressVerificationEnabled) {
                servicesArray.put("address_verification");
            }

            mainObject.put("services", servicesArray);

            JSONObject quickLivenessObject = new JSONObject();
            quickLivenessObject.put("file", faceBase64);
            mainObject.put("quick_liveness", quickLivenessObject);

            JSONObject documentObject = new JSONObject();
            documentObject.put("file", idFrontBase64);
            if (idBackBase64 != null) {
                documentObject.put("additional_file", idBackBase64);
            }

            // Add supported_types array
            JSONArray supportedTypesArray = new JSONArray();
            supportedTypesArray.put("id_card");
            supportedTypesArray.put("passport");
            supportedTypesArray.put("driving_license");
            documentObject.put("supported_types", supportedTypesArray);

            mainObject.put("document", documentObject);

            // Add document_two object if doc two images exist
            if (docTwoVerificationEnabled) {
                JSONObject documentTwoObject = new JSONObject();

                if (hasDocTwoFront) {
                    documentTwoObject.put("file", docTwoFrontBase64);
                }
                if (hasDocTwoBack) {
                    documentTwoObject.put("additional_file", docTwoBackBase64);
                }

                mainObject.put("document_two", documentTwoObject);
            }

            // Add address object if address images exist
            if (addressVerificationEnabled) {
                JSONObject addressObject = new JSONObject();

                if (hasAddressFront) {
                    addressObject.put("file", addressFrontBase64);
                }

                if (hasAddressBack) {
                    addressObject.put("additional_file", addressBackBase64);
                }

                mainObject.put("address", addressObject);
            }

            mainObject.put("device_id", Utilities.SimilarMethods.getDeviceId());

            if (clientReference != null) {
                clientReference = clientReference.trim();
                if (!clientReference.isEmpty() && !clientReference.matches(" ")) {
                    mainObject.put("client_reference", clientReference);
                }
            }

            // MARK: - Risk Profile
            if (requestModel.getConfigObject() != null && requestModel.getConfigObject().has("risk_profile")) {
                JSONObject riskProfile = requestModel.getConfigObject().optJSONObject("risk_profile");
                if (riskProfile != null) {
                    mainObject.put("risk_profile", riskProfile);
                }
            }

            // MARK: - AML
            if (requestModel.getConfigObject() != null && requestModel.getConfigObject().has("aml")) {
                JSONObject aml = requestModel.getConfigObject().optJSONObject("aml");
                if (aml != null) {
                    mainObject.put("aml", aml);
                }
            }

             String jsonPayload = mainObject.toString();

            RequestBody requestBody = RequestBody.create(
                    MediaType.parse("application/json"),
                    jsonPayload
            );

            CountingRequestBody countingRequestBody = new CountingRequestBody(requestBody,
                    (bytesUploaded, totalBytes) -> {
                        if (bytesUploaded == totalBytes) {
                            SingletonData.getInstance().getCameraListeners().fileUploaded();
                        }
                    });

            if (Utilities.SimilarMethods.isConnected()) {
                performKycApi(countingRequestBody, similarityScore, true);
            } else {
                Utilities.SimilarMethods.internetDialog(appColors);
            }

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/performKYCJsonObject");
        }
    }

    /**
     * method to call KYC API
     */
    private void performKycApi(RequestBody requestBody, float similarityScore, boolean isCaptured) {
        try {
            RequestModel requestModel = (RequestModel) IntentHelper.getInstance().getObject(ApiConstants.REQUEST_MODEL);

            SingletonData.getInstance().getApiInterface().performKyc(
                            Utilities.SimilarMethods.bearerToken(token),
                            Utilities.SimilarMethods.getDeviceDetails(),
                            "android",
                            requestBody)
                    .enqueue(new retrofit2.Callback<KycResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<KycResponse> call, Response<KycResponse> response) {
                            FaciaLogger.d("FaciaSDK-API", "performKyc onResponse: code=" + response.code() + ", isSuccessful=" + response.isSuccessful() + ", body=" + FaciaLogger.responseBody(response));

                            if (response.isSuccessful()) {
                                handleKycSuccess(response);
                            } else {

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

                                    handleKycError(response.code() == 400 && errorStr.contains("limit"), errorStr);
                                } catch (Exception e) {
                                    handleKycError(false, errorStr);
                                }

                                try {
                                    // send the full response body to the webhook (falls back to the parsed message)
                                    Webhooks.apiReport("Create Transaction (KYC), Unsuccessful: " + response.code(), fullResponse.isEmpty() ? errorStr : fullResponse, ApiConstants.PERFORM_KYC);
                                } catch (Exception e) {
                                    Webhooks.apiReport("Create Transaction (KYC), Unsuccessful: " + response.code(), "Response not successful", ApiConstants.PERFORM_KYC);
                                }
                            }

                            SingletonData.getInstance().setQuickRequestInProcess(false);
                        }

                        @Override
                        public void onFailure(retrofit2.Call<KycResponse> call, Throwable t) {
                            FaciaLogger.e("FaciaSDK-API", "performKyc onFailure: " + (t != null ? t.getMessage() : "null"), t);
                            try {
                                handleKycFailure(t.getMessage());
                            } catch (Exception e) {
                                handleKycFailure("");
                            }
                        }
                    });
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/performKycApi");
        }
    }


    /**
     * method to handle if KYC API's response is successful
     */
    private void handleKycSuccess(Response<KycResponse> response) {
        try {
            if (response.body() != null && Boolean.TRUE.equals(response.body().getStatus())) {
                KycData kycData = response.body().getResult().getData();
                SingletonData.getInstance().setReferenceId(kycData.getReferenceId());
                Webhooks.testingValues("Reference ID = " + kycData.getReferenceId());

                if (kycData.getReferenceId() == null || kycData.getReferenceId().isEmpty()) {
                    Webhooks.testingValues("Reference ID is null or empty and response.isSuccessful = " + response.isSuccessful());
                }

                SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                        View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);

                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                        fragmentCameraBinding.animationView, "facia_success.svg",
                        SingletonData.getInstance().getActivity().getString(R.string.transaction_created),
                        "kyc.request.submitted",
                        0.0, true, "", "");
            } else {
                // Failure case - status is false
                String errorMessage = response.body() != null ? response.body().getMessage() : "Unknown error";
                if (errorMessage == null || errorMessage.isEmpty()) {
                    Webhooks.testingValues("Error Message Value in KYC: " + errorMessage);
                }

                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                        fragmentCameraBinding.animationView, "facia_failure.svg",
                        SingletonData.getInstance().getActivity().getString(R.string.transaction_failed),
                        "kyc.transaction.failure",
                        0.0, true, "", errorMessage);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleKycSuccess");
        }
    }


    /**
     * method to handle if KYC API's response is not successful
     */
    private void handleKycError(Boolean isLimitExceeded, String errorStr) {
        try {
            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                    View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);

            if (isLimitExceeded) {
                navigateToTrialEnd();
            } else {
                // show the API error message when present, otherwise the generic fallback
                String displayText = (errorStr != null && !errorStr.trim().isEmpty())
                        ? errorStr
                        : SingletonData.getInstance().getActivity().getString(R.string.went_wrong);
                SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                        fragmentCameraBinding.animationView,
                        "facia_failure.svg",
                        displayText,
                        "error.occurred",
                        0.0,
                        true,
                        "",
                        errorStr
                );
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleKycError");
        }
    }


    /**
     * method to handle if failed to call KYC API successfully
     */
    private void handleKycFailure(String error) {
        try {
            SingletonData.getInstance().setQuickRequestInProcess(false);

            SingletonData.getInstance().getCameraListeners().setUi(View.GONE, View.GONE,
                    View.GONE, View.GONE, View.GONE, View.GONE, View.VISIBLE, View.GONE);

            SingletonData.getInstance().getCameraListeners().setAnimationViewsAndText(
                    fragmentCameraBinding.animationView,
                    "facia_failure.svg",
                    SingletonData.getInstance().getActivity().getString(R.string.went_wrong),
                    "error.occurred",
                    0.0,
                    true,
                    "",
                    ""
            );
            Webhooks.apiReport("Create Transaction (performKYC), Failure", error, ApiConstants.FACE_MATCH);

        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/ApiHelper/handleKycFailure");
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
            Webhooks.exceptionReport(e, "SimilarityApiHelper/navigateToTrialEnd");
        }
    }

}
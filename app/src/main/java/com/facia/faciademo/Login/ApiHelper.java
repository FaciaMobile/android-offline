package com.facia.faciademo.Login;

import static android.os.Build.VERSION.SDK_INT;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.widget.Toast;

import com.facia.faciademo.ApiCalling.ApiClient;
import com.facia.faciademo.ApiCalling.ApiInterface;
import com.facia.faciademo.ApiCalling.GetToken.GetToken;
import com.facia.faciademo.ErrorLogs;
import com.facia.faciademo.MainSetAndGetData;
import com.facia.faciademo.R;
import com.facia.faciademo.databinding.FragmentLoginBinding;
import com.facia.faciasdk.Activity.Helpers.Enums.DocumentType;
import com.facia.faciasdk.Activity.Helpers.Enums.FaceLivenessType;
import com.facia.faciasdk.Activity.Helpers.Enums.ServiceType;
import com.facia.faciasdk.Activity.Helpers.RequestListener;
import com.facia.faciasdk.FaciaAi;
import com.facia.faciasdk.Activity.Helpers.Enums.FaceDetectionThreshold;
import com.facia.faciasdk.Activity.Helpers.Enums.OvalSize;
import com.facia.faciasdk.Logs.Webhooks;
import com.google.gson.JsonObject;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import retrofit2.Response;

public class ApiHelper {
    private FragmentLoginBinding fragmentLoginBinding;
    private String selectedType;
    private String ovalSize;
    private Boolean photo_id_match;
    private DocumentType documentType;
    private String selectedOption;

    public ApiHelper(FragmentLoginBinding fragmentLoginBinding) {
        this.fragmentLoginBinding = fragmentLoginBinding;
    }

    /**
     * Show Progress Dialog During Api Calls
     **/
    public static void showProgressDialog(Activity parentActivity) {
        try {
//            hud = KProgressHUD.create(parentActivity).setStyle(
//                            KProgressHUD.Style.SPIN_INDETERMINATE)
//                    .setLabel("Please wait").setCancellable(false).setAnimationSpeed(2).setDimAmount(0.7f);
//            if (hud != null) {
//                if (!hud.isShowing()) {
//                    hud.show();
//                }
//            } else {
//                hud.show();
//            }
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "Login/ApiHelper/showProgressDialog");
        }
    }

    /**
     * Dismiss Progress Dialog After Api Calls
     **/
    public static void dismissProgressDialog(Activity parentActivity) {
        try {
//            if (hud != null)
//                parentActivity.runOnUiThread(() -> hud.dismiss());
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "Login/ApiHelper/dismissProgressDialog");
        }
    }

    /**
     * method to set json object to get token
     **/
    protected void getTokenJsonObject(Activity parentActivity, Context context, String email,
                                      String password) {
        try {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("email", email);
            jsonObject.addProperty("password", password);
            if (isConnected(context)) {
                showProgressDialog(parentActivity);
                getTokenApi(email, parentActivity, jsonObject);
            }
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "Login/ApiHelper/getTokenJsonObject");
        }
    }

    /**
     * Method to call api to get token
     *
     * @param JsonObject request object of api request body
     */
    private void getTokenApi(String email, Activity parentActivity, JsonObject JsonObject) {
        try {
            ApiInterface apiInterface = ApiClient.getClient().create(ApiInterface.class);
            apiInterface.getToken(JsonObject).enqueue(new retrofit2.Callback<GetToken>() {
                @Override
                public void onResponse(retrofit2.Call<GetToken> call, Response<GetToken> response) {
                    handleAPiSuccessResponse(response, parentActivity, email);
                }

                @Override
                public void onFailure(retrofit2.Call<GetToken> call, Throwable t) {
                    dismissProgressDialog(parentActivity);
                    if (t.getMessage() != null) {
                        ErrorLogs.apiReport(email, "Set token Api failure: " + t.getMessage(), "onFailure");
                    } else {
                        ErrorLogs.apiReport(email, "Set token Api failure", "onFailure");
                    }
                    Toast.makeText(parentActivity, R.string.went_wrong_app, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "Login/ApiHelper/getTokenApi");
        }
    }

    private File faceFile(Bitmap bmp) {
        File file = null;
        try {
            file = new File(MainSetAndGetData.getInstance().getActivity().getCacheDir(), "faceFrame.jpeg");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70 /*ignored for PNG*/, bos);
            byte[] bitmapData = bos.toByteArray();
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bitmapData);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/faceFile");
        }
        return file;
    }

    private File cardFile(Bitmap bmp) {
        File file = null;
        try {
            file = new File(MainSetAndGetData.getInstance().getActivity().getCacheDir(), "cardFrame.jpeg");
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 70 /*ignored for PNG*/, bos);
            byte[] bitmapData = bos.toByteArray();
            try {
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(bitmapData);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/cardFile");
        }
        return file;
    }


    /**
     * method to handle response if api is called successfully
     * if response is successful then calling Facia SDK's method
     * else displaying error
     *
     * @param response       response of GetToken API
     * @param parentActivity activity's instance
     * @param email          email of the user
     */
    private void handleAPiSuccessResponse(Response<GetToken> response, Activity parentActivity, String email) {
        try {
            if (response.isSuccessful()) {
                FaciaAi faciaAi = new FaciaAi();
                if (fragmentLoginBinding.serviceTypeRadioGroup.getCheckedRadioButtonId() == fragmentLoginBinding.matchIdOpt.getId()){
                    if (fragmentLoginBinding.matchIdThroughCam.isChecked()){
                        faciaAi.createRequest("", parentActivity, getconfig(), new RequestListener() {
                            @Override
                            public void requestStatus(HashMap<String, String> responseSet) {

                            }
                        });
                        faciaAi.createRequest(response.body().getResult().getData().getToken(), parentActivity, getconfig(),
                                responseSet ->
                                        Toast.makeText(parentActivity, responseSet.toString(), Toast.LENGTH_LONG).show());
                    }else {
//                        Bitmap faceBmp = BitmapFactory.decodeResource(MainSetAndGetData.getInstance().getActivity().getResources(),
//                                R.drawable.test_face);
//                        Bitmap docBmp = BitmapFactory.decodeResource(MainSetAndGetData.getInstance().getActivity().getResources(),
//                                R.drawable.test_card);
//                        faciaAi.checkSimilarity(response.body().getResult().getData().getToken(), parentActivity, faceFile(faceBmp),
//                                cardFile(docBmp), 0.80f, getconfig(), responseSet ->
//                                        Toast.makeText(parentActivity, responseSet.toString(), Toast.LENGTH_LONG).show());
                    }
                } else {
                    faciaAi.createRequest(response.body().getResult().getData().getToken(), parentActivity, getconfig(),
                            responseSet ->
                                    Toast.makeText(parentActivity, responseSet.toString(), Toast.LENGTH_LONG).show());
                }
            } else {
                try {
                    handleTokenApiError(email, parentActivity, response.errorBody().string());
                } catch (IOException e) {
                    ErrorLogs.exceptionReport(e, "Login/ApiHelper/handleAPiSuccessResponse-inner");
                }
            }
            dismissProgressDialog(parentActivity);
        } catch (Exception e) {
            dismissProgressDialog(parentActivity);
            ErrorLogs.exceptionReport(e, "Login/ApiHelper/handleAPiSuccessResponse");
        }
    }

    private JSONObject getconfig() throws JSONException {
        try {
            int selectedId = fragmentLoginBinding.thresholdRadioGroup.getCheckedRadioButtonId();
            if (selectedId == fragmentLoginBinding.lowOption.getId()) {
                selectedOption = "LOW";
            } else if (selectedId == fragmentLoginBinding.thresholdMediumOpt.getId()) {
                selectedOption = "MEDIUM";
            } else if (selectedId == fragmentLoginBinding.highOption.getId()) {
                selectedOption = "HIGH";
            }
            int sizeSelected = fragmentLoginBinding.ovalSizeRadioGroup.getCheckedRadioButtonId();
            if (sizeSelected == fragmentLoginBinding.smallOpt.getId()) {
                ovalSize = "SMALL";
            } else if (sizeSelected == fragmentLoginBinding.sizeMediumOpt.getId()) {
                ovalSize = "MEDIUM";
            } else if (sizeSelected == fragmentLoginBinding.largeOpt.getId()) {
                ovalSize = "LARGE";
            }

            int typeSelectedId = fragmentLoginBinding.livenessTypeRadioGroup.getCheckedRadioButtonId();
            if (typeSelectedId == fragmentLoginBinding.qlOpt.getId()) {
                selectedType = "QUICK";
            } else if (typeSelectedId == fragmentLoginBinding.dlOpt.getId()) {
                selectedType = "DETAILED";
            } else if (typeSelectedId == fragmentLoginBinding.defaultOpt.getId()) {
                selectedType = "DEFAULT";
            } else if (typeSelectedId == fragmentLoginBinding.additionalOpt.getId()) {
                selectedType = "ADDITIONAL";
            }

            int serviceSelectedId = fragmentLoginBinding.serviceTypeRadioGroup.getCheckedRadioButtonId();
            if (serviceSelectedId == fragmentLoginBinding.faceLivenessOpt.getId()) {
                photo_id_match = false;
            } else if (serviceSelectedId == fragmentLoginBinding.matchIdOpt.getId()) {
                photo_id_match = true;
            }
//            else if (serviceSelectedId == fragmentLoginBinding.docLivenessOpt.getId()) {
//                serviceType = ServiceType.DOCUMENT_LIVENESS;
//            }

//            int docTypeSelectedId = fragmentLoginBinding.docTypeRadioGroup.getCheckedRadioButtonId();
//            if (docTypeSelectedId == fragmentLoginBinding.cardOpt.getId()) {
//                documentType = DocumentType.ID_CARD;
//            } else if (docTypeSelectedId == fragmentLoginBinding.passportOpt.getId()) {
//                documentType = DocumentType.PASSPORT;
//            }

            JSONObject config = new JSONObject();
            config.put("showConsent", fragmentLoginBinding.showConsentSwitch.isChecked());
            config.put("showVerificationType", fragmentLoginBinding.showVerificationSwitch.isChecked());
//            config.put("showDocumentType", fragmentLoginBinding.showDocTypeSwitch.isChecked());
//            config.put("showAdditionalVerification", fragmentLoginBinding.showAddVerifSwitch.isChecked());
            config.put("showResult", fragmentLoginBinding.showResult.isChecked());
//            config.put("showFeedback", fragmentLoginBinding.showFeedbackSwitch.isChecked());
//            config.put("showGreetings", fragmentLoginBinding.showGreetingSwitch.isChecked());
//            config.put("qlFaceDetectionTimeout", fragmentLoginBinding.qlTimeSwitch.isChecked());
//            config.put("dlFaceDetectionTimeout", fragmentLoginBinding.dlTimeSwitch.isChecked());
//            config.put("dlEyesBlinkDetectionTimeout", fragmentLoginBinding.blinkTimeSwitch.isChecked());
//            config.put("qlBottomInstructionSheet", fragmentLoginBinding.qlBottomInstSwitch.isChecked());
//            config.put("isDemoApp", fragmentLoginBinding.isDemoAppSwitch.isChecked());
            config.put("emulatorDetection", fragmentLoginBinding.emulatorDetectionSwitch.isChecked());
            config.put("livenessRetryFlow", fragmentLoginBinding.qlLivenessRetrySwitch.isChecked());
            config.put("documentRetryFlow", fragmentLoginBinding.qlIdMatchRetrySwitch.isChecked());
            config.put("livenessRetryCount", fragmentLoginBinding.ql3dLivenessRetryCounterEdt.getText().toString().trim());
            config.put("documentRetryCount", fragmentLoginBinding.qlIdMatchRetryCounterEdt.getText().toString().trim());
            config.put("showMiddleInstructions", fragmentLoginBinding.ql3dLivenessRetryInstSwitch.isChecked());
//            config.put("showMiddleInstructions", fragmentLoginBinding.qlIdMatchRetryInstSwitch.isChecked());
            config.put("detectImproperLight", fragmentLoginBinding.detectLightSwitch.isChecked());
//            config.put("docLivenessOpt", fragmentLoginBinding.docLivenessOptSwitch.isChecked());
            config.put("faceDetectionThreshold", selectedOption);
            config.put("livenessType", selectedType);
            config.put("ovalSize", ovalSize);
            config.put("photo_id_match", photo_id_match);

//            config.put("documentType", documentType);


//            config = new JSONObject();
//            config.put("showConsent", "d");
//            config.put("showVerificationType", true);
//            config.put("showAdditionalVerification", true);
//            config.put("showResult", true);
//            config.put("showFeedback", true);
//            config.put("showGreetings", true);
//            config.put("qlFaceDetectionTimeout", true);
//            config.put("dlFaceDetectionTimeout", true);
//            config.put("dlEyesBlinkDetectionTimeout", true);
//            config.put("livenessRetryFlow", false);
//            config.put("showDocumentType", false);
//            config.put("emulatorDetection", true);
//            config.put("documentLiveness", false);
//            config.put("qlBottomInstructionSheet", false);
//            config.put("documentRetryFlow", false);
//            config.put("showMiddleInstructions", false);
//            config.put("isDemoApp", false);
//            config.put("detectImproperLight", false);
//            config.put("docLivenessOpt", false);
//            config.put("faceDetectionThreshold", "LOW");
//            config.put("ovalSize", "MEDIUM");
//            config.put("livenessType", "Additional");
//            config.put("livenessRetryCount", 3);
//            config.put("documentRetryCount", 3);
//            config.put("serviceType", ServiceType.FACE_LIVENESS);
//            config.put("documentType", DocumentType.ID_CARD);

            config.put("appInfo", "demo-app 1.0.1");
            return config;
        }catch (Exception e){
            ErrorLogs.exceptionReport(e, "Login/ApiHelper/getconfig");
            JSONObject config = new JSONObject();
            //demo app flow
            config.put("showConsent", true);
            config.put("showVerificationType", true);
            config.put("showAdditionalVerification", true);
            config.put("showResult", true);
            config.put("showFeedback", true);
            config.put("showGreetings", true);
            config.put("qlFaceDetectionTimeout", true);
            config.put("dlFaceDetectionTimeout", true);
            config.put("dlEyesBlinkDetectionTimeout", true);
            config.put("qlBottomInstructionSheet", false);
            config.put("isDemoApp", true);
            config.put("emulatorDetection", false);
            config.put("livenessRetryFlow", true);
            config.put("documentRetryFlow", true);
            config.put("documentRetryCount", 3);
            config.put("livenessRetryCount", 3);
            config.put("showMiddleInstructions", true);
            config.put("showMiddleInstructions", true);
            config.put("detectImproperLight", true);
            config.put("docLivenessOpt", true);
            config.put("faceDetectionThreshold", FaceDetectionThreshold.MEDIUM);
            config.put("livenessType", FaceLivenessType.DEFAULT_LIVENESS);
            config.put("ovalSize", OvalSize.LARGE);
            config.put("serviceType", ServiceType.FACE_LIVENESS);
            config.put("documentType", DocumentType.ID_CARD);

            return config;
        }
    }

    /**
     * method to handle api error
     * getting defined error from api result
     * and will toast it
     *
     * @param email          email of the user
     * @param parentActivity activity instance
     * @param errorString    error got from API
     */
    private void handleTokenApiError(String email, Activity parentActivity, String errorString) {
        try {
            ErrorLogs.apiReport(email, errorString, "GetTokenApi Response not successful");
            if (!new JSONObject(errorString).isNull("errors")) {
                if (!new JSONObject(new JSONObject(errorString).get("errors").toString()).isNull("email")) {
                    String emailError = new JSONObject(new JSONObject(errorString).get("errors").toString()).get("email").toString();
                    Toast.makeText(parentActivity, emailError.substring(2, emailError.length() - 2), Toast.LENGTH_SHORT).show();
                } else if (!new JSONObject(new JSONObject(errorString).get("errors").toString()).isNull("password")) {
                    String passwordError = new JSONObject(new JSONObject(errorString).get("errors").toString()).get("password").toString();
                    Toast.makeText(parentActivity, passwordError.substring(2, passwordError.length() - 2), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(parentActivity, new JSONObject(errorString).get("message").toString(), Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(parentActivity, R.string.went_wrong_app, Toast.LENGTH_SHORT).show();
            ErrorLogs.exceptionReport(e, "Login/ApiHelper/handleTokenApiError");
        }
    }

    /**
     * Method to check if internet is connected or not
     *
     * @return is internet connected or not i.e: true/false
     */
    private boolean isConnected(Context context) {
        boolean connected = false;
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
                    Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities capabilities = null;  // need ACCESS_NETWORK_STATE permission

            if (SDK_INT >= Build.VERSION_CODES.M) {
                capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            }
            if (SDK_INT >= Build.VERSION_CODES.M) {
                if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() ==
                        NetworkInfo.State.CONNECTED && capabilities != null && capabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                    //we are connected to a network
                    connected = true;
                } else if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) != null) {
                    if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() ==
                            NetworkInfo.State.CONNECTED && capabilities != null && capabilities.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
                        //we are connected to a network
                        connected = true;
                    } else {
                        Toast.makeText(context, R.string.internet_connection, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.internet_connection, Toast.LENGTH_SHORT).show();
                }
            } else {
                if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                    //we are connected to a network
                    connected = true;
                } else if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE) != null) {
                    if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED) {
                        //we are connected to a network
                        connected = true;
                    } else {
                        Toast.makeText(context, R.string.internet_connection, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.internet_connection, Toast.LENGTH_SHORT).show();
                }

            }
        }
        return connected;
    }
}

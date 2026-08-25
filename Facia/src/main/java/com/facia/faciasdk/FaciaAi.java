package com.facia.faciasdk;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.facia.faciasdk.Utils.FaciaLogger;
import android.widget.Toast;

import com.facia.faciasdk.Activity.FaciaVerify;
import com.facia.faciasdk.Activity.Helpers.Enums.DocumentType;
import com.facia.faciasdk.Activity.Helpers.Enums.FaceDetectionThreshold;
import com.facia.faciasdk.Activity.Helpers.Enums.ServiceType;
import com.facia.faciasdk.Activity.Helpers.IntentHelper;
import com.facia.faciasdk.Activity.Helpers.Enums.FaceLivenessType;
import com.facia.faciasdk.Activity.Helpers.Enums.OvalSize;
import com.facia.faciasdk.Activity.Helpers.RequestListener;
import com.facia.faciasdk.Activity.Helpers.RequestModel;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.AppColors;
import com.facia.faciasdk.Utils.Constants.ApiConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;

public class FaciaAi {
    AppColors appColors;

    /**
     * method to start SDK's activity for Liveness check
     *
     * @param parentActivity  activity that called SDK
     * @param token           user' token
     * @param requestListener listener to return callback/response
     */
    public void createRequest(String token, Activity parentActivity, JSONObject configObject, RequestListener requestListener) {
        try {
            appColors = new AppColors(configObject , parentActivity);
            setRequestModel(token, parentActivity, configObject, null, null, requestListener,
                    false, 0.0f);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "FaciaAi/faciaVerification");
        }
    }

    /**
     * method to start SDK's activity for Similarity (photo with id) check
     *
     * @param parentActivity  activity that called SDK
     * @param token           user' token
     * @param faceImage       face image to match with id
     * @param idImage         id/doc image to match with face
     * @param requestListener listener to return callback/response
     */
    public void checkSimilarity(String token, Activity parentActivity, File faceImage, File idImage,
                                float similarityScore, JSONObject configObject, RequestListener requestListener) {
        try {
            setRequestModel(token, parentActivity, configObject, faceImage, idImage, requestListener,
                    true, similarityScore);
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "FaciaAi/faciaVerification");
        }
    }

    /**
     * checking other key/values (token, listener activity etc)
     *
     * @param token             token to request facia services
     * @param parentActivity    instance of user activity
     * @param configObject      configuration JSONObject
     * @param faceImage         image of the face (in case of ID Scan service)
     * @param idImage           image of the face from Doc (in case of ID Scan service)
     * @param requestListener   listener to return response
     * @param isSimilarityCheck is ID Scan service requested or not
     * @param similarityScore   required similarity score
     */
    private void setRequestModel(String token, Activity parentActivity, JSONObject configObject, File faceImage, File idImage,
                                 RequestListener requestListener, Boolean isSimilarityCheck, float similarityScore) {
        try {
            if (requestListener == null) {
                Toast.makeText(parentActivity, "Request Listener cannot be null.", Toast.LENGTH_SHORT).show();
            } else if (token == null || token.equals("")) {
                terminateSDK(requestListener, "Token cannot be null/empty.");
            } else if (parentActivity == null) {
                terminateSDK(requestListener, "Activity Instance cannot be null.");
            } else {
//                if (!configObject.has("isDemoApp")) {
//                    configObject.put("isDemoApp", false);
//                }
//                if (configObject.getBoolean("isDemoApp")) {
//                    processRequest(token, parentActivity, configObject, faceImage, idImage,
//                            requestListener, isSimilarityCheck, similarityScore);
//                } else {

                if (isValidRetryCount(configObject, requestListener)) {
                    if (isSimilarityCheck) {
                        checkSimilarityInputs(token, parentActivity, configObject, faceImage,
                                idImage, requestListener, similarityScore);
                    } else {
//                        Log.d("Rao_Rizwan" , configObject.toString());
                        processRequest(token, parentActivity, getConfigObject(configObject, requestListener), null, null,
                                requestListener, false, 0.0f);
                    }
                }
//                }
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "FaciaAi/setRequestModel");
        }
    }

    private Boolean isValidRetryCount(JSONObject configObject, RequestListener requestListener) {
        try {
            if (configObject == null || configObject.length() == 0) {
                return true;
            } else {
                if (!configObject.has("livenessRetryCount")) {
                    configObject.put("livenessRetryCount", 0);
                } else if (configObject.getBoolean("livenessRetryFlow")) {
                    try {
                        int count = configObject.getInt("livenessRetryCount");
                        if (count < 0 || count > 5) {
                            terminateSDK(requestListener, "Retry count must be between 0 and 5 inclusive.");

                            return false;
                        }
                    } catch (Exception e) {
                        terminateSDK(requestListener, "Retry count must be a valid integer.");
                        return false;
                    }
                }
                if (!configObject.has("documentRetryCount")) {
                    configObject.put("documentRetryCount", 0);
                } else if (configObject.getBoolean("documentRetryFlow")) {
                    try {
                        int count = configObject.getInt("documentRetryCount");
                        if (count < 0 || count > 5) {
                            terminateSDK(requestListener, "Retry count must be between 0 and 5 inclusive.");
                            return false;
                        }
                    } catch (Exception e) {
                        terminateSDK(requestListener, "Retry count must be a valid integer.");
                        return false;
                    }
                }

                if (configObject.has("clientReference")) {
                    String clientReference = configObject.getString("clientReference").trim();

                    if (!clientReference.isBlank()) {
                        boolean isValid = clientReference.length() >= 5 &&
                                clientReference.length() <= 255 &&
                                clientReference.matches("^[a-zA-Z0-9-]+$");

                        if (!isValid) {
                            terminateSDK(requestListener, "Invalid client_reference");
                            return false;
                        }
                    }
                }

            }
        } catch (Exception e) {
            terminateSDK(requestListener, "Retry count must be a valid integer.");
            return false;
        }
        return true;
    }

    /**
     * method to check config object
     * if object is empty then setting all values
     * if any key/value is missing, setting that value
     *
     * @param configObject configuration JSONObject
     * @return returns the configuration object
     */
    private JSONObject getConfigObject(JSONObject configObject, RequestListener requestListener) {
        try {
            if (configObject == null || configObject.length() == 0) {
                return makeConfigObjectForDemo(configObject);
//                return makeConfigObject(configObject);
            } else {
//                setValueToConfig(configObject, "showAdditionalVerification", true);
//                setValueToConfig(configObject, "showFeedback", false);
//                setValueToConfig(configObject, "showGreetings", false);
//                setValueToConfig(configObject, "qlFaceDetectionTimeout", true);
//                setValueToConfig(configObject, "dlFaceDetectionTimeout", true);
//                setValueToConfig(configObject, "dlEyesBlinkDetectionTimeout", true);
//                setValueToConfig(configObject, "detectImproperLight", false);

                setValueToConfig(configObject, "showConsent", true);
                setValueToConfig(configObject,"doc_verification",false);
                setValueToConfig(configObject,"doc_two_verification",false);
                setValueToConfig(configObject,"address_verification",false);
                setValueToConfig(configObject , "doc_AML" , false);
                setValueToConfig(configObject,"perform_full_kyc",false);
                setValueToConfig(configObject,"card_autocapture",true);
                setValueToConfig(configObject,"showDeclineReason",false);
                setValueToConfig(configObject,"matchIdLiveness",true);
                setValueToConfig(configObject, "showVerificationType", true);
                setValueToConfig(configObject, "showResult", true);
                if (configObject.has("sensorDetection")){
                    configObject.put("emulatorDetection", false);
                } else {
                    setValueToConfig(configObject, "emulatorDetection", true);
                }
//                setValueToConfig(configObject, "emulatorDetection", true);
                setValueToConfig(configObject, "livenessRetryFlow", false);
                setValueToConfig(configObject, "documentRetryFlow", false);
                setValueToConfig(configObject, "showMiddleInstructions", false);
                setValueToConfig(configObject, "isDemoApp", false);
                setValueToConfig(configObject, "photo_id_match", false);
                setValueToConfig(configObject, "photo_id_match", false);
                setValueToConfig(configObject, "livenessInstructions", false);
                setValueToConfig(configObject, "halaMobilityConsent", false);
                setValueToConfig(configObject, "dimLightDetection", false);

                configObject.put("showAdditionalVerification", false);
                configObject.put("showFeedback", false);
                configObject.put("showGreetings", false);
                configObject.put("qlFaceDetectionTimeout", false);
                configObject.put("dlFaceDetectionTimeout", false);
                configObject.put("dlEyesBlinkDetectionTimeout", false);
                configObject.put("detectImproperLight", false);
                configObject.put("showDocumentType", false);
                configObject.put("docLivenessOpt", false);
                configObject.put("documentLiveness", false);

                if (!configObject.has("faceDetectionThreshold")) {
                    configObject.put("faceDetectionThreshold", FaceDetectionThreshold.LOW);
                } else if (configObject.getString("faceDetectionThreshold").equalsIgnoreCase("MEDIUM")) {
                    configObject.put("faceDetectionThreshold", FaceDetectionThreshold.MEDIUM);
                } else if (configObject.getString("faceDetectionThreshold").equalsIgnoreCase("HIGH")) {
                    configObject.put("faceDetectionThreshold", FaceDetectionThreshold.HIGH);
                } else {
                    configObject.put("faceDetectionThreshold", FaceDetectionThreshold.LOW);
                }
                if (!configObject.has("ovalSize")) {
                    configObject.put("ovalSize", OvalSize.MEDIUM);
                } else if (configObject.getString("ovalSize").equalsIgnoreCase("SMALL")) {
                    configObject.put("ovalSize", OvalSize.SMALL);
                } else if (configObject.getString("ovalSize").equalsIgnoreCase("LARGE")) {
                    configObject.put("ovalSize", OvalSize.LARGE);
                } else {
                    configObject.put("ovalSize", OvalSize.MEDIUM);
                }

                if(!configObject.has("clientReference")){
                    configObject.put("clientReference" , "");
                }
                if (!configObject.has("livenessType")) {
                    configObject.put("livenessType", FaceLivenessType.DEFAULT_LIVENESS);
                } else if (configObject.getString("livenessType").equalsIgnoreCase("QUICK")) {
                    configObject.put("livenessType", FaceLivenessType.QUICK_LIVENESS_ONLY);
                } else if (configObject.getString("livenessType").equalsIgnoreCase("DETAILED")) {
                    configObject.put("livenessType", FaceLivenessType.DETAILED_LIVENESS_ONLY);
//                } else if (configObject.getString("livenessType").equalsIgnoreCase("ADDITIONAL")) {
//                    configObject.put("livenessType", FaceLivenessType.ADDITIONAL_CHECK_LIVENESS);
                } else {
                    configObject.put("livenessType", FaceLivenessType.DEFAULT_LIVENESS);
                }

                // iOS parity guard: doc_verification requires photo_id_match to be explicitly true.
                // Mirrors iOS setDocVerification() in RequestModel.swift:L195-200 which silently
                // overrides doc_verification to false when photoMatch is false.
                if (configObject.optBoolean("doc_verification", false)
                        && !configObject.optBoolean("photo_id_match", false)) {
                    configObject.put("doc_verification", false);
                    configObject.put("address_verification", false);
                }

                if (configObject.getBoolean("photo_id_match")) {
                    configObject.put("serviceType", ServiceType.MATCH_TO_PHOTO_ID);
                } else {
                    configObject.put("serviceType", ServiceType.FACE_LIVENESS);
                }

//                if (!configObject.has("documentType")) {
//                    configObject.put("documentType", DocumentType.ID_CARD);
//                }
                return configObject;
            }
        } catch (JSONException e) {
            return makeConfigObjectForDemo(configObject);
        }
    }

    /**
     * method to check if a key exists or not, if not then setting key with the default value
     *
     * @param configObject configuration JSONObject
     * @param key          key to check if exists or not in configuration object
     * @param value        value of the key
     */
    private void setValueToConfig(JSONObject configObject, String key, Boolean value) {
        try {
            if (!configObject.has(key)) {
                configObject.put(key, value);
            } else {
                try {
                    configObject.getBoolean(key);
                } catch (Exception e) {
                    configObject.put(key, value);
                }
            }
        } catch (Exception e) {
            FaciaLogger.e(e);
        }
    }

    /**
     * method to set configuration object completely (if object is empty or null)
     *
     * @param configObject configuration JSONObject
     * @return will return the configuration object
     */
    private JSONObject makeConfigObject(JSONObject configObject) {
        try {
            configObject = new JSONObject();
            configObject.put("showConsent", true);
            configObject.put("doc_verification" , false);
            configObject.put("doc_two_verification" , false);
            configObject.put("address_verification" , false);
            configObject.put("doc_AML" , false);
            configObject.put("perform_full_kyc",false);
            configObject.put("card_autocapture",true);
            configObject.put("showDeclineReason",false);
            configObject.put("matchIdLiveness",true);
            configObject.put("showVerificationType", true);
            configObject.put("showAdditionalVerification", false);
            configObject.put("showResult", true);
            configObject.put("showFeedback", false);
            configObject.put("showGreetings", false);
            configObject.put("qlFaceDetectionTimeout", true);
            configObject.put("dlFaceDetectionTimeout", true);
            configObject.put("dlEyesBlinkDetectionTimeout", true);
            configObject.put("showDocumentType", false);
            if (configObject.has("sensorDetection")){
                configObject.put("emulatorDetection", false);
            } else {
                setValueToConfig(configObject, "emulatorDetection", true);
            }
//            configObject.put("emulatorDetection", true);
            configObject.put("documentLiveness", false);
            configObject.put("livenessInstructions", false);
            configObject.put("halaMobilityConsent", false);
            configObject.put("livenessRetryFlow", true);
            configObject.put("documentRetryFlow", true);
            configObject.put("showMiddleInstructions", true);
            configObject.put("isDemoApp", false);
            configObject.put("detectImproperLight", false);
            configObject.put("docLivenessOpt", false);
            configObject.put("faceDetectionThreshold", FaceDetectionThreshold.LOW);
            configObject.put("ovalSize", OvalSize.MEDIUM);
            configObject.put("livenessType", FaceLivenessType.DEFAULT_LIVENESS);
            configObject.put("livenessRetryCount", 3);
            configObject.put("documentRetryCount", 3);
            configObject.put("serviceType", ServiceType.FACE_LIVENESS);
            configObject.put("documentType", DocumentType.ID_CARD);
        } catch (Exception e) {
            FaciaLogger.e(e);
        }
        return configObject;
    }

    /**
     * method to set config object for demo app
     *
     * @param configObject configuration JSONObject
     * @return will return the configuration object
     */
    private JSONObject makeConfigObjectForDemo(JSONObject configObject) {
        try {
            configObject = new JSONObject();

            configObject.put("showConsent", true);
            configObject.put("clientReference", "");
            configObject.put("doc_verification" , false);
            configObject.put("doc_two_verification" , false);
            configObject.put("address_verification" , false);
            configObject.put("doc_AML" , false);
            configObject.put("perform_full_kyc",false);
            configObject.put("card_autocapture",true);
            configObject.put("showDeclineReason",false);
            configObject.put("matchIdLiveness",true);
            configObject.put("showVerificationType", true);
            configObject.put("showAdditionalVerification", false);
            configObject.put("showResult", true);
            configObject.put("showFeedback", false);
            configObject.put("showGreetings", false);
            configObject.put("qlFaceDetectionTimeout", false);
            configObject.put("dlFaceDetectionTimeout", false);
            configObject.put("dlEyesBlinkDetectionTimeout", false);
            configObject.put("showDocumentType", false);
            configObject.put("emulatorDetection", true);
            configObject.put("documentLiveness", false);
            configObject.put("livenessInstructions", false);
            configObject.put("halaMobilityConsent", false);
            configObject.put("livenessRetryFlow", false);
            configObject.put("documentRetryFlow", false);
            configObject.put("showMiddleInstructions", false);
            configObject.put("isDemoApp", false);
            configObject.put("detectImproperLight", false);
            configObject.put("docLivenessOpt", false);
            configObject.put("dimLightDetection", false);
            configObject.put("faceDetectionThreshold", FaceDetectionThreshold.LOW);
            configObject.put("ovalSize", OvalSize.MEDIUM);
            configObject.put("livenessType", FaceLivenessType.DEFAULT_LIVENESS);
            configObject.put("livenessRetryCount", 0);
            configObject.put("documentRetryCount", 0);
            configObject.put("serviceType", ServiceType.FACE_LIVENESS);
            configObject.put("documentType", DocumentType.ID_CARD);
        } catch (Exception e) {
            FaciaLogger.e(e);
        }
        return configObject;
    }

    /**
     * checking other key/values if user want to check similarity through giving face and doc images as input
     *
     * @param token           token to request facia services
     * @param parentActivity  instance of user activity
     * @param configObject    configuration JSONObject
     * @param faceImage       image of the face (in case of ID Scan service)
     * @param idImage         image of the face from Doc (in case of ID Scan service)
     * @param requestListener listener to return response
     * @param similarityScore required similarity score
     */
    private void checkSimilarityInputs(String token, Activity parentActivity, JSONObject configObject, File faceImage,
                                       File idImage, RequestListener requestListener, float similarityScore) throws JSONException {
        if (faceImage == null) {
            terminateSDK(requestListener, "Face Image cannot be null.");
        } else if (idImage == null) {
            terminateSDK(requestListener, "ID Image cannot be null.");
        } else {
            processRequest(token, parentActivity, getConfigObject(configObject, requestListener), faceImage, idImage,
                    requestListener, true, similarityScore);
        }
    }

    /**
     * method to set values to Request Model class and calling Activity to initiate SDK' flow
     *
     * @param token             token to request facia services
     * @param parentActivity    instance of user activity
     * @param configObject      configuration JSONObject
     * @param faceImage         image of the face (in case of ID Scan service)
     * @param idImage           image of the face from Doc (in case of ID Scan service)
     * @param requestListener   listener to return response
     * @param isSimilarityCheck is ID Scan service requested or not
     * @param similarityScore   required similarity score
     */
    private void processRequest(String token, Activity parentActivity, JSONObject configObject, File faceImage,
                                File idImage, RequestListener requestListener, Boolean isSimilarityCheck, float similarityScore) {
        RequestModel requestModel = new RequestModel();
        if(token == null || token.isEmpty()){
            Webhooks.testingValues("token is null or empty");}
        requestModel.setToken(token);
        requestModel.setParentActivity(parentActivity);
        requestModel.setRequestListener(requestListener);
        requestModel.setConfigObject(configObject);
        if (isSimilarityCheck) {
            requestModel.setSimilarity(true);
            requestModel.setSimilarityScore(similarityScore);
            requestModel.setFaceImage(faceImage);
            requestModel.setIdImage(idImage);
        } else {
            requestModel.setSimilarity(false);
        }
        SingletonData.getInstance().setSdkStarted(false);
        IntentHelper.getInstance().insertObject(ApiConstants.REQUEST_MODEL, requestModel);
        Intent intent = new Intent(parentActivity, FaciaVerify.class);
        parentActivity.startActivity(intent);
        parentActivity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_from_left);
    }


    /**
     * This method is being used to terminate initiation of SDK and show error to user
     * this is only invoked in case of null or empty request parameters
     */
    private void terminateSDK(RequestListener requestListener, String errorMessage) {
        HashMap<String, String> responseSet = new HashMap<>();
        responseSet.put("event", "invalid.request");
        responseSet.put("error", errorMessage);
        requestListener.requestStatus(responseSet);
    }
}
package com.facia.faciasdk.Feedback;

import android.os.Build;

import com.facia.faciasdk.ApiModels.SendFeedback.SendFeedback;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Utilities;
import com.facia.faciasdk.databinding.FragmentFeedbackBinding;
import com.google.gson.JsonObject;

import java.io.IOException;

import retrofit2.Response;

public class ApiHelper {
    private final FragmentFeedbackBinding fragmentFeedbackBinding;
    private final Boolean isUserSatisfied;
    private final String token;

    public ApiHelper(FragmentFeedbackBinding fragmentFeedbackBinding, Boolean isUserSatisfied, String token) {
        this.token = token;
        this.isUserSatisfied = isUserSatisfied;
        this.fragmentFeedbackBinding = fragmentFeedbackBinding;
    }

    /**
     * method to build json object to send feedback
     * checking internet, if available then invoking
     * further method to call api to submit feedback
     */
//    protected void sendFeedbackJsonObject() {
//        try {
//            JsonObject jsonObject = new JsonObject();
//            jsonObject.addProperty("reference_id", SingletonData.getInstance().getReferenceId());
//            jsonObject.addProperty("user_feedback", fragmentFeedbackBinding.suggestionEdtTxt.getText().toString().trim());
//            jsonObject.addProperty("is_user_satisfied", isUserSatisfied);
//            if (Utilities.SimilarMethods.isConnected()) {
//                sendFeedbackApi(jsonObject);
//            } else {
//                Utilities.SimilarMethods.internetDialog();
//            }
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "FeedbackFragment/ApiHelper/sendFeedbackJsonObject");
//        }
//    }

    /**
     * method to call API to send/submit feedback
     *
     * @param jsonObject jsonObject containing feedback related data
     */
//    private void sendFeedbackApi(JsonObject jsonObject) {
//        try {
//            SingletonData.getInstance().getApiInterface().sendFeedback(Utilities.SimilarMethods.bearerToken(token),
//                            Build.FINGERPRINT, "android", jsonObject)
//                    .enqueue(new retrofit2.Callback<SendFeedback>() {
//                        @Override
//                        public void onResponse(retrofit2.Call<SendFeedback> call, Response<SendFeedback> response) {
//                            if (!response.isSuccessful()) {
//                                try {
//                                    Webhooks.apiReport("Create Feedback, Unsuccessful: " + response.code(), response.errorBody().string(), ApiConstants.CREATE_FEEDBACK);
//                                } catch (IOException e) {
//                                    Webhooks.apiReport("Create Feedback, Unsuccessful: " + response.code(), "Response not successful", ApiConstants.CREATE_FEEDBACK);
//                                }
//                            }
//                        }
//                        @Override
//                        public void onFailure(retrofit2.Call<SendFeedback> call, Throwable t) {
//                            try {
//                                Webhooks.apiReport("Create Feedback, Failure", t.getMessage(), ApiConstants.CREATE_FEEDBACK);
//                            }catch (Exception e) {
//                                Webhooks.apiReport("Create Feedback, Failure", "", ApiConstants.CREATE_FEEDBACK);
//                            }
//                        }
//                    });
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "FeedbackFragment/ApiHelper/getResultApi");
//        }
//    }
}
package com.facia.faciasdk.Activity.Helpers;

import android.app.Activity;
import android.util.Log;
import com.facia.faciasdk.Utils.FaciaLogger;

import org.json.JSONObject;
import java.io.File;

public class RequestModel {
    private String token;
    private float similarityScore;
    private File faceImage, idImage, idBackImage;
    private File docTwoImage, docTwoBackImage;
    private File addressImage;
    private File addressBackImage;

    private boolean idBackSkipped = false;
    private boolean docTwoBackSkipped = false;
    private boolean addressBackSkipped = false;
    private Activity parentActivity;
    private Boolean isSimilarity = false;
    private RequestListener requestListener;
    private JSONObject configObject;

    public RequestModel() {
    }

    public float getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(float similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Activity getParentActivity() {
        return parentActivity;
    }

    public void setParentActivity(Activity parentActivity) {
        this.parentActivity = parentActivity;
    }

    public RequestListener getRequestListener() {
        return requestListener;
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }

    public File getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(File faceImage) {
        this.faceImage = faceImage;
    }

    public File getIdImage() {return idImage;}
    public void setIdImage(File idImage) {
        this.idImage = idImage;
    }
    public File getIdBackImage() {
        return idBackImage;
    }
    public void setIdBackImage(File idBackImage) {
        this.idBackImage = idBackImage;
    }

    public File getDocTwoImage() {return docTwoImage;}
    public void setDocTwoImage(File docTwoImage) {
        this.docTwoImage = docTwoImage;
    }
    public File getDocTwoBackImage() {
        return docTwoBackImage;
    }
    public void setDocTwoBackImage(File docTwoBackImage) {
        this.docTwoBackImage = docTwoBackImage;
    }

    public File getAddressImage() {return addressImage;}
    public void setAddressImage(File addressImage) {
        this.addressImage = addressImage;
    }
    public File getAddressBackImage() {return addressBackImage;}

    public void setAddressBackImage(File addressBackImage) {
        this.addressBackImage = addressBackImage;
    }
    public Boolean isSimilarity() {
        return isSimilarity;
    }

    public void setSimilarity(Boolean similarity) {
        isSimilarity = similarity;
    }

    public JSONObject getConfigObject() {
        return configObject;
    }

    public void setConfigObject(JSONObject configObject) {
        this.configObject = configObject;
    }

    public void setIdBackSkipped(boolean skipped) {
        this.idBackSkipped = skipped;
        FaciaLogger.d("RequestModel", "setIdBackSkipped: " + skipped);
    }

    public boolean isIdBackSkipped() {
        return this.idBackSkipped;
    }

    public void setAddressBackSkipped(boolean skipped) {
        this.addressBackSkipped = skipped;
        FaciaLogger.d("RequestModel", "setAddressBackSkipped: " + skipped);
    }

    public boolean isAddressBackSkipped() {
        return this.addressBackSkipped;
    }

    public void setDocTwoBackSkipped(boolean skipped) {
        this.docTwoBackSkipped = skipped;
        FaciaLogger.d("RequestModel", "setDocTwoBackSkipped: " + skipped);
    }

    public boolean isDocTwoBackSkipped() {
        return this.docTwoBackSkipped;
    }
}

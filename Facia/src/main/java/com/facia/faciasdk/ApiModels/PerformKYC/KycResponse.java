package com.facia.faciasdk.ApiModels.PerformKYC;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class KycResponse {

    @SerializedName("status")
    @Expose
    private Boolean status;

    @SerializedName("message")
    @Expose
    private String message;

    @SerializedName("result")
    @Expose
    private KycResult result;

    public Boolean getStatus() { return status; }
    public void setStatus(Boolean status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public KycResult getResult() { return result; }
    public void setResult(KycResult result) { this.result = result; }
}

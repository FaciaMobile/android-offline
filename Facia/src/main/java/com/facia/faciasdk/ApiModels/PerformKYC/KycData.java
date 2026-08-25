package com.facia.faciasdk.ApiModels.PerformKYC;

import androidx.annotation.Keep;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class KycData {

    @SerializedName("reference_id")
    @Expose
    private String referenceId;

    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
}

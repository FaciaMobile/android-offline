package com.facia.faciasdk.ApiModels.PerformKYC;

import androidx.annotation.Keep;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class KycResult {

    @SerializedName("data")
    @Expose
    private KycData data;

    public KycData getData() { return data; }
    public void setData(KycData data) { this.data = data; }
}

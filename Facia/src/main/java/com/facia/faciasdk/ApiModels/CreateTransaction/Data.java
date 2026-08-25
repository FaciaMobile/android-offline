
package com.facia.faciasdk.ApiModels.CreateTransaction;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class Data {

    @SerializedName("reference_id")
    @Expose
    private String referenceId;
//    @SerializedName("type")
//    @Expose
//    private String type;

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

//    public String getType() {
//        return type;
//    }
//
//    public void setType(String type) {
//        this.type = type;
//    }

}

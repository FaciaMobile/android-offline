
package com.facia.faciasdk.ApiModels.LivenessRequest;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class Data {
    @SerializedName("email")
    @Expose
    private String email;

    @SerializedName("$oid")
    @Expose
    private String $oid;

    public String get$oid() {
        return $oid;
    }

    public void set$oid(String $oid) {
        this.$oid = $oid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}

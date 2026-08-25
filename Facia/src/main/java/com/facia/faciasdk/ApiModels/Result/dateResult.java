
package com.facia.faciasdk.ApiModels.Result;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class dateResult {

    @SerializedName("$numberLong")
    @Expose
    private String $numberLong;

    public String get$numberLong() {
        return $numberLong;
    }

    public void set$numberLong(String $numberLong) {
        this.$numberLong = $numberLong;
    }

}

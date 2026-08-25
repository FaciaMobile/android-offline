
package com.facia.faciasdk.ApiModels.Result;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class FaceMatch {
    @SerializedName("is_match")
    @Expose
    private Integer is_match;

    public Integer getIs_match() {
        return is_match;
    }

    public void setIs_match(Integer is_match) {
        this.is_match = is_match;
    }
}

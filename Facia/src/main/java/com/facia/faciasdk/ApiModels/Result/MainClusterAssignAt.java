
package com.facia.faciasdk.ApiModels.Result;

import androidx.annotation.Keep;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Keep
public class MainClusterAssignAt {

    @SerializedName("$date")
    @Expose
    private dateResult $date;

    public dateResult get$date() {
        return $date;
    }

    public void set$date(dateResult $date) {
        this.$date = $date;
    }

}

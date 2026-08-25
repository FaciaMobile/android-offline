
package com.facia.faciademo.ApiCalling.GetToken;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Errors {

    @SerializedName("password")
    @Expose
    private List<String> password = null;

    @SerializedName("email")
    @Expose
    private List<String> email = null;

    public List<String> getEmail() {
        return email;
    }

    public void setEmail(List<String> email) {
        this.email = email;
    }

    public List<String> getPassword() {
        return password;
    }

    public void setPassword(List<String> password) {
        this.password = password;
    }

}

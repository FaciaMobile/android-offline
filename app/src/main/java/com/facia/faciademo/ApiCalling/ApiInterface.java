package com.facia.faciademo.ApiCalling;

import com.facia.faciademo.ApiCalling.GetToken.GetToken;
import com.google.gson.JsonObject;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiInterface {

    @POST("/backend/api/transaction/get-access-token/")
    Call<GetToken> getToken(@Body JsonObject JsonObject);

}
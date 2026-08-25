package com.facia.faciasdk.ApiModels;


import com.facia.faciasdk.Utils.Constants.ApiConstants;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class ApiClient {
    public static OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .callTimeout(720, TimeUnit.SECONDS)
            .readTimeout(720, TimeUnit.SECONDS)
            .connectTimeout(720, TimeUnit.SECONDS)
            .writeTimeout(720, TimeUnit.SECONDS)
            .build();
    private static Retrofit retrofit = null;

    /**
     * making retrofit instance
     * @return returning retrofit instance
     */
    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(ApiConstants.BASIC_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build();
        }
        return retrofit;
    }
}

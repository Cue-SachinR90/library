package com.android.cuelogic.library.webservice;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

import static com.android.cuelogic.library.constants.Constants.MAX_REQUEST_TIMEOUT_MS;

/**
 * Created by sachinrao on 17/01/18.
 */

public class OkHttpClientInstance {
    /**
     * single instance of okhttp client
     */
    private static OkHttpClient CLIENT;

    public static OkHttpClient getOkHttpClient() {
        if (CLIENT == null) {
            // create an instance of OkHttpClient builder
            OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
            okHttpBuilder.readTimeout(MAX_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .writeTimeout(MAX_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                    .connectTimeout(MAX_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS);/*
                    .addInterceptor(new OfflineCachingInterceptor());*/
///*            if (BuildConfig.DEBUG) {
//                LoggingInterceptor.Builder builder = new LoggingInterceptor.Builder()
//                        .loggable(BuildConfig.DEBUG)
//                        .setLevel(Level.BASIC)
//                        .log(Platform.INFO)
//                        .request("Request")
//                        .response("Response");
//                okHttpBuilder.addInterceptor(builder.build());
//            }*/
            //create client
            CLIENT = okHttpBuilder.build();
        }
        return CLIENT;
    }
}

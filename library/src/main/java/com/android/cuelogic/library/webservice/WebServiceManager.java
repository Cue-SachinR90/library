package com.android.cuelogic.library.webservice;

import android.os.Handler;
import android.os.Looper;

import com.android.cuelogic.library.webservice.interfaces.ProgressListener;
import com.android.cuelogic.library.webservice.interfaces.SyncListener;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is used to call the web-services
 * Created on 9/16/2017.
 */
public class WebServiceManager {
    /**
     * It is responsible for calling webservices.<br/>
     * OkHttp performs best when you create a single OkHttpClient instance and reuse it for all of your HTTP calls.
     * This is because each client holds its own connection pool and thread pools. Reusing connections and threads reduces latency and saves memory.
     * Conversely, creating a client for each request wastes resources on idle pools.
     *
     * @see OkHttpClient
     */
    private OkHttpClient client;
    private SyncListener syncListener;

    public WebServiceManager(SyncListener syncListener) {
        //get singleton client from the application
        client = OkHttpClientInstance.getOkHttpClient();
        this.syncListener = syncListener;
    }


    public synchronized void callWebService(String url, int taskID,final Class clazz){
        Request request = new Request.Builder()
                .url(url)
                .tag(taskID)
                .build();
        callWebservice(request, clazz);
    }

    /**
     * This is used to call the webservice on separate {@link Thread}
     *
     * @param request - request to be performed
     * @param clazz   - the response will be parsed according to this class
     */
    synchronized void callWebservice(Request request, final Class clazz) {
        //use new builder to creates a shallow copy, so you can change some configuration while leaving everything else the same.
        final OkHttpClient newClient = client.newBuilder()
                .addNetworkInterceptor(new ProgressInterceptor(syncListener))
                .build();
        //enqueue : todo: read the documentation
        newClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(final Call call, final IOException e) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        Request originalRequest = call.request();
                        int webServiceID = (int) originalRequest.tag();
                        syncListener.onSyncFailure(null, webServiceID, e.getLocalizedMessage());
                    }
                });
            }

            @Override
            public synchronized void onResponse(final Call call, Response response) throws IOException {
                //http://stackoverflow.com/questions/24246783/okhttp-response-callbacks-on-the-main-thread
                //Read the body before posting back to the main thread
                final OkHttpResponseDetails okHttpResponseDetails = new OkHttpResponseDetails(response, clazz);
                Handler handler = new Handler(Looper.getMainLooper());
                if (response.code() >= 200 && response.code() <= 204) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Request originalRequest = call.request();
                            int webServiceID = (int) originalRequest.tag();
                            syncListener.onSyncSuccess(okHttpResponseDetails, webServiceID);
                        }
                    });
                } else {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Request originalRequest = call.request();
                            int webServiceID = (int) originalRequest.tag();
                            syncListener.onSyncFailure(okHttpResponseDetails, webServiceID, okHttpResponseDetails.message);
                        }
                    });
                }
            }
        });
    }

    /**
     * Custom interceptor to detect progress of the
     */
    private class ProgressInterceptor implements Interceptor {
        private ProgressListener progressListener;

        ProgressInterceptor(ProgressListener progressListener) {
            this.progressListener = progressListener;
        }

        @Override
        public Response intercept(Interceptor.Chain chain) throws IOException {
            Response originalResponse = chain.proceed(chain.request());
            return originalResponse.newBuilder()
                    .body(new ProgressResponseBody(originalResponse.body(), progressListener))
                    .build();
        }
    }
}

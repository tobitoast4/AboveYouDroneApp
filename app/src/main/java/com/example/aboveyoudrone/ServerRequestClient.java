package com.example.aboveyoudrone;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class ServerRequestClient {
    public static int RESPONSE_CODE_SERVER_NOT_REACHABLE = -1;

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(url, params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }

    public static void delete(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.delete(url, params, responseHandler);
    }

    public static void post_raw_json(Context context, String url, StringEntity jsonParams, AsyncHttpResponseHandler responseHandler){
        // see https://stackoverflow.com/a/52688293/14522363
        client.post(context, url, jsonParams, "application/json", responseHandler);
    }

    public static class HttpClient {
        // https://loopj.com/android-async-http/

        private OnServerResponseListener listener;

        private Context context;

        public HttpClient() {
            this.listener = null;
            this.context = null;
        }

        // Assign the listener implementing events interface that will receive the events
        public void setOnServerResponseListener(OnServerResponseListener listener) {
            this.listener = listener;
        }

        public interface OnServerResponseListener {
            void onResponseLoaded(int status);
        }

        public void getStatus() throws JSONException {
            String url = "http://tobitoast.pythonanywhere.com/";
            ServerRequestClient.get(url, null, new JsonHttpResponseHandler() {
                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    listener.onResponseLoaded(statusCode);
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    // If the response is JSONObject instead of expected JSONArray
                    boolean usernameExists = true;
                    try {
                        usernameExists = response.getBoolean("exists");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    if(usernameExists){
                        listener.onResponseLoaded(1);
                    } else {
                        listener.onResponseLoaded(0);
                    }
                }
            });
        }
    }
}




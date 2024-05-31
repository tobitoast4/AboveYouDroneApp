package com.example.aboveyoudrone;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class ServerRequestClient {
    private static final String BASE_URL = "https://aboveyoudrone.pythonanywhere.com";
    public static int RESPONSE_CODE_SERVER_NOT_REACHABLE = -1;

    private static AsyncHttpClient client = new AsyncHttpClient();

    public static void get(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void delete(String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.delete(getAbsoluteUrl(url), params, responseHandler);
    }

    public static void post_raw_json(Context context, String url, StringEntity jsonParams, AsyncHttpResponseHandler responseHandler){
        // see https://stackoverflow.com/a/52688293/14522363
        client.post(context, getAbsoluteUrl(url), jsonParams, "application/json", responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}




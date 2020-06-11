package com.reactlibrary;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.app.ProgressDialog;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.payfort.fort.android.sdk.base.FortSdk;
import com.payfort.fort.android.sdk.base.callbacks.FortCallBackManager;
import com.payfort.fort.android.sdk.base.callbacks.FortCallback;
import com.payfort.sdk.android.dependancies.base.FortInterfaces;
import com.payfort.sdk.android.dependancies.models.FortRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PayfortActivity extends Activity {

    FortCallBackManager fortCallback;
    ProgressDialog pbLoading;
    String deviceId, isLive, accessCode, merchantIdentifier, requestPhrase, language,
            customerEmail, currency, amount, merchantReference, customerName, customerIp, paymentOption, orderDescription,
            responsePhrase, sdkToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payfort);

        parseData();

        pbLoading = new ProgressDialog(this);
        pbLoading.setMessage("Fetching data...");
        pbLoading.show();

        fortCallback = FortCallback.Factory.create();
        deviceId = FortSdk.getDeviceId(PayfortActivity.this);

        requestGetToken();
        //startFortRequest(sdkToken);
    }

    private void parseData() {
        Intent intent = getIntent();
        if (intent.hasExtra("is_live")) {
            isLive = intent.getStringExtra("is_live");
        } //
        if (intent.hasExtra("access_code")) {
            accessCode = intent.getStringExtra("access_code");
        } //
        if (intent.hasExtra("merchant_identify")) {
            merchantIdentifier = intent.getStringExtra("merchant_identify");
        } //
        if (intent.hasExtra("request_phrase")) {
            requestPhrase = intent.getStringExtra("request_phrase");
        } //
        if (intent.hasExtra("response_phrase")) {
            responsePhrase = intent.getStringExtra("response_phrase");
        }
        if (intent.hasExtra("customer_email")) {
            customerEmail = intent.getStringExtra("customer_email");
        } //
        if (intent.hasExtra("language")) {
            language = intent.getStringExtra("language");
        } //
        if (intent.hasExtra("currency")) {
            currency = intent.getStringExtra("currency");
        } //
        if (intent.hasExtra("amount")) {
            amount = intent.getStringExtra("amount");
        } //
        if (intent.hasExtra("merchant_reference")) {
            merchantReference = intent.getStringExtra("merchant_reference");
        } //
        if (intent.hasExtra("customer_name")) {
            customerName = intent.getStringExtra("customer_name");
        } //
        if (intent.hasExtra("customer_ip")) {
            customerIp = intent.getStringExtra("customer_ip");
        }
        if (intent.hasExtra("payment_option")) {
            paymentOption = intent.getStringExtra("payment_option");
        }
        if (intent.hasExtra("order_description")) {
            orderDescription = intent.getStringExtra("order_description");
        } //
        // if (intent.hasExtra("sdk_token")) {
        //     sdkToken = intent.getStringExtra("sdk_token");
        // }
    }

    private void requestGetToken() {
        String url = "https://sbpaymentservices.payfort.com/FortAPI/paymentApi";
        if(isLive.equals("1")) {
            url = "https://paymentservices.payfort.com/FortAPI/paymentApi";
        }

        RequestQueue queue = Volley.newRequestQueue(this);
        Map<String, String> params = new HashMap<>();
        params.put("service_command", "SDK_TOKEN");
        params.put("access_code", accessCode);
        params.put("merchant_identifier", merchantIdentifier);
        params.put("language", language);
        params.put("device_id", deviceId);
        params.put("signature", hashSignature(requestPhrase + "access_code=" + accessCode + "device_id=" + deviceId + "language=" + language + "merchant_identifier=" + merchantIdentifier + "service_command=SDK_TOKEN" + requestPhrase));
        JSONObject parameters = new JSONObject(params);
        JsonObjectRequest jsonRequest = new JsonObjectRequest(Request.Method.POST, url, parameters, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                pbLoading.dismiss();
                Log.e("SUCCESS", response.toString());
                try {
                    startFortRequest(response.getString("sdk_token"));
                } catch (JSONException e) {
                    e.printStackTrace();
                    finish();
                    RNPayfortSdkModule.onFail.invoke("Error while fetching data");
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                pbLoading.dismiss();
                Log.e("FAIL", error.toString());
                finish();
                RNPayfortSdkModule.onFail.invoke();
            }
        });
        queue.add(jsonRequest);
    }

    private String hashSignature(String originalString) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] encodedhash = digest.digest(
                originalString.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(encodedhash);
    }

    private String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private void startFortRequest(String sdkToken) {
        FortRequest fortrequest = new FortRequest();
        fortrequest.setRequestMap(collectRequestMap(sdkToken));
        fortrequest.setShowResponsePage(true);

        String environment = FortSdk.ENVIRONMENT.TEST;
        if (isLive.equals("1")) {
            environment = FortSdk.ENVIRONMENT.PRODUCTION;
        }

        try {
            FortSdk.getInstance().registerCallback(PayfortActivity.this, fortrequest, environment, 5, fortCallback, new FortInterfaces.OnTnxProcessed() {
                @Override
                public void onCancel(Map<String, Object> requestParamsMap, Map<String,
                        Object> responseMap) {
                    Log.e("Cancelled ", responseMap.toString());
                    finish();
                    String message = "Payment has been canceled";
                    if (responseMap.get("response_message") != null && !responseMap.get("response_message").toString().equals("")) {
                        message = responseMap.get("response_message").toString();
                    }
                    RNPayfortSdkModule.onFail.invoke(message);
                }

                @Override
                public void onSuccess(Map<String, Object> requestParamsMap, Map<String,
                        Object> fortResponseMap) {
                    Log.e("Success ", fortResponseMap.toString());
                    finish();

                    JSONObject responseObj = new JSONObject(fortResponseMap);
                    try {
                        WritableMap productMap = convertJsonToMap(responseObj);

                        RNPayfortSdkModule.onSuccess.invoke(productMap);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onFailure(Map<String, Object> requestParamsMap, Map<String,
                        Object> fortResponseMap) {
                    Log.e("Failure ", fortResponseMap.toString());
                    finish();
                    String message = "There is an error while process payment";
                    if (fortResponseMap.get("response_message") != null && !fortResponseMap.get("response_message").toString().equals("")) {
                        message = fortResponseMap.get("response_message").toString();
                    }
                    RNPayfortSdkModule.onFail.invoke(message);
                    // JSONObject responseObj = new JSONObject(fortResponseMap);
                    // try {
                    //     WritableMap productMap = convertJsonToMap(responseObj);

                    //     RNPayfortSdkModule.onSuccess.invoke(productMap);
                    // } catch (JSONException e) {
                    //     e.printStackTrace();
                    // }
                }

            });
        } catch (Exception e) {
            Log.e("execute Payment", "call FortSdk", e);
        }
    }

    private static WritableMap convertJsonToMap(JSONObject jsonObject) throws JSONException {
        WritableMap map = new WritableNativeMap();

        Iterator<String> iterator = jsonObject.keys();
        while (iterator.hasNext()) {
            String key = iterator.next();
            Object value = jsonObject.get(key);
            if (value instanceof JSONObject) {
                map.putMap(key, convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                map.putArray(key, convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                map.putBoolean(key, (Boolean) value);
            } else if (value instanceof Integer) {
                map.putInt(key, (Integer) value);
            } else if (value instanceof Double) {
                map.putDouble(key, (Double) value);
            } else if (value instanceof String) {
                map.putString(key, (String) value);
            } else {
                map.putString(key, value.toString());
            }
        }
        //map.putString("device_id", FortSdk.getDeviceId(PayfortActivity.this));
        return map;
    }

    private static WritableArray convertJsonToArray(JSONArray jsonArray) throws JSONException {
        WritableArray array = new WritableNativeArray();

        for (int i = 0; i < jsonArray.length(); i++) {
            Object value = jsonArray.get(i);
            if (value instanceof JSONObject) {
                array.pushMap(convertJsonToMap((JSONObject) value));
            } else if (value instanceof JSONArray) {
                array.pushArray(convertJsonToArray((JSONArray) value));
            } else if (value instanceof Boolean) {
                array.pushBoolean((Boolean) value);
            } else if (value instanceof Integer) {
                array.pushInt((Integer) value);
            } else if (value instanceof Double) {
                array.pushDouble((Double) value);
            } else if (value instanceof String) {
                array.pushString((String) value);
            } else {
                array.pushString(value.toString());
            }
        }
        return array;
    }

    private Map<String, Object> collectRequestMap(String sdkToken) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("command", "PURCHASE");
        requestMap.put("customer_email", customerEmail);
        requestMap.put("currency", currency);
        requestMap.put("amount", amount);
        requestMap.put("language", language);
        requestMap.put("merchant_reference", merchantReference);
        requestMap.put("customer_name", customerName);
        if (customerIp != null) {
            requestMap.put("customer_ip", customerIp);
        }
        if (paymentOption != null) {
            requestMap.put("payment_option", paymentOption);
        }
        requestMap.put("eci", "ECOMMERCE");
        if (orderDescription != null) {
            requestMap.put("order_description", orderDescription);
        }
        requestMap.put("sdk_token", sdkToken);
        Log.e("PayfortMap", requestMap.toString());
        return requestMap;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fortCallback.onActivityResult(requestCode, resultCode, data);
    }
}

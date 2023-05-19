package org.openforis.collect.android.Network;

import android.content.Context;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.jaredrummler.materialspinner.MaterialSpinner;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.openforis.collect.android.gui.MainActivity;
import org.openforis.collect.android.gui.util.GetLocation;
import org.openforis.collect.android.sqlite.DataModel.PreferenceSetting;
import org.openforis.collect.android.sqlite.DataModel.RecordHolder;
import org.openforis.collect.android.util.Permissions;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionSpec;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Cloud {

    public static class DefaultReturnCode {
        public static final int SUCCESS = 200;
        public static final int INTERNAL_SERVER_ERROR = 500;
        public static final int NOT_FOUND = 404;
    }

    public static final String DOMAIN_NAME = "http://34.87.74.224/collect/api";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    public static final int REQUEST_CONNECTION_TIMEOUT = 10;

    private static RecordHolder recHolder;

    public double LATITUDE;
    public double LONGITUDE;

    //=================== CREATE RECORD ========================//


    public void createRecord(final ResponseListener listener) {
        String url = DOMAIN_NAME + "/survey/77/data/records";

        String jsonRequest = "{\"rootEntityName\": \"\", " +
                "\"versionId\": \"\", " +
                "\"step\":\"ENTRY\", " +
                "\"preview\": \"" + false + "\"}";

        RequestBody requestBody = RequestBody.create(JSON, jsonRequest.toString());

        OkHttpClient client = defaultHttpClient();

        Request request = new Request.Builder()
                .post(requestBody)
                .header("Content-Type", "application/json")
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.out.println("onFailure --> " + e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    listener.onResponse(null);
                    System.out.println("onResponse 1 --> " + response.code());
                    try {
                        System.out.println("onResponse 2 --> " + response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        recHolder = new Gson().fromJson(response.body().string(), RecordHolder.class);
                        listener.onResponse(recHolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }


    //====================== RECORDS ============================//
    public void postRecords(String id, int nodeDefId, String attribute, String criteria, String value, int counter, final Context context) {
        String url = DOMAIN_NAME + "/command/record/attribute";

        String jsonRequest = "{" +
                "\"surveyId\" : 77, " +
                "\"recordId\" : \""+ id +"\", " +
                "\"recordStep\": \"ENTRY\", " +
                "\"parentEntityPath\": \"/surveyors\", " +
                "\"nodeDefId\": "+ nodeDefId +", " +
                "\"nodePath\": \""+ criteria +"\", " +
                "\"attributeType\": \""+ attribute +"\", " +
                "\"valueByField\": "+ value +", " +
                "\"preferredLanguage\": \"en\"" +
                "}";

        RequestBody requestBody = RequestBody.create(JSON, jsonRequest);

        OkHttpClient client = defaultHttpClient();

        Request request = new Request.Builder()
                .post(requestBody)
                .header("Content-Type", "application/json")
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                System.out.println("onFailure --> " + e.getMessage());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    System.out.println("postRecords response 1 --> " + response.code());
                    try {
                        System.out.println("postRecords response 2 --> " + response.body().string());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    //Success
                    PreferenceSetting preferenceSetting = new PreferenceSetting(PreferenceManager.getDefaultSharedPreferences(context));
                    String userId = preferenceSetting.getId();
                    String fullName = preferenceSetting.getFullName();
                    switch (counter) {
                        case 1:
                            Calendar c = Calendar.getInstance();
                            int hour =c.get(Calendar.HOUR_OF_DAY);
                            int minute =c.get(Calendar.MINUTE);
                            postRecords(userId, 19, "TIME", "/surveyors/created_at_time",
                                    "{ \"hour\": "+ hour +", \"minute\": "+ minute +" }", 2, context);
                            break;
                        case 2:
                            postRecords(userId, 20, "TEXT", "/surveyors/full_name",
                                    "{ \"value\": \""+ fullName +"\" }", 3, context);
                            break;
                        case 3:
                            //x is longitude y is latitude
                            double x = LONGITUDE;
                            double y = LATITUDE;
                            postRecords(userId, 21, "COORDINATE", "/surveyors/latlang",
                                    "{ \"y\": "+ y +", \"x\": "+ x +", \"srs\": \"EPSG:4326\" }", 4, context);
                            break;
                        case 4:
                            Calendar calendar = Calendar.getInstance();
                            int day = calendar.get(Calendar.DAY_OF_MONTH);
                            int year = calendar.get(Calendar.YEAR);
                            int month = calendar.get(Calendar.MONTH) + 1;
                            postRecords(userId, 22, "DATE", "/surveyors/created_at_date",
                                    "{ \"year\": "+ year +", \"month\": "+ month +", \"day\": "+ day +" }", 0, context);
                            break;
                        case 0:
                            //Successfully rendered all data
                            System.out.println("Already created records");
                            break;
                        default:
                            break;
                    }
                }
            }
        });
    }


    private static void printResult(String msg) {
        Log.d("Return: ", msg);
    }

    private static JSONObject resultException(Exception e) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("Failed", DefaultReturnCode.INTERNAL_SERVER_ERROR);
            jsonObject.put("Return Message", e.getMessage());
        } catch (JSONException e1){
            Log.e("Error: %s", e1.getMessage());
        }

        return jsonObject;
    }

    private static OkHttpClient defaultHttpClient(){
        return new OkHttpClient.Builder()
                .connectTimeout(REQUEST_CONNECTION_TIMEOUT, TimeUnit.SECONDS)
//                .connectionSpecs(Arrays.asList(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
                .build();
    }

}

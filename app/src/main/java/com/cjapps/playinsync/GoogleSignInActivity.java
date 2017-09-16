package com.cjapps.playinsync;

import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;

public class GoogleSignInActivity extends Activity {

    private static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;
    private static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
    private static final int GOOGLE_ACCOUNT_PICK = 1000;

    // REGISTRATION DETAILS

    private String NAME;
    private String EMAIL;
    private String U_ID;
    private String GOOGLE_TOKEN;
    private String GCM_ID;
    private String DP_PATH;

    private static Context context;

    private Intent social_connect_activity;
    private Activity google_sign_in_activity;

    private GoogleCloudMessaging gcm;

    private DatabaseManager db_manager;

    private AsyncTask<String, Integer, String> google_token_task;
    private AsyncTask<String, Integer, String> google_details_task;
    private AsyncTask<Void, Void, String> google_gcm_task;

    private String LOG_TAG = "com.cjapps.playinsync";

    private Button google_button;
    private TextView getting_details;
    private SmoothProgressBar getting_details_pb;

    private SharedPreferences.Editor config_editor;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_google_sign_in);
        social_connect_activity = new Intent(this,SocialConnectActivity.class);
        context = this;
        google_sign_in_activity = this;

        db_manager = new DatabaseManager(this);
        SharedPreferences config = getSharedPreferences("config", MODE_PRIVATE);
        config_editor = config.edit();

        google_button = (Button) findViewById(R.id.google_sign_in_button);

        getting_details = (TextView)findViewById(R.id.getting_details_tv);
        getting_details_pb = (SmoothProgressBar)findViewById(R.id.getting_details_pb);

        // GOOGLE SIGN IN BUTTON CLICKED
        google_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (checkPlayServices()) {
                    // SHOW ACCOUNT PICKER
                    Intent account_picker = AccountPicker.newChooseAccountIntent(null, null
                            , new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}
                            , true, null, null, null, null);
                    startActivityForResult(account_picker, GOOGLE_ACCOUNT_PICK);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onBackPressed() {
        if(google_token_task!=null) {
            if (google_token_task.getStatus() == AsyncTask.Status.RUNNING |
                    google_details_task.getStatus() == AsyncTask.Status.RUNNING |
                    google_gcm_task.getStatus() == AsyncTask.Status.RUNNING) {

                Toast.makeText(this, "Please wait", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode){
            case GOOGLE_ACCOUNT_PICK:{
                if (resultCode == RESULT_OK) {
                    String google_email_id = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                    Log.e(LOG_TAG, "EMAIL_ID " + google_email_id);

                    // GET THE EMAIL ID HERE
                    EMAIL = google_email_id;

                    // START TASK TO GET ACCESS TOKEN
                    google_token_task = new GoogleTokenTask();
                    google_token_task.execute(google_email_id);
                    break;

                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "You must pick an account", Toast.LENGTH_SHORT).show();
                    break;
                }

            }
            case REQUEST_CODE_RECOVER_PLAY_SERVICES:{
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Google Play Services must be installed.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                    break;
                }
            }
            case REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR:{
                google_token_task = new GoogleTokenTask();
                google_token_task.execute(EMAIL);

                break;
            }
        }
    }

    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                showErrorDialog(status);
            } else {
                Toast.makeText(this, "This device is not supported.",Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, this,REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    // GOOGLE ACCESS TOKEN ASYNC TASK

    private class GoogleTokenTask extends AsyncTask<String, Integer, String> {


        @Override
        protected void onPreExecute() {

            google_button.setVisibility(View.INVISIBLE);
            getting_details.setVisibility(View.VISIBLE);
            getting_details_pb.setVisibility(View.VISIBLE);

        }

        @Override
        protected String doInBackground(String... email) {
            try{
                String SCOPES = "oauth2:https://www.googleapis.com/auth/plus.login "+
                        "https://www.googleapis.com/auth/plus.stream.read "+
                        "https://www.googleapis.com/auth/plus.stream.write "+
                        "https://www.googleapis.com/auth/drive.file";

                // GET GOOGLE TOKEN HERE
                GOOGLE_TOKEN = GoogleAuthUtil.getToken(GoogleSignInActivity.context, email[0], SCOPES);

                Log.e(LOG_TAG, "GOOGLE_TOKEN" + GOOGLE_TOKEN);

            }catch (UserRecoverableAuthException e){
                // Unable to authenticate, such as when the user has not yet granted
                // the app access to the account, but the user can fix this.
                // Forward the user to an activity in Google Play services.
                Intent intent = e.getIntent();
                startActivityForResult(intent,REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
            }catch (GoogleAuthException ex){
                Log.e(LOG_TAG, "ERROR" + ex.getMessage());
            }catch (IOException ex){
                Log.e(LOG_TAG, "GOOGLE_TOKEN CONNECTION ERROR");
            }
            return GOOGLE_TOKEN;
        }

        @Override
        protected void onPostExecute(String result) {
            // process the result
            super.onPostExecute(result);

            // GET USER DETAILS TASK
            if(GOOGLE_TOKEN!=null){
                google_details_task = new GoogleUserDetailTask();
                google_details_task.execute();
            } else {
                google_button.setVisibility(View.VISIBLE);
                getting_details.setVisibility(View.INVISIBLE);
                getting_details_pb.setVisibility(View.INVISIBLE);
            }
        }
    }

    // GOOGLE USER DETAILS ASYNC TASK
    private class GoogleUserDetailTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... token) {

            String name;
            String dp_path;

            try{
                URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + GOOGLE_TOKEN);

                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                int sc = con.getResponseCode();

                if (sc == 200) {

                    InputStream is = con.getInputStream();
                    String json = readResponse(is);
                    name = getDetail(json, "given_name") + " " + getDetail(json, "family_name");
                    dp_path = getDetail(json, "picture");

                    Log.e(LOG_TAG, "NAME " + name);
                    Log.e(LOG_TAG, "PATH " + dp_path);

                    is.close();

                    // GET USER NAME HERE
                    NAME = name;
                    DP_PATH = dp_path;

                } else if (sc == 401) {

                    GoogleAuthUtil.invalidateToken(GoogleSignInActivity.this, GOOGLE_TOKEN);
                    Log.i(LOG_TAG, "Google Server auth error, please try again.");
                    Log.i(LOG_TAG, "Google Server auth error: " + readResponse(con.getErrorStream()));

                } else {
                    Log.i(LOG_TAG,"Google Server returned the following error code: " + sc);
                }
            }catch (MalformedURLException ignored){

            }catch (IOException ignored){

            }catch (JSONException ignored){

            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            // process the result
            super.onPostExecute(result);
            // START GCM REGISTRATION PROCESS
            google_gcm_task = new GCMRegistrationTask();
            google_gcm_task.execute();
        }
    }

    // GCM REGISTRATION ASYNC TASK
    private class GCMRegistrationTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            String msg = "";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(GoogleSignInActivity.this);
                }
                GCM_ID = gcm.register(GCMConfiguration.GOOGLE_SENDER_ID);

                Log.e(LOG_TAG, "GCM REG ID " + GCM_ID);

                // UPLOAD EMAIL ID,USERNAME, GCM REG ID on server

                HttpClient httpclient = new DefaultHttpClient();
                HttpPost httppost = new HttpPost("http://www.playinsync.com/mobile/sign_up.php");

                // Add your data
                List<NameValuePair> post_data = new ArrayList<NameValuePair>();

                post_data.add(new BasicNameValuePair("EMAIL", EMAIL));
                post_data.add(new BasicNameValuePair("NAME",NAME));
                post_data.add(new BasicNameValuePair("GCM_ID",GCM_ID));
                post_data.add(new BasicNameValuePair("DP_PATH",DP_PATH));

                httppost.setEntity(new UrlEncodedFormEntity(post_data));

                HttpResponse response = httpclient.execute(httppost);
                if (response != null) {
                    InputStream in = response.getEntity().getContent();
                    U_ID = parseResponseFromServer(in);
                }

                Log.e(LOG_TAG, "PIS " + post_data.toString() + "\n" + U_ID);

            } catch (IOException ex) {
                msg = "Error :" + ex.getMessage();
            }
            return msg;
        }

        @Override
        protected void onPostExecute(String msg) {

            // ENTER PARTIAL DETAILS
            if(db_manager.addMyDetails(U_ID,NAME,EMAIL,DP_PATH,"NA",GOOGLE_TOKEN,"NA","NA","NA","NA","NA",GCM_ID)){
                config_editor.putBoolean("google_sign_in_done",true);
                config_editor.apply();
            }

            // START SOCIAL CONNECT ACTIVITY
            google_sign_in_activity.finish();
            startActivity(social_connect_activity);
        }
    }

    // CONVERT INPUT STREAM TO STRING
    public String readResponse(InputStream is) throws IOException {

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int len;
        while ((len = is.read(data, 0, data.length)) >= 0) {
            bos.write(data, 0, len);
        }
        String response= new String(bos.toByteArray(), "UTF-8");
        Log.e(LOG_TAG, "RESPONSE_JSON " + response);
        return  response;
    }


    // RETURNS VALUE STRING FOR THE CORRESPONDING KEY
    private String getDetail(String jsonResponse,String key) throws JSONException {
        JSONObject profile = new JSONObject(jsonResponse);
        return profile.getString(key);
    }

    // PARSE RESPONSE FROM SERVER
    private String parseResponseFromServer(InputStream is) throws IOException {

        String response_data;
        String line;
        StringBuilder total = new StringBuilder();

        BufferedReader rd = new BufferedReader(new InputStreamReader(is));

        while ((line = rd.readLine()) != null) {
            total.append(line);
        }

        response_data =total.toString();

        int end = response_data.indexOf("<!-- Hosting24 Analytics Code -->");
        if(end!=-1)
            response_data=response_data.substring(0,end);

        return response_data.trim();
    }

}

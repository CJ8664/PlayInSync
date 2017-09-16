package com.cjapps.playinsync;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.LoggingBehavior;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;
import com.facebook.model.GraphUser;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.GooglePlayServicesAvailabilityException;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.android.gms.common.AccountPicker;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class LoginActivity extends Activity implements OnClickListener{

    private ImageView facebook_button;
    private ImageView google_button;
    private ImageView twitter_button;

    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    private GraphUser fb_user;
    private GoogleApiClient google_client;
    private static Context context;
    private ConnectionDetector cd;
    private AlertDialogManager alert = new AlertDialogManager();

    // Twitter
    private static Twitter twitter;
    private static RequestToken requestToken;

    private String FACEBOOK_TOKEN;
    private String email_id;
    private String SCOPES = "oauth2:https://www.googleapis.com/auth/userinfo.profile " +
            "https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/drive.file";
    private String GOOGLE_TOKEN = "";
    private String TWITTER_CONSUMER_KEY = "3lY01ml8RVEanlfuloDghA";
    private String TWITTER_CONSUMER_SECRET = "HfbgxioevcJOS8CJMitfewDTR88KW0cqol8bekeSk";

    // Preference Constants
    static String PREFERENCE_NAME = "twitter_oauth";
    static final String PREF_KEY_OAUTH_TOKEN = "oauth_token";
    static final String PREF_KEY_OAUTH_SECRET = "oauth_token_secret";
    static final String PREF_KEY_TWITTER_LOGIN = "isTwitterLogedIn";

    static final String TWITTER_CALLBACK_URL = "oauth://t4jsample";

    // Twitter oauth urls
    static final String URL_TWITTER_AUTH = "auth_url";
    static final String URL_TWITTER_OAUTH_VERIFIER = "oauth_verifier";
    static final String URL_TWITTER_OAUTH_TOKEN = "oauth_token";

    private final int FACEBOOK_REQUEST_CODE  = 1;
    private final int GOOGLE_REQUEST_CODE  = 2;
    private final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.login);

        context = this;

        cd = new ConnectionDetector(this);

        facebook_button = (ImageView)findViewById(R.id.facebook_login_button);
        facebook_button.setOnClickListener(this);
        google_button = (ImageView)findViewById(R.id.google_login_button);
        google_button.setOnClickListener(this);
        twitter_button = (ImageView)findViewById(R.id.twitter_login_button);
        twitter_button.setOnClickListener(this);

        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);

        new TwitterUserDetails().execute(null);
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){

            case R.id.facebook_login_button:{ // FACEBOOK

                Toast.makeText(LoginActivity.this,"FACEBOOK_CLICKED",Toast.LENGTH_SHORT).show();

                Session session = new Session(this);
                Session.setActiveSession(session);

                Session.OpenRequest open_session = new Session.OpenRequest(this).setCallback
                        (statusCallback);
                open_session.setPermissions("email");
                session.openForRead(open_session);

                break;
            }
            case R.id.google_login_button:{ // GOOGLE

                Toast.makeText(LoginActivity.this,"GOOGLE_CLICKED",Toast.LENGTH_SHORT).show();

                Intent account_picker = AccountPicker.newChooseAccountIntent(null, null, new String[]{"com.google"},
                        false, null, null, null, null);
                startActivityForResult(account_picker, 2);
                break;
            }
            case R.id.twitter_login_button:{ // Twitter
                new TwitterLoginTask().execute(null);
                break;
            }
        }
    }

    //---------- NEEDED For Google User Details---------------------//

    private static String readResponse(InputStream is) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[2048];
        int len = 0;
        while ((len = is.read(data, 0, data.length)) >= 0) {
            bos.write(data, 0, len);
        }
        return new String(bos.toByteArray(), "UTF-8");
    }

    private String getDetail(String jsonResponse,String key) throws JSONException {
        JSONObject profile = new JSONObject(jsonResponse);
        return profile.getString(key);
    }

    //----------END NEEDED For Google User Details---------------------//


    private void getFacebookUserDetails(Session session){

        Toast.makeText(LoginActivity.this,"IN USER DETAILS",Toast.LENGTH_LONG).show();
        FACEBOOK_TOKEN = session.getAccessToken();
        Log.i("PIS_fb_token", FACEBOOK_TOKEN);
        // make request to get facebook user info
        Request.newMeRequest(session, new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser user, Response response) {
                Log.i("fb", "fb user: " + user.toString());

                String fbId = user.getId();
                String fbName = user.getName();
                String gender = user.asMap().get("gender").toString();

                String email = "sds";

                if(user.asMap().get("email")!=null){
                    email = user.asMap().get("email").toString();
                }

                Toast.makeText(LoginActivity.this,"Details"+fbId+" "+fbName+" "+gender+" "+email,
                        Toast.LENGTH_LONG).show();
            }
        }).executeAsync();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){

            case FACEBOOK_REQUEST_CODE:{
                Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
                break;
            }
            case GOOGLE_REQUEST_CODE:{
                if (resultCode == RESULT_OK) {
                    email_id = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    Log.i("PIS_GOOGLE", email_id);
                    new GoogleTokenTask().execute(new String[]{email_id});
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "You must pick an account", Toast.LENGTH_SHORT).show();
                }
                break;
            }

        }
    }

    private class SessionStatusCallback implements Session.StatusCallback {

        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (session.isOpened()) {
                getFacebookUserDetails(session);
            }
        }
    }

    private class GoogleTokenTask extends AsyncTask<String, Integer, String> {

        private ProgressDialog progressDialog;
        String response_data="";

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(LoginActivity.context);
            progressDialog.setMessage("Please Wait...");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... email) {
            try{
                GOOGLE_TOKEN = GoogleAuthUtil.getToken(LoginActivity.context,email[0],SCOPES);
                //Toast.makeText(LoginActivity.context,GOOGLE_TOKEN,Toast.LENGTH_LONG).show();
                Log.i("PIS_GOOGGLE_TOKEN", GOOGLE_TOKEN);
            }catch (IOException ex){
                Toast.makeText(LoginActivity.context,"CONNECTION ERROR",Toast.LENGTH_LONG).show();
            }catch (UserRecoverableAuthException e){
                if (e instanceof GooglePlayServicesAvailabilityException) {
                    // The Google Play services APK is old, disabled, or not present.
                    // Show a dialog created by Google Play services that allows
                    // the user to update the APK
                    int statusCode = ((GooglePlayServicesAvailabilityException)e)
                            .getConnectionStatusCode();
                    Dialog dialog = GooglePlayServicesUtil.getErrorDialog(statusCode,
                            LoginActivity.this,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                    dialog.show();
                } else if (e instanceof UserRecoverableAuthException) {
                    // Unable to authenticate, such as when the user has not yet granted
                    // the app access to the account, but the user can fix this.
                    // Forward the user to an activity in Google Play services.
                    Intent intent = ((UserRecoverableAuthException)e).getIntent();
                    startActivityForResult(intent,
                            REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
                }
            }catch (GoogleAuthException ex){
                Log.e("PIS",ex.getMessage());
            }
            return GOOGLE_TOKEN;
        }

        @Override
        protected void onPostExecute(String result) {
            // process the result
            progressDialog.dismiss();
            super.onPostExecute(result);

            new GoogleUserDetailTask().execute(null);
        }

    }

    private class GoogleUserDetailTask extends AsyncTask<String, Integer, String> {

        private ProgressDialog progressDialog;
        String response_data="";

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(LoginActivity.context);
            progressDialog.setMessage("Please Wait...");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... token) {
            String name = "ERROR" ;
            try{
                URL url = new URL("https://www.googleapis.com/oauth2/v1/userinfo?access_token=" + GOOGLE_TOKEN);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                int sc = con.getResponseCode();
                if (sc == 200) {
                    InputStream is = con.getInputStream();
                    name = getDetail(readResponse(is), "given_name");
                    Log.i("PIS_GOOGLE_NAMW","Hello: " + name);
                    is.close();
                } else if (sc == 401) {
                    GoogleAuthUtil.invalidateToken(LoginActivity.this, GOOGLE_TOKEN);
                    Log.i("PIS_GOOGLE","Server auth error, please try again.");
                    Log.i("PIS_GOOGLE", "Server auth error: " + readResponse(con.getErrorStream()));
                } else {
                    Log.i("PIS_GOOGLE","Server returned the following error code: " + sc);
                }
            }catch (MalformedURLException ex){

            }catch (IOException ex){

            }catch (JSONException ex){

            }
            return name;
        }

        @Override
        protected void onPostExecute(String result) {
            // process the result
            progressDialog.dismiss();
            super.onPostExecute(result);
        }

    }

    private class TwitterLoginTask extends AsyncTask<String, Integer, String> {

        private ProgressDialog progressDialog;
        String response_data="";

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(LoginActivity.context);
            progressDialog.setMessage("Please Wait...");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... token) {

            ConfigurationBuilder builder = new ConfigurationBuilder();
            builder.setOAuthConsumerKey(TWITTER_CONSUMER_KEY);
            builder.setOAuthConsumerSecret(TWITTER_CONSUMER_SECRET);
            Configuration configuration = builder.build();

            TwitterFactory factory = new TwitterFactory(configuration);
            twitter = factory.getInstance();

            try {
                requestToken = twitter.getOAuthRequestToken(TWITTER_CALLBACK_URL);
                LoginActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, Uri
                        .parse(requestToken.getAuthenticationURL())));
            } catch (TwitterException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            // process the result
            progressDialog.dismiss();
            super.onPostExecute(result);

        }

    }

    private class TwitterUserDetails extends AsyncTask<String, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... token) {

            Uri uri = getIntent().getData();
            if (uri != null && uri.toString().startsWith(TWITTER_CALLBACK_URL)) {
                // oAuth verifier
                String verifier = uri.getQueryParameter(URL_TWITTER_OAUTH_VERIFIER);

                try {
                    // Get the access token
                    AccessToken accessToken = twitter.getOAuthAccessToken( requestToken, verifier);

                    Log.i("PIS_TWITTER_TOKEN",accessToken.getToken());
                    Log.i("PIS_TWITTER_SECRET",accessToken.getTokenSecret());
                    Log.i("Name ", accessToken.getScreenName());

                    long userID = accessToken.getUserId();
                    User user = twitter.showUser(userID);
                    String name= user.getName();

                    Log.i("NAME", name);

                } catch (Exception e) {
                    // Check log for login errors
                    Log.e("Twitter Login Error", "> " + e.getMessage());
                }
            }

            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

    }
}

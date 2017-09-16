package com.cjapps.playinsync;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.LoggingBehavior;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.Settings;

import net.londatiga.android.twitter.Twitter;
import net.londatiga.android.twitter.TwitterRequest;
import net.londatiga.android.twitter.TwitterUser;
import net.londatiga.android.twitter.oauth.OauthAccessToken;

import java.util.Arrays;
import java.util.List;

public class SocialConnectActivity extends Activity {

    private String LOG_TAG = "com.cjapps.playinsync";

    // Facebook Variables
    private Session.StatusCallback login_statusCallback = new LoginSessionStatusCallback();

    // Twitter Variables
    private Twitter mTwitter;
    public static final String CONSUMER_KEY = "3lY01ml8RVEanlfuloDghA";
    public static final String CONSUMER_SECRET = "HfbgxioevcJOS8CJMitfewDTR88KW0cqol8bekeSk";
    public static final String CALLBACK_URL = "http://www.playinsync.com";

    private Intent main_activity;
    private Activity social_connect_activity;

    private DatabaseManager db_manager;

    private Button twitter_connect;
    private Button facebook_connect;

    private SharedPreferences.Editor config_editor;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_social_connect);

        db_manager = new DatabaseManager(this);
        SharedPreferences config = getSharedPreferences("config", MODE_PRIVATE);
        config_editor = config.edit();

        mTwitter = new Twitter(this, CONSUMER_KEY, CONSUMER_SECRET, CALLBACK_URL);

        Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_RAW_RESPONSES);

        main_activity = new Intent(this,MainActivity.class);
        social_connect_activity = this;

        facebook_connect = (Button) findViewById(R.id.facebook_connect_button);
        twitter_connect = (Button) findViewById(R.id.twitter_connect_button);
        Button skip = (Button) findViewById(R.id.skip_button);


        if(config.getBoolean("twitter_sign_in_done",false)){
            twitter_connect.setVisibility(View.GONE);
        }
        if(config.getBoolean("twitter_sign_in_done",false)){
            twitter_connect.setVisibility(View.GONE);
        }

        facebook_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Session session = new Session(SocialConnectActivity.this);
                Session.setActiveSession(session);

                Session.OpenRequest open_session = new Session.OpenRequest(SocialConnectActivity.this)
                        .setCallback(login_statusCallback);
                open_session.setPermissions("public_profile", "user_friends", "user_actions.music"
                        , "publish_actions");
                session.openForPublish(open_session);
            }
        });

        twitter_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signinTwitter();
            }
        });

        skip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // START CREATE PROFILE ACTIVITY
                config_editor.putBoolean("social_connect_skipped",true);
                config_editor.apply();
                social_connect_activity.finish();
                startActivity(main_activity);
            }
        });

    }

    private void signinTwitter() {
        mTwitter.signin(new Twitter.SigninListener() {
            @Override
            public void onSuccess(OauthAccessToken accessToken, String userId, String screenName) {
                //success
                getTwitterCredentials();
            }

            @Override
            public void onError(String error) {
                //error
                Toast.makeText(social_connect_activity,"Error signing into your account"
                        ,Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getTwitterCredentials() {
        final ProgressDialog progressDlg = new ProgressDialog(this);

        progressDlg.setMessage("Getting credentials...");
        progressDlg.setCancelable(false);

        progressDlg.show();

        TwitterRequest request = new TwitterRequest(mTwitter.getConsumer(), mTwitter.getAccessToken());

        Log.e(LOG_TAG, "twitter_token " + mTwitter.getAccessToken().getToken());
        Log.e(LOG_TAG, "twitter_secret " + mTwitter.getAccessToken().getSecret());

        request.verifyCredentials(new TwitterRequest.VerifyCredentialListener() {

            @Override
            public void onSuccess(TwitterUser user) {
                progressDlg.dismiss();
                Log.e(LOG_TAG , "Hello " + user.name);

                if(db_manager.updateMyDetails("","","","","","","","","",mTwitter.getAccessToken().getToken()
                        ,mTwitter.getAccessToken().getSecret(),"")){
                    config_editor.putBoolean("twitter_sign_in_done",true);
                    config_editor.apply();
                }

                twitter_connect.setVisibility(View.GONE);
                Toast.makeText(social_connect_activity,"Connected with Twitter",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                progressDlg.dismiss();
                Log.e(LOG_TAG , "Error in credentials " + error);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
    }

    private class LoginSessionStatusCallback implements Session.StatusCallback {

        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (session.isOpened()) {
                getFacebookToken(session);
            }
        }
    }

    private void getFacebookToken(Session session){

        String FACEBOOK_TOKEN = session.getAccessToken();
        long expirationDate = session.getExpirationDate().getTime();
        List<String> permissions = session.getPermissions();

        String permission_string = "";

        Log.e(LOG_TAG, "fb_token" + FACEBOOK_TOKEN);
        Log.e(LOG_TAG, "token_expiration_date" + expirationDate +"");
        Log.e(LOG_TAG, "permissions" + Arrays.toString(permissions.toArray()));

        for (String permission : permissions) {
            permission_string += permission + ",";
        }
        permission_string = permission_string.substring(0,permission_string.length()-1);

        if(db_manager.updateMyDetails("","","","","","", FACEBOOK_TOKEN, expirationDate +""
                ,permission_string,"","","")){

            config_editor.putBoolean("facebook_sign_in_done",true);
            config_editor.apply();

            Toast.makeText(social_connect_activity,"Connected with Facebook",Toast.LENGTH_SHORT).show();

            facebook_connect.setVisibility(View.GONE);
        }


        /* make request to get facebook user info
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

                Log.e("Details", fbId + " " + fbName + " " + gender + " " + email);
            }
        }).executeAsync();
        */
    }
}

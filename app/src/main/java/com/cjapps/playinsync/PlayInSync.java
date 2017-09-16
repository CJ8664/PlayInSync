package com.cjapps.playinsync;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import fr.castorflex.android.smoothprogressbar.SmoothProgressBar;


public class PlayInSync extends Activity {

    private String LOG_TAG = "com.cjapps.playinsync";

    private AsyncTask<String, String, String> load_songs;
    private AsyncTask<String, String, String> load_contacts;

    private Intent google_sign_in_activity;
    private Intent social_connect_activity;
    private Intent main_activity;

    private SharedPreferences config;
    private SharedPreferences.Editor config_editor;

    private DatabaseManager db_manager;

    private TextView setting_up;
    private SmoothProgressBar setting_up_progress;

    private HashMap<String,String> hm_number = new HashMap<String, String>(10);

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        setting_up = (TextView)findViewById(R.id.setting_up_tv);
        setting_up_progress = (SmoothProgressBar)findViewById(R.id.setting_up_pb);

        google_sign_in_activity = new Intent(this,GoogleSignInActivity.class);
        social_connect_activity = new Intent(this,SocialConnectActivity.class);
        main_activity = new Intent(this,MainActivity.class);

        db_manager = new DatabaseManager(this);
        config = getSharedPreferences("config", MODE_PRIVATE);
        config_editor = config.edit();

        load_songs = new LoadSongsInDatabase();
        load_contacts = new LoadContactsInDatabase();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // SHOW THE SPLASH SCREEN
        ImageView splash_logo_text = (ImageView) findViewById(R.id.splash_logo_text);

        if(config.getBoolean("google_sign_in_done",false)) {
            splash_logo_text.setVisibility(View.INVISIBLE);
        }

        Animation fade_in = AnimationUtils.loadAnimation(this, R.anim.animation_fade_in);

        splash_logo_text.startAnimation(fade_in);

        fade_in.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                if (load_songs.getStatus() != AsyncTask.Status.RUNNING)
                load_songs.execute();
                if (load_contacts.getStatus() != AsyncTask.Status.RUNNING)
                load_contacts.execute();
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setting_up.setVisibility(View.VISIBLE);
                setting_up_progress.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

    }

    private class LoadContactsInDatabase extends AsyncTask<String,String,String> {

        int people_count = 0;

        @Override
        protected String doInBackground(String... strings) {
            // LOAD CONTACTS

            Uri PhoneCONTENT_URI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
            String Phone_CONTACT_ID = ContactsContract.CommonDataKinds.Phone.CONTACT_ID;
            String NUMBER = ContactsContract.CommonDataKinds.Phone.NUMBER;

            ContentResolver contentResolver = getContentResolver();

            Cursor unique_phone = contentResolver.query(PhoneCONTENT_URI
                    ,new String[]{NUMBER, Phone_CONTACT_ID},null,null,null);

            int unique_cursor_count = unique_phone.getCount();

            int unique_cursor_count_sp = config.getInt("unique_cursor_count",0);

            if(unique_cursor_count != unique_cursor_count_sp ) {

                config_editor.putInt("unique_cursor_count", unique_cursor_count);
                config_editor.apply();

                while (unique_phone.moveToNext()) { // GET UNIQUE PHONE NUMBERS

                    String phoneNumber = unique_phone.getString(0);
                    int length = phoneNumber.length() - 1;
                    if (length >= 10) {

                        phoneNumber = phoneNumber.replace(" ", "");
                        phoneNumber = phoneNumber.replace("-", "");
                        length = phoneNumber.length();
                        phoneNumber = phoneNumber.substring(length - 10, length);

                        String contact_id = unique_phone.getString(1);

                        if (!hm_number.containsKey(phoneNumber)) {
                            hm_number.put(phoneNumber, contact_id);
                        }
                    }
                }

                unique_phone.close();

                int hash_size = hm_number.size();
                int hash_size_sp = config.getInt("people_count", 0);

                if ( hash_size != hash_size_sp) {  // CONTACTS CHANGED

                    ArrayList<String> in_db_contact_pn = db_manager.getAllContactPhoneNumber();

                    if(hash_size>hash_size_sp) { // NEW CONTACTS

                        File phone_csv = null;
                        BufferedWriter brw = null;
                        try {
                            phone_csv = File.createTempFile("phone_numbers-", ".txt", getCacheDir());
                            brw = new BufferedWriter(new FileWriter(phone_csv));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        for (String phone : in_db_contact_pn) {  // GET NEW DISTINCT CONTACTS
                            hm_number.remove(phone);
                        }

                        for (String phone : hm_number.keySet()) {

                            String id = hm_number.get(phone);
                            try {
                                if (brw != null) {
                                    brw.append(phone).append("$").append(id).append("|");
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        try {
                            if (brw != null) {
                                brw.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        new CheckUserAndGetDPURL().execute(phone_csv); // CONTACTS TO CHECK FOR REGISTRATION

                    } else { // CONTACT DELETED

                        for (String phone : hm_number.keySet()) {
                            in_db_contact_pn.remove(phone);
                        }

                        db_manager.deleteContacts(in_db_contact_pn);

                        int count_in_sp = config.getInt("people_count",0);

                        count_in_sp-=in_db_contact_pn.size();

                        config_editor.putInt("people_count", count_in_sp);
                        config_editor.apply();

                        Log.e(LOG_TAG, in_db_contact_pn.size() + " contacts deleted");
                    }
                }
            } else {
                Log.e(LOG_TAG, "No new contact");
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {

            if(load_songs.getStatus() == Status.FINISHED){

                // START GOOGLE SIGN IN ACTIVITY
                PlayInSync.this.finish();
                if(!config.getBoolean("google_sign_in_done",false)) {
                    startActivity(google_sign_in_activity);
                } else if (config.getBoolean("social_connect_skipped",false) |
                        config.getBoolean("twitter_sign_in_done",false) |
                        config.getBoolean("facebook_sign_in_done",false) ){
                    startActivity(main_activity);
                } else {
                    startActivity(social_connect_activity);
                }
            }
        }

        class CheckUserAndGetDPURL extends AsyncTask<File,String,String>{

            @Override
            protected String doInBackground(File... files) {

                String postReceiverUrl = "http://www.playinsync.com/mobile/get_contact_details.php";

                try{

                    HttpClient httpClient = new DefaultHttpClient();
                    HttpPost httpPost = new HttpPost(postReceiverUrl);

                    FileBody fileBody = new FileBody(files[0]);

                    MultipartEntityBuilder builder = MultipartEntityBuilder.create();

                    builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

                    builder.addPart("file", fileBody);
                    httpPost.setEntity(builder.build());

                    HttpResponse response = httpClient.execute(httpPost);

                    if (response != null) {
                        InputStream in = response.getEntity().getContent();

                        String line;
                        BufferedReader rd = new BufferedReader(new InputStreamReader(in));

                        for(String phone : hm_number.keySet()){
                            String id = hm_number.get(phone);
                            line = rd.readLine();
                            String[] details = line.split("#");

                            for(int i = 0 ; i < details.length ; i++){
                                if(details[i].equals("")){
                                    details[i] = "NA";
                                }
                            }

                            db_manager.addContact(id,details[0],phone,details[1],details[2],details[3]);
                            people_count++;
                        }
                    }

                }catch (Exception ignored){

                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                int count_in_sp = config.getInt("people_count",0);

                people_count+=count_in_sp;

                config_editor.putInt("people_count", people_count);
                config_editor.apply();
                Log.e(LOG_TAG, people_count+ " new contacts added");
            }
        }
    }


    private class LoadSongsInDatabase extends AsyncTask<String,String,String> {

        @Override
        protected String doInBackground(String... strings) {

            // LOAD SONGS

            int count = 0;

            String[] projection = {
                    MediaStore.Audio.Media._ID,             // 0
                    MediaStore.Audio.Media.DISPLAY_NAME,    // 1
                    MediaStore.Audio.Media.TITLE,           // 2
                    MediaStore.Audio.Media.TRACK,           // 3
                    MediaStore.Audio.Media.ARTIST,          // 4
                    MediaStore.Audio.Media.COMPOSER,        // 5
                    MediaStore.Audio.Media.DURATION,        // 6
                    MediaStore.Audio.Media.ALBUM,           // 7
                    MediaStore.Audio.Media.YEAR,            // 8
                    MediaStore.Audio.Media.SIZE,            // 9
                    MediaStore.Audio.Media.DATA,            // 10
                    MediaStore.Audio.Media.ALBUM_ID,        // 11
                    MediaStore.Audio.Media.DATE_ADDED       // 12
            };

            String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND "
                    + MediaStore.Audio.Media.DURATION + " >= 120000";

            Cursor c;

            c = getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    MediaStore.Audio.Media.DATE_ADDED + " DESC");

            int song_cursor_count = c.getCount();

            int sp_song_count = config.getInt("song_count",0);

            if( song_cursor_count != sp_song_count) { // SONG ADDED OR DELETED

                ArrayList<String> in_db_ids = db_manager.getAllSongIDS();

                Log.e(LOG_TAG,"COUNTS " + song_cursor_count + ", " + sp_song_count + ", " + in_db_ids.size());

                if(song_cursor_count > in_db_ids.size()){ // NEW SONGS

                    while (c.moveToNext()) {

                        String id = c.getString(0);

                        if(!in_db_ids.contains(id)) {

                            String title = c.getString(2);
                            title = title.substring(0, 1).toUpperCase() + title.substring(1, title.length());

                            long duration = Long.parseLong(c.getString(6)) / 1000;

                            boolean result = db_manager.addSong(id, c.getString(10), title, c.getString(3)
                                    , c.getString(4), c.getString(5), duration + "", c.getString(9)
                                    , "lyrics", "genre", c.getString(7), c.getString(11), "0");

                            if (!result) {
                                Log.e(LOG_TAG, "Song already exits");
                            }

                            count++;
                        }
                    }

                    Log.e(LOG_TAG,  count + " new songs added");
                    count+=sp_song_count;

                } else { // SONGS REMOVED

                    while (c.moveToNext()) {
                        in_db_ids.remove(c.getString(0));
                    }

                    db_manager.deleteSong(in_db_ids);

                    count = sp_song_count - in_db_ids.size();
                }

                config_editor.putInt("song_count", count);
                config_editor.apply();

                // LOAD ALBUMS

                int album_count = db_manager.createAlbumsTable(PlayInSync.this);

                config_editor.putInt("album_count", album_count);
                config_editor.apply();

                Log.e(LOG_TAG,album_count + " albums added");

            } else {

                Log.e(LOG_TAG, "No change in songs");
            }

            config_editor.putBoolean("first_load_of_songs", false);
            config_editor.apply();

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if(load_contacts.getStatus() == Status.FINISHED){

                // START GOOGLE SIGN IN ACTIVITY
                PlayInSync.this.finish();
                if(!config.getBoolean("google_sign_in_done",false)) {
                    startActivity(google_sign_in_activity);
                } else if ((config.getBoolean("social_connect_skipped",false) |
                    config.getBoolean("twitter_sign_in_done",false) |
                    config.getBoolean("facebook_sign_in_done",false) )) {
                    startActivity(main_activity);
                } else {
                    startActivity(social_connect_activity);
                }
            }
        }
    }
}
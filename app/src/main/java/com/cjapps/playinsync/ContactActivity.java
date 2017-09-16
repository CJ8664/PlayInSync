package com.cjapps.playinsync;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

public class ContactActivity extends Activity {

    private TextView name;
    private TextView last_played;
    private ImageView profile_pic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact);

        name = (TextView)findViewById(R.id.contact_name);
        last_played = (TextView)findViewById(R.id.contact_last_played);
        profile_pic = (ImageView)findViewById(R.id.contact_dp_iv);

        Intent calling_intent = getIntent();
        String name_s = calling_intent.getStringExtra("name");
        String last_played_s = calling_intent.getStringExtra("last_played");
        String dp_url = calling_intent.getStringExtra("dp_url");

        name.setText(name_s);
        last_played.setText(last_played_s);
        MainActivity.song_art_loader.DisplayImage(dp_url,R.drawable.com_facebook_profile_default_icon,profile_pic);

        TabHost tabHost = (TabHost)findViewById(R.id.tabhost_contact);
        tabHost.setup();

        TabSpec favourites = tabHost.newTabSpec("First Tab");
        TabSpec recently_added = tabHost.newTabSpec("Second Tab");

        favourites.setIndicator("Favourites");
        favourites.setContent(R.id.tab_content_contact_favourites);

        recently_added.setIndicator("Recently Added");
        recently_added.setContent(R.id.tab_content_contact_recently_added);

        tabHost.addTab(favourites);
        tabHost.addTab(recently_added);
    }

}

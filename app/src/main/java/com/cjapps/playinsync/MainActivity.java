package com.cjapps.playinsync;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nineoldandroids.view.animation.AnimatorProxy;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;


public class MainActivity extends Activity implements ActionBar.TabListener {

    private ViewPager mViewPager;

    public static ImageLoader song_art_loader;

    public static Context main_context;
    private String LOG_TAG = "com.cjapps.playinsync";

    // NOW PLAYING VIEW

    public static RelativeLayout now_playing;

    public static ImageView now_playing_album_art;
    public static ImageView expanded_now_playing_album_art;

    public static TextView now_playing_song;
    public static TextView now_playing_artist;
    public static TextView now_playing_coming_up;

    public static Button now_playing_play_pause;
    public static Button now_playing_next;
    public static Button now_playing_previous;

    public ActionBar actionBar;

    private SlidingUpPanelLayout sliding_pane;

    //private String LOG_TAG = "com.cjapps.playinsync";
    public static final String SAVED_STATE_ACTION_BAR_HIDDEN = "saved_state_action_bar_hidden";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        song_art_loader = new ImageLoader(this);
        ImageLoader.MODE = 1;

        main_context = this;
        // Set up the action bar.
        actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }

        FragmentAdapter mSectionsPagerAdapter = new FragmentAdapter(getFragmentManager());

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        //RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mViewPager.getLayoutParams());
        //lp.setMargins(50, 0, 0, 0);
        //mViewPager.setLayoutParams(lp);

        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        actionBar.addTab(actionBar.newTab().setCustomView(R.layout.tab_discover).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setCustomView(R.layout.tab_songs).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setCustomView(R.layout.tab_playlists).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setCustomView(R.layout.tab_albums).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setCustomView(R.layout.tab_artists).setTabListener(this));
        actionBar.addTab(actionBar.newTab().setCustomView(R.layout.tab_people).setTabListener(this));

        //NOW PLAYING CODE

        now_playing = (RelativeLayout)findViewById(R.id.now_playing_view);

        now_playing_album_art =(ImageView)findViewById(R.id.now_playing_album_art);
        expanded_now_playing_album_art =(ImageView)findViewById(R.id.expanded_now_playing_album_art);

        now_playing_song = (TextView)findViewById(R.id.now_playing_song);
        now_playing_artist = (TextView)findViewById(R.id.now_playing_artist);
        now_playing_coming_up = (TextView)findViewById(R.id.now_playing_coming_up);

        now_playing_song.setSelected(true);
        now_playing_coming_up.setSelected(true);

        now_playing_play_pause = (Button)findViewById(R.id.now_playing_play_pause);
        now_playing_next = (Button)findViewById(R.id.now_playing_next);
        now_playing_previous = (Button)findViewById(R.id.now_playing_previous);


        sliding_pane = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        sliding_pane.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float slideOffset) {
                now_playing_play_pause.setVisibility(View.GONE);
                now_playing_next.setVisibility(View.GONE);
                now_playing_previous.setVisibility(View.GONE);
                now_playing_album_art.setVisibility(View.GONE);
            }

            @Override
            public void onPanelCollapsed(View view) {
                now_playing_play_pause.setVisibility(View.VISIBLE);
                now_playing_next.setVisibility(View.VISIBLE);
                now_playing_previous.setVisibility(View.VISIBLE);
                now_playing_album_art.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPanelExpanded(View view) {

            }

            @Override
            public void onPanelAnchored(View view) {}

            @Override
            public void onPanelHidden(View view) {}
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (sliding_pane != null) {
            if (sliding_pane.isActivated()) {
                sliding_pane.setOverlayed(false);
            } else {
                super.onBackPressed();
            }
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        int tab_pos = tab.getPosition();

        View tab_view = actionBar.getTabAt(tab_pos).getCustomView();

        ImageView icon = (ImageView)tab_view.findViewById(R.id.tab_icon);
        TextView icon_des = (TextView)tab_view.findViewById(R.id.tab_icon_description);

        icon_des.setTextColor(getResources().getColor(R.color.purple_light));

        switch (tab_pos){
            case 0:
                icon.setImageResource(R.drawable.discover_selected);
                break;
            case 1:
                icon.setImageResource(R.drawable.song_selected);
                break;
            case 2:
                icon.setImageResource(R.drawable.playlist_selected);
                break;
            case 3:
                icon.setImageResource(R.drawable.album_selected);
                break;
            case 4:
                icon.setImageResource(R.drawable.artist_selected);
                break;
            case 5:
                icon.setImageResource(R.drawable.people_selected);
                break;
        }

        mViewPager.setCurrentItem(tab_pos);

        if(tab.getPosition() == 0 | tab.getPosition() == 5){
            ImageLoader.MODE = 0;
        } else {
            ImageLoader.MODE = 1;
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        int tab_pos = tab.getPosition();

        View tab_view = actionBar.getTabAt(tab_pos).getCustomView();
        ImageView icon = (ImageView)tab_view.findViewById(R.id.tab_icon);
        TextView icon_des = (TextView)tab_view.findViewById(R.id.tab_icon_description);

        icon_des.setTextColor(getResources().getColor(R.color.grey));
        switch (tab_pos){
            case 0:
                icon.setImageResource(R.drawable.discover);
                break;
            case 1:
                icon.setImageResource(R.drawable.song);
                break;
            case 2:
                icon.setImageResource(R.drawable.playlist);
                break;
            case 3:
                icon.setImageResource(R.drawable.album);
                break;
            case 4:
                icon.setImageResource(R.drawable.artist);
                break;
            case 5:
                icon.setImageResource(R.drawable.people);
                break;
        }
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    public class FragmentAdapter extends FragmentPagerAdapter {

        public FragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return DiscoverFragment.newInstance();
                case 1:
                    return SongsFragment.newInstance();
                case 2:
                    return PlaylistsFragment.newInstance();
                case 3:
                    return AlbumsFragment.newInstance();
                case 4:
                    return ArtistsFragment.newInstance();
                case 5:
                    return PeopleFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 6;
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}

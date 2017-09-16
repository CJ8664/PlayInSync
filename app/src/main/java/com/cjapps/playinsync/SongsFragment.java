package com.cjapps.playinsync;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SectionIndexer;
import android.widget.TextView;

import com.cjapps.playinsync.MusicService.MusicBinder;
import com.cjapps.playinsync.SongDataStore.Song;
import com.facebook.AccessToken;
import com.facebook.AccessTokenSource;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;

import net.londatiga.android.twitter.TwitterRequest;
import net.londatiga.android.twitter.oauth.OauthAccessToken;
import net.londatiga.android.twitter.oauth.OauthConsumer;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SongsFragment extends Fragment{


    public FastScrollAdapter fastScrollAdapter;

    public int count = 0;

    public ListView songs_list_view;

    private Context song_fragment_context;

    private SongDataStore song_data_store;

    public static ArrayList<Song> song_store;

    // FOR PLAYING SONG

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;

    public static int current_song_position = 0;
    public static boolean isPlaying;

    private DatabaseManager db_manager;


    // FOR TWITTER

    public static final String CONSUMER_KEY = "3lY01ml8RVEanlfuloDghA";
    public static final String CONSUMER_SECRET = "HfbgxioevcJOS8CJMitfewDTR88KW0cqol8bekeSk";
    public static final String CALLBACK_URL = "http://www.playinsync.com";

    private OauthAccessToken twitter_token;
    private OauthConsumer twitter_consumer;

    public static SongsFragment newInstance() {
        SongsFragment fragment = new SongsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }
    public SongsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        song_fragment_context = MainActivity.main_context;
        song_data_store = new SongDataStore(song_fragment_context);

        db_manager = new DatabaseManager(song_fragment_context);
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicBinder binder = (MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(song_fragment_context, MusicService.class);
            song_fragment_context.bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);

            if(!MusicService.isPlaybackServiceRunning)
            song_fragment_context.startService(playIntent);

            MusicService.isPlaybackServiceRunning = true;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View main_view = inflater.inflate(R.layout.fragment_songs, container, false);
        songs_list_view = (ListView)main_view.findViewById(android.R.id.list);

        song_data_store = new SongDataStore(song_fragment_context);
        initializeAdapter();

        return main_view;
    }

    private void initializeAdapter() {

        songs_list_view.setFastScrollEnabled(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            songs_list_view.setFastScrollAlwaysVisible(true);
        }
        fastScrollAdapter = new FastScrollAdapter(MainActivity.main_context,
                android.R.layout.simple_list_item_1, android.R.id.text1);

        songs_list_view.setAdapter(fastScrollAdapter);
    }

    class SimpleAdapter extends ArrayAdapter<Song> implements PinnedSectionListView.PinnedSectionListAdapter {

        public boolean isMultiSelectOn = false;
        public ActionMode mActionMode;

        public SimpleAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);

            prepareSections();

            song_store = song_data_store.createSongList();

            for (Song item : song_store) {

                if (item.type == Song.SECTION) {
                    onSectionAdded(item, item.sectionPosition);
                }
                add(item);
            }

            count = song_store.size();


            MainActivity.now_playing_play_pause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(!isPlaying){
                        if(musicBound)
                        musicSrv.resume();
                        MainActivity.now_playing_play_pause
                                .setBackgroundResource(android.R.drawable.ic_media_pause);
                        isPlaying = true;
                    }else{
                        if(musicBound)
                        musicSrv.pause();
                        MainActivity.now_playing_play_pause
                                .setBackgroundResource(android.R.drawable.ic_media_play);
                        isPlaying = false;
                    }
                }
            });

            MainActivity.now_playing_next.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    int next_pos = song_store.get(current_song_position).next_pos;
                    if(next_pos!=-1){
                        current_song_position = next_pos;
                        Song next = getItem(next_pos);
                        if(next!=null){
                            if(musicBound)
                            musicSrv.playSong(next_pos);

                            MainActivity.song_art_loader.DisplayImage(next.album_art
                                    ,R.drawable.default_album_art
                                    ,MainActivity.now_playing_album_art);

                            MainActivity.song_art_loader.DisplayImage(next.album_art
                                    , R.drawable.default_album_art
                                    , MainActivity.expanded_now_playing_album_art);

                            MainActivity.now_playing_song.setText(next.title);
                            MainActivity.now_playing_artist.setText(next.artist);

                            MainActivity.now_playing_coming_up.setText("Coming up: "
                                    + getItem(next.next_pos).title);
                        }
                    }
                }
            });

            MainActivity.now_playing_previous.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    int prev_pos = song_store.get(current_song_position).prev_pos;
                    if(prev_pos!=-1){
                        current_song_position = prev_pos;
                        Song prev = getItem(prev_pos);
                        if(prev!=null){
                            if(musicBound)
                            musicSrv.playSong(prev_pos);

                            MainActivity.song_art_loader.DisplayImage(prev.album_art
                                    ,R.drawable.default_album_art
                                    ,MainActivity.now_playing_album_art);

                            MainActivity.song_art_loader.DisplayImage(prev.album_art
                                    , R.drawable.default_album_art
                                    , MainActivity.expanded_now_playing_album_art);

                            MainActivity.now_playing_song.setText(prev.title);
                            MainActivity.now_playing_artist.setText(prev.artist);

                            MainActivity.now_playing_coming_up.setText("Coming up: "
                                    + getItem(prev.next_pos).title);
                        }
                    }
                }
            });
        }

        protected void prepareSections() { }
        protected void onSectionAdded(Song section, int sectionPosition) { }

        private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

            // Called when the action mode is created; startActionMode() was called
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                // Inflate a menu resource providing context menu items
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.main, menu);
                return true;
            }

            // Called each time the action mode is shown. Always called after onCreateActionMode, but
            // may be called multiple times if the mode is invalidated.
            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false; // Return false if nothing is done
            }

            // Called when the user selects a contextual menu item
            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                Log.e("ACTION", "Performed");
                mode.finish(); // Action picked, so close the CAB
                return true;
            }

            // Called when the user exits the action mode
            @Override
            public void onDestroyActionMode(ActionMode mode) {
                isMultiSelectOn = false;
                clearSelection();
                notifyDataSetChanged();
                Log.e("CAB", "onDestroy");
                mActionMode = null;

                // RESTORE NOW PLAYING
                MainActivity.now_playing.setVisibility(View.VISIBLE);
            }
        };


        public void clearSelection(){

            int i = 0;
            Song temp;

            while(i<getCount()){
                temp = getItem(i);
                if(temp.type == Song.SONG){
                    temp.isSelected = false;
                }
                i++;
            }
        }

        @Override public View getView(final int position, View convertView, ViewGroup parent) {

            View song_view = convertView;

            final Song item = getItem(position);

            if (item.type == Song.SECTION) {

                if(song_view == null){
                    song_view = View.inflate(song_fragment_context,R.layout.custom_view_section_header,null);

                    ViewHolder viewHolder = new ViewHolder();

                    viewHolder.section_header = (TextView)song_view.findViewById(R.id.section_header_tv);
                    song_view.setTag(viewHolder);
                }

                ViewHolder holder = (ViewHolder) song_view.getTag();
                holder.section_header.setText(item.title);
                return song_view;
            }
            else{
                if (song_view == null) {
                    song_view = View.inflate(song_fragment_context,
                            R.layout.custom_view_song_list_item_layout,null);

                    ViewHolder viewHolder = new ViewHolder();

                    viewHolder.title = (TextView)song_view.findViewById(R.id.song_title);
                    viewHolder.artist = (TextView)song_view.findViewById(R.id.song_artist);
                    viewHolder.album_art = (ImageView)song_view.findViewById(R.id.album_art_iv);
                    viewHolder.expanded_options = (ImageButton)song_view.findViewById(R.id.expanded_options_b);

                    song_view.setTag(viewHolder);
                }

                ViewHolder holder = (ViewHolder) song_view.getTag();

                holder.title.setText(item.title);
                holder.artist.setText(item.artist);

                MainActivity.song_art_loader.DisplayImage(item.album_art,R.drawable.default_album_art
                        , holder.album_art);


                if(item.isSelected){
                    song_view.setBackgroundColor(getResources().getColor(android.R.color
                            .holo_blue_light));
                }

                holder.expanded_options.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        PopupMenu popup = new PopupMenu(song_fragment_context, view);
                        MenuInflater inflater = popup.getMenuInflater();
                        inflater.inflate(R.menu.song_actions, popup.getMenu());
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                switch (menuItem.getItemId()){
                                    case R.id.song_action_favourite :{

                                        break;
                                    }
                                    case R.id.song_action_add_to_playlist :{
                                        AddToPlaylistDialog dialog = new AddToPlaylistDialog(song_fragment_context);
                                        dialog.setData(item.id+"",item.duration,item.album_art);
                                        dialog.show();
                                        break;
                                    }
                                    case R.id.song_action_share :{

                                        break;
                                    }
                                    case R.id.song_action_post_on_facebook :{

                                        String[] details = db_manager.getFacebookTokenDetails();


                                        String fb_token = details[0];
                                        Date expiry = new Date(Long.parseLong(details[1]));
                                        String[] temp = details[2].split(",");
                                        List<String> permission = new ArrayList<String>();

                                        Log.e("token",fb_token);
                                        Log.e("date",expiry.toString());

                                        for (String perm:temp){
                                            permission.add(perm);
                                            Log.e("perm",perm);
                                        }

                                        AccessToken at = AccessToken.createFromExistingAccessToken(fb_token, expiry, null
                                                , AccessTokenSource.WEB_VIEW, permission);

                                        Session session = Session.openActiveSessionWithAccessToken(song_fragment_context
                                                , at, null);

                                        Bundle params = new Bundle();
                                        params.putString("song", "http://samples.ogp.me/461258627226537");
                                        params.putString("album", "http://samples.ogp.me/461258347226565");
                                        params.putString("playlist", "http://samples.ogp.me/461258467226553");
                                        params.putString("musician", "http://samples.ogp.me/390580850990722");
                                        new Request(
                                                session,
                                                "/me/music.listens",
                                                params,
                                                HttpMethod.POST,
                                                new Request.Callback() {
                                                    public void onCompleted(Response response) {
                                                        Log.e("Response",response.getRawResponse());
                                                    }
                                                }
                                        ).executeAsync();

                                        /*
                                        Bundle params = new Bundle();
                                        params.putString("message", "My post using My App Yo!");
                                        new Request(
                                                session,
                                                "/me/feed",
                                                params,
                                                HttpMethod.POST,
                                                new Request.Callback() {
                                                    public void onCompleted(Response response) {
                                                        Log.e("Response",response.getRawResponse());
                                                    }
                                                }
                                        ).executeAsync();
                                        */

                                        break;
                                    }
                                    case R.id.song_action_post_on_twitter :{

                                        String[] token_details = db_manager.getTwitterTokenDetails();

                                        Log.e("token",token_details[0]);
                                        Log.e("secret",token_details[1]);
                                        tweet("#NowPlaying " + item.title + "-" + item.artist + " #PlayInSync");

                                        break;
                                    }
                                    case R.id.song_action_tag_people :{
                                        break;
                                    }
                                }
                                return true;
                            }
                        });
                        popup.show();
                    }
                });

                song_view.setLongClickable(true);
                song_view.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {

                        if (mActionMode != null) {
                            return false;
                        }

                        // Start the CAB using the ActionMode.Callback defined above
                        mActionMode = getActivity().startActionMode(mActionModeCallback);

                        item.isSelected = true;

                        view.setBackgroundColor(getResources().getColor(android.R.color
                                .holo_blue_light));
                        isMultiSelectOn = true;

                        // HIDE NOW PLAYING
                        MainActivity.now_playing.setVisibility(View.GONE);
                        return true;
                    }
                });
                song_view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(isMultiSelectOn){
                            if(!item.isSelected){
                                item.isSelected = true;
                                view.setBackgroundColor(getResources().getColor(android.R.color
                                        .holo_blue_light));
                            }
                            else {
                                item.isSelected = false;
                                view.setBackgroundColor(Color.WHITE);
                            }
                        }else{

                            MainActivity.song_art_loader.DisplayImage(item.album_art
                                    , R.drawable.default_album_art
                                    , MainActivity.now_playing_album_art);

                            MainActivity.song_art_loader.DisplayImage(item.album_art
                                    , R.drawable.default_album_art
                                    , MainActivity.expanded_now_playing_album_art);

                            MainActivity.now_playing_song.setText(item.title);
                            MainActivity.now_playing_artist.setText(item.artist);

                            MainActivity.now_playing_coming_up.setText("Coming up: "
                                    + getItem(item.next_pos).title);

                            MainActivity.now_playing_play_pause
                                    .setBackgroundResource(android.R.drawable.ic_media_pause);

                            if(musicBound)
                            musicSrv.playSong(position);

                            current_song_position = position;

                            isPlaying = true;


                        }
                    }
                });
                return song_view;
            }

        }

        private void tweet(String status) {

            final ProgressDialog progressDlg = new ProgressDialog(song_fragment_context);

            progressDlg.setMessage("Sending...");
            progressDlg.setCancelable(false);

            progressDlg.show();

            String[] token_details = db_manager.getTwitterTokenDetails();

            twitter_consumer = new OauthConsumer(CONSUMER_KEY,CONSUMER_SECRET,CALLBACK_URL);
            twitter_token = new OauthAccessToken(token_details[0],token_details[1]);


            TwitterRequest request      = new TwitterRequest(twitter_consumer, twitter_token);

            String updateStatusUrl      = "https://api.twitter.com/1.1/statuses/update.json";

            List<NameValuePair> params  = new ArrayList<NameValuePair>(1);

            params.add(new BasicNameValuePair("status", status));

            request.createRequest("POST", updateStatusUrl, params, new TwitterRequest.RequestListener() {

                @Override
                public void onSuccess(String response) {
                    progressDlg.dismiss();

                    Log.e("Response",response);
                }

                @Override
                public void onError(String error) {
                    Log.e("Error", error);

                    progressDlg.dismiss();
                }
            });
        }

        @Override public int getViewTypeCount() {
            return 2;
        }

        @Override public int getItemViewType(int position) {
            return getItem(position).type;
        }

        @Override
        public boolean isItemViewTypePinned(int viewType) {
            return viewType == Song.SECTION;
        }

    }

    static class ViewHolder {
        TextView title;
        TextView artist;
        ImageView album_art;
        ImageButton expanded_options;

        TextView section_header;
    }

    class FastScrollAdapter extends SimpleAdapter implements SectionIndexer {

        private ArrayList<Song> sections ;

        public FastScrollAdapter(Context context, int resource, int textViewResourceId) {
            super(context, resource, textViewResourceId);
        }

        @Override protected void prepareSections() {
            sections = new ArrayList<Song>(10);
        }

        @Override protected void onSectionAdded(Song section, int sectionPosition) {
            sections.add(sectionPosition, section);
        }

        @Override
        public Song[] getSections() {

            Song[] temp = new Song[sections.size()];
            for(int i=0;i<temp.length;i++){
                temp[i] = sections.get(i);
            }
            return temp;
        }

        @Override public int getPositionForSection(int section) {
            if (section >= sections.size()) {
                section = sections.size() - 1;
            }
            return sections.get(section).listPosition;
        }

        @Override public int getSectionForPosition(int position) {
            if (position >= getCount()) {
                position = getCount() - 1;
            }
            return getItem(position).sectionPosition;
        }
    }
}

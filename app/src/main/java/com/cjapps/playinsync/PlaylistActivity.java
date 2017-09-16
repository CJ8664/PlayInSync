package com.cjapps.playinsync;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.commonsware.cwac.tlv.TouchListView;
import com.cjapps.playinsync.SongDataStore.Song;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;

public class PlaylistActivity extends ListActivity {

    private PlayListSongAdapter adapter;

    public ArrayList<String[]> song_arraylist;

    private RelativeLayout now_playing;
    private SlidingUpPanelLayout sliding_pane;

    // TODO save the order of songs, deleting songs

    public static final String SAVED_STATE_ACTION_BAR_HIDDEN = "saved_state_action_bar_hidden";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        final ActionBar actionBar = getActionBar();

        now_playing = (RelativeLayout)findViewById(R.id.now_playing_view);
        TouchListView playlist_songs = (TouchListView) getListView();

        playlist_songs.setDivider(null);

        String ids = getIntent().getStringExtra("song_ids");

        DatabaseManager db_manager = new DatabaseManager(this);
        song_arraylist = db_manager.getAllSongs(null);

        ArrayList<Song> songs = new ArrayList<Song>(10);

        for(String[] temp : song_arraylist){
            Song song_temp;
            if(ids.contains(temp[0]+"#")){
                song_temp = new Song(Song.SONG,temp[2], temp[4], temp[11], temp[6], Long.parseLong(temp[0]));
                songs.add(song_temp);
            }
        }

        adapter = new PlayListSongAdapter(this, songs);

        playlist_songs.setAdapter(adapter);


        playlist_songs.setDropListener(onDrop);
        playlist_songs.setRemoveListener(onRemove);

        sliding_pane = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        sliding_pane.setPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View view, float slideOffset) {
                if(slideOffset>0.75){
                    if(actionBar.isShowing())
                        actionBar.hide();
                }else{
                    if(!actionBar.isShowing())
                        actionBar.show();
                }

            }

            @Override
            public void onPanelCollapsed(View view) {
                now_playing.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPanelExpanded(View view) {
                now_playing.setVisibility(View.GONE);
            }

            @Override
            public void onPanelAnchored(View view) {

            }

            @Override
            public void onPanelHidden(View view) {

            }
        });

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_STATE_ACTION_BAR_HIDDEN, sliding_pane.isActivated());
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        /*if (sliding_pane != null && sliding_pane.isPanelExpanded() || sliding_pane.isPanelAnchored()) {
            sliding_pane.collapsePanel();
        } else {
            super.onBackPressed();
        }*/
    }

    private TouchListView.DropListener onDrop = new TouchListView.DropListener() {
        @Override
        public void drop(int from, int to) {
            Song item = adapter.getItem(from);

            adapter.remove(item);
            adapter.insert(item, to);
        }
    };

    private TouchListView.RemoveListener onRemove = new TouchListView.RemoveListener() {
        @Override
        public void remove(int which) {

            adapter.remove(adapter.getItem(which));
            adapter.notifyDataSetChanged();
        }
    };

    class PlayListSongAdapter extends ArrayAdapter<Song>{

        ArrayList<Song> items;

        public PlayListSongAdapter(Context context, ArrayList<Song> items){
            super(context,R.layout.custom_view_song_list_item_layout,items);
            this.items = items;
        }

        @Override
        public boolean hasStableIds() {
            return true;

        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Song getItem(int i) {
            return items.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }


        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {

            View rowView = convertView;

            if (rowView == null) {
                rowView = View.inflate(MainActivity.main_context,
                        R.layout.custom_view_playlist_song_item_layout,null);

                ViewHolder song_view_holder = new ViewHolder();

                song_view_holder.title = (TextView)rowView.findViewById(R.id.song_title);
                song_view_holder.artist = (TextView)rowView.findViewById(R.id.song_artist);
                song_view_holder.album_art = (ImageView)rowView.findViewById(R.id.album_art_iv);

                rowView.setTag(song_view_holder);
            }

            ViewHolder view_holder = (ViewHolder) rowView.getTag();


            Song temp = items.get(i);

            view_holder.title.setText(temp.title);
            view_holder.artist.setText(temp.artist);

            MainActivity.song_art_loader.DisplayImage(temp.album_art,R.drawable.default_album_art
                    , view_holder.album_art);

            return rowView;
        }


    }

    static class ViewHolder {
        TextView title;
        TextView artist;
        ImageView album_art;
    }
}

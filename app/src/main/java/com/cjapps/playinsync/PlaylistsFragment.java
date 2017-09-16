package com.cjapps.playinsync;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class PlaylistsFragment extends Fragment {

    public ListView playlist_list_view;

    private Context playlist_fragment_context;

    public DatabaseManager db_manager;

    public static PlayListAdapter playlist_adapter;

    public static PlaylistsFragment newInstance() {
        PlaylistsFragment fragment = new PlaylistsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public PlaylistsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlist_fragment_context = MainActivity.main_context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_playlists, container, false);
        playlist_list_view = (ListView)root.findViewById(R.id.playlist_list_view);

        playlist_adapter = new PlayListAdapter(playlist_fragment_context);

        playlist_adapter.registerDataSetObserver(new PlaylistModifiedDatasetObserver());
        playlist_list_view.setAdapter(playlist_adapter);
        return root;
    }

    class PlayListAdapter extends BaseAdapter{

        ArrayList<String[]> playlist_item;
        public PlayListAdapter(Context context){
            db_manager = new DatabaseManager(context);
            playlist_item = db_manager.getPlaylistFragmentData();
        }

        @Override
        public int getCount() {
            return playlist_item.size();
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {

            View root = convertView;

            if(root == null) {

                root = View.inflate(MainActivity.main_context, R.layout.custom_view_playlist_item, null);

                ViewHolder viewHolder = new ViewHolder();

                viewHolder.playlist_name = (TextView) root.findViewById(R.id.playlist_name_tv);
                viewHolder.playlist_song_count = (TextView) root.findViewById(R.id.playlist_song_count_tv);
                viewHolder.playlist_duration = (TextView) root.findViewById(R.id.playlist_duration_tv);

                viewHolder.aa = (ImageView)root.findViewById(R.id.playlist_default_aa);

                viewHolder.grid_aa = (RelativeLayout)root.findViewById(R.id.grid_album_art);

                viewHolder.aa1 = (ImageView)root.findViewById(R.id.playlist_aa1);
                viewHolder.aa2 = (ImageView)root.findViewById(R.id.playlist_aa2);
                viewHolder.aa3 = (ImageView)root.findViewById(R.id.playlist_aa3);
                viewHolder.aa4 = (ImageView)root.findViewById(R.id.playlist_aa4);

                root.setTag(viewHolder);

            }

            ViewHolder holder = (ViewHolder) root.getTag();

            final String[] playlist_details = playlist_item.get(i);

            holder.playlist_name.setText(playlist_details[0]);
            holder.playlist_song_count.setText("Total " + playlist_details[2] + " songs");

            int total_seconds = Integer.parseInt(playlist_details[1]);

            int seconds = (total_seconds) % 60 ;
            int minutes = (total_seconds / 60) % 60;
            int hours   = (total_seconds / (60*60)) % 24;

            String duration = "";

            if(hours>0){
                duration += hours + " hrs ";
            }
            if(minutes>0){
                duration+= minutes + " mins ";
            }
            if(seconds>0){
                duration+= seconds + " sec ";
            }

            holder.playlist_duration.setText("Playtime: " + duration);

            int song_count = Integer.parseInt(playlist_details[2]);

            if(song_count<4){

                String[] aa_ids = playlist_details[4].split("#");



                MainActivity.song_art_loader.DisplayImage(aa_ids[aa_ids.length-1]
                        ,R.drawable.default_album_art
                        , holder.aa);
            }else{

                holder.grid_aa.setVisibility(View.VISIBLE);

                String[] aa_ids = playlist_details[4].split("#");

                MainActivity.song_art_loader.DisplayImage(aa_ids[3]
                        ,R.drawable.default_album_art
                        , holder.aa1);
                MainActivity.song_art_loader.DisplayImage(aa_ids[2]
                        ,R.drawable.default_album_art
                        , holder.aa2);
                MainActivity.song_art_loader.DisplayImage(aa_ids[1]
                        ,R.drawable.default_album_art
                        , holder.aa3);
                MainActivity.song_art_loader.DisplayImage(aa_ids[0]
                        ,R.drawable.default_album_art
                        , holder.aa4);

            }


            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent open_playlist = new Intent(MainActivity.main_context,PlaylistActivity.class);
                    open_playlist.putExtra("song_ids",playlist_details[3]);
                    startActivity(open_playlist);
                }
            });

            return root;
        }
    }

    static class ViewHolder {
        TextView playlist_name;
        TextView playlist_song_count;
        TextView playlist_duration;

        ImageView aa;

        RelativeLayout grid_aa;

        ImageView aa1;
        ImageView aa2;
        ImageView aa3;
        ImageView aa4;

    }

    public class PlaylistModifiedDatasetObserver extends DataSetObserver{

        @Override
        public void onChanged() {
            super.onChanged();
            Log.e("","changed");
            playlist_adapter = new PlayListAdapter(playlist_fragment_context);
            playlist_adapter.registerDataSetObserver(new PlaylistModifiedDatasetObserver());
            playlist_list_view.setAdapter(playlist_adapter);
        }
    }
}

package com.cjapps.playinsync;



import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 *
 */
public class AlbumsFragment extends Fragment {

    public GridView album_grid_view;

    public DatabaseManager db_manager;

    public ArrayList<String[]> album_arraylist;

    public static AlbumsFragment newInstance() {
        AlbumsFragment fragment = new AlbumsFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public AlbumsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        db_manager = new DatabaseManager(MainActivity.main_context);

        View root = inflater.inflate(R.layout.fragment_albums, container, false);
        album_grid_view = (GridView)root.findViewById(R.id.album_grid_view);

        album_arraylist = db_manager.getAllAlbums();

        album_grid_view.setAdapter(new AlbumListAdapter());
        return root;
    }

    class AlbumListAdapter extends BaseAdapter {

        ArrayList<Item> albums;
        public AlbumListAdapter(){

            albums = new ArrayList<Item>();

            String name;
            String id;
            String artist;
            int artist_count;

            int index = 0;
            int count = album_arraylist.size();

            while (index < count){

                artist_count = Integer.parseInt(album_arraylist.get(index)[0]);
                name   = album_arraylist.get(index)[1];
                id     = album_arraylist.get(index)[2];
                artist = album_arraylist.get(index)[3];

                albums.add(new Item(name,artist,id,artist_count));
                index++;
            }
        }

        @Override
        public int getCount() {
            return albums.size();
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
        public View getView(int i, View view, ViewGroup viewGroup) {
            View root = View.inflate(MainActivity.main_context,R.layout.custom_view_album_item,null);

            ImageView aa_iv = (ImageView)root.findViewById(R.id.album_grid_album_art);
            TextView an_tv = (TextView)root.findViewById(R.id.album_grid_album_name);
            TextView aa_tv = (TextView)root.findViewById(R.id.album_grid_album_artist);

            Item temp = albums.get(i);

            an_tv.setText(temp.album_name);
            aa_tv.setText(temp.album_artist);

            MainActivity.song_art_loader.DisplayImage(temp.album_id,R.drawable.default_album_art
                    , aa_iv);

            root.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //startActivity(new Intent(MainActivity.main_context,PlaylistActivity.class));
                }
            });

            return root;
        }
    }

    class Item{

        String album_name;
        String album_artist;
        String album_id;
        int artist_count;

        public Item(String album_name, String album_artist, String album_id,int artist_count){

            this.album_name = album_name;
            this.album_id = album_id;
            this.album_artist = album_artist;
            this.artist_count = artist_count;
        }

    }


}

package com.cjapps.playinsync;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class AddToPlaylistDialog extends Dialog {

    private DatabaseManager db_manager;
    private EditText new_playlist_name;

    private static String playlist;
    private static String song_id;
    private static String duration;
    private static String aa_id;

    public AddToPlaylistDialog(final Context context) {
        super(context);
        setContentView(R.layout.custom_dialog_add_to_playlist);
        setTitle("Add To PlayList");
        setCancelable(false);

        ListView playlist_listview = (ListView) findViewById(R.id.existing_playlist_listview);

        Button cancel = (Button) findViewById(R.id.dialog_cancel);
        Button create = (Button) findViewById(R.id.dialog_create);
        new_playlist_name = (EditText)findViewById(R.id.new_playlist_edittext);

        db_manager = new DatabaseManager(context);

        final ArrayList<String> playlist_names = db_manager.getAllPlaylistName();

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(context
                ,android.R.layout.simple_list_item_1,playlist_names);

        playlist_listview.setAdapter(adapter);

        playlist_listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                playlist = playlist_names.get(i);
                if (db_manager.addSongToPlaylist(playlist, song_id, duration, aa_id)) {
                    Toast.makeText(context, "Song added to " + playlist, Toast.LENGTH_SHORT).show();
                    PlaylistsFragment.playlist_adapter.notifyDataSetChanged();
                }
                dismiss();
            }
        });

        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playlist = new_playlist_name.getText().toString();
                if (db_manager.addSongToPlaylist(playlist, song_id, duration, aa_id)) {
                    Toast.makeText(context, "Song added to " + playlist, Toast.LENGTH_SHORT).show();
                }
                dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }

    public void setData(String song_id, String duration, String aa_id){
        AddToPlaylistDialog.song_id = song_id;
        AddToPlaylistDialog.duration = duration;
        AddToPlaylistDialog.aa_id = aa_id;
    }
}

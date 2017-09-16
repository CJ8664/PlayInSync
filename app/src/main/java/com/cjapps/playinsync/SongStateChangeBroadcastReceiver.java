package com.cjapps.playinsync;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.InputStream;


public class SongStateChangeBroadcastReceiver extends BroadcastReceiver {

    private static final String SONG_CHANGED = "com.cjapps.playinsync.SONG_CHANGED";
    private static final String PLAY = "com.cjapps.playinsync.PLAY";
    private static final String PAUSE = "com.cjapps.playinsync.PAUSE";

    public SongStateChangeBroadcastReceiver(){

    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if(action.equals(SONG_CHANGED)) {
            int position = intent.getIntExtra("current_position", 1);

            SongDataStore.Song item = SongsFragment.song_store.get(position);

            MainActivity.song_art_loader.DisplayImage(item.album_art
                    , R.drawable.default_album_art
                    , MainActivity.now_playing_album_art);

            MainActivity.song_art_loader.DisplayImage(item.album_art
                    , R.drawable.default_album_art
                    , MainActivity.expanded_now_playing_album_art);

            MainActivity.now_playing_song.setText(item.title);
            MainActivity.now_playing_artist.setText(item.artist);
            MainActivity.now_playing_play_pause.setBackgroundResource(android.R.drawable.ic_media_pause);

            MainActivity.now_playing_coming_up.setText("Coming up: "
                    + SongsFragment.song_store.get(item.next_pos).title);

            Log.e("BROADCAST", "" + position);

        } else if (action.equals(PLAY)){
            MainActivity.now_playing_play_pause.setBackgroundResource(android.R.drawable.ic_media_pause);
            RemoteViews notification_view = new RemoteViews(getClass().getPackage().getName()
                    , R.layout.notification_main);
            notification_view.setImageViewResource(R.id.notification_play_pause,android.R.drawable.ic_media_pause);

        } else if (action.equals(PAUSE)){
            MainActivity.now_playing_play_pause.setBackgroundResource(android.R.drawable.ic_media_play);
            RemoteViews notification_view = new RemoteViews(getClass().getPackage().getName()
                    , R.layout.notification_main);
            notification_view.setImageViewResource(R.id.notification_play_pause,android.R.drawable.ic_media_play);
        }
    }
}
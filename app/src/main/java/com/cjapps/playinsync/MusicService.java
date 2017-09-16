package com.cjapps.playinsync;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.widget.Toast;

import com.cjapps.playinsync.SongDataStore.Song;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener
        , MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener{

    private MediaPlayer player;

    private Song song;
    private ArrayList<Song> songs;

    private final IBinder musicBind = new MusicBinder();

    private static final int NOTIFY_ID=1;
    public static boolean isPlaybackServiceRunning;

    private AppWidgetManager appWidgetManager;
    private RemoteViews remoteView;

    private int[] allWidgetIds;
    private ComponentName home_widget;
    private ComponentName remote_audio_controller;

    public static int current_position = 1;
    public AudioManager audio_manager;
    public OnAudioFocusChangeListener audio_focus_change_listener;

    public boolean isStoppedViaLoss;
    public int current_time_position;

    public String PLAY_CLICKED = "com.cjapps.playinsync.HomeWidget.PLAY_CLICKED";
    public String PAUSE_CLICKED = "com.cjapps.playinsync.HomeWidget.PAUSE_CLICKED";
    public String NEXT_CLICKED = "com.cjapps.playinsync.HomeWidget.NEXT_CLICKED";
    public String PREV_CLICKED = "com.cjapps.playinsync.HomeWidget.PREV_CLICKED";

    public MusicService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        isPlaybackServiceRunning = true;

        player =new MediaPlayer();
        player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(this);
        player.setOnCompletionListener(this);
        player.setOnErrorListener(this);

        audio_manager = (AudioManager) getSystemService(AUDIO_SERVICE);

        remote_audio_controller = new ComponentName(getApplicationContext()
                ,RemoteAudioControlReceiver.class);

        audio_focus_change_listener = new AudioManager.OnAudioFocusChangeListener() {
            // TODO what happens on a receiving a call
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // TEMPORARY LOSS
                    Log.e("trans","loss");
                    pause();

                }else if (focusChange == AudioManager.AUDIOFOCUS_GAIN_TRANSIENT){
                    Log.e("trans","gain");
                    resume();
                }else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    Log.e("focus","gain");
                    audio_manager.registerMediaButtonEventReceiver(remote_audio_controller);
                    resume();
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    Log.e("focus","loss");
                    audio_manager.unregisterMediaButtonEventReceiver(remote_audio_controller);
                    audio_manager.abandonAudioFocus(audio_focus_change_listener);
                    // Stop playback
                    if (player.isPlaying()) {

                        MainActivity.now_playing_play_pause.setBackgroundResource(android.R.drawable.ic_media_play);
                        current_time_position = player.getCurrentPosition();
                        player.reset();
                        SongsFragment.isPlaying = false;
                        HomeWidget.isPlaying =false;

                        isStoppedViaLoss = true;

                        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

                        remoteView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_home);
                        home_widget = new ComponentName(getApplicationContext(), HomeWidget.class);

                        remoteView.setImageViewResource(R.id.widget_play_pause, android.R.drawable.ic_media_play);
                        allWidgetIds = appWidgetManager.getAppWidgetIds(home_widget);

                        for(int id : allWidgetIds) {
                            appWidgetManager.updateAppWidget(id, remoteView);
                        }
                    }
                }
            }
        };

        SongDataStore song_data_store = new SongDataStore(this);
        songs = song_data_store.createSongList();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isPlaybackServiceRunning = false;
    }

    @Override
    public boolean onUnbind(Intent intent){
        //player.stop();
        //player.release();
        return false;
    }



    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void playSong(int position){

        int result = audio_manager.requestAudioFocus(audio_focus_change_listener, AudioManager.STREAM_MUSIC
                ,AudioManager.AUDIOFOCUS_GAIN);

        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            //play a song
            Log.e("focus","gained in playsong");
            this.song = songs.get(position);
            current_position = position;

            player.reset();

            Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    , song.id);
            try {
                player.setDataSource(getApplicationContext(),trackUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

            player.prepareAsync();

        }else{
            Toast.makeText(getApplicationContext(),"Some app is already playing a song"
                    , Toast.LENGTH_LONG)
                    .show();
        }

    }

    public void pause(){

        Intent now_playing_view_intent = new Intent();
        now_playing_view_intent.setAction("com.cjapps.playinsync.PAUSE");
        sendBroadcast(now_playing_view_intent);
        Log.e("Broadcast", "sent");

        HomeWidget.isPlaying = false;
        player.pause();

        remoteView = new RemoteViews(getApplicationContext().getPackageName()
                , R.layout.widget_home);
        appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

        home_widget = new ComponentName(getApplicationContext(),HomeWidget.class);
        allWidgetIds = appWidgetManager.getAppWidgetIds(home_widget);

        remoteView.setImageViewResource(R.id.widget_play_pause, android.R.drawable.ic_media_play);

        Intent self_intent = new Intent(getApplicationContext(), HomeWidget.class);
        self_intent.setAction(PLAY_CLICKED);
        PendingIntent pending_intent = PendingIntent.getBroadcast(getApplicationContext(), 0, self_intent, 0);
        remoteView.setOnClickPendingIntent(R.id.widget_play_pause, pending_intent);

        appWidgetManager.updateAppWidget(allWidgetIds,remoteView);


    }

    public void resume(){

        int result = audio_manager.requestAudioFocus(audio_focus_change_listener, AudioManager.STREAM_MUSIC
                ,AudioManager.AUDIOFOCUS_GAIN);

        if(!player.isPlaying() & result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED ){

            Log.e("focus","gained in resume");

            Intent now_playing_view_intent = new Intent();
            now_playing_view_intent.setAction("com.cjapps.playinsync.PLAY");
            sendBroadcast(now_playing_view_intent);
            Log.e("Broadcast", "sent");

            HomeWidget.isPlaying = true;

            if(isStoppedViaLoss){
                Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        , song.id);
                try {
                    player.setDataSource(getApplicationContext(),trackUri);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                player.prepareAsync();

            }else{
                player.start();
            }

            remoteView = new RemoteViews(getApplicationContext().getPackageName()
                    , R.layout.widget_home);
            appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

            home_widget = new ComponentName(getApplicationContext(),HomeWidget.class);
            allWidgetIds = appWidgetManager.getAppWidgetIds(home_widget);
            remoteView.setImageViewResource(R.id.widget_play_pause,android.R.drawable.ic_media_pause);
            Intent self_intent = new Intent(getApplicationContext(), HomeWidget.class);
            self_intent.setAction(PAUSE_CLICKED);
            PendingIntent pending_intent = PendingIntent.getBroadcast(getApplicationContext(), 0, self_intent, 0);
            remoteView.setOnClickPendingIntent(R.id.widget_play_pause, pending_intent);
            appWidgetManager.updateAppWidget(allWidgetIds, remoteView);

        }
    }

    public void next(){
        int next_pos = song.next_pos;
        current_position = next_pos;
        playSong(next_pos);
    }

    public void prev(){
        int prev_pos = song.prev_pos;
        current_position = prev_pos;
        playSong(prev_pos);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return musicBind;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        int next_pos = song.next_pos;
        current_position = next_pos;
        playSong(next_pos);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {

        if(isStoppedViaLoss){

            mediaPlayer.seekTo(current_time_position);
            isStoppedViaLoss = false;
            mediaPlayer.start();

        }else {

            HomeWidget.isPlaying = true;
            SongsFragment.isPlaying = true;
            mediaPlayer.start();

            // UPDATE WIDGET

            appWidgetManager = AppWidgetManager.getInstance(getApplicationContext());

            remoteView = new RemoteViews(getApplicationContext().getPackageName(), R.layout.widget_home);
            home_widget = new ComponentName(getApplicationContext(), HomeWidget.class);
            allWidgetIds = appWidgetManager.getAppWidgetIds(home_widget);

            for (int id : allWidgetIds) {

                Log.e("In SERVICE", "updating " + id);

                Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart/" + song.album_art);
                ContentResolver res = getContentResolver();
                Bitmap artwork;

                try {
                    InputStream in = res.openInputStream(sArtworkUri);
                    artwork = BitmapFactory.decodeStream(in);
                } catch (Exception e) {
                    // SET DEFAULT IMAGE
                    artwork = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art);
                }

                Intent now_playing_view_intent = new Intent();
                now_playing_view_intent.setAction("com.cjapps.playinsync.SONG_CHANGED");
                now_playing_view_intent.putExtra("current_position", current_position);
                sendBroadcast(now_playing_view_intent);
                Log.e("Broadcast", "sent");

                if (artwork == null) {
                    artwork = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art);
                }

                remoteView.setImageViewBitmap(R.id.widget_album_art, artwork);
                remoteView.setTextViewText(R.id.widget_song, song.title);
                remoteView.setTextViewText(R.id.widget_artist, song.artist);

                String next = songs.get(song.next_pos).title;
                if (next == null) {
                    Log.e("NULL", "next");
                }
                remoteView.setTextViewText(R.id.widget_coming_up, next);

                remoteView.setImageViewResource(R.id.widget_play_pause, android.R.drawable.ic_media_pause);

                appWidgetManager.updateAppWidget(id, remoteView);

                Intent notIntent = new Intent(this, MainActivity.class);
                notIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                PendingIntent pendInt = PendingIntent.getActivity(this, 0,
                        notIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                RemoteViews notification_view = new RemoteViews(getPackageName(),
                        R.layout.notification_main);

                notification_view.setImageViewBitmap(R.id.notification_album_art,artwork);
                notification_view.setTextViewText(R.id.notification_song,song.title);
                notification_view.setTextViewText(R.id.notification_artist,song.artist);
                notification_view.setTextViewText(R.id.notification_coming_up,next);

                Intent self_intent = new Intent(getApplicationContext(), HomeWidget.class);
                if(HomeWidget.isPlaying) {
                    self_intent.setAction(PAUSE_CLICKED);
                    HomeWidget.isPlaying = false;
                }else{
                    self_intent.setAction(PLAY_CLICKED);
                    HomeWidget.isPlaying = true;
                }
                PendingIntent pending_intent = PendingIntent.getBroadcast(getApplicationContext(), 0, self_intent, 0);
                notification_view.setOnClickPendingIntent(R.id.notification_play_pause, pending_intent);

                self_intent = new Intent(getApplicationContext(), HomeWidget.class);
                self_intent.setAction(NEXT_CLICKED);
                pending_intent = PendingIntent.getBroadcast(getApplicationContext(), 0, self_intent, 0);
                notification_view.setOnClickPendingIntent(R.id.notification_next, pending_intent);

                self_intent = new Intent(getApplicationContext(), HomeWidget.class);
                self_intent.setAction(PREV_CLICKED);
                pending_intent = PendingIntent.getBroadcast(getApplicationContext(), 0, self_intent, 0);
                notification_view.setOnClickPendingIntent(R.id.notification_previous, pending_intent);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

                builder.setContentIntent(pendInt)
                        .setTicker(song.title)
                        .setOngoing(true)
                        .setSmallIcon(android.R.drawable.ic_media_play)
                        .setContent(notification_view);

                Notification not = builder.build();

                startForeground(NOTIFY_ID, not);

            }
        }
    }

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
}

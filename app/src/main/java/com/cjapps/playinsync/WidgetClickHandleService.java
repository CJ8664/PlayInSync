package com.cjapps.playinsync;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class WidgetClickHandleService extends IntentService {

    public static final String PLAY_CLICKED = "com.cjapps.playinsync.HomeWidget.PLAY_CLICKED";
    public static final String PAUSE_CLICKED = "com.cjapps.playinsync.HomeWidget.PAUSE_CLICKED";
    public static final String NEXT_CLICKED = "com.cjapps.playinsync.HomeWidget.NEXT_CLICKED";
    public static final String PREV_CLICKED = "com.cjapps.playinsync.HomeWidget.PREV_CLICKED";

    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;

    public WidgetClickHandleService() {
        super("WidgetClickHandleService");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("IN SERVICE", "DESTROYED");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (PLAY_CLICKED.equals(action)) {
                handleActionPlay();
            } else if ( PAUSE_CLICKED.equals(action) ){
                handleActionPause();
            } else if (NEXT_CLICKED.equals(action)) {
                handleActionNext();
            } else if (PREV_CLICKED.equals(action)){
                handleActionPrevious();
            }
        }
    }

    private void handleActionPlay() {
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, BIND_AUTO_CREATE);
        startService(playIntent);
        if (musicBound) {
            musicSrv.resume();
        }
        Log.e("IN SERVICE", PLAY_CLICKED);
        stopSelf();
    }

    private void handleActionPause() {
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, BIND_AUTO_CREATE);
        startService(playIntent);
        if (musicBound) {
            musicSrv.pause();
        }
        Log.e("IN SERVICE", PAUSE_CLICKED);
        stopSelf();
    }

    private void handleActionNext() {
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, BIND_AUTO_CREATE);
        startService(playIntent);

        Log.e("IN SERVICE", NEXT_CLICKED);
        if (musicBound) {
            musicSrv.next();
        }
        stopSelf();
    }

    private void handleActionPrevious() {
        playIntent = new Intent(this, MusicService.class);
        bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
        startService(playIntent);
        Log.e("IN SERVICE", NEXT_CLICKED);
        if (musicBound) {
            musicSrv.prev();
        }
        stopSelf();
    }

    //connect to the service
    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
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
}

package com.cjapps.playinsync;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class HomeWidget extends AppWidgetProvider {

    public static boolean isPlaying;
    public static String PLAY_CLICKED = "com.cjapps.playinsync.HomeWidget.PLAY_CLICKED";
    public static String PAUSE_CLICKED = "com.cjapps.playinsync.HomeWidget.PAUSE_CLICKED";
    public static String NEXT_CLICKED = "com.cjapps.playinsync.HomeWidget.NEXT_CLICKED";
    public static String PREV_CLICKED = "com.cjapps.playinsync.HomeWidget.PREV_CLICKED";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int appWidgetId : appWidgetIds) {

            Intent self_intent;
            PendingIntent pending_intent;
            RemoteViews widget_views;

            widget_views = new RemoteViews(context.getPackageName(), R.layout.widget_home);

            self_intent = new Intent(context, HomeWidget.class);
            if(isPlaying) {
                self_intent.setAction(PAUSE_CLICKED);
                isPlaying = false;
            }else{
                self_intent.setAction(PLAY_CLICKED);
                isPlaying = true;
            }
            pending_intent = PendingIntent.getBroadcast(context, 0, self_intent, 0);
            widget_views.setOnClickPendingIntent(R.id.widget_play_pause, pending_intent);

            self_intent = new Intent(context, HomeWidget.class);
            self_intent.setAction(NEXT_CLICKED);
            pending_intent = PendingIntent.getBroadcast(context, 0, self_intent, 0);
            widget_views.setOnClickPendingIntent(R.id.widget_next, pending_intent);

            self_intent = new Intent(context, HomeWidget.class);
            self_intent.setAction(PREV_CLICKED);
            pending_intent = PendingIntent.getBroadcast(context, 0, self_intent, 0);
            widget_views.setOnClickPendingIntent(R.id.widget_previous, pending_intent);

            appWidgetManager.updateAppWidget(appWidgetId, widget_views);

            Log.e("ID_updated",""+appWidgetId);
            //updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }


    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(PLAY_CLICKED)) {
            Log.e("onReceive", PLAY_CLICKED);
            context.startService(new Intent(context, WidgetClickHandleService.class)
                    .setAction(PLAY_CLICKED));
        } else if (intent.getAction().equals(PAUSE_CLICKED)) {
            Log.e("onReceive", PAUSE_CLICKED);
            context.startService(new Intent(context, WidgetClickHandleService.class)
                    .setAction(PAUSE_CLICKED));
        } else if (intent.getAction().equals(NEXT_CLICKED)) {
            Log.e("onReceive", NEXT_CLICKED);
            context.startService(new Intent(context, WidgetClickHandleService.class)
                    .setAction(NEXT_CLICKED));
        } else if (intent.getAction().equals(PREV_CLICKED)) {
            Log.e("onReceive", PREV_CLICKED);
            context.startService(new Intent(context, WidgetClickHandleService.class)
                    .setAction(PREV_CLICKED));
        } else {
            super.onReceive(context, intent);
            Log.e("onReceive", intent.getAction());
        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId) {


    }


}



package com.cjapps.playinsync;

public interface GCMConfiguration {


    // CONSTANTS
    static final String YOUR_SERVER_URL = "http://www.playinsync.com/mobile/register.php";

    // Google project id
    static final String GOOGLE_SENDER_ID = "2896763436";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM Android Example";

    static final String DISPLAY_MESSAGE_ACTION = "com.cjapps.playinsync.DISPLAY_MESSAGE";

    static final String EXTRA_MESSAGE = "message";


}
package com.cjapps.playinsync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;

public class DatabaseManager extends SQLiteOpenHelper {

    public SQLiteDatabase db;

    public static final String DB_NAME = "playinsync";

    public static final int DATABASE_VERSION = 1;

    public static String TABLE_CONTACTS = "contacts";
    public static String TABLE_SONGS = "songs";
    public static String TABLE_ALBUMS = "albums";
    public static String TABLE_ME = "me";
    public static String TABLE_PLAYLISTS = "playlist";

    // CONTACTS TABLE COLUMNS

    public static String CONTACT_ID = "contact_id";
    public static String CONTACT_NAME = "contact_name";
    public static String CONTACT_LAST_PLAYED = "contact_last_played";
    public static String CONTACT_PHONE_NUMBER = "contact_phone_number";
    public static String CONTACT_DP_URL = "contact_dp_url";
    public static String CONTACT_REGISTERED_PLAYINSYNC = "contact_registered_playinsync";

    // SONGS TABLE COLUMNS

    public static String SONG_ID = "song_id";
    public static String SONG_PATH = "song_path";
    public static String SONG_TITLE = "song_title";
    public static String SONG_TRACK = "song_track";
    public static String SONG_ARTIST = "song_artist";
    public static String SONG_COMPOSER = "song_composer";
    public static String SONG_DURATION = "song_duration";
    public static String SONG_SIZE = "song_size";
    public static String SONG_LYRICS = "song_lyrics";
    public static String SONG_GENRE = "song_genre";
    public static String SONG_ALBUM = "song_album";
    public static String SONG_ALBUM_ID = "song_album_id";
    public static String SONG_RATING = "song_rating";

    // AlBUM TABLE COLUMNS

    public static String ALBUM_NAME = "album_name";
    public static String ALBUM_ID = "album_id";
    public static String ALBUM_ARTIST = "album_artist";
    public static String ALBUM_SONG_COUNT = "album_song_count";

    // PLAYLIST TABLE COLUMNS

    public static String PLAYLIST_NAME = "playlist_name";
    public static String PLAYLIST_SONG_IDS = "playlist_song_ids";
    public static String PLAYLIST_SONG_COUNT = "playlist_song_count";
    public static String PLAYLIST_DURATION = "playlist_duration";
    public static String PLAYLIST_ALBUM_ART_IDS = "playlist_album_art_ids";

    // ME TABLE COLUMNS

    public static String ME_NAME = "me_name";
    public static String ME_EMAIL = "me_email";
    public static String ME_PHONE = "me_phone";
    public static String ME_U_ID = "me_u_id";
    public static String ME_DP_PATH = "me_dp_path";
    public static String ME_GOOGLE_TOKEN = "me_google_token";
    public static String ME_FACEBOOK_TOKEN = "me_facebook_token";
    public static String ME_FACEBOOK_EXPIRATION_DATE = "me_facebook_expiration_date";
    public static String ME_FACEBOOK_PERMISSIONS = "me_facebook_permissions";
    public static String ME_TWITTER_TOKEN = "me_twitter_token";
    public static String ME_TWITTER_SECRET = "me_twitter_secret";
    public static String ME_GCM_ID = "me_gcm_id";

    public DatabaseManager(Context context) {
        super(context, DB_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {


        String CREATE_ME_TABLE = "CREATE TABLE " + TABLE_ME + "(" + ME_U_ID + " VARCHAR(20) "
                + "PRIMARY KEY," + ME_NAME + " VARCHAR(50)," + ME_PHONE + " VARCHAR(20)," + ME_EMAIL
                + " VARCHAR(100)," + ME_DP_PATH + " TEXT," + ME_GOOGLE_TOKEN + " TEXT," + ME_GCM_ID + " TEXT,"
                + ME_FACEBOOK_TOKEN + " TEXT,"+ ME_FACEBOOK_EXPIRATION_DATE + " TEXT,"
                + ME_FACEBOOK_PERMISSIONS + " TEXT," + ME_TWITTER_TOKEN + " TEXT,"
                + ME_TWITTER_SECRET + " TEXT" +")";

        sqLiteDatabase.execSQL(CREATE_ME_TABLE);
        Log.e("DatabaseManager","CREATED_ME_TABLE");

        String CREATE_SONGS_TABLE = "CREATE TABLE " + TABLE_SONGS + "(" + SONG_ID + " VARCHAR(50) "
                + "PRIMARY KEY," + SONG_PATH + " TEXT," + SONG_TITLE + " TEXT," + SONG_TRACK
                + " TEXT," + SONG_ARTIST + " TEXT," + SONG_COMPOSER + " TEXT," + SONG_DURATION
                + " VARCHAR(8),"+ SONG_SIZE + " VARCHAR(20)," +SONG_LYRICS + " TEXT,"+SONG_GENRE
                + " VARCHAR(30)," + SONG_ALBUM + " TEXT," + SONG_ALBUM_ID + " INTEGER," + SONG_RATING
                + " VARCHAR(5)"+ ")";

        sqLiteDatabase.execSQL(CREATE_SONGS_TABLE);
        Log.e("DatabaseManager","CREATED_SONGS_TABLE");

        String CREATE_ALBUMS_TABLE = "CREATE TABLE " + TABLE_ALBUMS + "(" + ALBUM_ID + " INTEGER "
                + "PRIMARY KEY," + ALBUM_NAME + " TEXT," + ALBUM_ARTIST + " TEXT,"
                + ALBUM_SONG_COUNT + " TEXT" + ")";

        sqLiteDatabase.execSQL(CREATE_ALBUMS_TABLE);
        Log.e("DatabaseManager","CREATED_ALBUMS_TABLE");

        String CREATE_PLAYLIST_TABLE = "CREATE TABLE " + TABLE_PLAYLISTS + "(" + PLAYLIST_NAME + " TEXT "
                + "PRIMARY KEY," + PLAYLIST_SONG_IDS + " TEXT," + PLAYLIST_SONG_COUNT + " TEXT,"
                + PLAYLIST_DURATION + " TEXT," + PLAYLIST_ALBUM_ART_IDS + " TEXT" + ")";

        sqLiteDatabase.execSQL(CREATE_PLAYLIST_TABLE);

        ContentValues cv = new ContentValues();
        cv.put(PLAYLIST_NAME,"Favourites");
        cv.put(PLAYLIST_SONG_IDS,"");
        cv.put(PLAYLIST_SONG_COUNT,"0");
        cv.put(PLAYLIST_DURATION,"0");
        cv.put(PLAYLIST_ALBUM_ART_IDS,"");

        sqLiteDatabase.insert(TABLE_PLAYLISTS,null,cv);

        Log.e("DatabaseManager","CREATE_PLAYLIST_TABLE");

        String CREATE_CONTACTS_TABLE = "CREATE TABLE " + TABLE_CONTACTS + "(" + CONTACT_ID
                + " VARCHAR(20) PRIMARY KEY," + CONTACT_NAME + " VARCHAR(50)," + CONTACT_LAST_PLAYED
                + " TEXT," + CONTACT_PHONE_NUMBER + " VARCHAR(20)," + CONTACT_DP_URL + " TEXT,"
                + CONTACT_REGISTERED_PLAYINSYNC + " VARCHAR(3)"+ ")";

        sqLiteDatabase.execSQL(CREATE_CONTACTS_TABLE);
        Log.e("DatabaseManager","CREATED_CONTACTS_TABLE");

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i2) {

    }

    public boolean addMyDetails(String user_id, String name, String email, String dp_path
            , String phone_number, String google_token, String facebook_token
            , String expiration_date,String permission, String twitter_token, String twitter_secret
            , String GCM_id ){

        db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(ME_NAME,name);
        cv.put(ME_EMAIL,email);
        cv.put(ME_PHONE,phone_number);
        cv.put(ME_U_ID,user_id);
        cv.put(ME_DP_PATH,dp_path);
        cv.put(ME_GOOGLE_TOKEN,google_token);
        cv.put(ME_FACEBOOK_TOKEN,facebook_token);
        cv.put(ME_FACEBOOK_EXPIRATION_DATE,expiration_date);
        cv.put(ME_FACEBOOK_PERMISSIONS,permission);
        cv.put(ME_TWITTER_TOKEN,twitter_token);
        cv.put(ME_TWITTER_SECRET,twitter_secret);
        cv.put(ME_GCM_ID,GCM_id);

        try{
            db.insert(TABLE_ME,null,cv);
            db.close();
            return  true;
        } catch (Exception e){
            return false;
        }


    }

    public boolean updateMyDetails(String user_id, String name, String email, String dp_path
            , String phone_number, String google_token, String facebook_token
            , String expiration_date,String permission, String twitter_token, String twitter_secret
            , String GCM_id ){

        db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();

        if(!name.equals("")) cv.put(ME_NAME,name);
        if(!email.equals("")) cv.put(ME_EMAIL,email);
        if(!email.equals("")) cv.put(ME_DP_PATH,dp_path);
        if(!phone_number.equals("")) cv.put(ME_PHONE,phone_number);
        if(!google_token.equals("")) cv.put(ME_GOOGLE_TOKEN,google_token);
        if(!facebook_token.equals("")) cv.put(ME_FACEBOOK_TOKEN,facebook_token);
        if(!expiration_date.equals("")) cv.put(ME_FACEBOOK_EXPIRATION_DATE,expiration_date);
        if(!permission.equals("")) cv.put(ME_FACEBOOK_PERMISSIONS,permission);
        if(!twitter_token.equals("")) cv.put(ME_TWITTER_TOKEN,twitter_token);
        if(!twitter_secret.equals("")) cv.put(ME_TWITTER_SECRET,twitter_secret);
        if(!GCM_id.equals("")) cv.put(ME_GCM_ID,GCM_id);

        try{
            db.update(TABLE_ME, cv, null, null);
            db.close();
            return  true;
        } catch (Exception e){
            return false;
        }
    }

    public String[] getMeTableDetails(String[] columns ){

        db = this.getReadableDatabase();

        Cursor details = db.query(TABLE_ME,columns,null,null,null,null,null);

        if(details!=null)
            details.moveToFirst();

        db.close();

        String[] result = new String[columns.length];

        if (details != null) {
            for(int i = 0;i<result.length;i++){
                result[i] = details.getString(i);
            }
        }

        return result;
    }

    public String[] getFacebookTokenDetails(){

        db = this.getReadableDatabase();

        Cursor details = db.query(TABLE_ME,new String[]{ME_FACEBOOK_TOKEN
                , ME_FACEBOOK_EXPIRATION_DATE, ME_FACEBOOK_PERMISSIONS},null,null,null,null,null);

        if(details!=null) {
            details.moveToFirst();
            return new String[]{details.getString(0), details.getString(1), details.getString(2)};
        }
        db.close();

        return null;
    }

    public String[] getTwitterTokenDetails(){

        db = this.getReadableDatabase();

        Cursor details = db.query(TABLE_ME,new String[]{ME_TWITTER_TOKEN, ME_TWITTER_SECRET}
                ,null,null,null,null,null);

        if(details!=null) {
            details.moveToFirst();
            return new String[]{details.getString(0), details.getString(1)};
        }
        db.close();

        return null;
    }

    public boolean addSong(String id, String path, String title, String track, String artist
            , String composer, String duration, String size, String lyrics, String genre
            , String album, String album_id, String rating){

        db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put(SONG_ID,id);
        cv.put(SONG_PATH,path);
        cv.put(SONG_TITLE,title);
        cv.put(SONG_TRACK,track);
        cv.put(SONG_ARTIST,artist);
        cv.put(SONG_COMPOSER,composer);
        cv.put(SONG_DURATION,duration);
        cv.put(SONG_SIZE,size);
        cv.put(SONG_LYRICS,lyrics);
        cv.put(SONG_GENRE,genre);
        cv.put(SONG_ALBUM,album);
        cv.put(SONG_ALBUM_ID,album_id);
        cv.put(SONG_RATING,rating);

        //Log.e("Song",cv.toString());

        try {
            db.insert(TABLE_SONGS, null, cv);
            db.close();
            return true;
        }catch (Exception e){
            return false;
        }

    }

    public boolean deleteSong(ArrayList<String> ids){

        db = this.getWritableDatabase();

        for(String id: ids){
            try {
                db.delete(TABLE_SONGS,SONG_ID + "=?",new String[]{id});
            }catch (Exception ignored){

            }
        }

        db.close();
        return true;
    }

    public ArrayList<String[]> getAllSongs(String sortBy){

        String[] columns= {SONG_ID, SONG_PATH, SONG_TITLE, SONG_TRACK, SONG_ARTIST, SONG_COMPOSER
                , SONG_DURATION, SONG_SIZE, SONG_LYRICS, SONG_GENRE, SONG_ALBUM, SONG_ALBUM_ID
                , SONG_RATING };

        db = this.getReadableDatabase();

        Cursor songs = db.query(TABLE_SONGS, columns, null, null, null, null, sortBy);

        ArrayList<String[]> song_list = new ArrayList<String[]>(songs.getCount());

        while (songs.moveToNext()){
            String[] temp = new String[13];
            for(int i = 0; i < 13 ; i++){
                temp[i] = songs.getString(i);
            }
            song_list.add(temp);
        }

        songs.close();
        db.close();

        return song_list;
    }

    public ArrayList<String> getAllSongIDS(){

        db = this.getReadableDatabase();

        Cursor songs = db.query(TABLE_SONGS, new String[]{SONG_ID}, null, null, null, null, null);

        ArrayList<String> song_id_list = new ArrayList<String>(songs.getCount());

        while (songs.moveToNext()) {
            song_id_list.add(songs.getString(0));
        }

        songs.close();
        db.close();

        return song_id_list;
    }

    public boolean addAlbum(String name, String artist, int id, int count){
        db = this.getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put(ALBUM_NAME,name);
        cv.put(ALBUM_ARTIST,artist);
        cv.put(ALBUM_ID,id);
        cv.put(ALBUM_SONG_COUNT,count);

        try{
            db.insert(TABLE_ALBUMS,null,cv);
            cv.clear();
            db.close();
            return true;
        }catch (Exception e){
            return false;
        }
    }

    public int createAlbumsTable(Context context){

        // CLEAR ALBUMS TABLE
        db = this.getWritableDatabase();
        db.delete(TABLE_ALBUMS,null,null);
        db.close();

        int count = 1;
        db = this.getReadableDatabase();

        Cursor albums = db.rawQuery("SELECT DISTINCT song_album_id, song_album, COUNT(song_album_id) FROM songs GROUP BY song_album_id ORDER BY song_album_id DESC",null);

        String name;
        int id;
        String artist;
        int artist_count;

        while (albums.moveToNext()){

            id = albums.getInt(0);
            name = albums.getString(1);
            artist_count = albums.getInt(2);

            if(artist_count == 1) {
                Cursor cursor = context.getContentResolver().query(
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        new String[]{MediaStore.Audio.Albums._ID, MediaStore.Audio.Albums.ARTIST},
                        MediaStore.Audio.Albums._ID + "=?",
                        new String[]{id+""},
                        null);

                cursor.moveToNext();

                artist = cursor.getString(1);

                cursor.close();
            }else{
                artist = "Various";
            }

            addAlbum(name,artist,id,artist_count);

            count++;
        }

        return count;
    }

    public ArrayList<String[]> getAllAlbums(){

        db = this.getReadableDatabase();

        Cursor album = db.rawQuery("SELECT " + ALBUM_SONG_COUNT + ", " + ALBUM_NAME +", "
                + ALBUM_ID + ", " + ALBUM_ARTIST + " FROM " + TABLE_ALBUMS + " GROUP BY "
                + ALBUM_ID +" ORDER BY " + ALBUM_ID + " DESC",null);

        ArrayList<String[]> album_list = new ArrayList<String[]>(10);
        while (album.moveToNext()){
            album_list.add(new String[]{album.getString(0), album.getString(1), album.getString(2)
                    , album.getString(3)});
        }

        album.close();
        db.close();

        return album_list;
    }

    public ArrayList<String> getAllPlaylistName(){

        db= this.getReadableDatabase();

        Cursor c = db.query(TABLE_PLAYLISTS,new String[]{PLAYLIST_NAME},null,null,null,null,null);

        ArrayList<String> names = new ArrayList<String>();
        while (c.moveToNext()){
            names.add(c.getString(0));
        }

        c.close();
        db.close();

        return names;
    }

    public ArrayList<String[]> getPlaylistFragmentData(){

        db= this.getReadableDatabase();

        String[] columns = new String[]{PLAYLIST_NAME, PLAYLIST_DURATION, PLAYLIST_SONG_COUNT
                ,PLAYLIST_SONG_IDS,PLAYLIST_ALBUM_ART_IDS};

        Cursor c = db.query(TABLE_PLAYLISTS,columns,null,null,null,null,null);

        ArrayList<String[]> playlist_item = new ArrayList<String[]>();

        while (c.moveToNext()){
            playlist_item.add(new String[]{c.getString(0), c.getString(1), c.getString(2), c.getString(3)
                    , c.getString(4)});
        }

        c.close();
        db.close();

        return playlist_item;

    }

    public boolean addSongToPlaylist(String playlist, String song_id, String duration, String aa_id){

        Log.e("ADD to playlist",playlist + ", " + song_id + ", " + duration + ", " + aa_id);

        String song_ids = "";
        String song_count = "0";
        String playlist_duration = "0";
        String playlist_aa_ids = "";

        db = this.getReadableDatabase();

        SQLiteDatabase db_temp = this.getWritableDatabase();

        String[] selection = new String[]{PLAYLIST_SONG_IDS,PLAYLIST_SONG_COUNT,PLAYLIST_DURATION
                ,PLAYLIST_ALBUM_ART_IDS};

        Cursor c_song_ids = db.query(TABLE_PLAYLISTS,selection,PLAYLIST_NAME + "=? "
                , new String[]{playlist},null,null,null);

        c_song_ids.moveToNext();

        if(c_song_ids.getCount() == 0){

            ContentValues cv = new ContentValues();
            cv.put(PLAYLIST_NAME,playlist);
            cv.put(PLAYLIST_SONG_IDS,"");
            cv.put(PLAYLIST_SONG_COUNT,"0");
            cv.put(PLAYLIST_DURATION,"");
            cv.put(PLAYLIST_ALBUM_ART_IDS,"");

            try {
                db_temp.insert(TABLE_PLAYLISTS, null, cv);
                cv.clear();
                db_temp.close();
            }catch(Exception ignored){

            }
        } else {

            song_ids = c_song_ids.getString(0);
            song_count = c_song_ids.getString(1);
            playlist_duration = c_song_ids.getString(2);
            playlist_aa_ids = c_song_ids.getString(3);

        }

        c_song_ids.close();
        db.close();

        if(!song_ids.contains(song_id+"#")){

            song_ids +=  song_id + "#";
            song_count = (Integer.parseInt(song_count) + 1) + "";
            playlist_duration = (Integer.parseInt(playlist_duration) + Integer.parseInt(duration)) + "";

            String[] aa_ids = playlist_aa_ids.split("#");

            if(aa_ids.length<4){

                playlist_aa_ids += aa_id + "#";

            } else{

                playlist_aa_ids = aa_ids[1] + "#" + aa_ids[2] + "#" + aa_ids[3] + "#" + aa_id + "#";

            }

            db = this.getWritableDatabase();

            ContentValues cv = new ContentValues();

            cv.put(PLAYLIST_SONG_IDS,song_ids);
            cv.put(PLAYLIST_SONG_COUNT,song_count);
            cv.put(PLAYLIST_DURATION,playlist_duration);
            cv.put(PLAYLIST_ALBUM_ART_IDS,playlist_aa_ids);

            try {
                db.update(TABLE_PLAYLISTS, cv, PLAYLIST_NAME + "= ? ", new String[]{playlist});
                cv.clear();
                db.close();
                return true;
            }catch (Exception e){
                return false;
            }
        }
        return false;
    }

    public ArrayList<String[]> getAllContacts(){
        db = this.getReadableDatabase();

        String[] columns = new String[]{CONTACT_ID,CONTACT_NAME,CONTACT_LAST_PLAYED,CONTACT_DP_URL
                ,CONTACT_PHONE_NUMBER};

        Cursor c = db.query(TABLE_CONTACTS,columns,CONTACT_REGISTERED_PLAYINSYNC + "=? "
                ,new String[]{"YES"},null,null,CONTACT_NAME);

        ArrayList<String[]> contacts = new ArrayList<String[]>(c.getCount());

        while (c.moveToNext()){
            String[] temp = new String[]{c.getString(0),c.getString(1),c.getString(2)
                    ,c.getString(3),c.getString(4)};
            contacts.add(temp);
        }

        c.close();
        db.close();

        return contacts;
    }

    public ArrayList<String> getAllContactPhoneNumber(){

        db = this.getReadableDatabase();

        Cursor c = db.query(TABLE_CONTACTS,new String[]{CONTACT_PHONE_NUMBER},null,null,null,null,CONTACT_NAME);

        ArrayList<String> contacts_ids = new ArrayList<String>(c.getCount());

        while (c.moveToNext()){
            contacts_ids.add(c.getString(0));
        }

        c.close();
        db.close();

        return contacts_ids;
    }

    public void addContact(String id, String name, String phone, String last_played
            , String dp_url, String registered){

        db =this.getWritableDatabase();

        ContentValues cv = new ContentValues();

        cv.put(CONTACT_ID,id);
        cv.put(CONTACT_NAME,name);
        cv.put(CONTACT_LAST_PLAYED,last_played);
        cv.put(CONTACT_PHONE_NUMBER,phone);
        cv.put(CONTACT_DP_URL,dp_url);
        cv.put(CONTACT_REGISTERED_PLAYINSYNC,registered);

        try {
            db.insert(TABLE_CONTACTS,null,cv);
            cv.clear();
            db.close();
        }catch (Exception ex){
            Log.e("INSERT INTO CONTACT", ex.getMessage());
        }
    }

    public void deleteContacts(ArrayList<String> in_db_contact_pn) {

        db =this.getWritableDatabase();

        for(String phone : in_db_contact_pn){
            try {
                db.delete(TABLE_CONTACTS, CONTACT_PHONE_NUMBER + "=?", new String[]{phone});
            } catch (Exception ignored){

            }
        }

        db.close();
    }
}

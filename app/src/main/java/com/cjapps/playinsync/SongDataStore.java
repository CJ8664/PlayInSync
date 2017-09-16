package com.cjapps.playinsync;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;

public class SongDataStore {

    public DatabaseManager db_manager;
    public ArrayList<String[]> song_arraylist;

    public ArrayList<Song> songs;

    public SongDataStore(Context context){

        db_manager = new DatabaseManager(context);
        song_arraylist = db_manager.getAllSongs(DatabaseManager.SONG_TITLE);

        songs = new ArrayList<Song>(10);
    }

    public ArrayList<Song> createSongList(){
        // FOR SONGS HAVING TITLE STARTING WITH NUMBER
        int sectionPosition = 0, listPosition = 0;

        Song item;

        String title = song_arraylist.get(0)[2];
        String artist;
        String album_art;
        String duration;
        long id;

        Song section;

        boolean startsWithNumber = false;

        char first = title.charAt(0);

        if(first>='0' & first<='9'){

            section = new Song(Song.SECTION, "#", "", "", "", 0);
            section.sectionPosition = sectionPosition;
            section.listPosition = listPosition++;
            songs.add(section);

            startsWithNumber = true;

        }else{
            Log.e("Contains", "no numeric songs");
        }

        int index = 0;
        int count = song_arraylist.size();
        if(startsWithNumber) {
            while (index < count){

                title = song_arraylist.get(index)[2];
                artist = song_arraylist.get(index)[4];
                album_art = song_arraylist.get(index)[11];
                duration = song_arraylist.get(index)[6];
                id = Long.parseLong(song_arraylist.get(index)[0]);

                first = title.charAt(0);

                if(first>='0' & first<='9'){

                    item = new Song(Song.SONG, title, artist, album_art, duration, id);
                    item.sectionPosition = sectionPosition;
                    item.listPosition = listPosition++;
                    songs.add(item);

                }else{
                    break;
                }
                index++;
            }
        }

        char prev = '9';
        char current;

        while (index < count){

            // Log.e("In", "ALPHABETIC SONGS");

            title = song_arraylist.get(index)[2];
            artist = song_arraylist.get(index)[4];
            album_art = song_arraylist.get(index)[11];
            duration = song_arraylist.get(index)[6];
            id = Long.parseLong(song_arraylist.get(index)[0]);

            current = title.charAt(0);

            if(prev != current ) {

                sectionPosition++;
                section = new Song(Song.SECTION, current+"", "", "", "", 0);
                section.sectionPosition = sectionPosition;
                section.listPosition = listPosition++;
                songs.add(section);

                prev = current;
            }

            item = new Song(Song.SONG, title, artist, album_art, duration, id);
            item.sectionPosition = sectionPosition;
            item.listPosition = listPosition++;
            songs.add(item);

            index++;
        }

        // FIRST Will always be a section

        for (int i = 1;i<songs.size()-1;i++){

            int next_pos;
            int prev_pos;

            Song curr = songs.get(i);
            Song next = songs.get(i+1);
            Song pre  = songs.get(i-1);

            if(curr.type == Song.SONG){
                if(next.type == Song.SECTION){
                    next_pos = i+2;
                }
                else{
                    next_pos = i+1;
                }
                if(pre.type == Song.SECTION){
                    prev_pos = i-2;
                }
                else{
                    prev_pos = i-1;
                }
                curr.curr_pos = i;
                curr.next_pos = next_pos;
                curr.prev_pos = prev_pos;

            }else{
                curr.curr_pos = -1;
                curr.next_pos = -1;
                curr.prev_pos = -1;
            }

            songs.set(i,curr);
        }


        Song init;

        init = songs.get(0);
        init.curr_pos=-1;
        init.next_pos=-1;
        init.prev_pos=-1;
        songs.set(0,init);

        init = songs.get(count-1);
        init.curr_pos = count-1;
        init.next_pos = 1;
        if(songs.get(count-2).type == Song.SECTION){
            init.prev_pos = -1;
        }
        else{
            init.prev_pos = count-2;
        }
        songs.set(count-1,init);

        init = songs.get(1);
        init.prev_pos = count-1;
        songs.set(1,init);

        return songs;
    }

    static class Song {

        public static final int SONG = 0;
        public static final int SECTION = 1;

        public final int type;

        public int curr_pos = -1;
        public int next_pos = -1;
        public int prev_pos = -1;

        public final String title;
        public final String artist;
        public final String album_art;
        public final String duration;
        public final long id;

        public boolean isSelected = false;

        public int sectionPosition;
        public int listPosition;

        public Song(int type, String title, String artist, String album_art, String duration, long id) {

            this.type = type;
            this.title = title;
            this.artist = artist;
            this.album_art = album_art;
            this.duration = duration;
            this.id = id;

        }

        @Override public String toString() {
            return title;
        }

    }
}

package de.madpage.bicyclekaraoke;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created by Mad on 18.08.2016.
 */
public class VideoFinder {
    ContentResolver contentResolver;
    ArrayList<String> videoList = new ArrayList<String>();
    private Random random = new Random();


    public VideoFinder(final ContentResolver contentResolver) {
        this.contentResolver = contentResolver;

    }

    public void updateVideos() {
        Uri uri= MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String condition=MediaStore.Video.Media.DATA +" like?";
        String[] selectionArguments=new String[]{"%/BicycleKaraokeVideo/%"};
        String sortOrder = MediaStore.Video.Media.DATE_TAKEN + " DESC";
        String[] projection = { MediaStore.Images.Media._ID, MediaStore.Images.Media.BUCKET_ID,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,MediaStore.Images.Media.DATA };
        Cursor cursor = contentResolver.query(uri,projection, condition, selectionArguments, sortOrder);


        int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
        int pathColumn=cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

        if(cursor!=null){
            videoList.clear();
            while(cursor.moveToNext()){
                videoList.add( cursor.getString(pathColumn) );
            }
        }
    }

    public String randomVideo(boolean refresh) throws FileNotFoundException{
        if (refresh) {
            updateVideos();
        }

        if (!videoList.isEmpty()) {
            int index = random.nextInt(videoList.size());
            return videoList.get(index);
        }
        throw new FileNotFoundException("No Video files found");

    }



}

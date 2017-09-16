package com.cjapps.playinsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class ImageLoader {

    MemoryCache memoryCache=new MemoryCache();
    FileCache fileCache;
    private Map<ImageView, String> imageViews=Collections.synchronizedMap(new WeakHashMap<ImageView, String>());
    ExecutorService executorService;

    Context context;
    Animation aa;

    public static int MODE;

    public ImageLoader(Context context){
        fileCache=new FileCache(context);
        this.context=context;
        executorService=Executors.newFixedThreadPool(10);
    }

    int stub_id = R.drawable.ic_launcher;
    public void DisplayImage(String url, int loader, ImageView imageView)
    {
        aa=new AlphaAnimation(0.5f,1);
        aa.setInterpolator(new DecelerateInterpolator());
        aa.setDuration(1200);

        stub_id = loader;
        imageViews.put(imageView, url);
        Bitmap bitmap=memoryCache.get(url);
        if(bitmap!=null) {
            imageView.setAnimation(aa);
            imageView.setImageBitmap(bitmap);
        }
        else
        {
            queuePhoto(url, imageView);
        }
    }

    private void queuePhoto(String url, ImageView imageView)
    {
        PhotoToLoad p=new PhotoToLoad(url, imageView);
        executorService.submit(new PhotosLoader(p));
    }

    private Bitmap getBitmap(String url){

        try {
            return new LoadImageTask().execute(url).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    class LoadImageTask extends AsyncTask<String,String,Bitmap>{
        @Override
        protected Bitmap doInBackground(String... url) {

            File f=fileCache.getFile(url[0]);

            //from SD cache
            Bitmap b = decodeFile(f);
            if(b!=null)
                return b;

            if(MODE == 0) { //from URL
                try {
                    Bitmap bitmap;
                    URL imageUrl = new URL(url[0]);
                    HttpURLConnection conn = (HttpURLConnection) imageUrl.openConnection();
                    conn.setConnectTimeout(30000);
                    conn.setReadTimeout(30000);
                    conn.setInstanceFollowRedirects(true);
                    InputStream is = conn.getInputStream();
                    OutputStream os = new FileOutputStream(f);
                    Utils.CopyStream(is, os);
                    os.close();
                    bitmap = decodeFile(f);
                    return bitmap;
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return null;
                }
            }else if (MODE == 1) { // from content
                Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart/" + url[0]);
                Bitmap artwork = null;
                ContentResolver res = MainActivity.main_context.getContentResolver();

                try {

                    InputStream in = res.openInputStream(sArtworkUri);

                    if(in!=null) {

                        artwork = BitmapFactory.decodeStream(in);
                        in.close();
                    }
                    return artwork;

                }catch (Exception e){
                    return null;
                }
            }
            return null;
        }
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f),null,o);

            //Find the correct scale value. It should be the power of 2.
            int scale=1;

            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException ignored) {}
        return null;
    }

    //Task for the queue
    private class PhotoToLoad
    {
        public String url;
        public ImageView imageView;
        public PhotoToLoad(String u, ImageView i){
            url=u;
            imageView=i;
        }
    }

    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;
        PhotosLoader(PhotoToLoad photoToLoad){
            this.photoToLoad=photoToLoad;
        }

        @Override
        public void run() {
            if(imageViewReused(photoToLoad))
                return;
            Bitmap bmp=getBitmap(photoToLoad.url);
            memoryCache.put(photoToLoad.url, bmp);
            if(imageViewReused(photoToLoad))
                return;
            BitmapDisplayer bd=new BitmapDisplayer(bmp, photoToLoad);
            Activity a=(Activity)photoToLoad.imageView.getContext();
            a.runOnUiThread(bd);
        }
    }

    boolean imageViewReused(PhotoToLoad photoToLoad){
        String tag=imageViews.get(photoToLoad.imageView);
        return tag == null || !tag.equals(photoToLoad.url);
    }

    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        PhotoToLoad photoToLoad;
        public BitmapDisplayer(Bitmap b, PhotoToLoad p){bitmap=b;photoToLoad=p;}
        public void run()
        {
            if(imageViewReused(photoToLoad))
                return;
            if(bitmap!=null){

                photoToLoad.imageView.setAnimation(aa);
                photoToLoad.imageView.setImageBitmap(bitmap);
            }
            else
                photoToLoad.imageView.setImageResource(stub_id);
        }
    }
}
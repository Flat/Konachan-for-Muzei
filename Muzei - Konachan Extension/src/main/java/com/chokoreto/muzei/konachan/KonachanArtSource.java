package com.chokoreto.muzei.konachan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;


import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.List;

import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.http.QueryMap;

import com.chokoreto.muzei.konachan.KonachanService.Posts;
import com.google.android.apps.muzei.api.UserCommand;


public class KonachanArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = "Konachan";
    private static final String SOURCE_NAME = "Konachan";
    static final UserCommand COMMAND_ID_DOWNLOAD = new UserCommand(1337, "Download");
    UserCommand usrNext = new UserCommand(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    List<UserCommand> cmList = new ArrayList<UserCommand>();
    static final int mID = 134;




    public KonachanArtSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        cmList.add(usrNext);
        cmList.add(COMMAND_ID_DOWNLOAD);
        setUserCommands(cmList);
    }
    @Override
    public void onCustomCommand(int i){
        if(i == 1337){
            downloadArtwork(getCurrentArtwork());
        }
    }
 public void downloadArtwork(final Artwork dlArt){
    try {
        final NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Downloading Wallpaper")
            .setContentText("Download in progress");
        final NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        new Thread(
            new Runnable() {
             @Override
             public void run() {
                int received = 0;
                int conLength;
                try {
                    URL dlTarget = new URL(dlArt.getImageUri().toString());
                    HttpURLConnection hCon = (HttpURLConnection)dlTarget.openConnection();
                    hCon.setDoInput(true);
                    hCon.connect();
                    conLength = hCon.getContentLength();
                    InputStream is = hCon.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is);
                    byte[] data = new byte[1024];
                    String fPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Muzei - Konachan";
                    File fDir = new File(fPath);
                    if (!fDir.exists()){
                         fDir.mkdirs();
                    }
                    String fileName;
                    String[] temp;
                    String fileExtension;
                    temp =dlArt.getImageUri().toString().split("\\.");
                    fileExtension = temp[temp.length-1];
                    if (dlArt.getTitle().length() > 200)
                    {
                        fileName = dlArt.getTitle().substring(0,200);
                    }
                    else
                    {
                        fileName = dlArt.getTitle();
                    }
                    File file = new File(fDir, fileName +"." + fileExtension);
                    FileOutputStream fOut = new FileOutputStream(file);
                    float percentComplete =0;
                    int count =0;
                    while((count = bis.read(data)) != -1){
                        received += count;
                        percentComplete = Math.round(((received*100)/conLength));
                        mBuilder.setProgress(100, Math.round(percentComplete), false);
                        mBuilder.setContentText(Integer.toString((int)Math.round(percentComplete)) + "% complete");
                        mNotificationManager.notify(mID, mBuilder.build());
                        fOut.write(data, 0, count);
                    }
                    fOut.flush();
                    fOut.close();
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(file));
                    sendBroadcast(intent);
                    Intent resultIntent = new Intent();
                    resultIntent.setAction(Intent.ACTION_VIEW);
                    resultIntent.setDataAndType(Uri.fromFile(file), "image/*");
                    PendingIntent resultPendingIntent = PendingIntent.getActivity(getApplicationContext() , 0, resultIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);
                    mBuilder.setContentText("Download complete")
                        .setProgress(0, 0, false);
                    mBuilder.setLargeIcon(decodeSampledBitmapFromFile(file, 150, 150));
                    mBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(decodeSampledBitmapFromFile(file,675, 337)));
                    Notification note = mBuilder.build();
                    note.flags = Notification.FLAG_AUTO_CANCEL;
                    mNotificationManager.notify(mID, note);
                }
                catch(MalformedURLException e){
                    Log.e(TAG, "Bad URL: " + e.getMessage());
                    NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Muzei - Konachan")
                        .setContentText("Download failed");
                    NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(mID, mBuilder.build());
                    Log.e(TAG, e.getMessage());
                }
                catch(IOException e){
                    NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(getApplicationContext())
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Muzei - Konachan")
                        .setContentText("Download failed");
                    NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(mID, mBuilder.build());
                    Log.e(TAG,e.getMessage());
                }
                 catch(Exception e){
                     NotificationCompat.Builder mBuilder =
                             new NotificationCompat.Builder(getApplicationContext())
                                     .setSmallIcon(R.drawable.ic_launcher)
                                     .setContentTitle("Muzei - Konachan")
                                     .setContentText("Download failed");
                     NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                     mNotificationManager.notify(mID, mBuilder.build());
                     Log.e(TAG,e.getMessage());
                     Log.e(TAG, "Exception: ", e);
                 }
            }
        }).start();
    }
    catch (Exception ex){
         Log.e(TAG, ex.getMessage());
     }
    }
    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;
        Config config = new Config(this);
        ConnectivityManager connMan = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo niWifi = connMan.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        int configTime = config.getTimeSet();
        final int ROTATE_TIME_MILLIS = configTime * 60 * 1000;
        if (config.prettyflyforaWifi().equals(true)){
            if (!niWifi.isConnected()){
                scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                Log.w(TAG, "Not connected to Wi-Fi, retrying in " + (System.currentTimeMillis() + ROTATE_TIME_MILLIS));
                return;
            }
        }
        removeStaleMD5(this);
        String serverBooru = config.getBooru();
        String strHyperText = config.getHyperTextProtocol(serverBooru);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.NONE)
                .setEndpoint(strHyperText + serverBooru)
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                        Log.e(TAG,retrofitError.getUrl()+ "\n"+ retrofitError.getBody());
                        if (retrofitError.isNetworkError()) {
                            return new RetryException();
                        }
                        int statusCode = retrofitError.getResponse().getStatus();
                        if((500 <= statusCode && statusCode < 600)){
                            return new RetryException();
                        }
                        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                        return retrofitError;
                    }
                })
                .build();
        Map<String, String> mapQueries = new HashMap<String, String>(200);
        if (serverBooru.equals("gelbooru.com")){
            mapQueries.put("page","dapi");
            mapQueries.put("s","post");
            mapQueries.put("q","index");
            mapQueries.put("json","1");
        }
        mapQueries.put("tags",config.strBuilder());
        mapQueries.put("limit",Config.limit);

        KonachanService service = restAdapter.create(KonachanService.class);
        List<Posts> response;
        try{
            response = service.getPopularPosts(config.getAPIString(serverBooru),mapQueries);
        }
        catch(Exception e){
            response = null;
        }

        if (response == null) {
            throw new RetryException();
        }

        if (response.size() == 0) {
            scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
            return;
        }

        Posts post;
        String token;
        Database.DatabaseHelper dbHelper = new Database.DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int respCounter= 0;
        while (true) {
            Cursor cursor;
            cursor = db.rawQuery("SELECT md5, timestamp FROM images WHERE md5=?", new String[] {getproperMD5(response,respCounter,this)});
            while(cursor.getCount() > 0 && respCounter + 1 <= response.size())
            {
                cursor.moveToFirst();
                final long CLEAR_MD5_TIME_MILLIS = config.getMD5Clear() * 60 * 1000;
                if(cursor.getLong(1) <= (System.currentTimeMillis() - CLEAR_MD5_TIME_MILLIS) && (System.currentTimeMillis() - CLEAR_MD5_TIME_MILLIS) > 0)
                {
                    db.execSQL("DELETE FROM images WHERE md5=? AND timestamp =?", new String[]{cursor.getString(0), cursor.getString(1)});
                    break;
                }
                else {
                    if (respCounter + 1 >= response.size()) {
                        break;
                    }
                    Log.i(TAG, "RESPCOUNTER: " + Integer.toString(respCounter));
                    respCounter += 1;
                    cursor = db.rawQuery("SELECT md5, timestamp FROM images WHERE md5=?", new String[]{getproperMD5(response,respCounter,this)});
                }
            }
            post = response.get(respCounter);
            db.execSQL("INSERT INTO images (md5, timestamp) VALUES (?, ?)",new String[] {getproperMD5(response,respCounter,this), Long.toString(System.currentTimeMillis())});
            token = Integer.toString(post.id);
            if (response.size() <= 1 || !TextUtils.equals(token, currentToken)) {
                break;
            }
            cursor.close();
            break;
        }
        dbHelper.close();
        db.close();

        publishArtwork(new Artwork.Builder()
                .title(post.tags)
                .byline(post.author)
                .imageUri(Uri.parse(post.file_url))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(config.getURLForLink(serverBooru) + post.id)))
                .build());
        Log.w(TAG,post.file_url);
        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    public static Bitmap decodeSampledBitmapFromFile(File file, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }
    public static void removeStaleMD5(Context context){
        Config config = new Config(context);
        final long CLEAR_MD5_TIME_MILLIS = config.getMD5Clear() * 60 * 1000;
        Database.DatabaseHelper dbHelper = new Database.DatabaseHelper(context);
        SQLiteDatabase dbWrite = dbHelper.getWritableDatabase();
        dbWrite.execSQL("DELETE FROM images WHERE timestamp <= ?",new String[] {Long.toString(System.currentTimeMillis() - CLEAR_MD5_TIME_MILLIS)});
        dbWrite.close();
        dbHelper.close();
    }
    public static String getproperMD5(List<Posts> response, int responseID, Context context){
        Config config = new Config(context);
        String serverBooru = config.getBooru();
        if (serverBooru.equals("gelbooru.com")){
            return response.get(responseID).hash;
        }
        else{
            return response.get(responseID).md5;
        }
    }

}

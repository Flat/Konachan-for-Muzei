package com.chokoreto.muzei.konachan;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
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
                    String fileExtension;
                    if (dlArt.getTitle().length() > 200)
                    {
                        fileName = dlArt.getTitle();
                        fileExtension = ".png";
                        fileName = fileName.substring(0,200);
                    }
                    else
                    {
                        fileName = dlArt.getTitle();
                        fileExtension = ".png";
                    }
                    File file = new File(fDir, fileName + fileExtension);
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
        String serverBooru = config.getBooru();
        String strHyperText = config.getHyperTextProtocol(serverBooru);
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setEndpoint(strHyperText + serverBooru)
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                        int statusCode = retrofitError.getResponse().getStatus();
                        Log.e(TAG,retrofitError.getUrl()+ "\n"+ retrofitError.getBody());
                        if (retrofitError.isNetworkError() || (500 <= statusCode && statusCode < 600)) {
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
        List<Posts> response = service.getPopularPosts(config.getAPIString(serverBooru),mapQueries);

        if (response == null) {
            throw new RetryException();
        }

        if (response.size() == 0) {
            scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
            return;
        }

        Random random = new Random(System.currentTimeMillis());
        Posts post;
        String token;
        while (true) {
            post = response.get(random.nextInt(response.size()));
            token = Integer.toString(post.id);
            if (response.size() <= 1 || !TextUtils.equals(token, currentToken)) {
                break;
            }
        }

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
}

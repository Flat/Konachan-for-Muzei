package com.chokoreto.muzei.konachan;

import android.app.NotificationManager;
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
            downloadArtwork();
        }
    }
 public void downloadArtwork(Artwork dlArt){
    try {
        NotificationCompat.Builder mBuilder =
        new NotificationCompat.Builder(this)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("Downloading Wallpaper")
            .setContentText("Download in progress");
        NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        new Thread(
            new Runnable() {
             @Override
             public void run() {
                long recieved = 0;
                long conLength;
                try {
                    URL dlTarget = new URL(dlArt.getImageUri().toString());
                    HttpURLConnection hCon = (HttpURLConnection)dlTarget.openConnection();
                    hCon.setDoInput(true);
                    hCon.connect();
                    conLength = hCon.getContentLength();
                    InputStream is = hCon.getInputStream();
                    BufferedInputStream bis = new BufferedInputStream(is)
                    Byte data = new Byte[1024];
                    String fPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Muzei - Konachan";
                    File fDir = new File(fPath);
                    if (!fDir.exists()){
                         fDir.mkdirs();
                    }
                    String fileName;
                    String fileExtention;
                    if (dlArt.getTitle().length() > 200)
                    {
                        fileName = dlArt.getTitle();
                        fileExtention = ".png";
                        fileName = fileName.substring(0,200);
                    }
                    else
                    {
                        fileName = dlArt.getTitle();
                        fileExtention = ".png";
                    }
                    File file = new File(fDir, fileName + fileExtention);
                    FileOutputStream fOut = new FileOutputStream(file);
                    int percentComplete =0;
                    while((count = bis.read(data)) != -1)){
                        recieved += count;
                        percentComplete = recieved(100/conLength);
                        mBuilder.setProgress(100, percentComplete, false);
                        mNotificationManager.notify(mID, mBuilder.build());
                        fOut.write(data, 0, count);
                    }
                    fOut.flush();
                    fOut.close();
                    Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    intent.setData(Uri.fromFile(file));
                    sendBroadcast(intent);
                    mBuilder.setContentText("Download complete")
                        .setProgress(0,0,false);
                    mNotifyManager.notify(id, mBuilder.build());
                }
                catch(MalformedURLException e){
                    Log.e(TAG, "Bad URL: " + e.getMessage());
                    NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Muzei - Konachan")
                        .setContentText("Download failed");
                    NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(mID, mBuilder.build());
                }
                catch(IOException e){
                    NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                         .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Muzei - Konachan")
                        .setContentText("Download failed");
                    Log.e(TAG,e.getLocalizedMessage());
                    NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(mID, mBuilder.build());
                }
            }
        }).start();
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
}

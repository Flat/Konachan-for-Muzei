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
import java.util.Random;
import java.util.List;

import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

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
    public void downloadArtwork(){
        if (getCurrentArtwork() != null){
            Artwork dlArt = getCurrentArtwork();
            try {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle("Muzei - Konachan")
                                .setContentText("Download started.");
                NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(mID, mBuilder.build());
                URL dlTarget = new URL(dlArt.getImageUri().toString());
                HttpURLConnection hCon = (HttpURLConnection)dlTarget.openConnection();
                hCon.setDoInput(true);
                hCon.connect();
                InputStream is = hCon.getInputStream();
                Bitmap dlImage = BitmapFactory.decodeStream(is);
                String fPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Muzei - Konachan";
                File fDir = new File(fPath);
                if (!fDir.exists()){
                    fDir.mkdirs();
                }
                File file = new File(fDir, dlArt.getTitle() + ".png");
                FileOutputStream fOut = new FileOutputStream(file);
                dlImage.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.flush();
                fOut.close();
                mBuilder = new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("Muzei - Konachan")
                        .setContentText("Download complete!");
                mNotificationManager.notify(mID,mBuilder.build());
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(file));
                sendBroadcast(intent);
            }
            catch(MalformedURLException e){
                Log.e(TAG, "Bad URL: " + e.getMessage());
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle("Muzei - Konachan")
                                .setContentText("Download failed.");
                NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(mID, mBuilder.build());

            }
            catch(IOException e){
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_launcher)
                                .setContentTitle("Muzei - Konachan")
                                .setContentText("Download failed.");
                NotificationManager mNotificationManager =(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(mID, mBuilder.build());
            }
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
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setServer("https://konachan.net")
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                        int statusCode = retrofitError.getResponse().getStatus();
                        if (retrofitError.isNetworkError() || (500 <= statusCode && statusCode < 600)) {
                            return new RetryException();
                        }
                        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                        return retrofitError;
                    }
                })
                .build();

        KonachanService service = restAdapter.create(KonachanService.class);
        List<Posts> response = service.getPopularPosts(config.strBuilder(), Config.limit);

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
                        Uri.parse("http://konachan.net/post/show/" + post.id)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}

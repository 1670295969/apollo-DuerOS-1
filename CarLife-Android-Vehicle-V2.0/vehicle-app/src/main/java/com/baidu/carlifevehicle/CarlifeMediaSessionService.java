package com.baidu.carlifevehicle;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;
import android.view.KeyEvent;

import androidx.media.session.MediaButtonReceiver;
import com.baidu.carlifevehicle.util.CommonParams;
import com.baidu.carlifevehicle.util.DisplayUtils;

public class CarlifeMediaSessionService extends Service {
    public static final String TAG = "CarlifeMediaSessionService";

    private static final int NOTIFICATION_ID = 101;
    private static final String CHANNEL_ID = "foreground_service_channel_media";
    private MediaSessionCompat mMediaSession;

    public static void start(Context context) {
        context.startService(new Intent(context, CarlifeMediaSessionService.class));
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "MyMediaSessionService onCreate");
        initMediaSession();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand : " + intent);
        if (intent!=null){
            Log.i(TAG, "onStartCommand : " + intent.getAction());

        }
        DisplayUtils.INSTANCE.showForegroundNotification(this,CHANNEL_ID,NOTIFICATION_ID);
        MediaButtonReceiver.handleIntent(this.mMediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        MediaSessionCompat mediaSessionCompat = this.mMediaSession;
        if (mediaSessionCompat != null) {
            mediaSessionCompat.setCallback(null);
            mediaSessionCompat.setActive(false);

            mediaSessionCompat.release();
        }
        // 服务被销毁时移除通知
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void initMediaSession() {
        this.mMediaSession = new MediaSessionCompat(this, "CarlifeMediaSessionService");
//        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
//                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        this.mMediaSession.setCallback(new MediaSessionCallBack(this));
        this.mMediaSession.setActive(true);
    }



}
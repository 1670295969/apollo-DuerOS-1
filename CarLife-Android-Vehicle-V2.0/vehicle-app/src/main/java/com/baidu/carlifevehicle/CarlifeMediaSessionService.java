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
    private static final String TAG = "MyMediaSessionService";

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
        Log.i("MyMediaSessionService", "MyMediaSessionService onCreate");
        initMediaSession();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("MyMediaSessionService", "onStartCommand : " + intent);
        if (intent!=null){
            Log.i("MyMediaSessionService", "onStartCommand : " + intent.getAction());

        }
        DisplayUtils.INSTANCE.showForegroundNotification(this,CHANNEL_ID,NOTIFICATION_ID);
        MediaButtonReceiver.handleIntent(this.mMediaSession, intent);
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        super.onDestroy();
        MediaSessionCompat mediaSessionCompat = this.mMediaSession;
        if (mediaSessionCompat != null) {
            mediaSessionCompat.release();
        }
        // 服务被销毁时移除通知
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void initMediaSession() {
        this.mMediaSession = new MediaSessionCompat(this, "MyMediaSessionService");
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setActive(true);
        this.mMediaSession.setCallback(new MediaSessionCompat.Callback() {
            public boolean onMediaButtonEvent(Intent mediaButtonIntent) {
                KeyEvent keyEvent = (KeyEvent) mediaButtonIntent.getParcelableExtra("android.intent.extra.KEY_EVENT");
                Log.i("MyMediaSessionService", "onMediaButtonEvent : " + keyEvent.getKeyCode());
                if (keyEvent.getKeyCode() != 87 && keyEvent.getKeyCode() != 88) {
                    return super.onMediaButtonEvent(mediaButtonIntent);
                }
                CarlifeMediaSessionService.this.performMediaButton(keyEvent);
                return true;
            }
        }, (Handler) null);
        this.mMediaSession.setActive(true);
    }

    public void performMediaButton(KeyEvent keyEvent) {
        int action = keyEvent.getAction();
        int keyCode = keyEvent.getKeyCode();
        if (action == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_MEDIA_NEXT) {
                sendHardKeyCodeEvent(CommonParams.KEYCODE_SEEK_ADD);
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS) {
                sendHardKeyCodeEvent(CommonParams.KEYCODE_SEEK_SUB);
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                    || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
            ) {
                sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_STOP);
            } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                    || keyCode == KeyEvent.KEYCODE_MEDIA_STOP
            ) {
                sendHardKeyCodeEvent(CommonParams.KEYCODE_MEDIA_START);
            }
        }
    }

    private void sendHardKeyCodeEvent(int keycode) {
        DisplayUtils.sendHardKeyCodeEvent(keycode);
    }
}
package com.baidu.carlifevehicle.audio.receiver;

import static com.baidu.carlife.sdk.Constants.MSG_CHANNEL_TOUCH;
import static com.baidu.carlife.sdk.internal.protocol.ServiceTypes.KEYCODE_PHONE_CALL;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.KeyEvent;

import com.baidu.android.common.net.ConnectManager;
import com.baidu.carlife.protobuf.CarlifeCarHardKeyCodeProto;
import com.baidu.carlife.sdk.internal.protocol.CarLifeMessage;
import com.baidu.carlife.sdk.internal.protocol.ServiceTypes;
import com.baidu.carlife.sdk.receiver.CarLife;
import com.baidu.carlifevehicle.util.CarlifeConfUtil;
import com.baidu.carlifevehicle.util.CommonParams;

public class CfMediaButtonReceiver extends BroadcastReceiver {

    public static int KEY_ACTION = 0;
    private static final String TAG =  CfMediaButtonReceiver.class.getSimpleName();

    public void sendHardKeyCodeEvent(int keycode) {
        try {
            Log.d(TAG, "sendHardKeyCodeEvent: " + ("keycode = " + keycode));
            CarLifeMessage message = CarLifeMessage.obtain(
                    MSG_CHANNEL_TOUCH,
                    ServiceTypes.MSG_TOUCH_CAR_HARD_KEY_CODE,
                    0);
            message.setServiceType(CommonParams.MSG_TOUCH_CAR_HARD_KEY_CODE);
            message.payload(
                    CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder()
                            .setKeycode(keycode)
                            .build());
            CarLife.receiver().postMessage(message);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void onReceive(Context context, Intent intent) {
        KeyEvent keyEvent;
        if ("android.intent.action.MEDIA_BUTTON".equals(intent.getAction()) && (keyEvent = (KeyEvent) intent.getParcelableExtra("android.intent.extra.KEY_EVENT")) != null) {
            int action = keyEvent.getAction();
            int keyCode = keyEvent.getKeyCode();
            Log.d(TAG, "carlife: onReceive is triggered!");
            if (KEY_ACTION > 0 && KEY_ACTION <= 2 && action == KEY_ACTION - 1) {
                switch (keyCode) {
                    case 85:
                        Log.d(TAG, "KEYCODE_MEDIA_PLAY_PAUSE");
                        if (!ModeService.getInstance().getIsUserPause()) {
                            sendHardKeyCodeEvent(32);
                            break;
                        } else {
                            sendHardKeyCodeEvent(31);
                            break;
                        }
                    case 86:
                        Log.d(TAG, CarlifeConfUtil.KEY_KEYCODE_MEDIA_STOP);
                        sendHardKeyCodeEvent(32);
                        break;
                    case 87:
                        Log.d(TAG, "KEYCODE_MEDIA_NEXT");
                        sendHardKeyCodeEvent(16);
                        break;
                    case 88:
                        Log.d(TAG, "KEYCODE_MEDIA_PREVIOUS");
                        sendHardKeyCodeEvent(15);
                        break;
//                    case TransportMediator.KEYCODE_MEDIA_PLAY /*126*/:
//                        Log.d(TAG, "KEYCODE_MEDIA_PLAY");
//                        TouchListenerManager.getInstance().sendHardKeyCodeEvent(31);
//                        break;
//                    case TransportMediator.KEYCODE_MEDIA_PAUSE /*127*/:
//                        Log.d(TAG, "KEYCODE_MEDIA_PAUSE");
//                        TouchListenerManager.getInstance().sendHardKeyCodeEvent(32);
//                        break;
                }
            }
            if (isOrderedBroadcast()) {
                abortBroadcast();
            }
        }
    }
}

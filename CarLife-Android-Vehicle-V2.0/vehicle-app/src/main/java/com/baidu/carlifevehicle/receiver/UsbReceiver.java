package com.baidu.carlifevehicle.receiver;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.carlife.sdk.util.Logger;
import com.baidu.carlifevehicle.CarlifeActivity;
import com.baidu.carlifevehicle.util.CarlifeConfUtil;
import com.baidu.carlifevehicle.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UsbReceiver extends BroadcastReceiver {

    private Handler handler = new Handler(Looper.getMainLooper());
    //UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    //                    Log.i("UsbReceiver",""+device);
    //mManufacturerName

    private static final Set<String> mManufacturerNameList = new HashSet<>();

    static {
        mManufacturerNameList.add("xiaomi");
        mManufacturerNameList.add("oppo");
        mManufacturerNameList.add("vivo");
        mManufacturerNameList.add("meizu");
        mManufacturerNameList.add("huawei");
        mManufacturerNameList.add("honor");

    }

    private void showFront(Context context) {

        Logger.d("UsbReceiver", "topActivity:");
        Intent intent = new Intent(context, CarlifeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }


    private Runnable launcherRunnable;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!TextUtils.equals(action, UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            return;
        }


        if (launcherRunnable != null) {
            handler.removeCallbacksAndMessages(null);
        }
        launcherRunnable = new Runnable() {
            @Override
            public void run() {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device == null) {
                    return;
                }
                String mName = device.getManufacturerName();
                if (mName != null) {
                    if (mManufacturerNameList.contains(mName.toLowerCase())) {
                        Log.i("UsbReceiver", "" + device);
                        boolean result = PreferenceUtil.getInstance()
                                .getBoolean(CarlifeConfUtil.CFG_USB_SHOW_UI, false);
                        if (result) {
                            try {
                                showFront(context);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }

                }
                launcherRunnable = null;
            }
        };
        handler.postDelayed(launcherRunnable, 5000);
    }
}


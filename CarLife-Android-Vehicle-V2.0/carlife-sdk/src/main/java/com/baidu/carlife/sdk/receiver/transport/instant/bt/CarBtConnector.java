package com.baidu.carlife.sdk.receiver.transport.instant.bt;


import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.StandardWatchEventKinds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CarBtConnector {

    interface BtTypeCallBack {
        void btBack(Object btType);
    }

    private static final int A2DP_SINK = 11;
    private static final String ACTION_A2DP_CONNECTION_STATE_CHANGED = "android.bluetooth.a2dp-sink.profile.action.CONNECTION_STATE_CHANGED";
    private static final String ACTION_AUDIO_STATE_CHANGED = "android.bluetooth.headsetclient.profile.action.AUDIO_STATE_CHANGED";
    private static final String ACTION_HEADSET_CLIENT_CONNECTION_STATE_CHANGED = "android.bluetooth.headsetclient.profile.action.CONNECTION_STATE_CHANGED";
    private static final int HEADSET_CLIENT = 16;
    public static final int STATE_BLUETOOTH_A2DP_CONNECTED = 13;
    public static final int STATE_BLUETOOTH_A2DP_DISCONNECTED = 14;
    public static final int STATE_BLUETOOTH_CONNECTED = 9;
    public static final int STATE_BLUETOOTH_DISCONNECTED = 15;
    public static final int STATE_BLUETOOTH_DISCOVER_FINISHED = 8;
    public static final int STATE_BLUETOOTH_DISCOVER_STARTED = 4;
    public static final int STATE_BLUETOOTH_HFP_CONNECTED = 10;
    public static final int STATE_BLUETOOTH_HFP_CONNECTING = 11;
    public static final int STATE_BLUETOOTH_HFP_DISCONNECTED = 12;
    public static final int STATE_BLUETOOTH_STATE_BOND_BONDED = 6;
    public static final int STATE_BLUETOOTH_STATE_BOND_BONDING = 5;
    public static final int STATE_BLUETOOTH_STATE_BOND_NONE = 7;
    public static final int STATE_BLUETOOTH_STATE_OFF = 0;
    public static final int STATE_BLUETOOTH_STATE_ON = 3;
    public static final int STATE_BLUETOOTH_STATE_TURNING_OFF = 1;
    public static final int STATE_BLUETOOTH_STATE_TURNING_ON = 2;
    private static final String TAG = "CarBtConnector";
    private static Class<?> sBluetoothA2dpSinkClass;
    private static Class<?> sBluetoothHeadsetClientClass;
    private Object mBluetoothA2dpSink;
    private BluetoothAdapter mBluetoothAdapter;
    private final BroadcastReceiver mBluetoothEventReceiver;
    private Object mBluetoothHeadSetClient;
    private final Context mContext;
    private BluetoothDevice mDisconnectedA2dpDevice;
    private BluetoothDevice mDisconnectedHeadSetDevice;
    private final OnStateChangeListener mListener;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    /* access modifiers changed from: private */
    private final ExecutorService mService = Executors.newSingleThreadExecutor();

    public interface OnStateChangeListener {
        void onStateChanged(int i, BluetoothDevice device);
    }

    static {
        try {
            sBluetoothA2dpSinkClass = Class.forName("android.bluetooth.BluetoothA2dpSink");
            sBluetoothHeadsetClientClass = Class.forName("android.bluetooth.BluetoothHeadsetClient");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "bluetooth a2dp sink class not found.", e);
        }
    }

    public CarBtConnector(Context context, OnStateChangeListener listener) {
        BroadcastReceiver r0 = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("android.bluetooth.device.action.FOUND".equals(action)) {
                    BluetoothDevice device = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.i(CarBtConnector.TAG, "device name: " + device.getName() + "\ndevice address: " + device.getAddress() + "\n");

                } else if ("android.bluetooth.adapter.action.DISCOVERY_STARTED".equals(action)) {
                    Log.i(CarBtConnector.TAG, "discover started");
                    CarBtConnector.this.notifyStep(STATE_BLUETOOTH_DISCOVER_STARTED,  null);
                } else if ("android.bluetooth.adapter.action.DISCOVERY_FINISHED".equals(action)) {
                    Log.i(CarBtConnector.TAG, "discover finished");
                    CarBtConnector.this.notifyStep(STATE_BLUETOOTH_DISCOVER_FINISHED,  null);
                } else if ("android.bluetooth.device.action.BOND_STATE_CHANGED".equals(action)) {
                    BluetoothDevice device2 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    switch (device2.getBondState()) {
                        case 10:
                            Log.d(CarBtConnector.TAG, "bond none");
                            CarBtConnector.this.notifyStep(STATE_BLUETOOTH_STATE_BOND_NONE, device2);
                            return;
                        case 11:
                            Log.d(CarBtConnector.TAG, "bonding......");
                            CarBtConnector.this.notifyStep(STATE_BLUETOOTH_STATE_BOND_BONDING, device2);
                            return;
                        case 12:
                            Log.d(CarBtConnector.TAG, "bonded");
                            CarBtConnector.this.notifyStep(STATE_BLUETOOTH_STATE_BOND_BONDED, device2);
                            CarBtConnector.this.rethinkBtBonded(device2);
                            return;
                        default:
                            return;
                    }
                } else if ("android.bluetooth.adapter.action.STATE_CHANGED".equals(action)) {
                    int bluetoothState = intent.getIntExtra("android.bluetooth.adapter.extra.STATE", 0);
                    Log.i(CarBtConnector.TAG, "bluetooth state changed, new status: " + bluetoothState);
                    if (bluetoothState == 12) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_STATE_ON,  null);
                        CarBtConnector.this.rethinkBtStateOn();
                    } else if (bluetoothState == 10) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_STATE_OFF,  null);
                        CarBtConnector.this.rethinkBtStateOff();
                    } else if (bluetoothState == 13) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_STATE_TURNING_ON,  null);
                    } else {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_STATE_ON,  null);
                    }
                } else if ("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED".equals(action)) {
                    int bluetoothState2 = intent.getIntExtra("android.bluetooth.adapter.extra.CONNECTION_STATE", 0);
                    BluetoothDevice device3 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.i(CarBtConnector.TAG, "connection state changed, state: " + bluetoothState2 + " device name: " + device3.getName() + ", device address: " + device3.getAddress());
                    if (bluetoothState2 == 2) {
                        Log.i(CarBtConnector.TAG, "connection state changed, state: " + bluetoothState2 + " device name: " + device3.getName() + ", device address: " + device3.getAddress());
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_CONNECTED, device3);
                        CarBtConnector.this.rethinkDeviceConnected(device3);
                    } else if (bluetoothState2 == 0) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_DISCONNECTED, device3);
                    }
                } else if ("android.bluetooth.device.action.ACL_CONNECTED".equals(action)) {
                    BluetoothDevice device4 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.i(CarBtConnector.TAG, "acl connected, device name:" + device4.getName() + ", device address:" + device4.getAddress());
                } else if ("android.bluetooth.device.action.ACL_DISCONNECTED".equals(action)) {
                    BluetoothDevice device5 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.i(CarBtConnector.TAG, "acl disconnected, device name:" + device5.getName() + ", device address:" + device5.getAddress());
                } else if (CarBtConnector.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
                    Log.i(CarBtConnector.TAG, "head set audio status changed: " + intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0));
                } else if (CarBtConnector.ACTION_A2DP_CONNECTION_STATE_CHANGED.equals(action)) {
                    int state = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                    BluetoothDevice device6 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.i(CarBtConnector.TAG, "a2dpState changed, state: " + state + " device name: " + device6.getName() + ", device address: " + device6.getAddress());
                    if (state == 2) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_A2DP_CONNECTED, device6);
                    } else if (state == 0) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_A2DP_DISCONNECTED, device6);
                    }
                } else if (CarBtConnector.ACTION_HEADSET_CLIENT_CONNECTION_STATE_CHANGED.equals(action)) {
                    int state2 = intent.getIntExtra("android.bluetooth.profile.extra.STATE", 0);
                    BluetoothDevice device7 = (BluetoothDevice) intent.getParcelableExtra("android.bluetooth.device.extra.DEVICE");
                    Log.i(CarBtConnector.TAG, "hfp changed, state: " + state2 + " device name: " + device7.getName() + ", device address: " + device7.getAddress());
                    if (state2 == 2) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_HFP_CONNECTED, device7);
                    } else if (state2 == 1) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_HFP_CONNECTING, device7);
                    } else if (state2 == 0) {
                        CarBtConnector.this.notifyStep(STATE_BLUETOOTH_HFP_DISCONNECTED, device7);
                    }
                }
            }
        };
        this.mBluetoothEventReceiver = r0;
        this.mContext = context;
        this.mListener = listener;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            this.mBluetoothAdapter = bluetoothManager.getAdapter();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.bluetooth.adapter.action.DISCOVERY_STARTED");
        intentFilter.addAction("android.bluetooth.adapter.action.DISCOVERY_FINISHED");
        intentFilter.addAction("android.bluetooth.device.action.FOUND");
        intentFilter.addAction("android.bluetooth.device.action.BOND_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_CONNECTED");
        intentFilter.addAction("android.bluetooth.device.action.ACL_DISCONNECTED");
        intentFilter.addAction("android.bluetooth.adapter.action.CONNECTION_STATE_CHANGED");
        intentFilter.addAction("android.bluetooth.adapter.action.STATE_CHANGED");
        intentFilter.addAction(ACTION_AUDIO_STATE_CHANGED);
        intentFilter.addAction(ACTION_A2DP_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(ACTION_HEADSET_CLIENT_CONNECTION_STATE_CHANGED);
        context.registerReceiver(r0, intentFilter);

        initProfileProxy(A2DP_SINK, new BtTypeCallBack() {
            @Override
            public void btBack(Object btType) {
                mBluetoothA2dpSink = btType;
            }
        });

        initProfileProxy(BluetoothProfile.A2DP, new BtTypeCallBack() {
            @Override
            public void btBack(Object btType) {
                BluetoothA2dp ba = (BluetoothA2dp) btType;
                List list = ba.getConnectedDevices();
                Log.d(TAG,"list="+list);
            }
        });

        initProfileProxy(BluetoothProfile.HEADSET, new BtTypeCallBack() {
            @Override
            public void btBack(Object btType) {
                BluetoothHeadset ba = (BluetoothHeadset) btType;
                List list = ba.getConnectedDevices();
                Log.d(TAG,"list="+list);
            }
        });
        initProfileProxy(HEADSET_CLIENT, new BtTypeCallBack() {
            @Override
            public void btBack(Object btType) {
                mBluetoothHeadSetClient = btType;
            }
        });
    }





    /* access modifiers changed from: private */
    public void notifyStep(int step, BluetoothDevice device) {
        if (step == STATE_BLUETOOTH_A2DP_CONNECTED
        || step == STATE_BLUETOOTH_CONNECTED
        || step == STATE_BLUETOOTH_HFP_CONNECTED){
            //TODO 保存已连接的蓝牙
        }
        this.mMainHandler.post(new Runnable() {


            public void run() {
                CarBtConnector.this.lambda$notifyStep$2$CarBtConnector(step, device);
            }
        });
    }

    public /* synthetic */ void lambda$notifyStep$2$CarBtConnector(int step, BluetoothDevice device) {
        OnStateChangeListener onStateChangeListener = this.mListener;
        if (onStateChangeListener != null) {
            onStateChangeListener.onStateChanged(step, device);
        }
    }

    public void runOnAsyncThread(Runnable action) {
        if (!this.mService.isShutdown()) {
            this.mService.submit(action);
        } else {
            Log.e(TAG, "thread pool executor has been shutdown");
        }
    }

    public void clearStatus() {

        this.mDisconnectedHeadSetDevice = null;
        this.mDisconnectedA2dpDevice = null;
    }

    public void release() {
        try {
            this.mContext.unregisterReceiver(this.mBluetoothEventReceiver);
        } catch (Exception e) {
        }
        this.mService.shutdown();
        clearStatus();
    }

    /* access modifiers changed from: private */
    public void rethinkDeviceConnected(BluetoothDevice device) {
//        runOnAsyncThread(new Runnable() {
//
//            public final void run() {
//                CarBtConnector.this.lambda$rethinkDeviceConnected$9$CarBtConnector(device);
//            }
//        });
    }

    public /* synthetic */ void lambda$rethinkDeviceConnected$9$CarBtConnector(BluetoothDevice device) {

        Log.i(TAG, "user manually connected a new device, no need to reconnect carlink device when disconnected");
        clearStatus();
    }

    /* access modifiers changed from: private */
    public void rethinkBtStateOn() {
        runOnAsyncThread(new Runnable() {
            public final void run() {
                CarBtConnector.this.lambda$rethinkBtStateOn$10$CarBtConnector();
            }
        });
    }

    public /* synthetic */ void lambda$rethinkBtStateOn$10$CarBtConnector() {

    }

    public void rethinkBtStateOff() {
        runOnAsyncThread(new Runnable() {
            public final void run() {
                CarBtConnector.this.clearStatus();
            }
        });
    }

    /* access modifiers changed from: private */
    public void rethinkBtBonded(BluetoothDevice device) {
        runOnAsyncThread(new Runnable() {

            public final void run() {
                CarBtConnector.this.lambda$rethinkBtBonded$11$CarBtConnector(device);
            }
        });
    }

    public /* synthetic */ void lambda$rethinkBtBonded$11$CarBtConnector(BluetoothDevice device) {

    }

    /* access modifiers changed from: private */
    public void rethinkA2DPConnected(BluetoothDevice device) {
        runOnAsyncThread(new Runnable() {

            public final void run() {
                CarBtConnector.this.lambda$rethinkA2DPConnected$12$CarBtConnector(device);
            }
        });
    }

    public /* synthetic */ void lambda$rethinkA2DPConnected$12$CarBtConnector(BluetoothDevice device) {

    }

    /* access modifiers changed from: private */
    public void rethinkHFPConnected(BluetoothDevice device) {
        runOnAsyncThread(new Runnable() {

            public final void run() {
                CarBtConnector.this.lambda$rethinkHFPConnected$13$CarBtConnector(device);
            }
        });
    }

    public /* synthetic */ void lambda$rethinkHFPConnected$13$CarBtConnector(BluetoothDevice device) {
            stopBluetoothHeadSetClient();
    }

    private boolean isHFPConnect() {
        int state = this.mBluetoothAdapter.getProfileConnectionState(HEADSET_CLIENT);
        if (state == 2) {
            Log.i(TAG, "HFP CLIENT already connected");
            return true;
        }
        Log.i(TAG, "HFP CLIENT not connected, " + state);
        return false;
    }

    private boolean isA2DPSinkConnect() {
        int state = this.mBluetoothAdapter.getProfileConnectionState(A2DP_SINK);
        if (state == 2) {
            Log.i(TAG, "A2DP SINK already connected");
            return true;
        }
        Log.i(TAG, "A2DP SINK not connected, " + state);
        return false;
    }

    private void checkBtState() {
        BluetoothAdapter bluetoothAdapter = this.mBluetoothAdapter;
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Log.i(TAG, "Enabling Bluetooth Action: " + this.mBluetoothAdapter.enable());
        }
    }

    private void cancelDiscover() {
        if (this.mBluetoothAdapter.isDiscovering()) {
            this.mBluetoothAdapter.cancelDiscovery();
        }
    }

    private void startDiscover() {
        cancelDiscover();
        this.mBluetoothAdapter.startDiscovery();
    }

    public boolean createBond(BluetoothDevice btDevice) {
        try {
            return ((Boolean) BluetoothDevice.class.getMethod("createBond", new Class[0]).invoke(btDevice, new Object[0])).booleanValue();
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            Log.e(TAG, "createBond exception", e);
            return false;
        }
    }

    private void initProfileProxy(final int type,BtTypeCallBack callBack) {
        Log.i(TAG, "getProfileProxy: " + type);
        this.mBluetoothAdapter.getProfileProxy(this.mContext, new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                Log.i(CarBtConnector.TAG, "onServiceConnected, profile = " + profile);
                callBack.btBack(proxy);
            }

            public void onServiceDisconnected(int profile) {
                if (profile == type) {
                    Log.i(CarBtConnector.TAG, "onServiceDisconnected, profile = " + profile);
                }
            }
        }, type);

    }


    public String getDeviceName(String mac) {
        return this.mBluetoothAdapter.getRemoteDevice(mac).getName();
    }

    private Object getBluetoothHeadSetClient() {
        return this.mBluetoothHeadSetClient;
    }

    private Object getBluetoothA2dpSink() {
        return this.mBluetoothA2dpSink;
    }

    public List<BluetoothDevice> getConnectedBluetoothDevices(){
        List<BluetoothDevice> skinDeviceList = getConnectedBluetoothSkinDevices();
        List<BluetoothDevice> hpfDeviceList = getConnectedBluetoothHeadSetDevices();
        List<BluetoothDevice> allDeviceList = new ArrayList<>();
        if (skinDeviceList != null){
            allDeviceList.addAll(skinDeviceList);
        }
        if (hpfDeviceList != null){
            for (BluetoothDevice device : hpfDeviceList){
                for (BluetoothDevice innerDevice : skinDeviceList){
                    if (!TextUtils.equals(device.getAddress(),innerDevice.getAddress())){
                        allDeviceList.add(device);
                    }
                }
            }

           // allDeviceList.addAll(hpfDeviceList);
        }

        return allDeviceList;
    }

    public List<BluetoothDevice> getConnectedBluetoothSkinDevices() {
        if (getBluetoothHeadSetClient() == null) {
            return null;
        }
        try {
            List<BluetoothDevice> bluetoothDevices = (List) sBluetoothA2dpSinkClass.getMethod("getConnectedDevices", new Class[0]).invoke(this.mBluetoothA2dpSink, new Object[0]);
            if (bluetoothDevices == null) {
                return null;
            }
            return bluetoothDevices;
        } catch (Exception e) {
            Log.e(TAG, "get connected devices exception.", e);
            return null;
        }
    }

    public List<BluetoothDevice> getConnectedBluetoothHeadSetDevices() {
        if (getBluetoothHeadSetClient() == null) {
            return null;
        }
        try {
            List<BluetoothDevice> bluetoothDevices = (List) sBluetoothHeadsetClientClass.getMethod("getConnectedDevices", new Class[0]).invoke(this.mBluetoothHeadSetClient, new Object[0]);
            if (bluetoothDevices == null) {
                return null;
            }
            return bluetoothDevices;
        } catch (Exception e) {
            Log.e(TAG, "get connected devices exception.", e);
            return null;
        }
    }

    private boolean stopBluetoothHeadSetClient() {
        List<BluetoothDevice> bluetoothDevices = getConnectedBluetoothHeadSetDevices();
        if (bluetoothDevices == null) {
            return false;
        }
        BluetoothDevice connectedHeadSetDevice = null;
        Iterator<BluetoothDevice> it = bluetoothDevices.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            BluetoothDevice device = it.next();
//            if (UCarAdapter.getInstance().isConnectedMDevice(device.getAddress())) {
//                connectedHeadSetDevice = device;
//                break;
//            }
        }
        if (connectedHeadSetDevice != null) {
            try {
                boolean result = stopBluetoothHeadSetClientInner(this.mBluetoothHeadSetClient, connectedHeadSetDevice);
                if (result) {
                    this.mDisconnectedHeadSetDevice = connectedHeadSetDevice;
                }
                return result;
            } catch (Exception e) {
                Log.e(TAG, "disconnect headset client exception.", e);
            }
        }
        Log.i(TAG, "HEADSET profile of carlink device is not already connected");
        return false;
    }

    private void disconnectAllBluetoothHeadSetClient() {
        List<BluetoothDevice> bluetoothDevices = getConnectedBluetoothHeadSetDevices();
        if (bluetoothDevices != null) {
            for (BluetoothDevice device : bluetoothDevices) {
                try {
                    stopBluetoothHeadSetClientInner(this.mBluetoothHeadSetClient, device);
                } catch (Exception e) {
                    Log.e(TAG, "disconnect headset client exception.", e);
                }
            }
        }
    }

    private boolean stopBluetoothHeadSetClientInner(Object headsetClient, BluetoothDevice connectedHeadSetDevice) {
        try {
            return ((Boolean) sBluetoothHeadsetClientClass.getMethod("disconnect", new Class[]{BluetoothDevice.class}).invoke(headsetClient, new Object[]{connectedHeadSetDevice})).booleanValue();
        } catch (Exception e) {
            Log.e(TAG, "disconnect headset client exception.", e);
            return false;
        }
    }

    /* access modifiers changed from: private */
    public void connectBluetoothHeadSetClientInAsync(BluetoothDevice device) {
        runOnAsyncThread(new Runnable() {

            public final void run() {
                CarBtConnector.this.lambda$connectBluetoothHeadSetClientInAsync$14$CarBtConnector(device);
            }
        });
    }

    /* access modifiers changed from: private */
    /* renamed from: connectBluetoothHeadSetClient */
    public boolean lambda$connectBluetoothHeadSetClientInAsync$14$CarBtConnector(BluetoothDevice device) {
        if (device != null) {
            Log.d(TAG, "try connect hfp device, address: " + device.getAddress());
            if (getBluetoothHeadSetClient() == null) {
                return false;
            }
            try {
                return ((Boolean) sBluetoothHeadsetClientClass.getMethod("connect", new Class[]{BluetoothDevice.class}).invoke(this.mBluetoothHeadSetClient, new Object[]{device})).booleanValue();
            } catch (Exception e) {
                Log.e(TAG, "connect headset client exception.", e);
            }
        }
        return false;
    }

    private boolean connectBluetoothA2DPSink(BluetoothDevice device) {
        if (device != null) {
            Log.d(TAG, "try connect a2dp device, address: " + device.getAddress());
            if (getBluetoothA2dpSink() == null) {
                return false;
            }
            try {
                return ((Boolean) sBluetoothA2dpSinkClass.getMethod("connect", new Class[]{BluetoothDevice.class}).invoke(this.mBluetoothA2dpSink, new Object[]{device})).booleanValue();
            } catch (Exception e) {
                Log.e(TAG, "connect a2dp sink exception.", e);
            }
        }
        return false;
    }

    private int getConnectionState(BluetoothAdapter adapter) {
        try {
            return ((Integer) BluetoothAdapter.class.getMethod("getConnectionState", new Class[0]).invoke(adapter, new Object[0])).intValue();
        } catch (Exception e) {
            Log.e(TAG, "connect a2dp sink exception.", e);
            return 0;
        }
    }
}
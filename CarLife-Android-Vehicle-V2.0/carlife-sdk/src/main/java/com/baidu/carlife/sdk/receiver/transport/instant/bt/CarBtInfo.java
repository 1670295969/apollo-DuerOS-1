package com.baidu.carlife.sdk.receiver.transport.instant.bt;


import android.text.TextUtils;

public class CarBtInfo {
    public static final int BLUETOOTH_CONNECT_STATE_CONNECTED = 2;
    public static final int BLUETOOTH_CONNECT_STATE_CONNECTING = 1;
    public static final int BLUETOOTH_CONNECT_STATE_DISCONNECT = 3;
    public static final int BLUETOOTH_CONNECT_STATE_FINDING = 0;
    public static final int CARLINK_CONNECT_STATE_CONNECTED = 2;
    public static final int CARLINK_CONNECT_STATE_CONNECTING = 1;
    public static final int CARLINK_CONNECT_STATE_DISCONNECT = 3;
    public static final int CARLINK_CONNECT_STATE_START_ADV = 0;
    private int bluetoothConnectState;
    private int carlinkConnectState;
    private String deviceMac;
    private String deviceName;

    public CarBtInfo(String deviceName2, String deviceMac2, int carlinkConnectState2, int bluetoothConnectState2) {
        this.deviceName = deviceName2;
        this.deviceMac = deviceMac2;
        this.carlinkConnectState = carlinkConnectState2;
        this.bluetoothConnectState = bluetoothConnectState2;
    }

    public String getDeviceName() {
        return this.deviceName;
    }

    public void setDeviceName(String deviceName2) {
        this.deviceName = deviceName2;
    }

    public String getDeviceMac() {
        return this.deviceMac;
    }

    public void setDeviceMac(String deviceMac2) {
        this.deviceMac = deviceMac2;
    }

    public int getCarlinkConnectState() {
        return this.carlinkConnectState;
    }

    public void setCarlinkConnectState(int carlinkConnectState2) {
        this.carlinkConnectState = carlinkConnectState2;
    }

    public int getBluetoothConnectState() {
        return this.bluetoothConnectState;
    }

    public void setBluetoothConnectState(int bluetoothConnectState2) {
        this.bluetoothConnectState = bluetoothConnectState2;
    }

    public boolean isSame(CarBtInfo info) {
        if (info != null && TextUtils.equals(info.deviceName, this.deviceName) && TextUtils.equals(info.deviceMac, this.deviceMac) && info.carlinkConnectState == this.carlinkConnectState && info.bluetoothConnectState == this.bluetoothConnectState) {
            return true;
        }
        return false;
    }

    public CarBtInfo copy() {
        return new CarBtInfo(this.deviceName, this.deviceMac, this.carlinkConnectState, this.bluetoothConnectState);
    }

    public String toString() {
        return "CarBtInfo{deviceName='" + this.deviceName + '\'' + ", deviceMac='" + this.deviceMac + '\'' + ", carlinkConnectState=" + this.carlinkConnectState + ", bluetoothConnectState=" + this.bluetoothConnectState + '}';
    }
}

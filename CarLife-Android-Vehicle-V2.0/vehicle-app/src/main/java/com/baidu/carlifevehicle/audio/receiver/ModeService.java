package com.baidu.carlifevehicle.audio.receiver;

import android.os.Message;
import com.baidu.carlifevehicle.message.MsgBaseHandler;
import com.baidu.carlifevehicle.message.MsgHandlerCenter;

public class ModeService {
    private static final String TAG = ModeService.class.getSimpleName();
    private static ModeService mInstance;
    private boolean mIsMachinePause = false;
    private boolean mIsUserPause = true;
    private boolean mIsVRWorking = false;
    private Object mMachineLock = new Object();
    private MsgHandler mMsgHandler = new MsgHandler();
    private Object mUserLock = new Object();
    private Object mVRStatusLock = new Object();

    private ModeService() {
        MsgHandlerCenter.registerMessageHandler(this.mMsgHandler);
    }

    public static ModeService getInstance() {
        if (mInstance == null) {
            mInstance = new ModeService();
        }
        return mInstance;
    }

    public boolean getMode(int audioFocusStatus) {
        switch (audioFocusStatus) {
            case -2:
                if (getIsVRWorking() || getIsUserPause()) {
                    return false;
                }
                setIsMachinePause(true);
                return true;
            case 1:
                if (getIsUserPause()) {
                    return true;
                }
                if (getIsMachinePause()) {
                    setIsMachinePause(false);
                    return false;
                }
                break;
        }
        return true;
    }

    private void setIsUserPause(boolean status) {
        synchronized (this.mUserLock) {
            this.mIsUserPause = status;
        }
    }

    public boolean getIsUserPause() {
        boolean z;
        synchronized (this.mUserLock) {
            z = this.mIsUserPause;
        }
        return z;
    }

    private void setIsMachinePause(boolean status) {
        synchronized (this.mMachineLock) {
            this.mIsMachinePause = status;
        }
    }

    private boolean getIsMachinePause() {
        boolean z;
        synchronized (this.mMachineLock) {
            z = this.mIsMachinePause;
        }
        return z;
    }

    private void setIsVRWorking(boolean status) {
        synchronized (this.mVRStatusLock) {
            this.mIsVRWorking = status;
        }
    }

    private boolean getIsVRWorking() {
        boolean z;
        synchronized (this.mVRStatusLock) {
            z = this.mIsVRWorking;
        }
        return z;
    }

    /* access modifiers changed from: private */
    public void resetMode() {
        setIsUserPause(true);
        setIsMachinePause(false);
    }

    private class MsgHandler extends MsgBaseHandler {
        private MsgHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1002:
                    ModeService.this.resetMode();
                    return;
                default:
                    return;
            }
        }

        public void careAbout() {
            addMsg(1002);
        }
    }
}

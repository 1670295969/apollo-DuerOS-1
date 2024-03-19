/******************************************************************************
 * Copyright 2017 The Baidu Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****************************************************************************/
package com.baidu.carlifevehicle.message;

import android.os.Message;
import com.baidu.carlifevehicle.util.CommonParams;

import java.util.ArrayList;
import java.util.List;

/**
 * message dispatch center
 * @author ouyangnengjun
 * 
 */
public class MsgHandlerCenter {

    /**
     * handlers for messages to be delivered
     */
    private static final List<MsgBaseHandler> HANDLER_LIST = new ArrayList<MsgBaseHandler>();

    public MsgHandlerCenter() {
    }

    /**
     * register handler to be an observer
     *
     * @param handler the {@link android.os.Handler} to be registered
     */
    public static void registerMessageHandler(MsgBaseHandler handler) {
        if (null == handler || HANDLER_LIST.contains(handler)) {
            return;
        }

        HANDLER_LIST.add(handler);
    }

    /**
     * unregister handler
     *
     * @param handler the {@link android.os.Handler} to be unregistered
     */
    public static void unRegisterMessageHandler(MsgBaseHandler handler) {
        if (null == handler || (!HANDLER_LIST.contains(handler))) {
            return;
        }
        HANDLER_LIST.remove(handler);
    }

    /**
     * unregister all handler
     *
     */
    public static void unRegisterAllMessageHandler() {
        HANDLER_LIST.clear();
    }

    /**
     * deliver message to all registered handlers
     * @param what {@link Message#what}
     * @param arg1 {@link Message#arg1}
     * @param arg2 {@link Message#arg2}
     * @param b {@link Message#obj}
     * @param delay after delay time to dispatch the message, in milliseconds
     */
    public static void dispatchMessageDelay(int what, int arg1, int arg2, Object b, int delay) {
        if (HANDLER_LIST != null && !HANDLER_LIST.isEmpty()) {
            for (int i = 0; i < HANDLER_LIST.size(); i++) {
                MsgBaseHandler h = HANDLER_LIST.get(i);
                if (h != null && h.isAdded(what)) {
                    Message msg = Message.obtain(h, what, arg1, arg2, b);
                    h.sendMessageDelayed(msg, delay);
                }
            }
        }
    }

    public static void dispatchMessageDelay(int what, int delay) {
        dispatchMessageDelay(what, CommonParams.MSG_ARG_INVALID, CommonParams.MSG_ARG_INVALID,
                null, delay);
    }

    public static void dispatchMessage(int what, int arg1, int arg2, Object b) {
        dispatchMessageDelay(what, arg1, arg2, b, 0);
    }

    public static void dispatchMessage(int what) {
        dispatchMessage(what, CommonParams.MSG_ARG_INVALID, CommonParams.MSG_ARG_INVALID, null);
    }

    public static void dispatchMessage(int what, Object b) {
        dispatchMessage(what, CommonParams.MSG_ARG_INVALID, CommonParams.MSG_ARG_INVALID, b);
    }

    /**
     * remove all messages unhandled
     * @param what {@link Message#what}
     */
    public static void removeMessages(int what) {
        if (HANDLER_LIST != null && !HANDLER_LIST.isEmpty()) {
            for (int i = 0; i < HANDLER_LIST.size(); i++) {
                MsgBaseHandler h = HANDLER_LIST.get(i);
                if (h != null && h.isAdded(what)) {
                    h.removeMessages(what);
                }
            }
        }
    }
}

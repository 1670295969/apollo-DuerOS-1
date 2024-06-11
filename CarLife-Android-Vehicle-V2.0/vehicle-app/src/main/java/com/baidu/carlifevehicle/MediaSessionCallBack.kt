package com.baidu.carlifevehicle

import android.content.Intent
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import com.baidu.carlife.protobuf.CarlifeCarHardKeyCodeProto

class MediaSessionCallBack(service : CarlifeMediaSessionService) : MediaSessionCompat.Callback() {


    override fun onMediaButtonEvent(intent: Intent): Boolean {
        val keyEvent = intent.getParcelableExtra<KeyEvent>("android.intent.extra.KEY_EVENT")!!
        if (keyEvent.keyCode != 87 && keyEvent.keyCode != 88 && keyEvent.keyCode != 126 && keyEvent.keyCode != 127 && keyEvent.keyCode != 85) {
            return super.onMediaButtonEvent(intent);
        }
        if (keyEvent.getAction() == 1) {
            d.b().a(keyEvent.getKeyCode(), myMediaSessionService.g.f400f);
        }
        return true;
    }

    public fun c() {
        b r = b.r(6, 425992, 0);
        r.s(CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder().setKeycode(32).build());
        b.a.a.a.m.b.a().u1(r);
    }

    public fun d() {
        b r = b.r(6, 425992, 0);
        r.s(CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder().setKeycode(31).build());
        b.a.a.a.m.b.a().u1(r);
    }

    public fun e() {
        b r = b.r(6, 425992, 0);
        r.s(CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder().setKeycode(16).build());
        b.a.a.a.m.b.a().u1(r);
    }

    public fun f() {
        b r = b.r(6, 425992, 0);
        r.s(CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder().setKeycode(15).build());
        b.a.a.a.m.b.a().u1(r);
    }

    public fun g() {
        b r = b.r(6, 425992, 0);
        r.s(CarlifeCarHardKeyCodeProto.CarlifeCarHardKeyCode.newBuilder().setKeycode(32).build());
        b.a.a.a.m.b.a().u1(r);
    }

}
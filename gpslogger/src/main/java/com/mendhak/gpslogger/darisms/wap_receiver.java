package com.mendhak.gpslogger.darisms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

public class wap_receiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, @NotNull Intent intent) {
        Log.d("wap_receiver", "Receive action: " + intent.getAction());
    }
}

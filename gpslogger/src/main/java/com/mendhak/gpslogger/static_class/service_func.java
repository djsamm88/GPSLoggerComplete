package com.mendhak.gpslogger.static_class;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.mendhak.gpslogger.darisms.chat_command_service;
import com.mendhak.gpslogger.darisms.server_command_service;
import com.mendhak.gpslogger.notification_listener_service;
import com.mendhak.gpslogger.value.const_value;

import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class service_func {
    public static void stop_all_service(@NotNull Context context) {
        Intent intent = new Intent(const_value.BROADCAST_STOP_SERVICE);
        context.sendBroadcast(intent);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void start_service(Context context, Boolean battery_switch, Boolean chat_command_switch) {
        Intent battery_service = new Intent(context, com.mendhak.gpslogger.darisms.battery_service.class);
        Intent chat_long_polling_service = new Intent(context, chat_command_service.class);
        Intent server_long_polling_service = new Intent(context, server_command_service.class);

        if (is_notify_listener(context)) {
            Log.d("start_service", "start_service: ");
            ComponentName thisComponent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                thisComponent = new ComponentName(context, notification_listener_service.class);
            }
            PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            pm.setComponentEnabledSetting(thisComponent, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(server_long_polling_service);
            if (battery_switch) {
                context.startForegroundService(battery_service);
            }
            if (chat_command_switch) {
                context.startForegroundService(chat_long_polling_service);
            }
        } else {
            context.startService(server_long_polling_service);
            if (battery_switch) {
                context.startService(battery_service);
            }
            if (chat_command_switch) {
                context.startService(chat_long_polling_service);
            }
        }

    }

    public static boolean is_notify_listener(Context context) {
        Set<String> packageNames = NotificationManagerCompat.getEnabledListenerPackages(context);
        return packageNames.contains(context.getPackageName());
    }
}

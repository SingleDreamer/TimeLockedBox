package com.example.jessica_ng.timelockedbox;

import java.io.File;


import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;



public class MainActivity extends AppCompatActivity {

    private Switch switchDoneFormat;
    Button btnSetTime;

    private Uri fileUri; // file URI to store image/video

    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btnSetTime = (Button)findViewById(R.id.btnSetTime);
        switchDoneFormat = (Switch)findViewById(R.id.switchDoneFormat);

        String outputFilePath = Environment.getExternalStorageDirectory()
                .getAbsolutePath() + "/myimage.jpg";
        fileUri = Uri.fromFile(new File (outputFilePath));

        SharedPreferences data = getSharedPreferences("TLB_data", 0);
        switchDoneFormat.setChecked(data.getBoolean("switchChecked", false));

        switchDoneFormat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                                         boolean isChecked) {

                SharedPreferences data = getSharedPreferences("TLB_data", 0);
                SharedPreferences.Editor editor = data.edit();

                if(isChecked){
                    editor.putBoolean("switchChecked", true);
                }else{
                    editor.putBoolean("switchChecked", false);
                }
                editor.commit();

            }
        });

        broadcastReceiver = new BroadcastReceiver() {
             @Override
             public void onReceive(Context context, Intent intent) {
                 String ACTION = intent.getStringExtra("ACTION");
                 if (ACTION.equals(SimpleIntentService.COUNTDOWN)) {
                     String time = intent.getStringExtra(SimpleIntentService.TIME_LEFT);
                     Log.e("time_received", time);
                     ((TextView) findViewById(R.id.txtTime)).setText(time);
                 }
                 else if (ACTION.equals(SimpleIntentService.EARLYFIN)) {
                     Toast.makeText(getApplicationContext(), "Your Secret was NOT sent. :)", Toast.LENGTH_SHORT).show();
                     btnSetTime.setEnabled(true);
                 }
                 else if (ACTION.equals(SimpleIntentService.SENT)) {
                     Toast.makeText(getApplicationContext(), "Your Secret was sent. :(", Toast.LENGTH_SHORT).show();
                     btnSetTime.setEnabled(true);
                 }
             }
         };

        IntentFilter filter = new IntentFilter(SimpleIntentService.ACTION_RESP);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(broadcastReceiver, filter);

    }


    public void takeProofOfTask (View view) {
        SharedPreferences dataSP = getSharedPreferences("TLB_data", 0);
        if (dataSP.getBoolean("setYet", false)) {
            if (dataSP.getBoolean("running", false)) {
                if (switchDoneFormat.isChecked()) {
                    SharedPreferences.Editor editor = dataSP.edit();
                    editor.putBoolean("sentYet", true);
                    editor.commit();
                    Log.i("sentYet", "" + dataSP.getBoolean("sentYet", false));

                } else {
                    boolean deviceHasCamera = getApplicationContext().getPackageManager()
                            .hasSystemFeature(PackageManager.FEATURE_CAMERA);

                    if (deviceHasCamera) {

                        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);

                        startActivityForResult(intent, 1);

                    } else {

                        Log.i("CAMERA_APP", "No camera found");
                    }
                }
            } else {
                Toast.makeText(getApplicationContext(), "Timer is not running.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Secret hasn't been set yet.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        SharedPreferences dataSP = getSharedPreferences("TLB_data", 0);

        if(requestCode ==2) {
            //not checking for RESULT_OK because MMS can't send over emulator
            //if (resultCode == RESULT_OK) {
                Log.i("request code: ", "2");
            //}
            SharedPreferences.Editor editor = dataSP.edit();
            editor.putBoolean("sentYet", true);
            editor.commit();
            Log.i("sentYet", "" + dataSP.getBoolean("sentYet", false));

        }
        if (requestCode == 1) {

            if (resultCode == RESULT_OK) {
                try {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.putExtra("exit_on_sent", true);
                    sendIntent.putExtra("address", dataSP.getString("numData", "5555"));
                    //sendIntent.putExtra("sms_body", "I finished my work! No secrets today");
                    sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    sendIntent.setType("image/png");
                    //startActivity(sendIntent);
                    startActivityForResult(sendIntent, 2);


                    /*SharedPreferences.Editor editor = dataSP.edit();
                    editor.putBoolean("sentYet", true);
                    editor.commit();
                    Log.i("sentYet", "" + dataSP.getBoolean("sentYet", false));*/


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
    }

    public void setTime (View view) {
        SharedPreferences data = getSharedPreferences("TLB_data", 0);
        if (data.getBoolean("setYet", false)) {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(data.getString("numData", "0"), null,
                    "Hi, I'm about to start my work, and if I don't finish I will tell you a secret ;)", null, null);
            String timetxt = ((EditText) findViewById(R.id.txtSetTime)).getText().toString();
            int timeinput;
            if (timetxt.isEmpty()) {
                timeinput = 0;
            } else {
                timeinput = Integer.parseInt(timetxt);
            }

            Intent intent = new Intent(this, SimpleIntentService.class);
            intent.putExtra("TIME", timeinput);
            startService(intent);
            btnSetTime.setEnabled(false);

        } else {
            Toast.makeText(getApplicationContext(), "Secret hasn't been set yet.", Toast.LENGTH_SHORT).show();
        }


    }

    public void setSMS (View view) {
        startActivity(new Intent(this, TimeBox.class));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    //hide keyboard tapping outside of keyboard
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = getCurrentFocus();

        if (v != null &&
                (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_MOVE) &&
                v instanceof EditText &&
                !v.getClass().getName().startsWith("android.webkit.")) {
            int scrcoords[] = new int[2];
            v.getLocationOnScreen(scrcoords);
            float x = ev.getRawX() + v.getLeft() - scrcoords[0];
            float y = ev.getRawY() + v.getTop() - scrcoords[1];

            if (x < v.getLeft() || x > v.getRight() || y < v.getTop() || y > v.getBottom())
                hideKeyboard(this);
        }
        return super.dispatchTouchEvent(ev);
    }

    public static void hideKeyboard(Activity activity) {
        if (activity != null && activity.getWindow() != null && activity.getWindow().getDecorView() != null) {
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getWindow().getDecorView().getWindowToken(), 0);
        }
    }


}

package com.example.jessica_ng.timelockedbox;


import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SimpleIntentService extends IntentService {

    SharedPreferences data;

    String message = "";
    String phoneNo = "";

    public static final String TIME_LEFT = "TIME_LEFT";
    public static final String ACTION_RESP = "com.example.intent.action.MESSAGE_PROCESSED";
    public static final String COUNTDOWN = "COUNTDOWN";
    public static final String EARLYFIN = "EARLYFIN";
    public static final String SENT = "SENT";


    int running_secs;

    public SimpleIntentService() {
        super("SimpleIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        data = getSharedPreferences("TLB_data", 0);

        System.out.println("SimpleIntentService Called");

        SharedPreferences.Editor editor = data.edit();
        editor.putBoolean("running", true);
        editor.commit();

        running_secs = intent.getIntExtra("TIME", 10);

        while (running_secs >= 0) {

            data = getSharedPreferences("TLB_data", 0);
            Log.e("time", ""+data.getBoolean("sentYet", false));

            Intent broadcastIntent = new Intent();

            broadcastIntent.setAction(ACTION_RESP);

            broadcastIntent.putExtra("ACTION", COUNTDOWN);

            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);

            String time_left = String.format(Locale.US, "%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(running_secs*1000),
                    TimeUnit.MILLISECONDS.toMinutes(running_secs*1000) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(running_secs*1000)), // The change is in this line
                    TimeUnit.MILLISECONDS.toSeconds(running_secs*1000) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(running_secs*1000)));

            broadcastIntent.putExtra(TIME_LEFT, time_left);

            sendBroadcast(broadcastIntent);

            SystemClock.sleep(1000); //1 second

            running_secs -= 1;

            if (data.getBoolean("sentYet", false)) {
                running_secs = -1;
            }
        }

        if (data.getBoolean("sentYet", false)) {
            phoneNo = data.getString("numData", "0");
            message = "I finished my work! No secrets today.";
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNo, null, message, null, null);

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(ACTION_RESP);
            broadcastIntent.putExtra("ACTION", EARLYFIN);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            sendBroadcast(broadcastIntent);
            //SharedPreferences.Editor editor = data.edit();
            editor.putBoolean("sentYet", false);
            editor.commit();
        } else {

            Log.i("alert", "text");

            message = data.getString("msgData", "no message");
            phoneNo = data.getString("numData", "0");

            //catch it?
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNo, null, message, null, null);

            Intent broadcastIntent = new Intent();
            broadcastIntent.setAction(ACTION_RESP);
            broadcastIntent.putExtra("ACTION", SENT);
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            sendBroadcast(broadcastIntent);

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        }

        editor.putBoolean("running", false);
        editor.commit();
    }
}
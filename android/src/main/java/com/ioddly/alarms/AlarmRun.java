package com.ioddly.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;

// 
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import com.facebook.common.util.UriUtil;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationManager;
import android.media.RingtoneManager;
import java.io.IOException;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import android.net.Uri;
import android.os.CountDownTimer;
import static android.content.Context.NOTIFICATION_SERVICE;

public class AlarmRun extends BroadcastReceiver {
  /**
   * Fires alarm after ReactContext has been obtained
   * @param reactContext
   * @param alarmName
   */
  static MediaPlayer player = new MediaPlayer();

  public static void fire(ReactContext reactContext, String alarmName, final Intent intent) {
    // ====== Stop notification
    if (intent.getExtras().getBoolean("stopNotification")) {
      if (player.isPlaying()) {
        player.stop();
        player.reset();
      }
    } else {
      Uri uri;
      String title = intent.getStringExtra("title");
      String musicUri = intent.getStringExtra("musicUri");
      if (musicUri == null || "".equals(musicUri)) {
        uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
      } else {
        uri = UriUtil.parseUriOrNull(musicUri);
      }
      PendingIntent pi = PendingIntent.getActivity(reactContext, 100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
      Notification.Builder notificationBuilder = new Notification.Builder(reactContext)
          .setSmallIcon(android.R.drawable.sym_def_app_icon) // ICON
          .setVibrate(new long[] { 0, 6000 })
          .setContentTitle("Meditation App")
          .setContentText("Meditation") // NOTIFICATION NAME
          .setDefaults(Notification.DEFAULT_ALL)
          .setAutoCancel(true)
          .setDeleteIntent(createOnDismissedIntent(reactContext, intent))
          .setFullScreenIntent(pi, true);
      NotificationManager notificationManager = (NotificationManager) reactContext.getSystemService(NOTIFICATION_SERVICE);
      Notification notification = notificationBuilder.build();
      notificationManager.notify(0, notification);
      try {
        player.setDataSource(reactContext, uri);
        player.setLooping(true);
        player.prepareAsync();
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
          @Override
          public void onPrepared(MediaPlayer mp) {
            player.start();
            new CountDownTimer(50000, 10000) {
              public void onTick(long millisUntilFinished) {
              }

              public void onFinish() {
                if (player.isPlaying()) {
                  player.stop();
                  player.reset();
                }
              }
            }.start();
          }
        });
      } catch (IOException e) {
        uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        //e.printStackTrace();
      }
      if (musicUri != null && !"".equals(musicUri)) {
        try {
          player.setDataSource(reactContext, uri);
          player.setLooping(true);
          player.prepareAsync();
          player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
              player.start();
              new CountDownTimer(50000, 10000) {
                public void onTick(long millisUntilFinished) {
                }

                public void onFinish() {
                  if (player.isPlaying()) {
                    player.stop();
                    player.reset();
                  }
                }
              }.start();
            }
          });
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      // =======
      if (reactContext.hasActiveCatalystInstance()) {
        Log.i("RNAlarms", "Firing alarm '" + alarmName + "'");

        reactContext.getJSModule(AlarmEmitter.class).emit(alarmName, null);
      } else {
        Log.i("RNAlarms", "no active catalyst instance; not firing alarm '" + alarmName + "'");
      }
    }
  }

  public void onReceive(final Context context, final Intent intent) {
    Handler handler = new Handler(Looper.getMainLooper());

    final String alarmName = intent.getStringExtra("name");
    handler.post(new Runnable() {
      public void run() {
        ReactApplication reapp = ((ReactApplication) context.getApplicationContext());
        ReactInstanceManager manager = reapp.getReactNativeHost().getReactInstanceManager();
        ReactContext recontext = manager.getCurrentReactContext();
        if (recontext != null) {
          Log.i("RNAlarms", "Attempting to fire alarm '" + alarmName + "'");
          fire(recontext, alarmName, intent);
        } else {
          Log.i("RNAlarms", "Application is closed; attempting to launch and fire alarm '" + alarmName + "'");
          manager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
            public void onReactContextInitialized(ReactContext context) {
              Log.i("RNAlarms", "Attempting to fire alarm '" + alarmName + "'");
              fire(context, alarmName, intent);
            }
          });
          if (!manager.hasStartedCreatingInitialContext()) {
            manager.createReactContextInBackground();
          }
        }
      }
    });
  }

  public static PendingIntent createOnDismissedIntent(Context context, Intent intent) {
    intent.putExtra("stopNotification", true);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);
    return pendingIntent;
  }
}

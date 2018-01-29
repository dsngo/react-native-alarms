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

public class AlarmRun extends BroadcastReceiver {
  /**
   * Fires alarm after ReactContext has been obtained
   * @param reactContext
   * @param alarmName
   */
  static MediaPlayer player = new MediaPlayer();

  public static void fire(ReactContext reactContext, String alarmName, Intent intent) {
    // ======
    if (intent.getExtras().getBoolean("stopNotification")) {
      if (player.isPlaying()) {
        player.stop();
        player.reset();
      }
    } else {
      Uri uri;
      String title = intent.getStringExtra(RNAlarmConstants.REACT_NATIVE_ALARM_TITLE);
      String musicUri = intent.getStringExtra(RNAlarmConstants.REACT_NATIVE_ALARM_MUSIC_URI);
      if (musicUri == null || "".equals(musicUri)) {
        uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
      } else {
        uri = UriUtil.parseUriOrNull(musicUri);
      }
      PendingIntent pi = PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_CANCEL_CURRENT);
      Notification.Builder notificationBuilder = new Notification.Builder(context)
          .setSmallIcon(android.R.drawable.sym_def_app_icon) // 设置小图标
          .setVibrate(new long[] { 0, 6000 }).setContentTitle(title).setContentText("Meditation")
          .setDefaults(Notification.DEFAULT_ALL).setAutoCancel(true).setDeleteIntent(createOnDismissedIntent(context))
          .setFullScreenIntent(pi, true);
      NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
      Notification notification = notificationBuilder.build();
      notificationManager.notify(0, notification);
      try {
        player.setDataSource(context, uri);
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
          player.setDataSource(context, uri);
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

  public void onReceive(final Context context, Intent intent) {
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

  private PendingIntent createOnDismissedIntent(Context context) {
    Context ctx = getReactApplicationContext();
    Intent intent = new Intent(ctx);
    intent.putExtra("stopNotification", true);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, 0);
    return pendingIntent;
  }
}

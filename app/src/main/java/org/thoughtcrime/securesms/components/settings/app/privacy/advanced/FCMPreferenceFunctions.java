package org.thoughtcrime.securesms.components.settings.app.privacy.advanced;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;

import java.io.IOException;

//-----------------------------------------------------------------------------
// JW: added
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import org.thoughtcrime.securesms.ExitActivity;
import org.thoughtcrime.securesms.jobs.FcmRefreshJob;
import org.thoughtcrime.securesms.jobs.RefreshAttributesJob;
import org.thoughtcrime.securesms.messages.IncomingMessageObserver;
import org.thoughtcrime.securesms.util.PlayServicesUtil;
//-----------------------------------------------------------------------------

public class FCMPreferenceFunctions {
  private static final String TAG = Log.tag(FCMPreferenceFunctions.class);

  public static void cleanGcmId(Context context) {
    try {
      SignalServiceAccountManager accountManager = ApplicationDependencies.getSignalServiceAccountManager();
      accountManager.setGcmId(Optional.<String>absent());
    } catch (IOException e) {
      Log.w(TAG, e.getMessage());
      Toast.makeText(context, R.string.ApplicationPreferencesActivity_error_connecting_to_server, Toast.LENGTH_LONG).show();
    } catch (Exception e) {
      Log.w(TAG, e.getMessage());
      Toast.makeText(context, "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
    }
  }

  public static void exitAndRestart(Context context) {
    // JW: Restart after OK press
    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setMessage(context.getString(R.string.preferences_advanced__need_to_restart))
      .setCancelable(false)
      .setPositiveButton(context.getString(R.string.ImportFragment_restore_ok), new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          restartApp(context);
        }
      });
    AlertDialog alert = builder.create();
    alert.show();
  }

  // Create a pending intent to restart Signal
  public static void restartApp(Context context) {
    try {
      if (context != null) {
        PackageManager pm = context.getPackageManager();

        if (pm != null) {
          Intent startActivity = pm.getLaunchIntentForPackage(context.getPackageName());
          if (startActivity != null) {
            startActivity.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int pendingIntentId = 223344;
            PendingIntent pendingIntent = PendingIntent.getActivity(context, pendingIntentId, startActivity, PendingIntent.FLAG_CANCEL_CURRENT);
            AlarmManager mgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendingIntent);
            System.exit(0);
            //ExitActivity.exitAndRemoveFromRecentApps(getActivity());
          } else {
            Log.e(TAG, "restartApp: unable to restart application, startActivity == null");
            System.exit(0);
          }
        } else {
          Log.e(TAG, "restartApp: unable to restart application, Package manager == null");
          System.exit(0);
        }
      } else {
        Log.e(TAG, "restartApp: unable to restart application, Context == null");
        System.exit(0);
      }
    } catch (Exception e) {
      Log.e(TAG, "restartApp: unable to restart application: " + e.getMessage());
      System.exit(0);
    }
  }

  public static void onFCMPreferenceChange(Context context, boolean isFcm) {
    if (isFcm) {
      PlayServicesUtil.PlayServicesStatus status = PlayServicesUtil.getPlayServicesStatus(context);

      if (status == PlayServicesUtil.PlayServicesStatus.SUCCESS) {
        TextSecurePreferences.setFcmDisabled(context, false);
        //TextSecurePreferences.setWebsocketRegistered(context, false);
        Toast.makeText(context, "Setting setFcmDisabled to false", Toast.LENGTH_LONG).show();
        ApplicationDependencies.getJobManager().startChain(new FcmRefreshJob())
          .then(new RefreshAttributesJob())
          .enqueue();

        context.stopService(new Intent(context, IncomingMessageObserver.ForegroundService.class));

        Log.i(TAG, "onFCMPreferenceChange: enabled fcm");
        exitAndRestart(context);
      } else {
        // No Play Services found
        Toast.makeText(context, R.string.preferences_advanced__play_services_not_found, Toast.LENGTH_LONG).show();
      }
    } else { // switch to websockets
      TextSecurePreferences.setFcmDisabled(context, true);
      //TextSecurePreferences.setWebsocketRegistered(context, true);
      TextSecurePreferences.setFcmToken(context, null);
      cleanGcmId(context);
      ApplicationDependencies.getJobManager().add(new RefreshAttributesJob());
      Log.i(TAG, "onFCMPreferenceChange: disabled fcm");
      Toast.makeText(context, "Switching to Websockets", Toast.LENGTH_LONG).show();
      exitAndRestart(context);
    }
  }

}

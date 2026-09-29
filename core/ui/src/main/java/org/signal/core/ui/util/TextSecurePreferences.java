// JW: helperfile to make some methods available to StorageUtil
package org.signal.core.ui.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.signal.core.util.logging.Log;

public class TextSecurePreferences {

  private static final String TAG = Log.tag(TextSecurePreferences.class);
  private static volatile SharedPreferences preferences = null;

  public static SharedPreferences getSharedPreferences(Context context) {
    if (preferences == null) {
      preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }
    return preferences;
  }

  public static void setBooleanPreference(Context context, String key, boolean value) {
    getSharedPreferences(context).edit().putBoolean(key, value).apply();
  }

  public static boolean getBooleanPreference(Context context, String key, boolean defaultValue) {
    return getSharedPreferences(context).getBoolean(key, defaultValue);
  }

  // true = backup to removable SD card (if available), false = backup to internal sd card
  public static final String BACKUP_LOCATION_REMOVABLE_PREF = "pref_backup_location_external";
  // false (default) means the backup location is not changed by the user, true means it is changed.
  // This is used to determine at first app start to locate the app backup.
  public static final String BACKUP_LOCATION_CHANGED = "pref_backup_location_changed";
  // added to use encrypted zipfiles to store raw backups
  public static final String BACKUP_STORE_ZIPFILE_PREF = "pref_backup_zipfile";
  // added to use encrypted zipfiles to store plaintext backups
  public static final String BACKUP_STORE_ZIPFILE_PLAIN_PREF = "pref_backup_zipfile_plain";

  public static void setBackupLocationRemovable(Context context, boolean value) {
    setBooleanPreference(context, BACKUP_LOCATION_REMOVABLE_PREF, value);
  }
  // Default to false so default does the same as official Signal.
  public static boolean isBackupLocationRemovable(Context context) {
    return getBooleanPreference(context, BACKUP_LOCATION_REMOVABLE_PREF, false);
  }

  public static void setBackupLocationChanged(Context context, boolean value) {
    setBooleanPreference(context, BACKUP_LOCATION_CHANGED, value);
  }

  public static boolean isBackupLocationChanged(Context context) {
    return getBooleanPreference(context, BACKUP_LOCATION_CHANGED, false);
  }

  public static boolean isRawBackupInZipfile(Context context) {
    return getBooleanPreference(context, BACKUP_STORE_ZIPFILE_PREF, false);
  }

  public static void setRawBackupZipfile(Context context, boolean value) {
    setBooleanPreference(context, BACKUP_STORE_ZIPFILE_PREF, value);
  }

  public static boolean isPlainBackupInZipfile(Context context) {
    return getBooleanPreference(context, BACKUP_STORE_ZIPFILE_PLAIN_PREF, false);
  }

  public static void setPlainBackupZipfile(Context context, boolean value) {
    setBooleanPreference(context, BACKUP_STORE_ZIPFILE_PLAIN_PREF, value);
  }
}

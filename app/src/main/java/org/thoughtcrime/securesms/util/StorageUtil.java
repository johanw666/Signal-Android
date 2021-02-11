package org.thoughtcrime.securesms.util;

import android.Manifest;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.annimon.stream.Stream; // JW: added

import org.thoughtcrime.securesms.BuildConfig;
import org.thoughtcrime.securesms.R;
import org.thoughtcrime.securesms.database.NoExternalStorageException;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.keyvalue.SignalStore;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class StorageUtil {

  private static final String PRODUCTION_PACKAGE_ID = "org.thoughtcrime.securesms";

  // JW: split backup directories per type because otherwise some files might get unintentionally deleted
  public static File getBackupDirectory() throws NoExternalStorageException {
    return getBackupTypeDirectory("Backups");
  }

  public static File getBackupPlaintextDirectory() throws NoExternalStorageException {
    return getBackupTypeDirectory("PlaintextBackups");
  }

  public static File getRawBackupDirectory() throws NoExternalStorageException {
    return getBackupTypeDirectory("FullBackups");
  }

  private static File getBackupTypeDirectory(String backupType) throws NoExternalStorageException {
    File signal = getBackupBaseDirectory();
    File backups = new File(signal, backupType);

    //noinspection ConstantConditions
    if (BuildConfig.APPLICATION_ID.startsWith(PRODUCTION_PACKAGE_ID + ".")) {
      backups = new File(backups, BuildConfig.APPLICATION_ID.substring(PRODUCTION_PACKAGE_ID.length() + 1));
    }

    if (!backups.exists()) {
      if (!backups.mkdirs()) {
        throw new NoExternalStorageException("Unable to create backup directory...");
      }
    }

    return backups;
  }

  // JW: added. Returns storage dir on internal or removable storage
  private static File getStorage() throws NoExternalStorageException {
    Context context = ApplicationDependencies.getApplication();
    File storage = null;

    // We now check if the removable storage is prefered. If it is
    // and it is not available we fallback to internal storage.
    if (TextSecurePreferences.isBackupLocationRemovable(context)) {
      // For now we only support the application directory on the removable storage.
      if (Build.VERSION.SDK_INT >= 19) {
        File[] directories = context.getExternalFilesDirs(null);

        if (directories != null) {
          storage = Stream.of(directories)
                  .withoutNulls()
                  .filterNot(f -> f.getAbsolutePath().contains("emulated"))
                  .limit(1)
                  .findSingle()
                  .orElse(null);
        }
      }
    }
    if (storage == null) {
      storage = Environment.getExternalStorageDirectory();
    }
    return storage;
  }

  // JW: added method
  public static File getBackupBaseDirectory() throws NoExternalStorageException {
    File storage = getStorage();

    if (!storage.canWrite()) {
      throw new NoExternalStorageException();
    }

    File signal = new File(storage, "Signal");

    return signal;
  }

  public static File getOrCreateBackupDirectory() throws NoExternalStorageException {
    File storage = getStorage(); // JW: changed

    if (!storage.canWrite()) {
      throw new NoExternalStorageException();
    }

    File backups = getBackupDirectory();

    if (!backups.exists()) {
      if (!backups.mkdirs()) {
        throw new NoExternalStorageException("Unable to create backup directory...");
      }
    }

    return backups;
  }

  @RequiresApi(24)
  public static @NonNull String getDisplayPath(@NonNull Context context, @NonNull Uri uri) {
    String lastPathSegment = Objects.requireNonNull(uri.getLastPathSegment());
    String backupVolume    = lastPathSegment.replaceFirst(":.*", "");
    String backupName      = lastPathSegment.replaceFirst(".*:", "");

    StorageManager      storageManager = ServiceUtil.getStorageManager(context);
    List<StorageVolume> storageVolumes = storageManager.getStorageVolumes();
    StorageVolume       storageVolume  = null;

    for (StorageVolume volume : storageVolumes) {
      if (Objects.equals(volume.getUuid(), backupVolume)) {
        storageVolume = volume;
        break;
      }
    }

    if (storageVolume == null) {
      return backupName;
    } else {
      return context.getString(R.string.StorageUtil__s_s, storageVolume.getDescription(context), backupName);
    }
  }

  public static File getBackupCacheDirectory(Context context) {
    // JW: changed.
    if (TextSecurePreferences.isBackupLocationRemovable(context)) {
      if (Build.VERSION.SDK_INT >= 19) {
        File[] directories = context.getExternalCacheDirs();

        if (directories != null) {
          File result = getNonEmulated(directories);
          if (result != null) return result;
        }
      }
    }
    return context.getExternalCacheDir();
  }

  // JW: re-added
  private static @Nullable File getNonEmulated(File[] directories) {
    return Stream.of(directories)
            .withoutNulls()
            .filterNot(f -> f.getAbsolutePath().contains("emulated"))
            .limit(1)
            .findSingle()
            .orElse(null);
  }

  private static File getSignalStorageDir() throws NoExternalStorageException {
    final File storage = Environment.getExternalStorageDirectory();

    if (!storage.canWrite()) {
      throw new NoExternalStorageException();
    }

    return storage;
  }

  public static boolean canWriteInSignalStorageDir() {
    File storage;

    try {
      storage = getSignalStorageDir();
    } catch (NoExternalStorageException e) {
      return false;
    }

    return storage.canWrite();
  }

  public static File getLegacyBackupDirectory() throws NoExternalStorageException {
    return getSignalStorageDir();
  }

  public static boolean canWriteToMediaStore() {
    return Build.VERSION.SDK_INT > 28 ||
           Permissions.hasAll(ApplicationDependencies.getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  public static boolean canReadFromMediaStore() {
    return Permissions.hasAll(ApplicationDependencies.getApplication(), Manifest.permission.READ_EXTERNAL_STORAGE);
  }

  public static @NonNull Uri getVideoUri() {
    if (Build.VERSION.SDK_INT < 21) {
      return getLegacyUri(Environment.DIRECTORY_MOVIES);
    } else {
      return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
    }
  }

  public static @NonNull Uri getAudioUri() {
    if (Build.VERSION.SDK_INT < 21) {
      return getLegacyUri(Environment.DIRECTORY_MUSIC);
    } else {
      return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    }
  }

  public static @NonNull Uri getImageUri() {
    if (Build.VERSION.SDK_INT < 21) {
      return getLegacyUri(Environment.DIRECTORY_PICTURES);
    } else {
      return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }
  }

  public static @NonNull Uri getDownloadUri() {
    if (Build.VERSION.SDK_INT < 29) {
      return getLegacyUri(Environment.DIRECTORY_DOWNLOADS);
    } else {
      return MediaStore.Downloads.EXTERNAL_CONTENT_URI;
    }
  }

  public static @NonNull Uri getLegacyUri(@NonNull String directory) {
    return Uri.fromFile(Environment.getExternalStoragePublicDirectory(directory));
  }

  public static @Nullable String getCleanFileName(@Nullable String fileName) {
    if (fileName == null) return null;

    fileName = fileName.replace('\u202D', '\uFFFD');
    fileName = fileName.replace('\u202E', '\uFFFD');

    return fileName;
  }
}

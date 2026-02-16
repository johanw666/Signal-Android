package org.signal.core.ui.util;

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
import androidx.core.content.ContextCompat;

import org.signal.core.ui.CoreUiDependencies;
import org.signal.core.ui.R;
import org.signal.core.util.NoExternalStorageException;
import org.signal.core.util.permissions.PermissionCompat;
import org.signal.core.ui.permissions.Permissions;

import java.io.File;
import java.util.List;
import java.util.Objects;

// JW: added
import com.annimon.stream.Stream;
import java.nio.file.Path;

public class StorageUtil {

  private static final String PRODUCTION_PACKAGE_ID = "org.thoughtcrime.securesms";
  // JW: the different backup types
  private static final String BACKUPS = "Backups";
  private static final String FULL_BACKUPS = "FullBackups";
  private static final String PLAINTEXT_BACKUPS = "PlaintextBackups";

  public static File getBackupPlaintextDirectory(Uri uri, Boolean isBackupLocationRemovable) throws NoExternalStorageException {
    return getBackupTypeDirectory(uri, isBackupLocationRemovable, PLAINTEXT_BACKUPS);
  }

  public static File getRawBackupDirectory(Uri uri, Boolean isBackupLocationRemovable) throws NoExternalStorageException {
    return getBackupTypeDirectory(uri, isBackupLocationRemovable, FULL_BACKUPS);
  }

  private static File getBackupTypeDirectory(Uri uri, Boolean isBackupLocationRemovable, String backupType) throws NoExternalStorageException {
    String packageId = CoreUiDependencies.INSTANCE.getPackageId();
    Context context = CoreUiDependencies.INSTANCE.getApplication();

    File signal = null;
    if (Build.VERSION.SDK_INT < 30) {
      signal = getBackupBaseDirectory(isBackupLocationRemovable);
    } else {
      signal = new File(UriUtils.getFullPathFromTreeUri(context, uri));
    }
    // For android 11+, if the last part ends with "Backups", remove that and add the backupType so
    // we still can use the Backups, FulBackups etc. subdirectories when the chosen backup folder
    // is a subdirectory called Backups.
    if (Build.VERSION.SDK_INT >= 30 && !backupType.equals("")) {
      Path selectedDir = signal.toPath();
      if (selectedDir.endsWith(BACKUPS)) {
        signal = selectedDir.getParent().toFile();
      }
    }
    File backups = new File(signal, backupType);

    //noinspection ConstantConditions
    if (packageId.startsWith(PRODUCTION_PACKAGE_ID + ".")) {
      backups = new File(backups, packageId.substring(PRODUCTION_PACKAGE_ID.length() + 1));
    }

    if (!backups.exists()) {
      if (!backups.mkdirs()) {
        throw new NoExternalStorageException("Unable to create backup directory...");
      }
    }

    return backups;
  }

  // JW: added. Returns storage dir on internal or removable storage
  private static File getStorage(Boolean isBackupLocationRemovable) throws NoExternalStorageException {
    Context context = CoreUiDependencies.INSTANCE.getApplication();
    File storage = null;

    // We now check if the removable storage is prefered. If it is
    // and it is not available we fallback to internal storage.
    if (isBackupLocationRemovable) {
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
  public static File getBackupBaseDirectory(Boolean isBackupLocationRemovable) throws NoExternalStorageException {
    File storage = getStorage(isBackupLocationRemovable);

    if (!storage.canWrite()) {
      throw new NoExternalStorageException();
    }

    File signal = new File(storage, "Signal");

    return signal;
  }

  public static File getOrCreateBackupDirectory() throws NoExternalStorageException {
    Boolean isBackupLocationRemovable = true; // JW: TODO
    File storage = getStorage(isBackupLocationRemovable); // JW: changed

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

  public static File getOrCreateBackupV2Directory() throws NoExternalStorageException {
    File storage = Environment.getExternalStorageDirectory();

    if (!storage.canWrite()) {
      throw new NoExternalStorageException();
    }

    File backups = getBackupV2Directory();

    if (!backups.exists()) {
      if (!backups.mkdirs()) {
        throw new NoExternalStorageException("Unable to create backup directory...");
      }
    }

    return backups;
  }

  public static File getBackupDirectory() throws NoExternalStorageException {
    File   storage   = Environment.getExternalStorageDirectory();
    File   signal    = new File(storage, "Signal");
    File   backups   = new File(signal, "Backups");
    String packageId = CoreUiDependencies.INSTANCE.getPackageId();

    //noinspection ConstantConditions
    if (packageId.startsWith(PRODUCTION_PACKAGE_ID + ".")) {
      backups = new File(backups, packageId.substring(PRODUCTION_PACKAGE_ID.length() + 1));
    }

    return backups;
  }

  public static File getBackupV2Directory() throws NoExternalStorageException {
    File   storage   = Environment.getExternalStorageDirectory();
    File   backups   = new File(storage, "Signal");
    String packageId = CoreUiDependencies.INSTANCE.getPackageId();

    //noinspection ConstantConditions
    if (packageId.startsWith(PRODUCTION_PACKAGE_ID + ".")) {
      backups = new File(storage, packageId.substring(PRODUCTION_PACKAGE_ID.length() + 1));
    }

    return backups;
  }

  @RequiresApi(24)
  public static @NonNull String getDisplayPath(@NonNull Context context, @NonNull Uri uri) {
    String lastPathSegment = Objects.requireNonNull(uri.getLastPathSegment());
    String backupVolume    = lastPathSegment.replaceFirst(":.*", "");
    String backupName      = lastPathSegment.replaceFirst(".*:", "");

    StorageManager      storageManager = ContextCompat.getSystemService(context, StorageManager.class);
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

  // JW: made public
  public static File getSignalStorageDir() throws NoExternalStorageException {
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

  public static boolean canWriteToMediaStore() {
    return Build.VERSION.SDK_INT > 28 ||
           Permissions.hasAll(CoreUiDependencies.INSTANCE.getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
  }

  public static boolean canReadAnyFromMediaStore() {
    return Permissions.hasAny(CoreUiDependencies.INSTANCE.getApplication(), PermissionCompat.forImagesAndVideos());
  }

  public static boolean canOnlyReadSelectedMediaStore() {
    return Build.VERSION.SDK_INT >= 34 &&
           Permissions.hasAll(CoreUiDependencies.INSTANCE.getApplication(), Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
           !Permissions.hasAny(CoreUiDependencies.INSTANCE.getApplication(), Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO);
  }

  public static @NonNull Uri getVideoUri() {
    return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
  }

  public static @NonNull Uri getAudioUri() {
    return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
  }

  public static @NonNull Uri getImageUri() {
    return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
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

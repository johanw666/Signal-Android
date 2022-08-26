/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms.database;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;

import androidx.annotation.NonNull;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.crypto.AttachmentSecret;
import org.thoughtcrime.securesms.crypto.AttachmentSecretProvider;
import org.thoughtcrime.securesms.backup.BackupPassphrase;
import org.thoughtcrime.securesms.crypto.DatabaseSecret;
import org.thoughtcrime.securesms.crypto.DatabaseSecretProvider;
import org.thoughtcrime.securesms.crypto.KeyStoreHelper;
import org.thoughtcrime.securesms.util.Base64;
import org.thoughtcrime.securesms.util.FileUtilsJW;
import org.thoughtcrime.securesms.util.JsonUtils;
import org.thoughtcrime.securesms.util.StorageUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.String;
import java.nio.channels.FileChannel;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;

public class EncryptedBackupExporter {
  
  private static final String TAG = Log.tag(EncryptedBackupExporter.class);

  // Files used to store the DatabaseSecret, AttachmentSecret and LogSecret, required after the transfer to SQLCipher in Signal 4.16
  private static final String databaseSecretFile = "databasesecret.txt";
  private static final String attachmentSecretFile = "attachmentsecret.txt";
  private static final String logSecretFile = "logsecret.txt";
  private static final String backupKeyFile = "backupkey.txt";
  private static final String exportDirectory = "SignalExport";
  private static final String secretsExportDirectory = "SignalSecrets";

  public static void exportToSd(Context context) throws NoExternalStorageException, IOException {
    verifyExternalStorageForExport(context);
    AttachmentSecretProvider asp = AttachmentSecretProvider.getInstance(context);
    DatabaseSecret dbs = DatabaseSecretProvider.getOrCreateDatabaseSecret(context);
    AttachmentSecret ats = asp.getOrCreateAttachmentSecret();
    byte[] lgs = getOrCreateLogSecret(context);
    String bks = BackupPassphrase.get(context);
    exportDirectory(context, "");
    exportSecrets(context, dbs, ats, lgs, bks);
    if (TextSecurePreferences.isRawBackupInZipfile(context)) {
      File test = new File(getEncryptedZipfileName());
      if (test.exists()) {
        test.delete();
      }
      FileUtilsJW.createEncryptedZipfile(context, getEncryptedZipfileName(), getExportDirectoryPath(context), getExportSecretsDirectory(context));
      deleteRawBackupFiles(context);
    }
  }

  public static void importFromSd(Context context) throws NoExternalStorageException, IOException {
    // Store in a boolean because settings might change after restore
    boolean rawBackupInZipfile = TextSecurePreferences.isRawBackupInZipfile(context);
    // Extract the zipfile
    if (rawBackupInZipfile) {
      FileUtilsJW.extractEncryptedZipfile(context, getEncryptedZipfileName(), StorageUtil.getRawBackupDirectory().getAbsolutePath());
    }
    verifyExternalStorageForImport(context);
    importDirectory(context, "");
    importSharedSettings(context);
    importSecrets(context);
    if (rawBackupInZipfile) {
      deleteRawBackupFiles(context);
    }
  }

  private static void importSharedSettings(Context context) {
    String tempFileName = "tempsettings";
    String settingsFile = "org.thoughtcrime.securesms_preferences";

    File fromPrefFile = new File(getExportDirectoryPath(context) + File.separator + "shared_prefs" + File.separator + settingsFile + ".xml");
    // Copy fromFile to shared_prefs
    File toTempFile = new File(context.getFilesDir() + File.separator + ".." + File.separator + "shared_prefs" + File.separator + tempFileName + ".xml");
    migrateFile(fromPrefFile, toTempFile);
    // Get default preferences
    SharedPreferences defaultPreferences = context.getSharedPreferences(settingsFile, Context.MODE_PRIVATE);
    // Load settings from backup file
    SharedPreferences newPreferences = context.getSharedPreferences(tempFileName, Context.MODE_PRIVATE);
    // Write settings to current file
    copySharedPreferences(newPreferences, defaultPreferences);
    // delete temp file
    toTempFile.delete();
  }

  public static void copySharedPreferences(SharedPreferences fromPreferences, SharedPreferences toPreferences) {
    SharedPreferences.Editor editor = toPreferences.edit();
    editor.clear();
    copySharedPreferences(fromPreferences, editor);
    editor.commit();
  }

  public static void copySharedPreferences(SharedPreferences fromPreferences, SharedPreferences.Editor toEditor) {
    for (Map.Entry<String, ?> entry : fromPreferences.getAll().entrySet()) {
      Object value = entry.getValue();
      String key = entry.getKey();
      //Log.w(TAG, "copySharedPreferences: Key = " + key + ", Value = " + value.toString());
      if (value instanceof String) {
        toEditor.putString(key, ((String) value));
      } else if (value instanceof Set) {
        toEditor.putStringSet(key, (Set<String>) value); // EditorImpl.putStringSet already creates a copy of the set
      } else if (value instanceof Integer) {
        toEditor.putInt(key, (Integer) value);
      } else if (value instanceof Long) {
        toEditor.putLong(key, (Long) value);
      } else if (value instanceof Float) {
        toEditor.putFloat(key, (Float) value);
      } else if (value instanceof Boolean) {
        toEditor.putBoolean(key, (Boolean) value);
      }
    }
  }

  // Replace the secrets with the new versions. These values might change when the backup is
  // restored on another device as where it is made.
  private static void importSecrets(Context context) {
    DatabaseSecret dbs = getDatabaseSecretFromBackup(context);
    AttachmentSecret ats = getAttachmentSecretFromBackup(context);
    byte[] lgs = getLogSecretFromBackup(context);
    String bks = getBackupKeyFromBackup(context);

    if (dbs != null) {
      overwriteDatabaseSecret(context, dbs);
    }
    if (ats != null) {
      overwriteAttachmentSecret(context, ats);
    }
    if (lgs != null) {
      overwriteLogSecret(context, lgs);
    }
    if (bks != null) {
	    BackupPassphrase.set(context, bks);
    }
  }

  private static String getExportDatabaseSecretFullName(Context context) {
    return getExportSecretsDirectory(context) + databaseSecretFile;
  }

  private static String getExportAttachmentSecretFullName(Context context) {
    return getExportSecretsDirectory(context) + attachmentSecretFile;
  }

  private static String getExportLogSecretFullName(Context context) {
    return getExportSecretsDirectory(context) + logSecretFile;
  }
  
  private static String getExportBackupKeyFullName(Context context) {
    return getExportSecretsDirectory(context) + backupKeyFile;
  }

  private static String getExportBaseDirectory(Context context) {
    String basedir = Environment.getExternalStorageDirectory().getAbsolutePath();
    try {
      basedir = StorageUtil.getRawBackupDirectory().getAbsolutePath();
    } catch (NoExternalStorageException e) {
      Log.w(TAG, "getExportBaseDirectory failed: " + e.toString());
    }
    return basedir;
  }

  private static String getExportSecretsDirectory(Context context) {
    return getExportBaseDirectory(context) + File.separator + secretsExportDirectory + File.separator;
  }

  private static String getExportDirectoryPath(Context context) {
    return getExportBaseDirectory(context) + File.separator + exportDirectory;
  }

  private static void verifyExternalStorageForExport(Context context) throws NoExternalStorageException {
    if (!Environment.getExternalStorageDirectory().canWrite())
      throw new NoExternalStorageException();

    String exportDirectoryPath = getExportDirectoryPath(context);
    File exportDirectory       = new File(exportDirectoryPath);

    if (!exportDirectory.exists())
      exportDirectory.mkdir();
  }

  private static void verifyExternalStorageForImport(Context context) throws NoExternalStorageException {
    if (!Environment.getExternalStorageDirectory().canRead() ||
        !(new File(getExportDirectoryPath(context)).exists()))
        throw new NoExternalStorageException();
  }

  private static void migrateFile(File from, File to) {
    try {
      if (from.exists()) {
        FileChannel source      = new FileInputStream(from).getChannel();
        FileChannel destination = new FileOutputStream(to).getChannel();

        destination.transferFrom(source, 0, source.size());
        source.close();
        destination.close();
      }
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
    }
  }

  private static void exportDirectory(Context context, String directoryName) throws IOException {
    if (!directoryName.equals("/lib") && !directoryName.equals("/code_cache") && !directoryName.equals("/cache")) {
      File directory       = new File(context.getFilesDir().getParent() + File.separatorChar + directoryName);
      File exportDirectory = new File(getExportDirectoryPath(context) + File.separatorChar + directoryName);

      if (directory.exists()) {
        exportDirectory.mkdirs();

        File[] contents = directory.listFiles();

        if (contents == null)
          throw new IOException("directory.listFiles() is null for " + context.getFilesDir().getParent() + File.separatorChar + directoryName + "!");

        for (int i=0;i<contents.length;i++) {
          File localFile = contents[i];

          // Don't export the libraries
          if ( localFile.getAbsolutePath().contains("libaesgcm.so") ||
               localFile.getAbsolutePath().contains("libconscrypt_jni.so") ||
               localFile.getAbsolutePath().contains("libnative-utils.so") ||
               localFile.getAbsolutePath().contains("libringrtc.so") ||
               localFile.getAbsolutePath().contains("libringrtc_rffi.so") ||
               localFile.getAbsolutePath().contains("libargon2.so") ||
               localFile.getAbsolutePath().contains("libsqlcipher.so") ) {
           // Do nothing
          } else if (localFile.isFile()) {        
            File exportedFile = new File(exportDirectory.getAbsolutePath() + File.separator + localFile.getName());
            migrateFile(localFile, exportedFile);
          } else {
            exportDirectory(context, directoryName + File.separator + localFile.getName());
          }
        }
      } else {
        Log.w(TAG, "Could not find directory: " + directory.getAbsolutePath());
      }
    }
  }

  private static void importDirectory(Context context, String directoryName) throws IOException {
    File directory       = new File(getExportDirectoryPath(context) + File.separator + directoryName);
    File importDirectory = new File(context.getFilesDir().getParent() + File.separator + directoryName);

    if (directory.exists() && directory.isDirectory()) {
      importDirectory.mkdirs();

      File[] contents = directory.listFiles();

      for (File exportedFile : contents) {
        if (exportedFile.isFile()) {
          File localFile = new File(importDirectory.getAbsolutePath() + File.separator + exportedFile.getName());
          migrateFile(exportedFile, localFile);
        } else if (exportedFile.isDirectory()) {
          importDirectory(context, directoryName + File.separator + exportedFile.getName());
        }
      }
    }
  }

  // Store the encrypted secrets in a file
  private static void exportSecrets(Context context, DatabaseSecret dbs, AttachmentSecret ats, byte[] lgs, String bks) {
    File exportDirectory = new File(getExportSecretsDirectory(context));
    if (!exportDirectory.exists()) {
      exportDirectory.mkdir();
    }
    writeStringToFile(new File(getExportDatabaseSecretFullName(context)), dbs.asString());
    writeStringToFile(new File(getExportAttachmentSecretFullName(context)), ats.serialize());
    writeStringToFile(new File(getExportLogSecretFullName(context)), Base64.encodeBytes(lgs));
    if (bks != null) { // Backupkey is optional
      writeStringToFile(new File(getExportBackupKeyFullName(context)), bks);
    }
  }

  private static void writeStringToFile(File file, String str) {
    try {
      file.createNewFile();
      FileOutputStream fOut = new FileOutputStream(file);
      OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fOut);
      outputStreamWriter.write(str);
      outputStreamWriter.close();
      fOut.flush();
      fOut.close();
    } catch (IOException e) {
      Log.v(TAG, "File write failed: " + e.toString());
    }
  }

  private static DatabaseSecret getDatabaseSecretFromBackup(Context context) {
    DatabaseSecret dbs = null;
    File databaseSecretExportFile = new File(getExportDatabaseSecretFullName(context));

    try {
      if (databaseSecretExportFile.exists()) {
        String encoded = "";
        FileInputStream fIn = new FileInputStream(databaseSecretExportFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fIn);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(bufferedReader.readLine());
        encoded = stringBuilder.toString();

        inputStreamReader.close();
        fIn.close();

        dbs = new DatabaseSecret(encoded);
      }
    } catch (IOException e) {
      Log.w(TAG, "getDatabaseSecretFromBackup file read failed: " + e.toString());
    }
    return dbs;
  }

  private static AttachmentSecret getAttachmentSecretFromBackup(Context context) {
    AttachmentSecret ats = null;
    File attachmentSecretExportFile = new File(getExportAttachmentSecretFullName(context));

    try {
      if (attachmentSecretExportFile.exists()) {
        String encoded = "";
        FileInputStream fIn = new FileInputStream(attachmentSecretExportFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fIn);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(bufferedReader.readLine());
        encoded = stringBuilder.toString();

        inputStreamReader.close();
        fIn.close();

        ats = new AttachmentSecret();
        try {
          ats = JsonUtils.fromJson(encoded, AttachmentSecret.class);
        } catch (IOException e) {
          throw new AssertionError(e);
        }
      }
    } catch (IOException e) {
      Log.w(TAG, "getAttachmentSecretFromBackup file read failed: " + e.toString());
    }
    return ats;
  }

  private static byte[] getLogSecretFromBackup(Context context) {
    byte[] lgs = null;
    File logSecretExportFile = new File(getExportLogSecretFullName(context));

    try {
      if (logSecretExportFile.exists()) {
        String encoded = "";
        FileInputStream fIn = new FileInputStream(logSecretExportFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fIn);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(bufferedReader.readLine());
        encoded = stringBuilder.toString();

        inputStreamReader.close();
        fIn.close();

        lgs = new byte[32];
        lgs = Base64.decode(encoded);
      }
    } catch (IOException e) {
      Log.w(TAG, "getLogSecretFromBackup file read failed: " + e.toString());
    }
    return lgs;
  }
  
  private static String getBackupKeyFromBackup(Context context) {
	  String bks = null;
    // Standard backup is optional, so it can be quite normal if the file does not exist
    File backupPasswordExportFile = new File(getExportBackupKeyFullName(context));

    try {
      if (backupPasswordExportFile.exists()) {
        FileInputStream fIn = new FileInputStream(backupPasswordExportFile);
        InputStreamReader inputStreamReader = new InputStreamReader(fIn);
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(bufferedReader.readLine());
        bks = stringBuilder.toString();
      }
    } catch (IOException e) {
      Log.w(TAG, "getBackupKeyFromBackup file read failed: " + e.toString());
    }
    return bks;
  }

  // JW: store an existing DatabaseSecret in the settingsfile.
  private static void overwriteDatabaseSecret(@NonNull Context context, @NonNull DatabaseSecret databaseSecret) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(databaseSecret.asBytes());
      TextSecurePreferences.setDatabaseEncryptedSecret(context, encryptedSecret.serialize());
    } else {
      TextSecurePreferences.setDatabaseUnencryptedSecret(context, databaseSecret.asString());
    }
  }

  private static void overwriteAttachmentSecret(@NonNull Context context, @NonNull AttachmentSecret attachmentSecret) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(attachmentSecret.serialize().getBytes());
      TextSecurePreferences.setAttachmentEncryptedSecret(context, encryptedSecret.serialize());
    } else {
      TextSecurePreferences.setAttachmentUnencryptedSecret(context, attachmentSecret.serialize());
    }
  }

  private static void overwriteLogSecret(@NonNull Context context, @NonNull byte[] lgs) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(lgs);
      TextSecurePreferences.setLogEncryptedSecret(context, encryptedSecret.serialize());
    } else {
      TextSecurePreferences.setLogUnencryptedSecret(context, Base64.encodeBytes(lgs));
    }
  }

  // LogSecretProvider class functions. Copied here because they are all private
  static byte[] getOrCreateLogSecret(@NonNull Context context) {
    String unencryptedSecret = TextSecurePreferences.getLogUnencryptedSecret(context);
    String encryptedSecret   = TextSecurePreferences.getLogEncryptedSecret(context);

    if      (unencryptedSecret != null) return parseUnencryptedSecret(unencryptedSecret);
    else if (encryptedSecret != null)   return parseEncryptedSecret(encryptedSecret);
    else                                return createAndStoreSecret(context);
  }

  private static byte[] parseUnencryptedSecret(String secret) {
    try {
      return Base64.decode(secret);
    } catch (IOException e) {
      throw new AssertionError("Failed to decode the unecrypted secret.");
    }
  }

  private static byte[] parseEncryptedSecret(String secret) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.SealedData.fromString(secret);
      return KeyStoreHelper.unseal(encryptedSecret);
    } else {
      throw new AssertionError("OS downgrade not supported. KeyStore sealed data exists on platform < M!");
    }
  }

  private static byte[] createAndStoreSecret(@NonNull Context context) {
    SecureRandom random = new SecureRandom();
    byte[]       secret = new byte[32];
    random.nextBytes(secret);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KeyStoreHelper.SealedData encryptedSecret = KeyStoreHelper.seal(secret);
      TextSecurePreferences.setLogEncryptedSecret(context, encryptedSecret.serialize());
    } else {
      TextSecurePreferences.setLogUnencryptedSecret(context, Base64.encodeBytes(secret));
    }
    return secret;
  }

  private static String getEncryptedZipfileName() {
    try {
      String backupPath = StorageUtil.getRawBackupDirectory().getAbsolutePath();
      return backupPath + File.separator + "SignalExport.zip";
    } catch (NoExternalStorageException e) {
      Log.w(TAG, "getEncryptedZipfileName failed: " + e.toString());
      return Environment.getExternalStorageDirectory().getAbsolutePath();
    }
  }

  // Delete the exported contents of the data dir and the unencrypted keys.
  private static void deleteRawBackupFiles(Context context) {
    FileUtilsJW.secureDeleteRecursive(new File(getExportSecretsDirectory(context)));
    FileUtilsJW.deleteRecursive(new File(getExportDirectoryPath(context)));
  }
}

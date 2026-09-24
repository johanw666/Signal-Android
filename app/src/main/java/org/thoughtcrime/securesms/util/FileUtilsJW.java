package org.thoughtcrime.securesms.util;

import android.content.Context;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.backup.BackupPassphrase;
import org.thoughtcrime.securesms.keyvalue.SignalStore;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.String;
import java.security.SecureRandom;


public class FileUtilsJW {

  private static final String TAG = Log.tag(FileUtilsJW.class);

  //------------------------------------------------------------------------------------------------
  // Handle backups in encrypted zipfiles
  public static boolean createEncryptedZipfile(Context context, String zipFileName, String exportDirectory, String exportSecretsDirectory) {
    try {
      String password = getBackupPassword(context);
      ZipFile zipFile = new ZipFile(zipFileName);
      ZipParameters parameters = new ZipParameters();
      parameters.setCompressionMethod(CompressionMethod.STORE); // Encrypted data is uncompressable anyway
      //parameters.setCompressionLevel(CompressionLevel.FASTEST);
      if (password.length() > 0 ) {
        parameters.setEncryptFiles(true);
        parameters.setEncryptionMethod(EncryptionMethod.AES);
        parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        zipFile.setPassword(password.toCharArray());
      }
      zipFile.addFolder(new File(exportSecretsDirectory), parameters);
      zipFile.addFolder(new File(exportDirectory), parameters);
    } catch (ZipException e) {
      Log.w(TAG, "createEncryptedZipfile failed: " + e.toString());
      return false;
    }
    return true;
  }

  public static boolean createEncryptedPlaintextZipfile(Context context, String zipFileName, String inputFileName) {
    try {
      String password = getBackupPassword(context);
      ZipFile zipFile = new ZipFile(zipFileName);
      ZipParameters parameters = new ZipParameters();
      parameters.setCompressionLevel(CompressionLevel.MAXIMUM);
      if (password.length() > 0 ) {
        parameters.setEncryptFiles(true);
        parameters.setEncryptionMethod(EncryptionMethod.AES);
        parameters.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
        zipFile.setPassword(password.toCharArray());
      }
      zipFile.addFile(inputFileName, parameters);
    } catch (ZipException e) {
      Log.w(TAG, "createEncryptedPlaintextZipfile failed: " + e.toString());
      return false;
    }
    return true;
  }

  // Get the password of the regular backup. If there is no regular backup set, return an empty string.
  public static String getBackupPassword(Context context) {
    String password = "";
    Boolean chatBackupsEnabled = SignalStore.settings().isBackupEnabled();
    if (chatBackupsEnabled) {
      password = BackupPassphrase.get(context);
      if (password == null) {
        Log.w(TAG, "createEncryptedZipfile: empty zipfile password");
        password = "";
      }
      // Plaintext storage of password may contain spaces
      password = password.replace(" ", "");
    }
    return password;
  }

  public static boolean extractEncryptedZipfile(Context context, String fileName, String directoryName) {
    String password = getBackupPassword(context);

    try {
      ZipFile zipFile = new ZipFile(fileName);
      if (zipFile.isEncrypted()) {
        zipFile.setPassword(password.toCharArray());
      }
      zipFile.extractAll(directoryName);
    } catch (Exception e) {
      Log.w(TAG, "extractEncryptedZipfile failed: " + e.toString());
      return false;
    }
    return true;
  }
  //------------------------------------------------------------------------------------------------

  public static void deleteRecursive(File fileOrDirectory) {
    if (fileOrDirectory.isDirectory()) {
      for (File child : fileOrDirectory.listFiles()) {
        deleteRecursive(child);
      }
    }
    fileOrDirectory.delete();
  }

  public static void secureDeleteRecursive(File fileOrDirectory) {
    if (fileOrDirectory.isDirectory()) {
      for (File child : fileOrDirectory.listFiles()) {
        secureDeleteRecursive(child);
      }
    }
    try {
      if (!fileOrDirectory.isFile()) {
        fileOrDirectory.delete();
      } else {
        secureDelete(fileOrDirectory);
      }
    } catch (IOException e) {
      Log.w(TAG, "secureDeleteRecursive failed: " + e.toString());
    }
  }

  // Not perfect on wear-leveling flash memory but still better than nothing.
  public static void secureDelete(File file) throws IOException {
    if (file.exists()) {
      long length = file.length();
      SecureRandom random = new SecureRandom();
      RandomAccessFile raf = new RandomAccessFile(file, "rws");
      raf.seek(0);
      raf.getFilePointer();
      byte[] data = new byte[64];
      long pos = 0;
      while (pos < length) {
        random.nextBytes(data);
        raf.write(data);
        pos += data.length;
      }
      raf.close();
      file.delete();
    }
  }
}

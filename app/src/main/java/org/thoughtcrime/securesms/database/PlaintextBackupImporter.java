package org.thoughtcrime.securesms.database;

import android.content.Context;
import android.os.Environment;

import net.sqlcipher.database.SQLiteStatement;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.util.FileUtilsJW;
import org.thoughtcrime.securesms.util.StorageUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class PlaintextBackupImporter {

  private static final String TAG = PlaintextBackupImporter.class.getSimpleName();

  public static void importPlaintextFromSd(Context context)
      throws NoExternalStorageException, IOException
  {
    Log.w(TAG, "importPlaintext()");
    // Unzip zipfile first
    if (TextSecurePreferences.isPlainBackupInZipfile(context)) {
      File zipFile = getPlaintextExportZipFile();
      FileUtilsJW.extractEncryptedZipfile(context, zipFile.getAbsolutePath(), StorageUtil.getBackupPlaintextDirectory().getAbsolutePath());
    }
    SmsDatabase    db          = (SmsDatabase)DatabaseFactory.getSmsDatabase(context);
    SQLiteDatabase transaction = db.beginTransaction();

    try {
      ThreadDatabase threads         = DatabaseFactory.getThreadDatabase(context);
      XmlBackup      backup          = new XmlBackup(getPlaintextExportFile().getAbsolutePath());
      Set<Long>      modifiedThreads = new HashSet<>();
      XmlBackup.XmlBackupItem item;

      // TODO: we might have to split this up in chunks of about 5000 messages to prevent these errors:
      // java.util.concurrent.TimeoutException: net.sqlcipher.database.SQLiteCompiledSql.finalize() timed out after 10 seconds
      while ((item = backup.getNext()) != null) {
        Recipient       recipient  = Recipient.external(context, item.getAddress());
        long            threadId   = threads.getThreadIdFor(recipient);
        SQLiteStatement statement  = db.createInsertStatement(transaction);

        if (item.getAddress() == null || item.getAddress().equals("null"))
          continue;

        if (!isAppropriateTypeForImport(item.getType()))
          continue;

        addStringToStatement(statement, 1, recipient.getId().serialize());
        addNullToStatement(statement, 2);
        addLongToStatement(statement, 3, item.getDate());
        addLongToStatement(statement, 4, item.getDate());
        addLongToStatement(statement, 5, item.getProtocol());
        addLongToStatement(statement, 6, item.getRead());
        addLongToStatement(statement, 7, item.getStatus());
        addTranslatedTypeToStatement(statement, 8, item.getType());
        addNullToStatement(statement, 9);
        addStringToStatement(statement, 10, item.getSubject());
        addStringToStatement(statement, 11, item.getBody());
        addStringToStatement(statement, 12, item.getServiceCenter());
        addLongToStatement(statement, 13, threadId);
        modifiedThreads.add(threadId);
        statement.execute();
      }

      for (long threadId : modifiedThreads) {
        threads.update(threadId, true);
      }

      Log.w(TAG, "Exited loop");
    } catch (XmlPullParserException e) {
      Log.w(TAG, e);
      throw new IOException("XML Parsing error!");
    } finally {
      db.endTransaction(transaction);
    }
    // Delete the plaintext file if zipfile is present
    if (TextSecurePreferences.isPlainBackupInZipfile(context)) {
      getPlaintextExportFile().delete(); // Insecure, leaves possiblyrecoverable plaintext on device
      // FileUtilsJW.secureDelete(getPlaintextExportFile()); // much too slow
    }
  }

  private static File getPlaintextExportFile() throws NoExternalStorageException {
    File backup         = new File(StorageUtil.getBackupPlaintextDirectory(), "SignalPlaintextBackup.xml");
    File previousBackup = new File(StorageUtil.getLegacyBackupDirectory(), "SignalPlaintextBackup.xml");
    File oldBackup      = new File(Environment.getExternalStorageDirectory(), "TextSecurePlaintextBackup.xml");

    if (backup.exists()) return backup;
    else if (previousBackup.exists()) return previousBackup;
    else if (oldBackup.exists()) return oldBackup;
    else return backup;
  }

  private static File getPlaintextExportZipFile() throws NoExternalStorageException {
    return new File(StorageUtil.getBackupPlaintextDirectory(), "SignalPlaintextBackup.zip");
  }

  @SuppressWarnings("SameParameterValue")
  private static void addTranslatedTypeToStatement(SQLiteStatement statement, int index, int type) {
    statement.bindLong(index, SmsDatabase.Types.translateFromSystemBaseType(type));
  }

  private static void addStringToStatement(SQLiteStatement statement, int index, String value) {
    if (value == null || value.equals("null")) statement.bindNull(index);
    else                                       statement.bindString(index, value);
  }

  private static void addNullToStatement(SQLiteStatement statement, int index) {
    statement.bindNull(index);
  }

  private static void addLongToStatement(SQLiteStatement statement, int index, long value) {
    statement.bindLong(index, value);
  }

  private static boolean isAppropriateTypeForImport(long theirType) {
    long ourType = SmsDatabase.Types.translateFromSystemBaseType(theirType);

    return ourType == MmsSmsColumns.Types.BASE_INBOX_TYPE ||
           ourType == MmsSmsColumns.Types.BASE_SENT_TYPE ||
           ourType == MmsSmsColumns.Types.BASE_SENT_FAILED_TYPE;
  }
}

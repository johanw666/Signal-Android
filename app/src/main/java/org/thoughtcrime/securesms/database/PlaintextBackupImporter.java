package org.thoughtcrime.securesms.database;

import android.content.Context;
import android.os.Environment;

import net.zetetic.database.sqlcipher.SQLiteStatement;

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

  private static final String TAG = Log.tag(PlaintextBackupImporter.class);

  public static SQLiteStatement createMessageInsertStatement(SQLiteDatabase database) {
    return database.compileStatement("INSERT INTO " + MessageTable.TABLE_NAME + " (" +
                                     MessageTable.FROM_RECIPIENT_ID + ", " +
                                     MessageTable.DATE_SENT + ", " +
                                     MessageTable.DATE_RECEIVED + ", " +
                                     MessageTable.READ + ", " +
                                     MessageTable.MMS_STATUS + ", " +
                                     MessageTable.TYPE + ", " +
                                     MessageTable.BODY + ", " +
                                     MessageTable.THREAD_ID +  ", " +
                                     MessageTable.TO_RECIPIENT_ID +  ") " +
                                     " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
  }

  public static void importPlaintextFromSd(Context context) throws NoExternalStorageException, IOException
  {
    Log.i(TAG, "importPlaintext()");
    // Unzip zipfile first if required
    if (TextSecurePreferences.isPlainBackupInZipfile(context)) {
      File zipFile = getPlaintextExportZipFile();
      FileUtilsJW.extractEncryptedZipfile(context, zipFile.getAbsolutePath(), StorageUtil.getBackupPlaintextDirectory().getAbsolutePath());
    }
    MessageTable   table       = SignalDatabase.messages();
    SQLiteDatabase transaction = table.beginTransaction();

    try {
      ThreadTable    threadTable     = SignalDatabase.threads();
      XmlBackup      backup          = new XmlBackup(getPlaintextExportFile().getAbsolutePath());
      Set<Long>      modifiedThreads = new HashSet<>();
      XmlBackup.XmlBackupItem item;

      // TODO: we might have to split this up in chunks of about 5000 messages to prevent these errors:
      // java.util.concurrent.TimeoutException: net.sqlcipher.database.SQLiteCompiledSql.finalize() timed out after 10 seconds
      while ((item = backup.getNext()) != null) {
        Recipient       recipient  = Recipient.external(context, item.getAddress());
        long            threadId   = threadTable.getOrCreateThreadIdFor(recipient);
        SQLiteStatement statement  = createMessageInsertStatement(transaction);

        if (item.getAddress() == null || item.getAddress().equals("null"))
          continue;

        if (!isAppropriateTypeForImport(item.getType()))
          continue;

        addStringToStatement(statement, 1, recipient.getId().serialize());
        addLongToStatement(statement, 2, item.getDate());
        addLongToStatement(statement, 3, item.getDate());
        addLongToStatement(statement, 4, item.getRead());
        addLongToStatement(statement, 5, item.getStatus());
        addTranslatedTypeToStatement(statement, 6, item.getType());
        addStringToStatement(statement, 7, item.getBody());
        addLongToStatement(statement, 8, threadId);
        addLongToStatement(statement, 9, item.getRecipient());
        modifiedThreads.add(threadId);
        //statement.execute();
        long rowId = statement.executeInsert();
      }

      for (long threadId : modifiedThreads) {
        threadTable.update(threadId, true);
      }

      table.setTransactionSuccessful();
    } catch (XmlPullParserException e) {
      Log.w(TAG, e);
      throw new IOException("XML Parsing error!");
    } finally {
      table.endTransaction(transaction);
    }
    // Delete the plaintext file if zipfile is present
    if (TextSecurePreferences.isPlainBackupInZipfile(context)) {
      getPlaintextExportFile().delete(); // Insecure, leaves possibly recoverable plaintext on device
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
    statement.bindLong(index, translateFromSystemBaseType(type));
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
    long ourType = translateFromSystemBaseType(theirType);

    return ourType == MessageTypes.BASE_INBOX_TYPE ||
           ourType == MessageTypes.BASE_SENT_TYPE ||
           ourType == MessageTypes.BASE_SENT_FAILED_TYPE;
  }

  public static long translateFromSystemBaseType(long theirType) {
    switch ((int)theirType) {
      case 1: return MessageTypes.BASE_INBOX_TYPE;
      case 2: return MessageTypes.BASE_SENT_TYPE;
      case 3: return MessageTypes.BASE_DRAFT_TYPE;
      case 4: return MessageTypes.BASE_OUTBOX_TYPE;
      case 5: return MessageTypes.BASE_SENT_FAILED_TYPE;
      case 6: return MessageTypes.BASE_OUTBOX_TYPE;
    }

    return MessageTypes.BASE_INBOX_TYPE;
  }
}

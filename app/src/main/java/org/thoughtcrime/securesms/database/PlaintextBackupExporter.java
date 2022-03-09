package org.thoughtcrime.securesms.database;

import android.content.Context;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.MmsMessageRecord;
import org.thoughtcrime.securesms.database.model.SmsMessageRecord;
import org.thoughtcrime.securesms.database.SignalDatabase;
import org.thoughtcrime.securesms.util.FileUtilsJW;
import org.thoughtcrime.securesms.util.StorageUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;

import java.io.File;
import java.io.IOException;

public class PlaintextBackupExporter {
  private static final String TAG = Log.tag(PlaintextBackupExporter.class);

  private static final String FILENAME = "SignalPlaintextBackup.xml";
  private static final String ZIPFILENAME = "SignalPlaintextBackup.zip";

  public static void exportPlaintextToSd(Context context)
      throws NoExternalStorageException, IOException
  {
    exportPlaintext(context);
  }

  public static File getPlaintextExportFile() throws NoExternalStorageException {
    return new File(StorageUtil.getBackupPlaintextDirectory(), FILENAME);
  }

  private static File getPlaintextZipFile() throws NoExternalStorageException {
    return new File(StorageUtil.getBackupPlaintextDirectory(), ZIPFILENAME);
  }

  private static void exportPlaintext(Context context)
      throws NoExternalStorageException, IOException
  {
    boolean testmmsexport = false;
    SmsDatabase      database = (SmsDatabase)SignalDatabase.sms();
    MmsDatabase      mmsdb    = (MmsDatabase)SignalDatabase.mms();
    int              count    = database.getMessageCount();
    int              mmscount = mmsdb.getMessageCount();
    if (!testmmsexport) mmscount = 0;
    XmlBackup.Writer writer   = new XmlBackup.Writer(getPlaintextExportFile().getAbsolutePath(), count + mmscount);

    SmsMessageRecord record;
    MmsMessageRecord mmsrecord;

    SmsDatabase.Reader smsreader = null;
    MmsDatabase.Reader mmsreader = null;
    int                skip      = 0;
    int                ROW_LIMIT = 500;

    do {
      if (smsreader != null)
        smsreader.close();

      smsreader = database.readerFor(database.getMessages(skip, ROW_LIMIT));

      while ((record = smsreader.getNext()) != null) {
        XmlBackup.XmlBackupItem item =
            new XmlBackup.XmlBackupItem(0,
                                        record.getIndividualRecipient().getSmsAddress().or("null"),
                                        record.getIndividualRecipient().getDisplayName(context),
                                        record.getDateReceived(),
                                        MmsSmsColumns.Types.translateToSystemBaseType(record.getType()),
                                        null,
                                        record.getDisplayBody(context).toString(),
                                        null,
                                        1,
                                        record.getDeliveryStatus(),
                                        getTransportType(record));

        writer.writeItem(item);
      }

      skip += ROW_LIMIT;
    } while (smsreader.getCount() > 0);

    if(testmmsexport) {
      int i = 0;
      Log.w(TAG, "Number of mms to export: " + mmscount);

      skip = 0;
      do {
        i++;
        Log.w(TAG, "Exporting mms: " + i);

        if (mmsreader != null)
          mmsreader.close();

        mmsreader = mmsdb.readerFor(mmsdb.getMessages(skip, ROW_LIMIT));
        Log.w(TAG, "readerFor: skip = " + skip);

        while ((mmsrecord = (MmsMessageRecord) mmsreader.getNext()) != null) {
          XmlBackup.XmlBackupItem item =
            new XmlBackup.XmlBackupItem(0,
              mmsrecord.getIndividualRecipient().getSmsAddress().or("null"),
              mmsrecord.getIndividualRecipient().getDisplayName(context),
              mmsrecord.getDateReceived(),
              MmsSmsColumns.Types.translateToSystemBaseType(mmsrecord.getType()),
              null,
              mmsrecord.getDisplayBody(context).toString(),
              null,
              1,
              mmsrecord.getDeliveryStatus(),
              getTransportType(mmsrecord));
          Log.w(TAG, "mmsrecord exported: " + mmsrecord.getIndividualRecipient().getSmsAddress().or("null") + ": " + mmsrecord.getDisplayBody(context).toString());
          writer.writeItem(item);
        }

        skip += ROW_LIMIT;
      } while (mmsreader.getCount() > 0);
    } // testmmsexport

    writer.close();

    if (TextSecurePreferences.isPlainBackupInZipfile(context)) {
      File test = new File(getPlaintextZipFile().getAbsolutePath());
      if (test.exists()) {
        test.delete();
      }
      FileUtilsJW.createEncryptedPlaintextZipfile(context, getPlaintextZipFile().getAbsolutePath(), getPlaintextExportFile().getAbsolutePath());
      getPlaintextExportFile().delete(); // Insecure, leaves possiblyrecoverable plaintext on device
      // FileUtilsJW.secureDelete(getPlaintextExportFile()); // much too slow
    }
  }

  private static String getTransportType(MessageRecord messageRecord) {
    String transportText = "-";
    if (messageRecord.isOutgoing() && messageRecord.isFailed()) {
      transportText = "-";
    } else if (messageRecord.isPending()) {
      transportText = "Pending";
    } else if (messageRecord.isPush()) {
      transportText = "Data";
    } else if (messageRecord.isMms()) {
      transportText = "MMS";
    } else {
      transportText = "SMS";
    }
    return transportText;
  }
}

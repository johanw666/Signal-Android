package org.thoughtcrime.securesms.database;

import android.content.Context;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.database.model.MessageRecord;
import org.thoughtcrime.securesms.database.model.MmsMessageRecord;
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
    MessageTable     messagetable = SignalDatabase.messages();
    int              count        = messagetable.getMessageCount();
    XmlBackup.Writer writer       = new XmlBackup.Writer(getPlaintextExportFile().getAbsolutePath(), count);

    MessageRecord record;

    MessageTable.MmsReader messagereader = null;
    int                    skip      = 0;
    int                    ROW_LIMIT = 500;

    do {
      if (messagereader != null)
        messagereader.close();

      messagereader = messagetable.mmsReaderFor(messagetable.getMessages(skip, ROW_LIMIT));

      try {
        while ((record = messagereader.getNext()) != null) {
          XmlBackup.XmlBackupItem item =
              new XmlBackup.XmlBackupItem(0,
                                          record.getFromRecipient().getSmsAddress().orElse("null"),
                                          record.getFromRecipient().getDisplayName(context),
                                          record.getDateReceived(),
                                          translateToSystemBaseType(record.getType()),
                                          null,
                                          record.getDisplayBody(context).toString(),
                                          null,
                                          1,
                                          record.getDeliveryStatus(),
                                          getTransportType(record),
                                          record.getToRecipient().getId().toLong());

          writer.writeItem(item);
        }
      }
      catch (Exception e) {
        Log.w(TAG, "messagereader.getNext() failed: " + e.getMessage());
      }

      skip += ROW_LIMIT;
    } while (messagereader.getCount() > 0);

    writer.close();

    if (TextSecurePreferences.isPlainBackupInZipfile(context)) {
      File test = new File(getPlaintextZipFile().getAbsolutePath());
      if (test.exists()) {
        test.delete();
      }
      FileUtilsJW.createEncryptedPlaintextZipfile(context, getPlaintextZipFile().getAbsolutePath(), getPlaintextExportFile().getAbsolutePath());
      getPlaintextExportFile().delete(); // Insecure, leaves possibly recoverable plaintext on device
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

  public static int translateToSystemBaseType(long type) {
    if (isInboxType(type)) return 1;
    else if (isOutgoingMessageType(type)) return 2;
    else if (isFailedMessageType(type)) return 5;

    return 1;
  }

  public static boolean isInboxType(long type) {
    return (type & MessageTypes.BASE_TYPE_MASK) == MessageTypes.BASE_INBOX_TYPE;
  }

  public static boolean isOutgoingMessageType(long type) {
    for (long outgoingType : MessageTypes.OUTGOING_MESSAGE_TYPES) {
      if ((type & MessageTypes.BASE_TYPE_MASK) == outgoingType)
        return true;
    }

    return false;
  }

  public static boolean isFailedMessageType(long type) {
    return (type & MessageTypes.BASE_TYPE_MASK) == MessageTypes.BASE_SENT_FAILED_TYPE;
  }
}

package org.thoughtcrime.securesms.database;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.google.android.mms.pdu_alt.PduHeaders;

import net.zetetic.database.sqlcipher.SQLiteStatement;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.AttachmentId;
import org.thoughtcrime.securesms.database.model.GroupRecord;
import org.thoughtcrime.securesms.database.whatsapp.WaDbOpenHelper;
import org.thoughtcrime.securesms.mms.MmsException;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.recipients.RecipientId;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.thoughtcrime.securesms.database.MessageTable.DATE_RECEIVED;
import static org.thoughtcrime.securesms.database.MessageTable.DATE_SENT;
import static org.thoughtcrime.securesms.database.MessageTable.TYPE;
import static org.thoughtcrime.securesms.database.MessageTable.MMS_MESSAGE_TYPE;
import static org.thoughtcrime.securesms.database.MessageTable.MMS_STATUS;
import static org.thoughtcrime.securesms.database.MessageTable.TABLE_NAME;
import static org.thoughtcrime.securesms.database.MessageTable.VIEW_ONCE;
import static org.thoughtcrime.securesms.database.MessageTable.DATE_SERVER;
import static org.thoughtcrime.securesms.database.MessageTable.EXPIRES_IN;
import static org.thoughtcrime.securesms.database.MessageTable.READ;
import static org.thoughtcrime.securesms.database.MessageTable.RECIPIENT_ID;
import static org.thoughtcrime.securesms.database.MessageTable.SMS_SUBSCRIPTION_ID;
import static org.thoughtcrime.securesms.database.MessageTable.THREAD_ID;
import static org.thoughtcrime.securesms.database.MessageTable.BASE_INBOX_TYPE;
import static org.thoughtcrime.securesms.database.MessageTable.BASE_SENT_TYPE;
import static org.thoughtcrime.securesms.database.MessageTable.UNIDENTIFIED;

public class WhatsappBackupImporter {

    private static final String TAG = org.thoughtcrime.securesms.database.PlaintextBackupImporter.class.getSimpleName();

    private static android.database.sqlite.SQLiteDatabase openWhatsappDb(Context context) throws NoExternalStorageException {
        try {
            android.database.sqlite.SQLiteOpenHelper db = new WaDbOpenHelper(context);
            android.database.sqlite.SQLiteDatabase newdb = db.getReadableDatabase();
            return newdb;
        } catch(Exception e2){
            throw new NoExternalStorageException();
        }
    }

    public static void importWhatsappFromSd(Context context, ProgressDialog progressDialog, boolean importGroups, boolean avoidDuplicates, boolean importMedia)
            throws NoExternalStorageException, IOException
    {
        Log.w(TAG, "importWhatsapp(): importGroup: " + importGroups + ", avoidDuplicates: " + avoidDuplicates);
        android.database.sqlite.SQLiteDatabase whatsappDb = openWhatsappDb(context);
        MessageTable messageDb           = SignalDatabase.messages();
        //MmsTable mmsDb                   = SignalDatabase.mms();
        AttachmentTable attachmentDb     = SignalDatabase.attachments();
        SQLiteDatabase smsDbTransaction = messageDb.beginTransaction();
        int numMessages = getNumMessages(whatsappDb, importMedia);
        progressDialog.setMax(numMessages);
        try {
            ThreadTable threads            = SignalDatabase.threads();
            GroupTable groups              = SignalDatabase.groups();
            WhatsappBackup backup          = new WhatsappBackup(whatsappDb);
            Set<Long>      modifiedThreads = new HashSet<>();
            WhatsappBackup.WhatsappBackupItem item;

            int msgCount = 0;
            while ((item = backup.getNext()) != null) {
                msgCount++;
                progressDialog.setProgress(msgCount);
                Recipient recipient = getRecipient(context, item);
                if (isGroupMessage(item) && !importGroups) continue;
                long threadId = getThreadId(item, groups, threads, recipient);

                if (threadId == -1) continue;

                if (isMms(item)) {
                    if (!importMedia) continue;
                    if (avoidDuplicates && wasMsgAlreadyImported(smsDbTransaction, MessageTable.TABLE_NAME, MessageTable.DATE_SENT, threadId, recipient, item)) continue;
                    List<Attachment> attachments = WhatsappBackup.getMediaAttachments(whatsappDb, item);
                    if (attachments != null && attachments.size() > 0) insertMms(messageDb, attachmentDb, item, recipient, threadId, attachments);
                } else {
                    if (item.getBody() == null) continue; //Ignore empty msgs for e.g. change of security numbers
                    if (avoidDuplicates && wasMsgAlreadyImported(smsDbTransaction, MessageTable.TABLE_NAME, MessageTable.DATE_SENT, threadId, recipient, item)) continue;
                    insertSms(messageDb, smsDbTransaction, item, recipient, threadId);
                }
                modifiedThreads.add(threadId);
            }

            for (long threadId : modifiedThreads) {
                threads.update(threadId, true);
            }

            whatsappDb.setTransactionSuccessful();
            Log.w(TAG, "Exited loop");
        } catch (Exception e) {
            Log.w(TAG, e);
            throw new IOException("Whatsapp Import error!");
        } finally {
            whatsappDb.close();
            messageDb.endTransaction(smsDbTransaction);
        }

    }

    private static boolean wasMsgAlreadyImported(SQLiteDatabase db, String tableName, String dateField, long threadId, Recipient recipient, WhatsappBackup.WhatsappBackupItem item) {
        String[] cols  = new String[] {"COUNT(*)"};
        String   query = THREAD_ID + " = ? AND " + dateField + " = ? AND " + RECIPIENT_ID + " = ?";
        String[] args  = new String[]{String.valueOf(threadId), String.valueOf(item.getDate()), String.valueOf(recipient.getId().serialize())};

        try (Cursor cursor = db.query(tableName, cols, query, args, null, null, null)) {
            if (cursor != null) {
                if (cursor.moveToFirst() && cursor.getInt(0) > 0) {
                    cursor.close();
                    return true;
                }
                cursor.close();
            }
        }
        return false;
    }

    private static int getNumMessages(android.database.sqlite.SQLiteDatabase whatsappDb, boolean importMedia) {
        String whereClause = "";
        if (!importMedia) whereClause = " WHERE data!=''";
        try {
            Cursor c = whatsappDb.rawQuery("SELECT COUNT(*) FROM messages" + whereClause, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    int count = c.getInt(0);
                    return count;
                }
                c.close();
            }
        }catch(Exception e2){
            Log.w(TAG, e2.getMessage());
        }
        return 0;
    }

    private static Recipient getRecipient(Context context, WhatsappBackup.WhatsappBackupItem item) {
        Recipient recipient;
        if (item.getAddress() == null) {
            recipient = Recipient.self();
        } else {
            recipient = Recipient.external(context, item.getAddress());
        }
        return recipient;
    }

    private static long getThreadId(WhatsappBackup.WhatsappBackupItem item, GroupTable groups, ThreadTable threads, Recipient recipient) {
        long threadId;
        if (isGroupMessage(item)) {
            RecipientId threadRecipientId = getGroupId(groups, item, recipient);
            if (threadRecipientId == null) return -1;
            try {
                Recipient threadRecipient = Recipient.resolved(threadRecipientId);
                threadId = threads.getOrCreateThreadIdFor(threadRecipient);
            } catch (Exception e) {
                Log.v(TAG, "Group not found: " + item.getGroupName());
                return -1;
            }
        } else {
            threadId = threads.getOrCreateThreadIdFor(recipient);
        }
        return threadId;
    }

    private static boolean isMms(WhatsappBackup.WhatsappBackupItem item) {
        if (item.getMediaWaType() != 0) return true;
        return false;
    }

    private static void insertMms(MessageTable mmsDb, AttachmentTable attachmentDb, WhatsappBackup.WhatsappBackupItem item, Recipient recipient, long threadId, List<Attachment> attachments) throws MmsException {
        List<Attachment> quoteAttachments = new LinkedList<>();
        ContentValues contentValues = new ContentValues();
        contentValues.put(DATE_SENT, item.getDate());
        contentValues.put(DATE_SERVER, item.getDate());
        contentValues.put(RECIPIENT_ID, recipient.getId().serialize());
        if (item.getType() == 1) {
            contentValues.put(TYPE, BASE_INBOX_TYPE);
        } else {
            contentValues.put(TYPE, BASE_SENT_TYPE);
        }
        contentValues.put(MMS_MESSAGE_TYPE, PduHeaders.MESSAGE_TYPE_RETRIEVE_CONF);
        contentValues.put(THREAD_ID, threadId);
        contentValues.put(MMS_STATUS, MessageTable.MmsStatus.DOWNLOAD_INITIALIZED);
        contentValues.put(DATE_RECEIVED, item.getDate());
        contentValues.put(SMS_SUBSCRIPTION_ID, -1);
        contentValues.put(EXPIRES_IN, 0);
        contentValues.put(VIEW_ONCE, 0);
        contentValues.put(READ, 1);
        contentValues.put(UNIDENTIFIED, 0);

        SQLiteDatabase transaction = mmsDb.beginTransaction();
        long messageId = transaction.insert(TABLE_NAME, null, contentValues);

        Map<Attachment, AttachmentId> insertedAttachments = attachmentDb.insertAttachmentsForMessage(messageId, attachments, quoteAttachments);
        mmsDb.setTransactionSuccessful();
        mmsDb.endTransaction();

    }

    private static void insertSms(MessageTable smsDb, SQLiteDatabase transaction, WhatsappBackup.WhatsappBackupItem item, Recipient recipient, long threadId) {
        SQLiteStatement statement  = PlaintextBackupImporter.createMessageInsertStatement(transaction);

        addStringToStatement(statement, 1, recipient.getId().serialize());
        addLongToStatement(statement, 2, item.getDate());
        addLongToStatement(statement, 3, item.getDate());
        addLongToStatement(statement, 4, item.getRead());
        addLongToStatement(statement, 5, item.getStatus());
        addTranslatedTypeToStatement(statement, 6, item.getType());
        addStringToStatement(statement, 7, item.getBody());
        addLongToStatement(statement, 8, threadId);

        statement.execute();
        statement.close();
    }

    private static RecipientId getGroupId(GroupTable groups, WhatsappBackup.WhatsappBackupItem item, Recipient recipient) {
        if (item.getGroupName() == null) return null;
        List<GroupRecord> groupRecords = groups.getGroupsContainingMember(recipient.getId(), false);
        for (GroupRecord group : groupRecords) {
            if (group.getTitle().equals(item.getGroupName())) {
                return group.getRecipientId();
            }
        }
        return null;
    }

    private static boolean isGroupMessage(WhatsappBackup.WhatsappBackupItem item) {
        if (item.getGroupName() != null) return true;
        return false;
    }

    @SuppressWarnings("SameParameterValue")
    private static void addTranslatedTypeToStatement(SQLiteStatement statement, int index, int type) {
        statement.bindLong(index, PlaintextBackupImporter.translateFromSystemBaseType(type));
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
        long ourType = PlaintextBackupImporter.translateFromSystemBaseType(theirType);

        return ourType == BASE_INBOX_TYPE ||
                ourType == MessageTable.BASE_SENT_TYPE ||
                ourType == MessageTable.BASE_SENT_FAILED_TYPE;
    }
}

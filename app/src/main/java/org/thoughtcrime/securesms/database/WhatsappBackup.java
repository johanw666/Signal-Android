package org.thoughtcrime.securesms.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.attachments.Attachment;
import org.thoughtcrime.securesms.attachments.UriAttachment;
import org.thoughtcrime.securesms.providers.BlobProvider;
import org.thoughtcrime.securesms.util.MediaUtil;
import org.thoughtcrime.securesms.util.Util;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WhatsappBackup {

    public static final String TAG = WhatsappBackup.class.getSimpleName();

    private static final String PROTOCOL       = "protocol";
    private static final String ADDRESS        = "address";
    private static final String CONTACT_NAME   = "contact_name";
    private static final String DATE           = "date";
    private static final String READABLE_DATE  = "readable_date";
    private static final String TYPE           = "type";
    private static final String SUBJECT        = "subject";
    private static final String BODY           = "body";
    private static final String SERVICE_CENTER = "service_center";
    private static final String READ           = "read";
    private static final String STATUS         = "status";
    private static final String TOA            = "toa";
    private static final String SC_TOA         = "sc_toa";
    private static final String LOCKED         = "locked";
    private static final String TRANSPORT      = "transport"; // JW: added
    private static final String GROUP_NAME     = "group_name"; // SW: added

    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

    private final android.database.sqlite.SQLiteDatabase whatsappDb;
    private long dbOffset = 0l;

    public WhatsappBackup(android.database.sqlite.SQLiteDatabase whatsappDb)  {
        this.whatsappDb = whatsappDb;
    }

    public static List<Attachment> getMediaAttachments(SQLiteDatabase whatsappDb, WhatsappBackupItem item) {
        List<Attachment> attachments = new LinkedList<>();
        try {
            Cursor c = whatsappDb.rawQuery("SELECT * FROM message_media WHERE message_row_id=" + item.getWaMessageId() +" LIMIT 1", null);
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        File storagePath = Environment.getExternalStorageDirectory();
                        String filePath = storagePath.getAbsolutePath() + File.separator + "Android/media/com.whatsapp/WhatsApp" + File.separator + c.getString(c.getColumnIndex("file_path"));
                        int size = c.getInt(c.getColumnIndex("file_size"));
                        String type = c.getString(c.getColumnIndex("mime_type"));
                        File file = new File(filePath);
                        if (!file.exists()) return attachments;
                        Uri uri = Uri.fromFile(file);
                        String name = filePath;
                        if (type.equals("image/jpeg")) {
                            Attachment attachment = new UriAttachment(uri, MediaUtil.IMAGE_JPEG, AttachmentDatabase.TRANSFER_PROGRESS_DONE,
                                    size, name, false, false, false, false, item.getMediaCaption(), null, null, null, null);
                            attachments.add(attachment);
                        } else if (type.equals("video/mp4")) {
                            Attachment attachment = new UriAttachment(uri, MediaUtil.VIDEO_MP4, AttachmentDatabase.TRANSFER_PROGRESS_DONE,
                                    size, name, false, false, false, false, item.getMediaCaption(), null, null, null, null);
                            attachments.add(attachment);
                        } else if (type.equals("audio/ogg; codecs=opus")) {
                        Attachment attachment = new UriAttachment(uri, MediaUtil.AUDIO_UNSPECIFIED, AttachmentDatabase.TRANSFER_PROGRESS_DONE,
                                size, name, true, false, false, false, null, null, null, null, null);
                        attachments.add(attachment);
                        } else {
                            return attachments; // Ignore everything that is not an image or a video for the moment
                        }
                        return attachments;
                    }
                    while (c.moveToNext());
                }
                c.close();
            }
        }catch(Exception e2){
            Log.w(TAG, e2.getMessage());
        }
        return attachments;
    }

    public WhatsappBackup.WhatsappBackupItem getNext() {
        WhatsappBackup.WhatsappBackupItem item = null;
        try {
            Cursor c = whatsappDb.rawQuery("SELECT * FROM messages LIMIT "+ dbOffset + ", 1", null);
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        item = new WhatsappBackup.WhatsappBackupItem();
                        item.subject       = null;
                        item.body          = c.getString(c.getColumnIndex("data"));
                        item.protocol      = 0;
                        String rawAddress = c.getString(c.getColumnIndex("key_remote_jid"));
                        if (rawAddress != null && rawAddress.trim().length() > 0) {
                            item.address = "+" + rawAddress.split("@")[0]; // Only keep the phone number
                        }
                        if (item.address.contains("-")) { // Check if it's a group message
                            item.groupName = getGroupName(c.getString(c.getColumnIndex("key_remote_jid")));
                            rawAddress = c.getString(c.getColumnIndex("remote_resource"));
                            if (rawAddress != null && rawAddress.trim().length() > 0) {
                                item.address = "+" + c.getString(c.getColumnIndex("remote_resource")).split("@")[0];
                            } else {
                                item.address = null;
                            }
                        }
                        item.contactName   = null;
                        item.date          = c.getLong(c.getColumnIndex("timestamp"));
                        item.readableDate  = dateFormatter.format(item.date);
                        int fromMe = c.getInt(c.getColumnIndex("key_from_me"));
                        item.type          = (int)(fromMe == 1 ? 2 : 1);
                        item.serviceCenter = null;
                        item.read          = 1;
                        item.status        = SmsDatabase.Status.STATUS_COMPLETE;
                        item.transport     = "Data";
                        item.mediaWaType   = c.getInt(c.getColumnIndex("media_wa_type"));
                        item.waMessageId   = c.getLong(c.getColumnIndex("_id"));
                        item.mediaCaption  = c.getString(c.getColumnIndex("media_caption"));
                    }
                    while (c.moveToNext());
                }
                c.close();
            }
        }catch(Exception e2){
            Log.w(TAG, e2.getMessage());
        }

        dbOffset++;
        //if (dbOffset == 3000) return null; // Limit number of imported messages for quick testing
        return item;
    }

    private String getGroupName(String keyRemoteJid) {
        try {

            Cursor c = whatsappDb.rawQuery("SELECT subject FROM chat JOIN jid ON chat.jid_row_id=jid._id WHERE jid.raw_string='" + keyRemoteJid + "' LIMIT 1", null);
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        String groupName        = c.getString(c.getColumnIndex("subject"));
                        return groupName;
                    }
                    while (c.moveToNext());
                }
                c.close();
            }
        }catch(Exception e2) {
            Log.w(TAG, e2.getMessage());
        }
        return null;
    }

    public static class WhatsappBackupItem {
        private int    protocol;
        private String address;
        private String contactName;
        private long   date;
        private String readableDate;
        private int    type;
        private String subject;
        private String body;
        private String serviceCenter;
        private int    read;
        private int    status;
        private String transport; // JW: added
        private String groupName; // SW: added
        private int mediaWaType; // SW: added
        private long waMessageId; // SW: added
        private String mediaCaption;

        public WhatsappBackupItem() {}

        public WhatsappBackupItem(int protocol, String address, String contactName, long date, int type,
                             String subject, String body, String serviceCenter, int read, int status,
                             String transport) // JW: added
        {
            this.protocol      = protocol;
            this.address       = address;
            this.contactName   = contactName;
            this.date          = date;
            this.readableDate  = dateFormatter.format(date);
            this.type          = type;
            this.subject       = subject;
            this.body          = body;
            this.serviceCenter = serviceCenter;
            this.read          = read;
            this.status        = status;
            this.transport     = transport; // JW: added
            this.groupName     = null;
            this.mediaWaType   = 0;
            this.waMessageId   = 0;
        }

        public int getProtocol() {
            return protocol;
        }

        public String getAddress() {
            return address;
        }

        public String getContactName() {
            return contactName;
        }

        public long getDate() {
            return date;
        }

        public String getReadableDate() {
            return readableDate;
        }

        public int getType() {
            return type;
        }

        public String getSubject() {
            return subject;
        }

        public String getBody() {
            return body;
        }

        public String getServiceCenter() {
            return serviceCenter;
        }

        public int getRead() {
            return read;
        }

        public int getStatus() {
            return status;
        }

        public String getGroupName() {
            return groupName;
        }

        public int getMediaWaType() {
            return mediaWaType;
        }

        public long getWaMessageId() {
            return waMessageId;
        }

        public String getTransport() { return transport; } // JW: added

        public String getMediaCaption() {
            return mediaCaption;
        }
    }
}

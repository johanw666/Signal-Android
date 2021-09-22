package org.thoughtcrime.securesms.database.whatsapp;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import org.thoughtcrime.securesms.database.NoExternalStorageException;

import java.io.File;

/**
 * Created by Man on 10/31/2016.
 */

public class WaDbContext extends ContextWrapper {

    public WaDbContext(Context base) {
        super(base);
    }

    @Override
    public File getDatabasePath(String name)  {
        File sdcard = Environment.getExternalStorageDirectory();
        String dbfile = sdcard.getAbsolutePath() + File.separator + name;
        if (!dbfile.endsWith(".db")) {
            dbfile += ".db" ;
        }

        File result = new File(dbfile);

        if (Log.isLoggable("DEBUG_CONTEXT", Log.WARN)) {
            Log.w("DEBUG_CONTEXT", "getDatabasePath(" + name + ") = " + result.getAbsolutePath());
        }

        return result;
    }

    /* this version is called for android devices >= api-11. thank to @damccull for fixing this. */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler errorHandler) {
        return openOrCreateDatabase(name,mode, factory);
    }

    /* this version is called for android devices < api-11 */
    @Override
    public SQLiteDatabase openOrCreateDatabase(String name, int mode, SQLiteDatabase.CursorFactory factory) {
        SQLiteDatabase result = SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), null);
        // SQLiteDatabase result = super.openOrCreateDatabase(name, mode, factory);
        if (Log.isLoggable("DEBUG_CONTEXT", Log.WARN)) {
            Log.w("DEBUG_CONTEXT", "openOrCreateDatabase(" + name + ",,) = " + result.getPath());
        }
        return result;
    }
}

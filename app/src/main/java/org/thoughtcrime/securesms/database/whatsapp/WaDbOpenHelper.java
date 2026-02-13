package org.thoughtcrime.securesms.database.whatsapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


public class WaDbOpenHelper extends SQLiteOpenHelper {

    private static String DB_NAME = "msgstore.db";

    public WaDbOpenHelper(Context context)
    {
        super(new WaDbContext(context), DB_NAME, null, 3);

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
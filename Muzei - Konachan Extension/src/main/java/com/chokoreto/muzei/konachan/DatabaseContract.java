package com.chokoreto.muzei.konachan;

import android.provider.BaseColumns;

public final class DatabaseContract {
    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DatabaseContract() {}

    /* Inner class that defines the table contents */
    public static abstract class ImagesEntry implements BaseColumns {
        public static final String TABLE_NAME = "images";
        public static final String COLUMN_NAME_MD5 = "md5";
        public static final String COLUMN_NAME_MD5_ID = "md5id";
        public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
    }
}
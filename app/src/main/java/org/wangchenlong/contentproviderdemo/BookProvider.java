package org.wangchenlong.contentproviderdemo;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

/**
 * 书籍提供者
 * <p/>
 * Created by wangchenlong on 16/6/14.
 */
public class BookProvider extends ContentProvider {

    private static final String TAG = "DEBUG-WCL: " + BookProvider.class.getSimpleName();

    public static final String AUTHORITY = "org.wangchenlong.book.provider"; // 与AndroidManifest保持一致
    public static final Uri BOOK_CONTNET_URI = Uri.parse("content://" + AUTHORITY + "/book");
    public static final Uri USER_CONTNET_URI = Uri.parse("content://" + AUTHORITY + "/user");

    public static final int BOOK_URI_CODE = 0;
    public static final int USER_URI_CODE = 1;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    // 关联Uri和Uri_Code
    static {
        sUriMatcher.addURI(AUTHORITY, "book", BOOK_URI_CODE);
        sUriMatcher.addURI(AUTHORITY, "user", USER_URI_CODE);
    }

    private Context mContext;
    private SQLiteDatabase mDb;

    @Override public boolean onCreate() {
        showLogs("onCreate 当前线程: " + Thread.currentThread().getName());
        mContext = getContext();

        return false;
    }

    private void initProviderData() {
        mDb = new DbOpenHelper(mContext).getWritableDatabase();
        mDb.execSQL("delete from " + DbOpenHelper.BOOK_TABLE_NAME);
        mDb.execSQL("delete from " + DbOpenHelper.USER_TABLE_NAME);
        mDb.execSQL("insert into book values(3,'Android');");
        mDb.execSQL("insert into book values(4, 'iOS');");
        mDb.execSQL("insert into book values(5, 'HTML5');");
        mDb.execSQL("insert into user values(1, 'Spike', 1);");
        mDb.execSQL("insert into user values(2, 'Wang', 0);");
    }

    @Nullable @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        showLogs("query 当前线程: " + Thread.currentThread().getName());
        String tableName = getTableName(uri);
        if (TextUtils.isEmpty(tableName)) {
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        return mDb.query(tableName, projection, selection, selectionArgs, null, null, sortOrder, null);
    }

    @Nullable @Override public String getType(Uri uri) {
        showLogs("getType");
        return null;
    }

    @Nullable @Override public Uri insert(Uri uri, ContentValues values) {
        showLogs("insert");
        String table = getTableName(uri);
        if (TextUtils.isEmpty(table)) {
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        mDb.insert(table, null, values);

        // 插入数据后通知改变
        mContext.getContentResolver().notifyChange(uri, null);
        return null;
    }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        showLogs("delete");

        String table = getTableName(uri);
        if (TextUtils.isEmpty(table)) {
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        int count = mDb.delete(table, selection, selectionArgs);
        if (count > 0) {
            mContext.getContentResolver().notifyChange(uri, null);
        }

        return count; // 返回删除的函数
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        showLogs("update");

        String table = getTableName(uri);
        if (TextUtils.isEmpty(table)) {
            throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
        int row = mDb.update(table, values, selection, selectionArgs);
        if (row > 0) {
            mContext.getContentResolver().notifyChange(uri, null);
        }

        return row; // 返回更新的行数
    }

    private String getTableName(Uri uri) {
        String tableName = null;
        switch (sUriMatcher.match(uri)) {
            case BOOK_URI_CODE:
                tableName = DbOpenHelper.BOOK_TABLE_NAME;
                break;
            case USER_URI_CODE:
                tableName = DbOpenHelper.USER_TABLE_NAME;
                break;
            default:
                break;
        }
        return tableName;
    }

    private void showLogs(String msg) {
        Log.e(TAG, msg);
    }
}

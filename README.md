# 使用 ContentProvider 跨进程共享数据

> 欢迎Follow我的[GitHub](https://github.com/SpikeKing)

**ContentProvider**主要应用于进程间数据共享. 对于应用而言, 多进程并不会经常使用, 因而较少使用ContentProvider, 是最不常见的四大组件(Activity, Service, BroadcastReceiver, ContentProvider). 但是其优异的性能与便捷, 对于多应用共享数据而言, 非常重要, 比如共享同一份计步数据等. 开发者只有掌握多种技能, 才能在开发中游刃有余, 用最优的方式完成项目, 提升应用性能, 间接提高用户体验. 本文借用Demo, 讲解ContentProvider共享数据的要点.

本文源码的GitHub[下载地址](https://github.com/SpikeKing/ContentProviderDemo)

---

## SQLite

ContentProvider需要媒介进行数据存储, 最常用的就是SQLite数据库. 

SQLite数据库继承``SQLiteOpenHelper``类, 提供数据库名称, 表名, 版本. 在onCreate方法中, 创建数据库表, 添加字段.

本示例使用两张表, 书籍和用户.

``` java
public class DbOpenHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "book_provider.db";
    public static final String BOOK_TABLE_NAME = "book";
    public static final String USER_TABLE_NAME = "user";

    private static final int DB_VERSION = 1;

    private String CREATE_BOOK_TABLE = "CREATE TABLE IF NOT EXISTS "
            + BOOK_TABLE_NAME + "(_id INTEGER PRIMARY KEY, name TEXT)";

    private String CREATE_USER_TABLE = "CREATE TABLE IF NOT EXISTS "
            + USER_TABLE_NAME + "(_id INTEGER PRIMARY KEY, name TEXT, sex INT)";

    public DbOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BOOK_TABLE);
        db.execSQL(CREATE_USER_TABLE);
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
```

> 直接使用数据库的情况较少, 也比较复杂, 推荐使用一些经典ORM数据库, 如[Sugar](https://github.com/satyan/sugar)等, 简化管理. ORM, 即对象关系映射.

---

## ContentProvider

ContentProvider提供数据访问的接口, CRUD增删改查. 在onCreate中, 初始化数据库, 并添加数据.

``` java
@Override public boolean onCreate() {
    showLogs("onCreate 当前线程: " + Thread.currentThread().getName());
    mContext = getContext();

    initProviderData(); // 初始化Provider数据

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
```

CRUD的参数是Uri, 数据库需要使用表名, 为了便于从Uri映射到表名, 使用关系转换.

``` java
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
```

添加数据``insert``, 可以注册内容改变的监听, 插入数据时, 广播更新, 即``notifyChange``.

``` java
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
```

删除数据``delete``, 返回删除数据的数量, 大于0即删除成功.

``` java
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
```

修改数据``update``, 与删除类似, 返回修改数据的数量.

``` java
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
```

查询数据``query``, 返回数据库的游标, 处理数据.

``` java
@Nullable @Override
public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
    showLogs("query 当前线程: " + Thread.currentThread().getName());
    String tableName = getTableName(uri);
    if (TextUtils.isEmpty(tableName)) {
        throw new IllegalArgumentException("Unsupported URI: " + uri);
    }
    return mDb.query(tableName, projection, selection, selectionArgs, null, null, sortOrder, null);
}
```

> 注意Uri和表名的转换可能为空, 使用``TextUtils.isEmpty``判空.

---

## 共享数据

使用ContentProvider的独立进程, 模拟进程间共享数据.

``` xml
<provider
    android:name=".BookProvider"
    android:authorities="org.wangchenlong.book.provider"
    android:permission="org.wangchenlong.BOOK_PROVIDER"
    android:process=":provider"/>
```

> 在AndroidManifest中, 把Provider注册在``:provider``进程中, 与主进程分离.

添加数据, 通过Uri找到ContentProvider, 使用``ContentResolver``的``insert``方法, 添加``ContentValues``数据. 

``` java
public void addBooks(View view) {
    Uri bookUri = BookProvider.BOOK_CONTENT_URI;
    ContentValues values = new ContentValues();
    values.put("_id", 6);
    values.put("name", "信仰上帝");
    getContentResolver().insert(bookUri, values);
}
```

查询数据``query``, 与数据库的使用方式类似, 解析出Cursor, 通过移动Cursor, 找到所有匹配的结果.

``` java
public void showBooks(View view) {
    String content = "";
    Uri bookUri = BookProvider.BOOK_CONTENT_URI;
    Cursor bookCursor = getContentResolver().query(bookUri, new String[]{"_id", "name"}, null, null, null);
    if (bookCursor != null) {
        while (bookCursor.moveToNext()) {
            Book book = new Book();
            book.bookId = bookCursor.getInt(0);
            book.bookName = bookCursor.getString(1);
            content += book.toString() + "\n";
            Log.e(TAG, "query book: " + book.toString());
            mTvShowBooks.setText(content);
        }
        bookCursor.close();
    }
}
```

---

效果

![效果](https://raw.githubusercontent.com/SpikeKing/ContentProviderDemo/master/articles/demo-anim.gif)

ContentProvider封装了跨进程共享的逻辑, 我们只需要Uri即可访问数据, 使用共享数据非常便捷, 需要掌握简单的使用方式.

OK, that's all! Enjoy it!

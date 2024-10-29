package ProjectDodaSQL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;



public class Database extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "messageDB"; // データベースの名前
    private static final int DATABASE_VERSION = 2; // データベースのバージョン

    // テーブル情報の定義
    public static final String TABLE_MESSAGES = "board"; // テーブル名
    public static final String COLUMN_ID = "ID";         // ID
    public static final String CHAT_MESSAGE = "message"; // メッセージ内容
    public static final String CHAT_DATE    = "date";    // 送信日
    public static final String CHAT_TIME    = "time";    // 送信時間
    public static final String LATITUDE = "latitude";    // 緯度
    public static final String LONGITUDE = "longitude";  // 経度

    private static final String CREATE_TABLE_MESSAGES = "create table " + TABLE_MESSAGES + " ("
            + COLUMN_ID + " integer primary key autoincrement NOT NULL, "
            + CHAT_MESSAGE + " text NOT NULL, "
            + CHAT_DATE + " text, "
            + CHAT_TIME + " text, "
            + LATITUDE + " real,"
            + LONGITUDE + " real)"
            + ";";

    // コンストラクタ
    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // テーブル作成メソッド
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_MESSAGES); // テーブル作成
    }

    // データベースのバージョン更新メソッド
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        onCreate(db);
    }

    // メッセージをテーブルに挿入するメソッド
    public void insertMessage(String message, double latitude, double longitude) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        // 日付と時間を指定されたフォーマットで取得
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        values.put(CHAT_MESSAGE, message);  // メッセージ内容を保存
        values.put(CHAT_DATE, date);        // 送信日を保存
        values.put(CHAT_TIME, time);        // 送信時間を保存
        values.put(LATITUDE, latitude);     // 緯度を保存
        values.put(LONGITUDE, longitude);   // 経度を保存

        db.insert(TABLE_MESSAGES, null, values);
        db.close();  // データベースを閉じる
    }

    // 全メッセージを取得するメソッド
    public List<String> getAllMessages() {
        List<String> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT " + CHAT_MESSAGE + ", " + CHAT_DATE + ", " + CHAT_TIME + " FROM " + TABLE_MESSAGES;
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                String message = cursor.getString(cursor.getColumnIndexOrThrow(CHAT_MESSAGE));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(CHAT_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(CHAT_TIME));
                messages.add("[" + date + " " + time + "] " + message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return messages;
    }
}
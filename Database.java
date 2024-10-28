package com.example.projectdodasql;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import java.util.List;
import java.util.ArrayList;
import android.database.Cursor;



public class Database extends SQLiteOpenHelper{

    private static final String DATABASE_NAME = "messageDB"; // データベースの名前
    private static final int DATABASE_VERSION = 1; // データベースのバージョン

    // テーブル情報の定義
    public static final String TABLE_MESSAGES = "board"; // テーブル名
    public static final String COLUMN_ID = "ID";         // ID
    public static final String CHAT_MESSAGE = "message"; // メッセージ内容
    public static final String CHAT_DATE    = "date";    // 送信日
    public static final String LATITUDE = "latitude";    // 緯度
    public static final String LONGITUDE = "longitude";  // 経度

    private static final String CREATE_TABLE_MESSAGES = "create table " + TABLE_MESSAGES + " ("
            + COLUMN_ID + " integer primary key autoincrement NOT NULL, "
            + CHAT_MESSAGE + " text NOT NULL, "
            + CHAT_DATE + " datetime default current_timestamp, "
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
    public void insertMessage(String message, double latitude, double longitude){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(CHAT_MESSAGE, message);  // メッセージ内容を保存
        values.put(LATITUDE, latitude);     // 緯度を保存
        values.put(LONGITUDE, longitude);   // 経度を保存

        db.insert(TABLE_MESSAGES, null, values);
        db.close();  // データベースを閉じる
    }

    // tuika
    public List<String> getAllMessages() {
        List<String> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        String selectQuery = "SELECT message FROM board";
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                messages.add(cursor.getString(cursor.getColumnIndexOrThrow("message")));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();

        return messages;
    }

}

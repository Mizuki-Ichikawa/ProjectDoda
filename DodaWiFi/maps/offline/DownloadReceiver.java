package com.example.ddwifi4;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

public class DownloadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("DownloadReceiver", "onReceive called");

        long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
        Log.d("DownloadReceiver", "Received download ID: " + id);

        SharedPreferences prefs = context.getSharedPreferences("com.example.ddwifi4", Context.MODE_PRIVATE);
        long downloadId = prefs.getLong("downloadId", -1);
        Log.d("DownloadReceiver", "Expected download ID: " + downloadId);

        if (id == downloadId) {
            // ダウンロードのステータスを取得
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor cursor = manager.query(query);
            if (cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                int status = cursor.getInt(statusIndex);
                int reason = cursor.getInt(reasonIndex);
                Log.d("DownloadReceiver", "Download status: " + status);
                Log.d("DownloadReceiver", "Download reason: " + reason);

                if (status == DownloadManager.STATUS_SUCCESSFUL) {
                    // ダウンロード成功
                    Toast.makeText(context, "ダウンロードが完了しました。", Toast.LENGTH_SHORT).show();
                    Intent mapIntent = new Intent(context, MapActivity.class);
                    mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(mapIntent);
                } else {
                    // ダウンロード失敗
                    String reasonText = getDownloadFailureReason(reason);
                    Toast.makeText(context, "ダウンロードが失敗しました。理由：" + reasonText, Toast.LENGTH_LONG).show();
                    Log.d("DownloadReceiver", "Download failed: " + reasonText);
                }
            } else {
                Log.d("DownloadReceiver", "Cursor is empty.");
            }
            cursor.close();
        }
    }

    // ダウンロード失敗の理由を取得するメソッド
    private String getDownloadFailureReason(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                return "再開できません";
            case DownloadManager.ERROR_DEVICE_NOT_FOUND:
                return "デバイスが見つかりません";
            case DownloadManager.ERROR_FILE_ALREADY_EXISTS:
                return "ファイルが既に存在します";
            case DownloadManager.ERROR_FILE_ERROR:
                return "ファイルエラー";
            case DownloadManager.ERROR_HTTP_DATA_ERROR:
                return "HTTPデータエラー";
            case DownloadManager.ERROR_INSUFFICIENT_SPACE:
                return "空き容量不足";
            case DownloadManager.ERROR_TOO_MANY_REDIRECTS:
                return "リダイレクトが多すぎます";
            case DownloadManager.ERROR_UNHANDLED_HTTP_CODE:
                return "未処理のHTTPコード";
            case DownloadManager.ERROR_UNKNOWN:
                return "不明なエラー";
            default:
                return "エラーコード：" + reason;
        }
    }
}

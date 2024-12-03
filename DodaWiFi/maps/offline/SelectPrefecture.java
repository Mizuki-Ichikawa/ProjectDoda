package com.example.ddwifi4;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class SelectPrefecture extends AppCompatActivity {

    // 都道府県名とマップファイルの URL
    private Map<String, String> prefectureMapUrls;
    private long downloadId; // ダウンロードID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectprefecture);

        LinearLayout linearLayout = findViewById(R.id.linearLayout);
        // 「完了」ボタンの参照を取得
        Button btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(v -> {
            // MapActivity を起動
            Intent intent = new Intent(SelectPrefecture.this, MapActivity.class);
            startActivity(intent);
            // 現在のアクティビティを終了
            finish();
        });

        // マップファイルの URL を設定
        prefectureMapUrls = new HashMap<>();
        prefectureMapUrls.put("hokkaido", "https://download.mapsforge.org/maps/v5/asia/japan/hokkaido.map"); // 157MB
        prefectureMapUrls.put("tohoku", "https://download.mapsforge.org/maps/v5/asia/japan/tohoku.map"); // 213MB
        prefectureMapUrls.put("kanto", "https://download.mapsforge.org/maps/v5/asia/japan/kanto.map");   // 356MB
        prefectureMapUrls.put("chubu", "https://download.mapsforge.org/maps/v5/asia/japan/chubu.map");   // 329MB
        prefectureMapUrls.put("kansai", "https://download.mapsforge.org/maps/v5/asia/japan/kansai.map");   // 230MB
        prefectureMapUrls.put("chugoku", "https://download.mapsforge.org/maps/v5/asia/japan/chugoku.map"); // 121MB
        prefectureMapUrls.put("shikoku", "https://download.mapsforge.org/maps/v5/asia/japan/shikoku.map"); // 121MB
        prefectureMapUrls.put("kyushu", "https://download.mapsforge.org/maps/v5/asia/japan/kyushu.map");  // 204MB




        for (String region : prefectureMapUrls.keySet()) {
            Button button = new Button(this);
            button.setText(region + " の地図をダウンロード");
            button.setOnClickListener(v -> {
                String mapUrl = prefectureMapUrls.get(region);
                downloadMapFile(region + ".map", mapUrl);

                // ダウンロード完了後に選択した地域名を MapActivity に渡す
                Intent intent = new Intent(SelectPrefecture.this, MapActivity.class);
                intent.putExtra("selectedRegion", region+ ".map");  // 選択した地域を渡す
                startActivity(intent);
                finish();

            });
            linearLayout.addView(button);
        }

        // ダウンロード完了を検知する BroadcastReceiver を登録
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), null, null, Context.RECEIVER_NOT_EXPORTED);
    }

    // ダウンロード完了時の BroadcastReceiver を追加
    private final BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (downloadId == id) {
                // ダウンロードが完了した場合、MapActivity を起動
                Toast.makeText(SelectPrefecture.this, "ダウンロードが完了しました。", Toast.LENGTH_SHORT).show();

                // ダウンロード完了後に選択した地域名を MapActivity に渡す
                String selectedRegion = getIntent().getStringExtra("selectedRegion");  // 選択した地域を取得
                Intent mapIntent = new Intent(SelectPrefecture.this, MapActivity.class);
                mapIntent.putExtra("selectedRegion", selectedRegion);  // 選択した地域を渡す
                startActivity(mapIntent);

                // 現在のアクティビティを終了
                finish();
            }
        }
    };


    private void downloadMapFile(String fileName, String url) {
        // ダウンロードマネージャーを使用してファイルをダウンロード
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle(fileName);
        request.setDescription("マップファイルをダウンロードしています");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // ダウンロード先を設定（外部ファイルディレクトリ）
        request.setDestinationInExternalFilesDir(this, null, fileName);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);

        Toast.makeText(this, "ダウンロードを開始しました", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onDownloadComplete);
    }
}

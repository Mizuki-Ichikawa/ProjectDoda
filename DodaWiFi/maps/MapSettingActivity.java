package com.example.ddwifi4;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageButton;
import android.content.Intent;
import android.view.View;

import android.app.AlertDialog;

public class MapSettingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapsetting);

        // 掲示板ページに戻るメソッド
        ImageButton btnReturnToBoard = findViewById(R.id.btnReturnToBoard);
        btnReturnToBoard.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapSettingActivity.this, Database.class);
                startActivity(intent);
                finish();
            }
        });

        // ホーム（通信ページ）に戻るメソッド
        ImageButton btnReturnToHome = findViewById(R.id.btnReturnToHome);
        btnReturnToHome.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MapSettingActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 地図ページに戻るメソッド
        ImageButton btnReturnToMap = findViewById(R.id.btnReturnToMap);
        btnReturnToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MapSettingActivity.this, MapActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // 地図1追加ボタン押された時
        ImageButton btnAddMap1 = findViewById(R.id.btnAddMap1);
        btnAddMap1.setOnClickListener(v -> {
            showDownLoadChecking("地図1", () -> {
                showByteChecking(500, () -> {
                    showOverwrapChecking("地図1", () -> {
                        showFinishAddMap("地図1", null);
                    });
                });
            });

        });

        // 地図2追加ボタン押された時
        ImageButton btnAddMap2 = findViewById(R.id.btnAddMap2);
        btnAddMap2.setOnClickListener(v -> {
            NotConnectedInternet("map1", () ->{
                ShortageStorage(() ->{
                    FailDownloadMap("map1", null);
                });
            });
        });

        // 地図3追加ボタン押された時
        ImageButton btnAddMap3 = findViewById(R.id.btnAddMap3);
        btnAddMap3.setOnClickListener(v -> {
            // 処理内容を追加
        });

    }

    // 「地図をダウンロードしますか」のUI
    private void showDownLoadChecking(String mapName, Runnable onConfirmed){
        new AlertDialog.Builder(this)
                .setTitle("ダウンロード確認")
                .setMessage(mapName + "をダウンロードしますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    // ダウンロード範囲の選択
                    if(onConfirmed != null) onConfirmed.run();
                })
                .setNegativeButton("いいえ", (dialog, which) -> {
                    showStoppingAction(null);
                })
                .show();
    }

    // 地図の上書き確認UI
    private void showOverwrapChecking(String mapName, Runnable onConfirmed){
        new AlertDialog.Builder(this)
                .setTitle("上書き確認")
                .setMessage(mapName + "を上書きしますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    // 既存のMapを削除、ダウンロード範囲の選択
                    if(onConfirmed != null) onConfirmed.run();
                })
                .setNegativeButton("いいえ", (dialog, which) -> {
                    showStoppingAction(null);
                })
                .show();
    }

    // バイト数の確認UI
    private void showByteChecking(int mapByte, Runnable onConfirmed){
        new AlertDialog.Builder(this)
                .setTitle("データサイズ確認")
                .setMessage("ダウンロードする地図は、" + mapByte + "byteです。\n\n" +
                            "この地図をダウンロードしますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    // インターネットの確認、空き容量の確認、地図のダウンロード開始
                    if(onConfirmed != null) onConfirmed.run();
                })
                .setNegativeButton("いいえ", (dialog, which) -> {
                    showStoppingAction(null);
                })
                .show();
    }

    // 追加完了UI
    private void showFinishAddMap(String mapName, Runnable onConfirmed){
        new AlertDialog.Builder(this)
                .setTitle("ダウンロード完了")
                .setMessage(mapName + "のダウンロードが完了しました。")
                .setPositiveButton("確認", (dialog, which) -> {
                    // 何もしない
                    if(onConfirmed != null) onConfirmed.run();
                })
                .show();
    }

    private void showStoppingAction(Runnable onConfirmed){
        new AlertDialog.Builder(this)
                .setTitle("処理中止")
                .setMessage("処理を中止しました。")
                .setPositiveButton("確認", (dialog, which) -> {
                    // 何もしない
                    if(onConfirmed != null) onConfirmed.run();
                })
                .show();
    }

    // エラー文1(インターネットに接続されてないケース)
    private void NotConnectedInternet(String mapName, Runnable onRetry){
        new AlertDialog.Builder(this)
                .setTitle("エラー")
                .setMessage("インターネットに接続されていないため、" + mapName + "をダウンロードできません。\n\n" +
                            "インターネットの接続状況を確認してください。")
                .setIcon(R.drawable.baseline_error_24)
                .setPositiveButton("確認", (dialog, which) -> {
                    // 何もしない
                    if(onRetry != null) onRetry.run();
                })
                .show();
    }

    // エラー文2(空き容量が足りていないケース)
    private void ShortageStorage(Runnable onRetry){
        new AlertDialog.Builder(this)
                .setTitle("エラー")
                .setMessage("端末に空き容量が不足しています。\n\n" +
                            "空き容量を確保してください。")
                .setIcon(R.drawable.baseline_error_24)
                .setPositiveButton("確認", (dialog, which) -> {
                    // 何もしない
                    if(onRetry != null) onRetry.run();
                })
                .show();
    }

    // エラー文3(地図のダウンロードに失敗したケース)
    private void FailDownloadMap(String mapName, Runnable onRetry){
        new AlertDialog.Builder(this)
                .setTitle("エラー")
                .setMessage(mapName + "のダウンロードに失敗しました。\n\n" +
                        "もう一度地図をダウンロードしてください。")
                .setIcon(R.drawable.baseline_error_24)
                .setPositiveButton("確認", (dialog, which) -> {
                    // 何もしない
                    if(onRetry != null) onRetry.run();
                })
                .show();
    }


}

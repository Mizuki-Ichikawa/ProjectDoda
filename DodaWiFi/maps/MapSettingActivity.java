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
            showDownLoadChecking("地図1");
        });

        // 地図2追加ボタン押された時
        ImageButton btnAddMap2 = findViewById(R.id.btnAddMap2);
        btnAddMap2.setOnClickListener(v -> {
            showDownLoadChecking("地図2");
        });

        // 地図3追加ボタン押された時
        ImageButton btnAddMap3 = findViewById(R.id.btnAddMap3);
        btnAddMap3.setOnClickListener(v -> {
            showDownLoadChecking("地図3");
        });

    }

    // 「地図をダウンロードしますか」のUI
    private void showDownLoadChecking(String mapName){
        new AlertDialog.Builder(this)
                .setTitle("ダウンロード確認")
                .setMessage(mapName + "をダウンロードしますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    // ダウンロード範囲の選択
                })
                .setNegativeButton("いいえ", (dialog, which) -> {
                    showStoppingAction();
                })
                .show();
    }

    // 地図の上書き確認UI
    private void showOverwrapChecking(String mapName){
        new AlertDialog.Builder(this)
                .setTitle("上書き確認")
                .setMessage(mapName + "を上書きしますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    // 既存のMapを削除、ダウンロード範囲の選択
                })
                .setNegativeButton("いいえ", (dialog, which) -> {
                    showStoppingAction();
                })
                .show();
    }

    // バイト数の確認UI
    private void showByteChecking(int mapByte){
        new AlertDialog.Builder(this)
                .setTitle("データサイズ確認")
                .setMessage("ダウンロードする地図は、" + mapByte + "です。\n" +
                            "この地図をダウンロードしますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    // インターネットの確認、空き容量の確認、地図のダウンロード開始
                })
                .setNegativeButton("いいえ", (dialog, which) -> {
                    showStoppingAction();
                })
                .show();
    }

    // 追加完了UI
    private void showFinishAddMap(String mapName){
        new AlertDialog.Builder(this)
                .setTitle("ダウンロード完了")
                .setMessage(mapName + "のダウンロードが完了しました。")
                .setPositiveButton("確認", (dialog, which) -> {
                    // 何もしない
                })
                .show();
    }

    private void showStoppingAction(){
        new AlertDialog.Builder(this)
                .setTitle("処理中止")
                .setMessage("処理を中止しました。")
                .setPositiveButton("確認", (dialog, which) -> {
                    // 何もしない
                })
                .show();
    }



}

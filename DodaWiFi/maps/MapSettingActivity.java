package com.example.ddwifi4;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.os.StatFs;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.content.Intent;
import android.view.View;

import android.app.AlertDialog;

import org.osmdroid.tileprovider.MapTileProviderBasic;
import org.osmdroid.tileprovider.modules.TileDownloader;
import org.osmdroid.tileprovider.modules.TileWriter;
import org.osmdroid.util.TileSystem;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.views.overlay.Polygon;
import android.graphics.Color;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import android.widget.Toast;


public class MapSettingActivity extends AppCompatActivity {
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapsetting);

        // MapViewの初期化
        mapView = findViewById(R.id.mapView);
        if(mapView != null){
            mapView.setBuiltInZoomControls(true);
            mapView.setMultiTouchControls(true);
        }else{
            throw new IllegalStateException("MapViewが正しく初期化されていません");
        }

        // 掲示板ページに戻るメソッド
        ImageButton btnReturnToBoard = findViewById(R.id.btnReturnToBoard);
        btnReturnToBoard.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MapSettingActivity.this, Database.class);
                //startActivity(intent);
                //finish();
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
            });

        });

        // 地図2追加ボタン押された時
        ImageButton btnAddMap2 = findViewById(R.id.btnAddMap2);
        btnAddMap2.setOnClickListener(v -> {

        });

        // 地図3追加ボタン押された時
        ImageButton btnAddMap3 = findViewById(R.id.btnAddMap3);
        btnAddMap3.setOnClickListener(v -> {
            // 処理内容を追加
            showDownLoadChecking("地図3", () -> {
            });
        });

    }

    // 「地図をダウンロードしますか」のUI
    private void showDownLoadChecking(String mapName, Runnable onConfirmed){
        new AlertDialog.Builder(this)
                .setTitle("ダウンロード確認")
                .setMessage(mapName + "をダウンロードしますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    // ダウンロード選択ページへの遷移
                    moveToRangeSelection(mapName);
                    if(onConfirmed != null) onConfirmed.run();
                })
                .setNegativeButton("いいえ", (dialog, which) -> {
                    showStoppingAction(null);
                })
                .show();
    }

    // 範囲選択ページへの遷移
    private void moveToRangeSelection(String mapName) {
        Intent intent = new Intent(MapSettingActivity.this, SelectPrefecture.class);
        intent.putExtra("mapName", mapName); // 地図名を次のページに渡す
        startActivity(intent);
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
}

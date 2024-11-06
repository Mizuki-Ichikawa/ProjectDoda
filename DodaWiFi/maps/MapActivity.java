package com.example.ddwifi4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;

public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private DatabaseHelper dbHelper;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // OSMDroidの設定
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());


        // MapViewの初期化
        mapView = findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        // 権限のリクエスト
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });

        // 地図の初期位置を設定
        mapView.getController().setZoom(15.0);
        mapView.getController().setCenter(new org.osmdroid.util.GeoPoint(35.6895, 139.6917)); // 東京の座標（例）
        dbHelper = new DatabaseHelper(this);

        // ピンを立てる例
        Marker marker = new Marker(mapView);
        marker.setPosition(new org.osmdroid.util.GeoPoint(35.6895, 139.6917)); // 東京の座標（例）
        marker.setTitle("Tokyo");
        marker.setSubDescription("This is Tokyo.");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        mapView.getOverlays().add(marker);

        // 戻るボタンの設定
        Button btnReturnToBoard = findViewById(R.id.btnReturnToBoard);
        btnReturnToBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 掲示板のページ（Databaseアクティビティ）に戻る
                Intent intent = new Intent(MapActivity.this, Database.class);
                startActivity(intent);
                finish();
            }
        });
        // 📍
        getData();
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_REQUEST_CODE);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            boolean permissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    permissionsGranted = false;
                    break;
                }
            }
            if (!permissionsGranted) {
                Toast.makeText(this, "必要な権限が付与されていません", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void getData() {
        // データベースから投稿を取得
        List<Post> posts = dbHelper.getAllPosts();

        for (Post post : posts) {
            double latitude = post.getLatitude();
            double longitude = post.getLongitude();
            String content = post.getContent();

            // 緯度と経度が存在する場合、地図上にマーカーを追加
            if (latitude != 0.0 && longitude != 0.0) {
                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(latitude, longitude));
                marker.setTitle(content); // マーカーのタイトルとして投稿内容を表示
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
            }
        }
        mapView.invalidate(); // 地図を再描画
    }

    @Override
    protected void onResume() {
        super.onResume();

        mapView.getOverlays().clear(); // 既存のオーバーレイをクリア
        getData(); // 最新のデータでマーカーを再描画
        mapView.invalidate(); // 再描画
    }



    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause(); // OSMDroidのライフサイクル管理
    }
}
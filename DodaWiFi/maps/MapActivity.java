package com.example.ddwifi4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import android.widget.ImageButton;


public class MapActivity extends AppCompatActivity {

    private MapView mapView;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private DatabaseHelper dbHelper;
    private LocationManager locationManager;

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
        ImageButton btnReturnToBoard = findViewById(R.id.btnReturnToBoard);
        btnReturnToBoard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 掲示板のページ（Databaseアクティビティ）に戻る
                Intent intent = new Intent(MapActivity.this, Database.class);
                startActivity(intent);
                finish();
            }
        });

        // 位置情報サービスを初期化
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 現在位置を取得
        requestCurrentLocation();
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

    private void requestCurrentLocation() {
        // 権限が付与されているか確認
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 必要な場合は権限をリクエスト
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS_REQUEST_CODE);
            return;
        }

        // 位置情報をリクエスト
        locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000, // 更新間隔（ミリ秒）
                10,   // 更新距離（メートル）
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        // 現在地の更新
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        // 位置情報が取得されているか確認するログ
                        Log.d("LocationUpdate", "Latitude: " + latitude + ", Longitude: " + longitude);
                        showCurrentLocationMarker(latitude, longitude);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {}

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {}

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {}
                }
        );
    }

    private void showCurrentLocationMarker(double latitude, double longitude) {
        // 現在地のマーカーを作成
        Marker currentLocationMarker = new Marker(mapView);
        currentLocationMarker.setPosition(new GeoPoint(latitude, longitude));
        currentLocationMarker.setTitle("現在地");
        currentLocationMarker.setIcon(getResources().getDrawable(R.drawable.marker_blue)); // 自分用の📍アイコン
        currentLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

        // マーカーを地図に追加
        mapView.getOverlays().add(currentLocationMarker);
        mapView.getController().setCenter(new GeoPoint(latitude, longitude)); // 地図を現在地に移動
        mapView.invalidate();
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
            int likeCount = post.getLikeCount(); // いいね数を取得

            // 緯度と経度が存在する場合、地図上にマーカーを追加
            if (latitude != 0.0 && longitude != 0.0) {
                Marker marker = new Marker(mapView);
                marker.setPosition(new GeoPoint(latitude, longitude));
                marker.setTitle(content);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                // いいね数に応じてマーカーの色を設定
                if (likeCount == 0) {
                    marker.setIcon(getResources().getDrawable(R.drawable.marker_green));
                } else if (likeCount == 1) {
                    marker.setIcon(getResources().getDrawable(R.drawable.marker_light_green));
                } else if (likeCount == 2) {
                    marker.setIcon(getResources().getDrawable(R.drawable.marker_yellow));
                } else if (likeCount == 3) {
                    marker.setIcon(getResources().getDrawable(R.drawable.marker_orange));
                } else {
                    marker.setIcon(getResources().getDrawable(R.drawable.marker_red));
                }

                mapView.getOverlays().add(marker); // マーカーを地図に追加
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
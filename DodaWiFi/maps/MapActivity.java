package com.example.ddwifi4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import org.osmdroid.config.Configuration;
import org.osmdroid.mapsforge.BuildConfig;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import android.widget.ImageButton;


public class MapActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET
    };
    private MapView mapView;
    private DatabaseHelper dbHelper;
    private LocationManager locationManager;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 権限をリクエスト
        requestPermissionsIfNeeded();

        // OSMDroidのキャッシュディレクトリを指定
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        File osmdroidBasePath = new File(getFilesDir(), "osmdroid");
        Configuration.getInstance().setOsmdroidBasePath(osmdroidBasePath);
        Configuration.getInstance().setOsmdroidTileCache(new File(osmdroidBasePath, "cache"));

        dbHelper = new DatabaseHelper(this);

        // 掲示板ページに戻る
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

        // 地図設定ボタンに遷移
        ImageButton btnMoveToSetting = findViewById(R.id.btnMoveToSetting);
        btnMoveToSetting.setOnClickListener(new View.OnClickListener(){;
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MapActivity.this, MapSettingActivity.class);
                startActivity(intent);
            }
        });

        // 位置情報サービスを初期化
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 現在位置を取得
        requestCurrentLocation();
        // 📍
        getData();
    }
    // 権限に関するプログラム
    // 権限のリクエストをするメソッド
    private void requestPermissionsIfNeeded() {
        // 必要な権限が付与されていない場合にリクエスト
        List<String> missingPermissions = new ArrayList<>();
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }
        // 権限が不足している場合のみリクエストを実行
        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    missingPermissions.toArray(new String[0]),
                    PERMISSIONS_REQUEST_CODE);
        } else {
            onPermissionsGranted();
        }
    }
    // ユーザーが権限を拒否した場合の処理
    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("権限が必要です")
                .setMessage("地図データを利用するためには、位置情報とストレージの権限が必要です。設定から権限を付与してください。")
                .setPositiveButton("設定に移動", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", getPackageName(), null));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void onPermissionsGranted() {
        // 権限が付与された後の処理を記述
        initializeMap();
    }
    // リクエスト結果確認
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                Toast.makeText(this, "すべての必要な権限が付与されました", Toast.LENGTH_SHORT).show();
                onPermissionsGranted();
            } else {
                Toast.makeText(this, "必要な権限が付与されていません", Toast.LENGTH_SHORT).show();
                showPermissionRationale();
            }
        }
    }


    // 地図情報の初期化
    private void initializeMap() {
        // OSMDroidの設定
        Configuration.getInstance().setUserAgentValue(getApplicationContext().getPackageName());

        // MapViewの初期化
        mapView = findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true); // ズームコントロールを有効化
        mapView.setMultiTouchControls(true); // マルチタッチ操作を有効化

        // 地図の初期位置を設定 (東京の例)
        GeoPoint initialPoint = new GeoPoint(35.6895, 139.6917); // 緯度経度
        mapView.getController().setCenter(initialPoint); // 地図の中心を設定
        mapView.getController().setZoom(15.0); // 初期ズームレベルを設定
    }

    // データベース関連
    private void requestCurrentLocation() {
        // 権限が付与されているか確認
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 必要な場合は権限をリクエスト
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_CODE );
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

    // 情報の価値の重み付けメソッド
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
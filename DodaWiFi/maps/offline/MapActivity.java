package com.example.ddwifi4;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.osmdroid.config.Configuration;
import org.osmdroid.mapsforge.MapsForgeTileProvider;
import org.osmdroid.mapsforge.MapsForgeTileSource;
import org.osmdroid.tileprovider.modules.IFilesystemCache;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.util.List;

public class MapActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private MapView mapView;
    private String mapFileName = "kanto.map"; // 使用するマップファイル名を英数字に変更
    private DatabaseHelper dbHelper;
    private LocationManager locationManager;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);


        // OSMDroid の設定
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // AndroidGraphicFactory の初期化
        AndroidGraphicFactory.createInstance(this.getApplication());

        // MapView の初期化
        mapView = findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);

        dbHelper = new DatabaseHelper(this);



        // 地図設定ページに遷移するボタンの設定
        ImageButton btnMoveToSetting = findViewById(R.id.btnMoveToSetting);
        btnMoveToSetting.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, MapSettingActivity.class);
            startActivity(intent);
        });

        // 掲示板に戻るボタンの設定
        ImageButton btnReturnToBoard = findViewById(R.id.btnReturnToBoard);
        btnReturnToBoard.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, Database.class);
            startActivity(intent);
        });


        // 受け取った地域名を確認して地図ファイルを設定
        Intent intent = getIntent();
        String selectedRegion = intent.getStringExtra("selectedRegion");
        if (selectedRegion != null) {
            mapFileName = selectedRegion; // 選択された地域に対応するファイル名
        }else{ // elseから追加いらないかも
            loadOnlineMap();
            //mapFileName = "default.map";
        }


        // マップをロード
        loadMap();

        // 位置情報サービスを初期化
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        // 現在位置を取得
        requestCurrentLocation();
        // 📍
        getData();
    }

    // 追加いらないかも
    private void loadOnlineMap() {
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

    private void loadMap() {
        // インターネット接続をチェック
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();

        // 外部ファイルディレクトリを取得
        File externalFilesDir = getExternalFilesDir(null);

        // マップファイルへのパスを作成
        File mapFile = new File(externalFilesDir, mapFileName);
        Log.d("MapActivity", "Map file path: " + mapFile.getAbsolutePath());
        Log.d("MapActivity", "Map file exists: " + mapFile.exists());

        if (isConnected) {
            // インターネット接続されている場合、オンラインマップを使用
            loadOnlineMap();

            // もしオフラインマップが存在しない場合、ダウンロード画面に遷移
            /*
            if (!mapFile.exists()) {
                Intent intent = new Intent(this, SelectPrefecture.class);
                startActivity(intent);
            }

             */
        } else {
            // インターネット未接続の場合、オフラインマップを使用
            if (mapFile.exists()) {
                // マップファイルが存在する場合、オフラインマップを読み込む
                loadMapFromMapsforge(mapFile);
            } else {
                // オフラインマップが存在しない場合、アラートダイアログを表示
                showOfflineMapDownloadDialog();
            }
        }
    }

    private void showOfflineMapDownloadDialog() {
        new AlertDialog.Builder(this)
                .setTitle("オフラインマップ未ダウンロード")
                .setMessage("オフラインマップのダウンロードがされていません。地図を表示するには、ダウンロードが必要です。")
                .setPositiveButton("OK", (dialog, which) -> {
                    // ダウンロード画面に遷移など
                    Intent intent = new Intent(this, SelectPrefecture.class);
                    startActivity(intent);
                })
                .setCancelable(false)
                .show();
    }


    private void loadMapFromMapsforge(File mapFile) {
        // Mapsforge のタイルソースを作成
        MapsForgeTileSource.createInstance(this.getApplication());
        File[] mapFiles = {mapFile};

        // MapsForgeTileSourceの作成 (テーマなしで作成)
        MapsForgeTileSource tileSource = MapsForgeTileSource.createFromFiles(
                mapFiles,
                null, // レンダリングテーマは後で設定
                "CustomMap" // 任意のタイルソース名
        );

        if (tileSource != null) {
            // タイルプロバイダを作成
            IFilesystemCache tileCache = null; // キャッシュを使用しない場合
            MapsForgeTileProvider tileProvider = new MapsForgeTileProvider(
                    new SimpleRegisterReceiver(this),
                    tileSource,
                    tileCache // キャッシュが必要ならここに指定
            );

            // MapView にタイルプロバイダを設定
            mapView.setTileProvider(tileProvider);

            // 地図の初期位置を設定（例：東京駅）
            GeoPoint initialPoint = new GeoPoint(35.681236, 139.767125);
            mapView.getController().setCenter(initialPoint);
            mapView.getController().setZoom(12.0);
        } else {
            // タイルソースの作成に失敗した場合の処理
            Toast.makeText(this, "タイルソースの作成に失敗しました。", Toast.LENGTH_LONG).show();
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        // AndroidGraphicFactory のリソースを解放
        AndroidGraphicFactory.clearResourceMemoryCache();
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

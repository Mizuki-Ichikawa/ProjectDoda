package com.example.ddwifi4;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.Manifest;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.AlertDialog;

import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.*;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.util.TileSystem;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.net.*;
import android.os.Environment;
import android.os.StatFs;

public class SelectRange extends AppCompatActivity {
    private MapView mapView;
    private GeoPoint startPoint;
    private GeoPoint endPoint;
    private ProgressBar progressBar;
    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 100;
    private boolean isSelectingRange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectrange);

        // ストレージ権限の確認
        checkStoragePermission();

        // MapViewの初期化
        mapView = findViewById(R.id.mapView); // ローカル変数を削除し、クラスフィールドを使用
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0); // 初期ズーム設定
        mapView.getController().setCenter(new GeoPoint(35.6895, 139.6917)); // 東京の座標を中央に設定

        // ProgressBarの初期化
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        progressBar.setMax(100);

        // 範囲選択を有効化
        enableRangeSelection();

        // 完了ボタンを押して設定ページに戻る
        Button btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(view -> {
            if (startPoint != null && endPoint != null) {
                Intent intent = new Intent(SelectRange.this, MapSettingActivity.class);
                intent.putExtra("startLat", startPoint.getLatitude());
                intent.putExtra("startLon", startPoint.getLongitude());
                intent.putExtra("endLat", endPoint.getLatitude());
                intent.putExtra("endLon", endPoint.getLongitude());
                startActivity(intent);
            } else {
                Toast.makeText(SelectRange.this, "範囲が選択されていません。", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ストレージの権限チェック
    private void checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION);
            }
        }
    }

    // 範囲選択関連プログラム
    // ダウンロード範囲を指定する
    public void enableRangeSelection() {
        isSelectingRange = true; // 範囲選択モードを有効化

        mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (isSelectingRange) {
                    if (startPoint == null) {
                        startPoint = p;
                        Toast.makeText(SelectRange.this, "範囲選択を開始しました", Toast.LENGTH_SHORT).show();
                    } else {
                        endPoint = p;

                        if (startPoint != null && endPoint != null) {
                            coveredSelectedMap(startPoint, endPoint);

                            // ダウンロード確認ダイアログを表示
                            showDownloadDialog();

                            // 範囲選択モードを終了
                            isSelectingRange = false;
                        }
                    }
                }
                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                // 長押しは無視（追加の動作が必要ならここで実装）
                return false;
            }
        }));
    }

    // ダウンロード範囲を半透明の四角のタイルで覆う
    private void coveredSelectedMap(GeoPoint startPoint, GeoPoint endPoint) {
        BoundingBox boundingBox = new BoundingBox(
                Math.max(startPoint.getLatitude(), endPoint.getLatitude()), // 北緯
                Math.max(startPoint.getLongitude(), endPoint.getLongitude()), // 東経
                Math.min(startPoint.getLatitude(), endPoint.getLatitude()), // 南緯
                Math.min(startPoint.getLongitude(), endPoint.getLongitude()) // 西経
        );

        Polygon polygon = new Polygon();
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(boundingBox.getLatNorth(), boundingBox.getLonWest())); // 北西
        points.add(new GeoPoint(boundingBox.getLatNorth(), boundingBox.getLonEast())); // 北東
        points.add(new GeoPoint(boundingBox.getLatSouth(), boundingBox.getLonEast())); // 南東
        points.add(new GeoPoint(boundingBox.getLatSouth(), boundingBox.getLonWest())); // 南西
        points.add(new GeoPoint(boundingBox.getLatNorth(), boundingBox.getLonWest())); // 閉じる

        polygon.setPoints(points);
        polygon.setStrokeColor(Color.RED);
        polygon.setFillColor(Color.argb(50, 0, 255, 0)); // 半透明の緑色
        mapView.getOverlays().add(polygon);
        mapView.invalidate();
    }


    // 推定ダウンロードサイズの計算
    private int calculateEstimatedSize(GeoPoint startPoint, GeoPoint endPoint) {
        if (startPoint == null || endPoint == null) {
            Log.e("CalculateSize", "開始点または終了点が設定されていません");
            return 0;
        }

        BoundingBox boundingBox = new BoundingBox(
            Math.max(startPoint.getLatitude(), endPoint.getLatitude()),
            Math.max(startPoint.getLongitude(), endPoint.getLongitude()),
            Math.min(startPoint.getLatitude(), endPoint.getLatitude()),
            Math.min(startPoint.getLongitude(), endPoint.getLongitude())
        );

        // タイル数を計算
        int zoomLevel = 15; // 必要に応じて調整
        int startX = calculateTileX(boundingBox.getLonWest(), zoomLevel);
        int endX = calculateTileX(boundingBox.getLonEast(), zoomLevel);
        int startY = calculateTileY(boundingBox.getLatNorth(), zoomLevel);
        int endY = calculateTileY(boundingBox.getLatSouth(), zoomLevel);

        int tileCountX = Math.abs(endX - startX) + 1;
        int tileCountY = Math.abs(endY - startY) + 1;
        int tileCount = tileCountX * tileCountY;

        // タイル1枚あたりのサイズ（例: 500 KB）
        int tileSize = 500; // KB
        return tileCount * tileSize; // 推定サイズ（KB）
    }

    // ダウンロードサイズ確認用のダイアログを表示
    private void showDownloadDialog() {
        if (startPoint == null || endPoint == null) {
            Toast.makeText(this, "範囲が正しく選択されていません。もう一度選択してください。", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("ダウンロード確認")
                .setMessage("選択した範囲の地図をダウンロードしますか？")
                .setPositiveButton("はい", (dialog, which) -> {
                    if (startPoint == null || endPoint == null) {
                        Toast.makeText(this, "範囲が選択されていません。処理を中止します。", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BoundingBox box = new BoundingBox(
                            Math.max(startPoint.getLatitude(), endPoint.getLatitude()),
                            Math.max(startPoint.getLongitude(), endPoint.getLongitude()),
                            Math.min(startPoint.getLatitude(), endPoint.getLatitude()),
                            Math.min(startPoint.getLongitude(), endPoint.getLongitude())
                    );

                    downloadMap(box, 10, 15, null);
                })
                .setNegativeButton("いいえ", null)
                .show();
    }

    // 端末がインターネットに接続されているかの確認
    private boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (manager != null) {
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                // API 23以降のチェック
                if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION);
                }
                NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));
            }else{
                // API 23以前のチェック
                NetworkInfo networkInfo = manager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnectedOrConnecting();
            }
        }
        return false; // `manager`がnullの場合は接続されていない
    }

    // 端末のストレージが足りるかどうかの確認
    private boolean isStorageAvailable(Context context, int mapKB){
        // アプリ専用外部ストレージのパスを取得
        File storageDir = getExternalFilesDir(null);

        if(storageDir != null && storageDir.exists()){
            StatFs stat = new StatFs(storageDir.getPath());

            // 空き容量を取得
            long availableBytes;
            if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){
                availableBytes = stat.getFreeBytes(); // API26
            }else{
                availableBytes = (long) stat.getAvailableBlocksLong()*stat.getBlockSizeLong();
            }

            long requiredBytes = mapKB*1024L; // change KB -> Bytes

            return availableBytes >= requiredBytes;
        }

        return false;
    }

    private File prepareStorageDirectory() {
        File storageDir = getExternalFilesDir(null);
        if (storageDir == null || !storageDir.exists()) {
            storageDir = new File(getFilesDir(), "offline_maps");
            if (!storageDir.mkdirs()) {
                Log.e("StorageError", "ストレージディレクトリの作成に失敗しました");
                return null;
            }
        }
        Log.d("StoragePath", "ストレージディレクトリ: " + storageDir.getAbsolutePath());
        return storageDir;
    }

    // 地図のダウンロード
    private void downloadMap(BoundingBox boundingBox, int minZoom, int maxZoom, Runnable onDownloadFinished) {
        Log.d("BoundingBox", "North: " + boundingBox.getLatNorth() +
                ", South: " + boundingBox.getLatSouth() +
                ", East: " + boundingBox.getLonEast() +
                ", West: " + boundingBox.getLonWest());

        // ストレージディレクトリの準備
        File storageDir = prepareStorageDirectory();
        if (storageDir == null) {
            Toast.makeText(this, "ストレージディレクトリの作成に失敗しました。", Toast.LENGTH_SHORT).show();
            return;
        }

        // MBTilesファイルの作成
        File mbTilesFile = new File(storageDir, "offline_map.mbtiles");
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(mbTilesFile, null);
        createMBTilesTables(db);

        // ProgressBarを表示する
        ProgressBar progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(100);
        progressBar.setProgress(0);

        new Thread(() -> {
            try {
                int totalTiles = 0;
                for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
                    int startX = calculateTileX(boundingBox.getLonWest(), zoom);
                    int endX = calculateTileX(boundingBox.getLonEast(), zoom);
                    int startY = calculateTileY(boundingBox.getLatNorth(), zoom);
                    int endY = calculateTileY(boundingBox.getLatSouth(), zoom);
                    totalTiles += (endX - startX + 1) * (endY - startY + 1);
                }

                // タイルがない場合のエラーチェック
                if (totalTiles == 0) {
                    Log.e("DownloadError", "総タイル数が0です。ダウンロードを中止します。");
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this, "ダウンロードするタイルがありません。", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                int[] currentTile = {0};
                for (int zoom = minZoom; zoom <= maxZoom; zoom++) {
                    int startX = calculateTileX(boundingBox.getLonWest(), zoom);
                    int endX = calculateTileX(boundingBox.getLonEast(), zoom);
                    int startY = calculateTileY(boundingBox.getLatNorth(), zoom);
                    int endY = calculateTileY(boundingBox.getLatSouth(), zoom);


                    for (int x = startX; x <= endX; x++) {
                        for (int y = startY; y <= endY; y++) {
                            String tileUrl = "https://tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png";
                            Log.d("TileCalculation", "Zoom: " + zoom + ", X: " + x + ", Y: " + y + ", URL: " + tileUrl);
                            byte[] tileData = null;
                            try {
                                tileData = downloadTile(tileUrl);
                            } catch (Exception e) {
                                Log.e("TileDownload", "タイルダウンロード中にエラー: URL=" + tileUrl + ", " + e.getMessage());
                                continue; // このタイルをスキップ
                            }

                            if (tileData != null) {
                                saveTileToMBTiles(db, zoom, x, y, tileData);
                            }
                            // プログレスを更新
                            currentTile[0]++;
                            int progress = (currentTile[0] * 100) / totalTiles;
                            runOnUiThread(() -> progressBar.setProgress(progress));

                            // **ここにログを追加**
                            Log.d("DownloadProgress", "Zoom: " + zoom + ", Current Tile: " + currentTile[0] + "/" + totalTiles);
                            // サーバー負荷軽減
                            Thread.sleep(500);
                            if (tileData == null) {
                                Log.e("TileDownload", "タイルデータがnull: URL=" + tileUrl);
                                continue; // 次のタイルに進む
                            }
                        }
                    }
                }

                // ダウンロード完了後の処理
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "地図のダウンロードが完了しました。", Toast.LENGTH_SHORT).show();
                    if (onDownloadFinished != null) {
                        onDownloadFinished.run();
                    }
                });
            } catch (Exception e) {
                Log.e("DownloadError", "地図ダウンロード中にエラーが発生しました", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    FailDownloadMap("選択した地図", null);
                });
            }
        }).start();
    }

    // タイルのX座標を計算
    private int calculateTileX(double longitude, int zoom) {
        return (int) Math.floor((longitude + 180) / 360 * Math.pow(2, zoom));
    }

    // タイルのY座標を計算
    private int calculateTileY(double latitude, int zoom) {
        double radLat = Math.toRadians(latitude);
        return (int) Math.floor((1 - Math.log(Math.tan(radLat) + 1 / Math.cos(radLat)) / Math.PI) / 2 * Math.pow(2, zoom));
    }

    // タイルをダウンロードする
    private byte[] downloadTile(String tileUrl) throws Exception {
        URL url = new URL(tileUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.connect();
            int responseCode = connection.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                Log.e("TileDownload", "タイルが見つかりません: " + tileUrl);
                return null; // タイルが見つからない場合はnullを返す
            }

            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } finally {
            connection.disconnect();
        }
    }


    // MBTilesテーブルを作成
    private void createMBTilesTables(SQLiteDatabase db) {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS metadata (name TEXT, value TEXT)");
            db.execSQL("CREATE TABLE IF NOT EXISTS tiles (zoom_level INTEGER, tile_column INTEGER, tile_row INTEGER, tile_data BLOB)");
            db.execSQL("INSERT INTO metadata (name, value) VALUES ('name', 'Offline Map')");
            db.execSQL("INSERT INTO metadata (name, value) VALUES ('type', 'baselayer')");
            db.execSQL("INSERT INTO metadata (name, value) VALUES ('version', '1.0')");
            Log.d("DB", "MBTilesテーブル作成成功");
        } catch (Exception e) {
            Log.e("DB", "MBTilesテーブル作成失敗: " + e.getMessage());
        }
    }

    // MBTilesにタイルを保存
    private void saveTileToMBTiles(SQLiteDatabase db, int zoom, int x, int y, byte[] tileData) {
        try {
            if (tileData == null) {
                Log.e("DB", "タイルデータがnullのため挿入をスキップ: Zoom=" + zoom + ", X=" + x + ", Y=" + y);
                return;
            }

            String sql = "INSERT INTO tiles (zoom_level, tile_column, tile_row, tile_data) VALUES (?, ?, ?, ?)";
            db.execSQL(sql, new Object[]{zoom, x, y, tileData});
            Log.d("DB", "タイル挿入成功: Zoom=" + zoom + ", X=" + x + ", Y=" + y);
        } catch (Exception e) {
            Log.e("DB", "タイル挿入失敗: " + e.getMessage());
        }
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
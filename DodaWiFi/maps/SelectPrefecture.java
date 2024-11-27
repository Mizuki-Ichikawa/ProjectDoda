package com.example.ddwifi4;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.util.BoundingBox;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SelectPrefecture extends AppCompatActivity {

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectprefecture);

        LinearLayout linerLayout = findViewById(R.id.linearLayout);
        progressBar = findViewById(R.id.progressBar);

        String[] prefectures = {
                "北海道", "東京都", "神奈川県"
        };

        for (String prefecture : prefectures) {
            TextView textView = new TextView(this);
            textView.setText(prefecture);
            textView.setTextSize(16);
            textView.setPadding(16, 16, 16, 16);

            Button button = new Button(this);
            button.setText("選択");
            button.setOnClickListener(v -> {
                BoundingBox boundingBox = getBoundingBoxForPrefecture(prefecture);
                if (boundingBox != null) {
                    downloadTiles(prefecture, boundingBox, 12); // ズーム12を指定
                } else {
                    Toast.makeText(this, "該当する県の情報がありません", Toast.LENGTH_SHORT).show();
                }
            });

            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            rowLayout.addView(textView, new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
            rowLayout.addView(button);

            linerLayout.addView(rowLayout);
        }
    }

    private BoundingBox getBoundingBoxForPrefecture(String prefecture) {
        Map<String, BoundingBox> prefectureBoundingBoxes = new HashMap<>();
        prefectureBoundingBoxes.put("北海道", new BoundingBox(47.13, 151.35, 41.15, 139.26));
        prefectureBoundingBoxes.put("東京都", new BoundingBox(35.898, 139.953, 35.523, 139.675));
        prefectureBoundingBoxes.put("神奈川県", new BoundingBox(35.605, 139.845, 35.251, 139.226));
        return prefectureBoundingBoxes.get(prefecture);
    }

    private void downloadTiles(String prefecture, BoundingBox boundingBox, int zoomLevel) {
        Toast.makeText(this, prefecture + "の地図をダウンロードします", Toast.LENGTH_SHORT).show();

        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(0);

        new Thread(() -> {
            try {
                File storageDir = new File(getFilesDir(), "offline_maps");
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }

                File zipFile = new File(storageDir, prefecture + "_tiles.zip");
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));

                int startX = calculateTileX(boundingBox.getLonWest(), zoomLevel);
                int endX = calculateTileX(boundingBox.getLonEast(), zoomLevel);
                int startY = calculateTileY(boundingBox.getLatNorth(), zoomLevel);
                int endY = calculateTileY(boundingBox.getLatSouth(), zoomLevel);

                int totalTiles = (endX - startX + 1) * (endY - startY + 1);
                int[] currentTile = {0};

                for (int x = startX; x <= endX; x++) {
                    for (int y = startY; y <= endY; y++) {
                        String tileUrl = "https://tile.openstreetmap.org/" + zoomLevel + "/" + x + "/" + y + ".png";
                        byte[] tileData = downloadTile(tileUrl);

                        if (tileData != null) {
                            ZipEntry zipEntry = new ZipEntry(zoomLevel + "/" + x + "/" + y + ".png");
                            zos.putNextEntry(zipEntry);
                            zos.write(tileData);
                            zos.closeEntry();
                        }

                        currentTile[0]++;
                        int progress = (currentTile[0] * 100) / totalTiles;
                        runOnUiThread(() -> progressBar.setProgress(progress));

                        Thread.sleep(200); // サーバー負荷軽減
                    }
                }

                zos.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, prefecture + "のタイルをZIPに保存しました", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("DownloadError", "エラーが発生しました", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "ダウンロード中にエラーが発生しました", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private int calculateTileX(double longitude, int zoom) {
        return (int) Math.floor((longitude + 180) / 360 * Math.pow(2, zoom));
    }

    private int calculateTileY(double latitude, int zoom) {
        double radLat = Math.toRadians(latitude);
        return (int) Math.floor((1 - Math.log(Math.tan(radLat) + 1 / Math.cos(radLat)) / Math.PI) / 2 * Math.pow(2, zoom));
    }

    private byte[] downloadTile(String tileUrl) {
        try {
            URL url = new URL(tileUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();

            if (connection.getResponseCode() != 200) {
                Log.e("TileDownload", "Failed to download: " + tileUrl);
                return null;
            }

            InputStream inputStream = connection.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();

        } catch (Exception e) {
            Log.e("TileDownload", "Error downloading tile: " + tileUrl, e);
            return null;
        }
    }
}

package com.example.ddwifi4;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

public class SelectRange extends AppCompatActivity {
    private MapView mapView; // クラスフィールドとして宣言
    private GeoPoint startPoint;
    private GeoPoint endPoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectrange);

        // MapViewの初期化
        mapView = findViewById(R.id.mapView); // ローカル変数を削除し、クラスフィールドを使用
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0); // 初期ズーム設定
        mapView.getController().setCenter(new GeoPoint(35.6895, 139.6917)); // 東京の座標を中央に設定

        // 範囲選択の初期化
        selectRangeOfMap(() -> {
            // 範囲選択が完了した場合の処理
            Toast.makeText(this, "範囲選択が完了しました！", Toast.LENGTH_SHORT).show();
        });

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

    // ダウンロード範囲を指定する
    public void selectRangeOfMap(Runnable onRangeSelected) {
        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    startPoint = (GeoPoint) mapView.getProjection().fromPixels(
                            (int) event.getX(), (int) event.getY());
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    endPoint = (GeoPoint) mapView.getProjection().fromPixels(
                            (int) event.getX(), (int) event.getY());
                    // 選択範囲の視覚化
                    coveredSelectedMap(startPoint, endPoint);

                    // 範囲選択終了後の処理を実行
                    if (onRangeSelected != null) {
                        onRangeSelected.run();
                    }
                }
                return true;
            }
        });
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
}

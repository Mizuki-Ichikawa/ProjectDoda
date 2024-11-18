package com.example.ddwifi4;

import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

public class SelectRange extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selectrange);

        // MapViewの初期化
        MapView mapView = findViewById(R.id.mapView);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0); // 初期ズーム設定
        mapView.getController().setCenter(new GeoPoint(35.6895, 139.6917)); // 東京の座標を中央に設定

        // 完了ボタンを押して設定ページに戻る
        Button btnDone = findViewById(R.id.btnDone);
        btnDone.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SelectRange.this, MapSettingActivity.class);
                startActivity(intent);
                //finish();
            }
        });
    }

}

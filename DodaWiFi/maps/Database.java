package com.example.ddwifi4;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.location.Location;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.ArrayAdapter;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.provider.Settings;
import com.google.android.gms.location.*;
import android.location.LocationManager;


public class Database extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final int DEFAULT_DISTANCE = 30000; // デフォルトの距離 (30km)

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private List<Post> postList;
    private List<Post> filteredList;
    private DatabaseHelper dbHelper;
    private FloatingActionButton fabAdd;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ImageButton btnRefresh;
    private View searchBar;
    private EditText etSearch;
    private ImageButton btnSearch, btnBack, btnSettings;
    private ImageButton btnHome;
    private FusedLocationProviderClient fusedLocationClient;

    private double myLatitude = 0.0;
    private double myLongitude = 0.0;
    private int selectedDistance = DEFAULT_DISTANCE; // 選択した距離範囲の初期値

    private LocationCallback locationCallback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database);

        dbHelper = new DatabaseHelper(this);
        postList = dbHelper.getAllPosts();
        filteredList = new ArrayList<>(postList);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        requestLocationPermission();

        setupAdapter();

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        btnRefresh = findViewById(R.id.btn_refresh);
        searchBar = findViewById(R.id.searchBar);
        etSearch = findViewById(R.id.et_search);
        btnSearch = findViewById(R.id.btn_search);
        btnBack = findViewById(R.id.btn_back);
        btnHome = findViewById(R.id.btnHome);
        btnSettings = findViewById(R.id.btnSettings);

        btnSearch.setOnClickListener(v -> searchBar.setVisibility(View.VISIBLE));
        btnBack.setOnClickListener(v -> {
            searchBar.setVisibility(View.GONE);
            etSearch.setText("");
            filterPostsByDistance(selectedDistance);
            // 地図📍追加
            Button mapButton = findViewById(R.id.btn_map);
            mapButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Database.this, MapActivity.class);
                    intent.putExtra("refreshMap", true); // 地図をリフレッシュするフラグを追加
                    startActivity(intent);
                }
            });

        });

        btnHome.setOnClickListener(v -> finish());
        btnSettings.setOnClickListener(v -> showSettingsDialog());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterPostsByDistance(selectedDistance);
                adapter.setSearchText(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        fabAdd = findViewById(R.id.fab_add);
        fabAdd.setOnClickListener(view -> showAddPostDialog());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            refreshPostList();
            swipeRefreshLayout.setRefreshing(false);
        });

        btnRefresh.setOnClickListener(v -> refreshPostList());

        Button mapButton = findViewById(R.id.btn_map);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Database.this, MapActivity.class);
                startActivity(intent);
            }
        });

    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("設定");

        View settingsView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        builder.setView(settingsView);

        Button btnSetRange = settingsView.findViewById(R.id.btnSetRange);
        updateRangeButtonText(btnSetRange); // 現在の表示範囲をボタンテキストに反映

        btnSetRange.setOnClickListener(v -> showDistanceRangeDialog(btnSetRange));

        builder.setPositiveButton("閉じる", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void showDistanceRangeDialog(Button btnSetRange) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("コメントの表示範囲");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_distance_settings, null);
        builder.setView(dialogView);

        Spinner spinnerDistance = dialogView.findViewById(R.id.spinnerDistance);

        String[] distances = {"1m", "100m", "500m", "1km", "5km", "10km", "30km", "50km", "100km"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, distances);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDistance.setAdapter(adapter);

        builder.setPositiveButton("設定", (dialog, which) -> {
            String selectedDistanceStr = (String) spinnerDistance.getSelectedItem();
            selectedDistance = parseDistance(selectedDistanceStr);
            updateRangeButtonText(btnSetRange); // 選択された距離をボタンテキストに更新
            filterPostsByDistance(selectedDistance);
        });

        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void updateRangeButtonText(Button btnSetRange) {
        String rangeText = "コメントの表示範囲の設定 (現在: " + (selectedDistance >= 1000 ? (selectedDistance / 1000) + " km" : selectedDistance + " m") + ")";
        btnSetRange.setText(rangeText);
    }

    private int parseDistance(String distance) {
        if (distance.endsWith("m")) {
            return Integer.parseInt(distance.replace("m", ""));
        } else if (distance.endsWith("km")) {
            return Integer.parseInt(distance.replace("km", "")) * 1000;
        }
        return DEFAULT_DISTANCE;
    }

    private void filterPostsByDistance(int maxDistanceMeters) {
        long currentTime = System.currentTimeMillis();
        filteredList.clear();

        Location myLocation = new Location(LocationManager.GPS_PROVIDER);
        myLocation.setLatitude(myLatitude);
        myLocation.setLongitude(myLongitude);

        for (Post post : postList) {
            Location postLocation = new Location(LocationManager.GPS_PROVIDER);
            postLocation.setLatitude(post.getLatitude());
            postLocation.setLongitude(post.getLongitude());

            float distanceInMeters = myLocation.distanceTo(postLocation);
            if (distanceInMeters <= maxDistanceMeters && post.getExpiryTimestamp() > currentTime) {
                filteredList.add(post);
            }
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showAddPostDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("新しい投稿");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_post, null);
        builder.setView(dialogView);

        EditText inputContent = dialogView.findViewById(R.id.input_content);
        TextView selectedDateText = dialogView.findViewById(R.id.selected_date_text);
        Button selectDateButton = dialogView.findViewById(R.id.btn_select_date);

        final Calendar calendar = Calendar.getInstance();
        final boolean[] isDateSelected = {false};

        selectDateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        calendar.set(year, month, dayOfMonth);
                        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                                (timeView, hourOfDay, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    calendar.set(Calendar.MINUTE, minute);
                                    selectedDateText.setText(calendar.getTime().toString());
                                    isDateSelected[0] = true;
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true);
                        timePickerDialog.show();
                    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
            datePickerDialog.show();
        });

        builder.setPositiveButton("投稿する", (dialog, which) -> {
            String content = inputContent.getText().toString();
            long expiryTimestamp;

            if (isDateSelected[0]) {
                expiryTimestamp = calendar.getTimeInMillis();
            } else {
                Calendar defaultExpiry = Calendar.getInstance();
                defaultExpiry.add(Calendar.HOUR, 24);
                expiryTimestamp = defaultExpiry.getTimeInMillis();
            }

            if (!content.isEmpty()) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                } else {
                    getLocationAndSavePost(content, expiryTimestamp);
                }
            }
        });

        builder.setNegativeButton("キャンセル", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void getLocationAndSavePost(String content, long expiryTimestamp) {
        try {
            if (!isLocationEnabled()) {
                showLocationSettingsDialog();
                return;
            }
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();

                            // 重複確認を行いながら投稿を追加
                            dbHelper.addPostIfNotDuplicate(content, Long.toString(expiryTimestamp), expiryTimestamp, latitude, longitude);

                            Post newPost = dbHelper.getLatestPost();
                            postList.add(0, newPost);
                            filteredList.add(0, newPost);
                            adapter.notifyItemInserted(0);
                            recyclerView.scrollToPosition(0);
                        } else {
                            requestNewLocationData(() -> {
                                getLocationAndSavePost(content, expiryTimestamp);
                            });
                        }
                    });
        } catch (SecurityException e) {
            Toast.makeText(this, "位置情報の取得に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void getCurrentLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if(!isLocationEnabled()) {
                    showLocationSettingsDialog();
                    return;
                }
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, location -> {
                            if (location != null) {
                                myLatitude = location.getLatitude();
                                myLongitude = location.getLongitude();
                                setupAdapter();
                            } else {
                                // 位置情報が取得できなかった場合、新しい位置情報をリクエスト
                                requestNewLocationData(() -> {
                                    getCurrentLocation();
                                });
                            }
                        });
            } else {
                // パーミッションが許可されていない場合、再度リクエスト
                requestLocationPermission();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "位置情報の取得に失敗しました: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        try {
            gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch(Exception ex) {
            // 無視
        }

        try {
            networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {
            // 無視
        }

        return gpsEnabled || networkEnabled;
    }

    private void showLocationSettingsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("位置情報が無効です")
                .setMessage("位置情報サービスを有効にしてください")
                .setPositiveButton("設定", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData(Runnable onLocationUpdated) {
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setNumUpdates(1);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    myLatitude = location.getLatitude();
                    myLongitude = location.getLongitude();
                    if (onLocationUpdated != null) {
                        onLocationUpdated.run();
                    }
                } else {
                    Toast.makeText(Database.this, "位置情報が取得できませんでした。位置情報サービスが有効か確認してください。", Toast.LENGTH_LONG).show();
                }
                // コールバックを削除
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        };

        fusedLocationClient.requestLocationUpdates(
                locationRequest, locationCallback,
                Looper.getMainLooper()
        );
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        } else {
            getCurrentLocation();
        }
    }

    private void setupAdapter() {
        adapter = new PostAdapter(this, filteredList, myLatitude, myLongitude);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 位置情報サービスが有効になったかもしれないので、再度位置情報を取得
        getCurrentLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 位置情報の更新を停止
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "位置情報のパーミッションが必要です", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void refreshPostList() {
        postList.clear();
        postList.addAll(dbHelper.getAllPosts());
        filterPostsByDistance(selectedDistance);
    }
}
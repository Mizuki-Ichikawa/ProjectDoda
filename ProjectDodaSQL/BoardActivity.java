// Boardページに関するクラス
package com.example.projectdodasql;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

public class BoardActivity extends AppCompatActivity {

    private Database dbHelper;
    private ListView boardListView;
    private Button chatButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_board);

        dbHelper = new Database(this);

        boardListView = findViewById(R.id.boardListView);
        chatButton = findViewById(R.id.chatButton);

        loadMessages();

        chatButton.setOnClickListener(view -> {
            Intent intent = new Intent(BoardActivity.this, MainActivity.class);
            startActivity(intent);
        });
    }

    private void loadMessages() {
        List<String> messages = dbHelper.getAllMessages();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, messages);
        boardListView.setAdapter(adapter);
    }
}
package com.example.projectdodasql;

import android.content.Intent;
import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private Database dbHelper; // DatabaseHelperのインスタンス
    private EditText messageInput; // メッセージ入力フィールド
    private Button sendButton; // 送信ボタン
    private Button boardButton; // Board画面へのボタン

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // DatabaseHelperを初期化
        dbHelper = new Database(this);

        // UI コンポーネントの取得
        messageInput = findViewById(R.id.writeMsg);
        sendButton = findViewById(R.id.sendButton);
        boardButton = findViewById(R.id.boardButton); // Boardボタンを取得

        // sendButton が押された時の処理
        sendButton.setOnClickListener(view -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                // メッセージと仮の位置情報（緯度・経度）をデータベースに保存
                dbHelper.insertMessage(message, 0, 0);

                // 入力フィールドをクリア
                messageInput.setText("");
            }
        });

        // boardButton が押された時の処理
        boardButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, BoardActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        dbHelper.close(); // アクティビティ終了時にデータベースを閉じる
        super.onDestroy();
    }
}

package com.example.filemanager;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

public class TextViewerActivity extends AppCompatActivity {
    
    private TextView textView;
    private LinearProgressIndicator progressIndicator;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_viewer);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        
        textView = findViewById(R.id.textView);
        progressIndicator = findViewById(R.id.progressIndicator);
        
        String filePath = getIntent().getStringExtra("file_path");
        if (filePath != null) {
            loadFileContent(new File(filePath));
        }
    }
    
    private void loadFileContent(File file) {
        progressIndicator.setVisibility(android.view.View.VISIBLE);
        
        new Thread(() -> {
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                
                mainHandler.post(() -> {
                    textView.setText(content.toString());
                    progressIndicator.setVisibility(android.view.View.GONE);
                    setTitle(file.getName());
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "读取文件失败", Toast.LENGTH_SHORT).show();
                    progressIndicator.setVisibility(android.view.View.GONE);
                    finish();
                });
            }
        }).start();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
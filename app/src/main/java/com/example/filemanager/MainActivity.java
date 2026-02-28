package com.example.filemanager;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.anggrayudi.storage.SimpleStorageHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private File currentDirectory;
    private SimpleStorageHelper storageHelper;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        storageHelper = new SimpleStorageHelper(this);
        initViews();
        checkPermissions();
    }
    
    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        MaterialButton btnEncrypt = findViewById(R.id.btnEncrypt);
        MaterialButton btnDecrypt = findViewById(R.id.btnDecrypt);
        MaterialButton btnZip = findViewById(R.id.btnZip);
        MaterialButton btnUnzip = findViewById(R.id.btnUnzip);
        MaterialButton btnRefresh = findViewById(R.id.btnRefresh);
        
        btnEncrypt.setOnClickListener(v -> showEncryptDialog());
        btnDecrypt.setOnClickListener(v -> showDecryptDialog());
        btnZip.setOnClickListener(v -> showZipDialog());
        btnUnzip.setOnClickListener(v -> showUnzipDialog());
        btnRefresh.setOnClickListener(v -> loadFiles(currentDirectory));
    }
    
    private void checkPermissions() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {
                    if (report.areAllPermissionsGranted()) {
                        currentDirectory = Environment.getExternalStorageDirectory();
                        loadFiles(currentDirectory);
                    } else {
                        Toast.makeText(MainActivity.this, "需要存储权限", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
                
                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                    token.continuePermissionRequest();
                }
            }).check();
    }
    
    private void loadFiles(File directory) {
        if (directory != null && directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                fileAdapter = new FileAdapter(files, new FileAdapter.OnFileClickListener() {
                    @Override
                    public void onFileClick(File file) {
                        if (file.isDirectory()) {
                            currentDirectory = file;
                            loadFiles(file);
                        } else {
                            openFile(file);
                        }
                    }
                });
                recyclerView.setAdapter(fileAdapter);
            }
        }
    }
    
    private void openFile(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith(".txt") || fileName.endsWith(".json") || 
            fileName.endsWith(".md") || fileName.endsWith(".log")) {
            // 打开文本文件
            Intent intent = new Intent(this, TextViewerActivity.class);
            intent.putExtra("file_path", file.getAbsolutePath());
            startActivity(intent);
        } else {
            // 尝试用其他应用打开
            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri uri = FileProvider.getUriForFile(this, 
                getPackageName() + ".fileprovider", file);
            intent.setDataAndType(uri, getMimeType(fileName));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "无法打开文件", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private String getMimeType(String fileName) {
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".md")) return "text/markdown";
        if (fileName.endsWith(".log")) return "text/plain";
        return "*/*";
    }
    
    private void showEncryptDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password, null);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("加密文件")
            .setView(dialogView)
            .setPositiveButton("加密", (dialog, which) -> {
                String password = etPassword.getText().toString();
                if (!password.isEmpty()) {
                    selectFileForEncryption(password);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showDecryptDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password, null);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);
        
        new MaterialAlertDialogBuilder(this)
            .setTitle("解密文件")
            .setView(dialogView)
            .setPositiveButton("解密", (dialog, which) -> {
                String password = etPassword.getText().toString();
                if (!password.isEmpty()) {
                    selectFileForDecryption(password);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showZipDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("压缩文件")
            .setMessage("选择要压缩的文件或文件夹")
            .setPositiveButton("选择", (dialog, which) -> selectFileForZip())
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void showUnzipDialog() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("解压文件")
            .setMessage("选择要解压的ZIP文件")
            .setPositiveButton("选择", (dialog, which) -> selectFileForUnzip())
            .setNegativeButton("取消", null)
            .show();
    }
    
    private void selectFileForEncryption(String password) {
        storageHelper.openFilePicker(100, file -> {
            try {
                File encryptedFile = FileEncryptor.encrypt(file, password);
                Toast.makeText(this, "加密成功: " + encryptedFile.getName(), Toast.LENGTH_SHORT).show();
                loadFiles(currentDirectory);
            } catch (Exception e) {
                Toast.makeText(this, "加密失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void selectFileForDecryption(String password) {
        storageHelper.openFilePicker(101, file -> {
            if (file.getName().endsWith(".aes")) {
                try {
                    File decryptedFile = FileEncryptor.decrypt(file, password);
                    Toast.makeText(this, "解密成功: " + decryptedFile.getName(), Toast.LENGTH_SHORT).show();
                    loadFiles(currentDirectory);
                } catch (Exception e) {
                    Toast.makeText(this, "解密失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "请选择.aes文件", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void selectFileForZip() {
        storageHelper.openFilePicker(102, file -> {
            try {
                File zipFile = ZipHelper.zip(file);
                Toast.makeText(this, "压缩成功: " + zipFile.getName(), Toast.LENGTH_SHORT).show();
                loadFiles(currentDirectory);
            } catch (Exception e) {
                Toast.makeText(this, "压缩失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void selectFileForUnzip() {
        storageHelper.openFilePicker(103, file -> {
            if (file.getName().endsWith(".zip")) {
                try {
                    File outputDir = ZipHelper.unzip(file, currentDirectory);
                    Toast.makeText(this, "解压成功到: " + outputDir.getName(), Toast.LENGTH_SHORT).show();
                    loadFiles(currentDirectory);
                } catch (Exception e) {
                    Toast.makeText(this, "解压失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "请选择.zip文件", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        storageHelper.getDiskManager().onActivityResult(requestCode, resultCode, data);
    }
}
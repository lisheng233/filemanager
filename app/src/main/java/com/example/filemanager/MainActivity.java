package com.example.filemanager;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final int FILE_PICK_REQUEST_CODE = 100;
    
    private RecyclerView recyclerView;
    private FileAdapter fileAdapter;
    private File currentDirectory;
    private String currentPassword;
    private String currentOperation;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
        
        btnEncrypt.setOnClickListener(v -> showPasswordDialog("encrypt"));
        btnDecrypt.setOnClickListener(v -> showPasswordDialog("decrypt"));
        btnZip.setOnClickListener(v -> {
            currentOperation = "zip";
            pickFile();
        });
        btnUnzip.setOnClickListener(v -> {
            currentOperation = "unzip";
            pickFile("*/*");
        });
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
                        initFileSystem();
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
    
    private void initFileSystem() {
        currentDirectory = Environment.getExternalStorageDirectory();
        loadFiles(currentDirectory);
    }
    
    private void loadFiles(File directory) {
        if (directory != null && directory.exists() && directory.canRead()) {
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
            Intent intent = new Intent(this, TextViewerActivity.class);
            intent.putExtra("file_path", file.getAbsolutePath());
            startActivity(intent);
        } else {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(this, 
                    getPackageName() + ".fileprovider", file);
                intent.setDataAndType(uri, getMimeType(fileName));
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
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
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".pdf")) return "application/pdf";
        return "*/*";
    }
    
    private void showPasswordDialog(String operation) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(operation.equals("encrypt") ? "加密文件" : "解密文件");
        
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_password, null);
        TextInputEditText etPassword = dialogView.findViewById(R.id.etPassword);
        builder.setView(dialogView);
        
        builder.setPositiveButton("确定", (dialog, which) -> {
            String password = etPassword.getText().toString();
            if (!password.isEmpty()) {
                currentPassword = password;
                currentOperation = operation;
                pickFile(operation.equals("decrypt") ? "application/octet-stream" : "*/*");
            } else {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    private void pickFile() {
        pickFile("*/*");
    }
    
    private void pickFile(String mimeType) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, FILE_PICK_REQUEST_CODE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                
                Toast.makeText(this, "正在处理: " + uri.getLastPathSegment(), Toast.LENGTH_SHORT).show();
                
                try {
                    File file = FilePickerHelper.getFileFromUri(this, uri);
                    
                    if (currentOperation.equals("encrypt") || currentOperation.equals("decrypt")) {
                        if (currentPassword == null || currentPassword.isEmpty()) {
                            Toast.makeText(this, "请先输入密码", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    
                    if (currentOperation.equals("encrypt")) {
                        File encryptedFile = FileEncryptor.encrypt(file, currentPassword);
                        Toast.makeText(this, "加密成功: " + encryptedFile.getName(), Toast.LENGTH_SHORT).show();
                    } else if (currentOperation.equals("decrypt")) {
                        if (file.getName().endsWith(".aes")) {
                            File decryptedFile = FileEncryptor.decrypt(file, currentPassword);
                            Toast.makeText(this, "解密成功: " + decryptedFile.getName(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "请选择.aes文件", Toast.LENGTH_SHORT).show();
                        }
                    } else if (currentOperation.equals("zip")) {
                        File zipFile = ZipHelper.zip(file);
                        Toast.makeText(this, "压缩成功: " + zipFile.getName(), Toast.LENGTH_SHORT).show();
                    } else if (currentOperation.equals("unzip")) {
                        if (file.getName().endsWith(".zip")) {
                            File outputDir = ZipHelper.unzip(file, currentDirectory);
                            Toast.makeText(this, "解压成功到: " + outputDir.getName(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "请选择ZIP文件", Toast.LENGTH_SHORT).show();
                        }
                    }
                    
                    // 删除临时文件
                    if (file != null && file.exists()) {
                        file.delete();
                    }
                    
                    loadFiles(currentDirectory);
                    
                } catch (Exception e) {
                    Toast.makeText(this, "操作失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                }
            }
        }
    }
}

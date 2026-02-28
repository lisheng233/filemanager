package com.example.filemanager;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

public class FileEncryptor {
    
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final int SALT_LENGTH = 8;
    private static final int IV_LENGTH = 16;
    
    public static File encrypt(File inputFile, String password) throws Exception {
        // 生成随机盐和IV
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(salt);
        new SecureRandom().nextBytes(iv);
        
        // 从密码生成密钥
        SecretKey key = generateKey(password, salt);
        
        // 创建输出文件
        String outputPath = inputFile.getAbsolutePath() + ".aes";
        File outputFile = new File(outputPath);
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            // 写入盐和IV
            fos.write(salt);
            fos.write(iv);
            
            // 初始化加密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            
            // 加密文件内容
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] output = cipher.update(buffer, 0, bytesRead);
                if (output != null) {
                    fos.write(output);
                }
            }
            
            byte[] output = cipher.doFinal();
            if (output != null) {
                fos.write(output);
            }
        }
        
        return outputFile;
    }
    
    public static File decrypt(File inputFile, String password) throws Exception {
        if (!inputFile.getName().endsWith(".aes")) {
            throw new IllegalArgumentException("不是加密文件");
        }
        
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // 读取盐和IV
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            fis.read(salt);
            fis.read(iv);
            
            // 从密码生成密钥
            SecretKey key = generateKey(password, salt);
            
            // 创建输出文件
            String outputPath = inputFile.getAbsolutePath()
                .substring(0, inputFile.getAbsolutePath().length() - 4);
            String basePath = outputPath;
            int counter = 1;
            while (new File(outputPath).exists()) {
                outputPath = basePath + "_decrypted_" + counter++;
            }
            File outputFile = new File(outputPath);
            
            // 初始化解密器
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] output = cipher.update(buffer, 0, bytesRead);
                    if (output != null) {
                        fos.write(output);
                    }
                }
                
                byte[] output = cipher.doFinal();
                if (output != null) {
                    fos.write(output);
                }
            }
            
            return outputFile;
        }
    }
    
    private static SecretKey generateKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
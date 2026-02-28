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
        byte[] salt = new byte[SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(salt);
        new SecureRandom().nextBytes(iv);
        
        SecretKey key = generateKey(password, salt);
        
        String outputPath = inputFile.getAbsolutePath() + ".aes";
        File outputFile = new File(outputPath);
        int counter = 1;
        while (outputFile.exists()) {
            outputFile = new File(inputFile.getAbsolutePath() + "_" + counter++ + ".aes");
        }
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile)) {
            
            fos.write(salt);
            fos.write(iv);
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            
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
            byte[] salt = new byte[SALT_LENGTH];
            byte[] iv = new byte[IV_LENGTH];
            fis.read(salt);
            fis.read(iv);
            
            SecretKey key = generateKey(password, salt);
            
            String outputPath = inputFile.getAbsolutePath()
                .substring(0, inputFile.getAbsolutePath().length() - 4);
            String basePath = outputPath;
            int counter = 1;
            while (new File(outputPath).exists()) {
                outputPath = basePath + "_decrypted_" + counter++;
            }
            File outputFile = new File(outputPath);
            
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

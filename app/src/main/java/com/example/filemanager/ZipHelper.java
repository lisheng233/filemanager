package com.example.filemanager;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipHelper {
    
    public static File zip(File source) throws IOException {
        String zipPath = source.getAbsolutePath() + ".zip";
        File zipFile = new File(zipPath);
        
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            if (source.isDirectory()) {
                zipDirectory(source, source.getName(), zos);
            } else {
                zipFile(source, source.getName(), zos);
            }
        }
        
        return zipFile;
    }
    
    private static void zipDirectory(File folder, String parentFolder, 
                                     ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                zipDirectory(file, parentFolder + "/" + file.getName(), zos);
            } else {
                zipFile(file, parentFolder + "/" + file.getName(), zos);
            }
        }
    }
    
    private static void zipFile(File file, String entryName, 
                                ZipOutputStream zos) throws IOException {
        ZipEntry zipEntry = new ZipEntry(entryName);
        zos.putNextEntry(zipEntry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                zos.write(buffer, 0, bytesRead);
            }
        }
        
        zos.closeEntry();
    }
    
    public static File unzip(File zipFile, File destinationDir) throws IOException {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }
        
        String outputDirName = zipFile.getName().replace(".zip", "");
        File outputDir = new File(destinationDir, outputDirName);
        outputDir.mkdirs();
        
        byte[] buffer = new byte[8192];
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();
            
            while (zipEntry != null) {
                File newFile = new File(outputDir, zipEntry.getName());
                
                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // 创建父目录
                    new File(newFile.getParent()).mkdirs();
                    
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int bytesRead;
                        while ((bytesRead = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }
                }
                
                zipEntry = zis.getNextEntry();
            }
            
            zis.closeEntry();
        }
        
        return outputDir;
    }
}
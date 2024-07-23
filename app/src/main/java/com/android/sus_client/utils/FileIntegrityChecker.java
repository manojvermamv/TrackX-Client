package com.android.sus_client.utils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileIntegrityChecker {

    public static String getIntegrityHash(String filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256"); // You can choose other algorithms as needed
        byte[] buffer = new byte[8192];
        int bytesRead;

        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(filePath))) {
            while ((bytesRead = fis.read(buffer)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
        }

        byte[] digest = md.digest();
        return bytesToHex(digest);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static boolean verifyIntegrity(String filePath, String expectedHash) throws IOException, NoSuchAlgorithmException {
        String actualHash = getIntegrityHash(filePath);
        return actualHash.equals(expectedHash);
    }

    public static void verifyIntegrityTest() throws Exception {
        String filePath = "/path/to/your/file";
        String expectedHash = "your_expected_hash_value";

        if (verifyIntegrity(filePath, expectedHash)) {
            System.out.println("File integrity is valid!");
        } else {
            System.out.println("File integrity is compromised!");
        }
    }
}
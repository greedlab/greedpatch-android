package com.greedlab.patch.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * Created by Bell on 16/9/21.
 */

public class MD5Util {

    public static String getFileMD5(File file) {
        return MD5Util.getFileHash(file, "MD5");
    }

    @SuppressWarnings("WeakerAccess")
    public static String getFileHash(File file, String hashType) {
        InputStream ins = null;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance(hashType);
            ins = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = ins.read(buffer)) != -1) {
                md5.update(buffer, 0, len);
            }
            byte[] hash = md5.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                if ((b & 0xFF) < 0x10) hex.append("0");
                hex.append(Integer.toHexString(b & 0xFF));
            }
            return hex.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ins = null;
            }
        }
        return null;
    }
}

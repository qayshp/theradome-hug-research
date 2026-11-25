/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import com.screens.HTTPConnectionUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

public class DownloadUtil {
    private static final int BUFFER_SIZE = 4096;
    public String downloadPath = "";
    public String hexVersion = "";

    public int downLoadProcess(String fileUrl, String downloadDir, JProgressBar fancyPB, JLabel progresslblMsg1) {
        HTTPConnectionUtil httpDownLoadUtil;
        block7: {
            FileOutputStream outputStream = null;
            httpDownLoadUtil = new HTTPConnectionUtil();
            if (httpDownLoadUtil.downLoadFile(fileUrl) != 1) break block7;
            System.out.println("http connection established");
            InputStream inputStream = httpDownLoadUtil.getInputStream();
            this.downloadPath = String.valueOf(downloadDir) + File.separator + httpDownLoadUtil.getFileName();
            try {
                outputStream = new FileOutputStream(this.downloadPath);
            }
            catch (Exception ex) {
                return -1;
            }
            try {
                System.out.println("Output file created at  " + this.downloadPath);
                byte[] buffer = new byte[4096];
                int bytesRead = -1;
                long totalBytesRead = 0L;
                long fLength = httpDownLoadUtil.fileLength();
                int completedSize = 0;
                System.out.println(" File Length: " + Long.toString(fLength));
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    completedSize = (int)((totalBytesRead += (long)bytesRead) * 100L / fLength);
                    fancyPB.setValue(completedSize);
                    progresslblMsg1.setText(String.valueOf(Integer.toString(completedSize)) + "%");
                    System.out.print(" " + Integer.toString(completedSize));
                }
                System.out.println("File Path: " + this.downloadPath);
                System.out.println("====File write completed== \n\rLength :" + Long.toString(totalBytesRead));
                outputStream.close();
                httpDownLoadUtil.disconnect();
                return 1;
            }
            catch (IOException e) {
                System.out.println("HTTP file download failed2");
                return 0;
            }
        }
        System.out.println("HTTP file download failed");
        httpDownLoadUtil.disconnect();
        return 0;
    }

    String getHomePath() {
        String downloadDir = System.getProperty("user.home");
        System.out.println("window home path " + downloadDir);
        int i = 0;
        while (i < downloadDir.length()) {
            char chr = downloadDir.charAt(i);
            if (chr == '\\') {
                this.downloadPath = String.valueOf(this.downloadPath) + '\\';
            }
            this.downloadPath = String.valueOf(this.downloadPath) + chr;
            ++i;
        }
        System.out.println("modified path " + this.downloadPath);
        return downloadDir;
    }
}


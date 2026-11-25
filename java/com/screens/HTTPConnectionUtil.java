/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HTTPConnectionUtil {
    private HttpURLConnection httpURLConn;
    private InputStream inputStream;
    private String fileName;
    private long fileLength;

    public int downLoadFile(String fileURL) throws IOException {
        int resCode;
        try {
            URL url = new URL(fileURL);
            this.httpURLConn = (HttpURLConnection)url.openConnection();
            this.httpURLConn.addRequestProperty("User-Agent", "Mozilla/4.76");
            resCode = this.httpURLConn.getResponseCode();
            System.out.println("output code:" + resCode);
        }
        catch (Exception ex) {
            return 0;
        }
        if (resCode == 200) {
            try {
                this.fileLength = this.httpURLConn.getContentLength();
                System.out.println("URL:" + this.httpURLConn.getURL());
                String urlStr = this.httpURLConn.getURL().getFile();
                int index = urlStr.lastIndexOf(47);
                this.fileName = urlStr.substring(index + 1);
                System.out.println("fileLength = " + this.fileLength);
                System.out.println("fileName = " + this.fileName);
                this.inputStream = this.httpURLConn.getInputStream();
            }
            catch (Exception exx) {
                return 0;
            }
            return 1;
        }
        System.out.println("HTTP failed");
        return 0;
    }

    public InputStream getInputStream() {
        return this.inputStream;
    }

    public long fileLength() {
        return this.fileLength;
    }

    public String getFileName() {
        return this.fileName;
    }

    public void disconnect() throws IOException {
        this.inputStream.close();
        this.httpURLConn.disconnect();
    }
}


/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class DownloadHexGithub {
    private static boolean isRedirected(Map<String, List<String>> header) {
        for (String hv : header.get(null)) {
            if (!hv.contains(" 301 ") && !hv.contains(" 302 ")) continue;
            return true;
        }
        return false;
    }

    public static void main(String[] args) throws Throwable {
        String link = "https://raw.githubusercontent.com/renuraja03/fwupdate/master/version.txt";
        String fileName = "version.txt";
        URL url = new URL(link);
        HttpURLConnection http = (HttpURLConnection)url.openConnection();
        Map<String, List<String>> header = http.getHeaderFields();
        while (DownloadHexGithub.isRedirected(header)) {
            link = header.get("Location").get(0);
            url = new URL(link);
            http = (HttpURLConnection)url.openConnection();
            header = http.getHeaderFields();
        }
        System.out.println("URL" + url);
        System.out.println("URL" + http);
        System.out.println("URL" + header);
        InputStream input = http.getInputStream();
        byte[] buffer = new byte[4096];
        int n = -1;
        FileOutputStream output = new FileOutputStream(new File(fileName));
        long length = 0L;
        while ((n = input.read(buffer)) != -1) {
            length += (long)n;
            ((OutputStream)output).write(buffer, 0, n);
        }
        System.out.println("read commpleted :  length" + Long.toString(length));
        ((OutputStream)output).close();
    }
}


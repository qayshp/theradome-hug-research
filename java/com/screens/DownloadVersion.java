/*
 * Decompiled with CFR 0.152.
 */
package com.screens;

import com.screens.HTTPConnectionUtil;
import java.io.IOException;
import java.io.InputStream;

public class DownloadVersion {
    private static final int BUFFER_SIZE = 4096;
    public String downloadPath = "";
    public String hexVersion = "";
    public int hexVersionInt = 0;

    public int downLoadProcess(String fileUrl) {
        block4: {
            HTTPConnectionUtil httpDownLoadUtil = new HTTPConnectionUtil();
            try {
                if (httpDownLoadUtil.downLoadFile(fileUrl) != 1) break block4;
                StringBuilder strb = new StringBuilder();
                InputStream inputStream = httpDownLoadUtil.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead = -1;
                bytesRead = inputStream.read(buffer);
                if (bytesRead != -1) {
                    String str;
                    byte[] bytebuf = new byte[10];
                    bytebuf[0] = buffer[9];
                    bytebuf[1] = buffer[10];
                    bytebuf[2] = buffer[11];
                    bytebuf[3] = 0;
                    this.hexVersionInt = (buffer[9] - 48) * 100 + (buffer[10] - 48) * 10 + buffer[11] - 48;
                    this.hexVersion = str = new String(bytebuf, "UTF-8");
                }
                System.out.print("Version found : " + this.hexVersion);
                httpDownLoadUtil.disconnect();
                return 1;
            }
            catch (IOException e) {
                System.out.println("HTTP file download failed2");
                return 0;
            }
        }
        System.out.println("HTTP file download failed");
        return 0;
    }

    String getVersion() {
        return this.hexVersion;
    }

    int getHexVersionInt() {
        return this.hexVersionInt;
    }
}


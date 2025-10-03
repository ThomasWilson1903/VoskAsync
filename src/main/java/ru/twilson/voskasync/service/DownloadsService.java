package ru.twilson.voskasync.service;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.File;
import java.io.IOException;

@Slf4j
@UtilityClass
public class DownloadsService {

    @SneakyThrows
    public static File downloadFile(String fileUrl) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(fileUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    File oggFile = File.createTempFile("voice", ".ogg");
                    try (java.io.InputStream inStream = entity.getContent();
                         java.io.FileOutputStream outStream = new java.io.FileOutputStream(oggFile)) {
                        inStream.transferTo(outStream);
                    }
                    log.info("Скачан в файл {}", oggFile.getAbsolutePath());
                    return oggFile;
                }
            }
        }
        throw new IOException();
    }
}

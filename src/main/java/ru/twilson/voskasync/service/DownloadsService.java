package ru.twilson.voskasync.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadsService {

    private final CloseableHttpClient httpClient;

    public File downloadFile(String fileUrl) throws IOException {

        HttpGet request = new HttpGet(fileUrl);

        try (CloseableHttpResponse response = httpClient.execute(request)) {

            int status = response.getStatusLine().getStatusCode();

            if (status < 200 || status >= 300) {
                throw new IOException("Failed to download file. HTTP status=" + status + " url=" + fileUrl);
            }

            HttpEntity entity = response.getEntity();

            if (entity == null) {
                throw new IOException("Empty response entity for url=" + fileUrl);
            }

            Path tempFile = Files.createTempFile("voice_", ".ogg");

            try (InputStream inputStream = entity.getContent()) {
                Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            File file = tempFile.toFile();

            log.info("File downloaded: {} ({} bytes)", file.getAbsolutePath(), file.length());

            return file;
        }
    }
}

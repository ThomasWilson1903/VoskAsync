package ru.twilson.voskasync.service.vosk;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.twilson.voskasync.configuration.event.VoskAsyncEvent;
import ru.twilson.voskasync.dto.TranscriptionRequest;
import ru.twilson.voskasync.dto.TranscriptionResponse;
import ru.twilson.voskasync.service.AudioProcessor;
import ru.twilson.voskasync.service.AudioService;
import ru.twilson.voskasync.service.DownloadsService;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;

import static ru.twilson.voskasync.service.vosk.RemoteVoskRecognizer.SERVICE_VPS;


@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voskasync.handling_type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalVoskRecognizer {

    private final ObjectMapper objectMapper;
    private final AudioService audioService;
    private final DownloadsService downloadFile;
    private final AudioProcessor audioProcessor;
    private final ApplicationEventPublisher eventPublisher;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @SneakyThrows
    @KafkaListener(topics = SERVICE_VPS, groupId = "speech-workers")
    public void consume(String message) {
        log.info("Получено сообщение из топика {}: {}", SERVICE_VPS, message);
        TranscriptionRequest transcriptionRequest = objectMapper.readValue(message, TranscriptionRequest.class);
        String result = recognize(transcriptionRequest.getUrlFile());
        TranscriptionResponse transcriptionResponse = new TranscriptionResponse(transcriptionRequest.getId(), result);
        kafkaTemplate.send(transcriptionRequest.getQueue(), objectMapper.writeValueAsString(transcriptionResponse));
    }

    @SneakyThrows
    public String recognize(String url) {
        File voiceFile = audioService.convertOggToWav(downloadFile.downloadFile(url));
        return processAudioFile(voiceFile);
    }

    public void giveRecognize(TranscriptionRequest transcriptionRequest) {
        String recognize = recognize(transcriptionRequest.getUrlFile());
        eventPublisher.publishEvent(new VoskAsyncEvent(this, transcriptionRequest.getId(), recognize));
    }

    @SneakyThrows
    private String processAudioFile(File voiceFile) {
        try (InputStream is = new FileInputStream(voiceFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] audioData = baos.toByteArray();
            AudioFormat format = audioService.getAudioFormatFromFile(voiceFile);
            int silenceThresholdDb = audioService.detectSilenceThreshold(voiceFile);
            List<byte[]> bytes = audioService.splitBySilence(audioData, format, silenceThresholdDb, 250, 1000);
            return audioProcessor.recognizeAllAudios(bytes, audioService.getAudioDuration(voiceFile) >= 237);
        } finally {
            Files.deleteIfExists(voiceFile.toPath());
        }
    }
}

package ru.twilson.voskasync.service.vosk.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;
import ru.twilson.voskasync.configuration.VoskConfiguration;
import ru.twilson.voskasync.dto.AudioBatch;
import ru.twilson.voskasync.service.vosk.VoskService;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static ru.twilson.voskasync.service.AudioService.*;
import static ru.twilson.voskasync.service.DownloadsService.downloadFile;


@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vosk.handling.type", havingValue = "LOCAL", matchIfMissing = true)
public class VoskServiceLocal implements VoskService {

    public static final String NAME_QUEUE_RABBITMQ = "service.vps.agent";

    private final Model voskModel;
    //    private final RabbitAdmin rabbitAdmin;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;
//    private final RabbitTemplate rabbitTemplate;


    @SneakyThrows
    public String recognize(String url) {
        File voiceFile = convertOggToWav(downloadFile(url));
        return processAudioFile(voiceFile);
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
            AudioFormat format = getAudioFormatFromFile(voiceFile);
            int silenceThresholdDb = detectSilenceThreshold(voiceFile);
            List<byte[]> bytes = splitBySilence(audioData, format, silenceThresholdDb, 250, 1000);
            return recognizeAllAudios(bytes, getAudioDuration(voiceFile) >= 237);
        } finally {
            Files.deleteIfExists(voiceFile.toPath());
        }
    }

    private String recognizeAllAudios(List<byte[]> audios, boolean splitEnable) throws InterruptedException, ExecutionException {
        List<Future<String>> futures = new ArrayList<>();
        if (false) {
//            int consumerCount = getConsumerCount();
//        if (splitEnable && consumerCount > 0) {
//            List<List<byte[]>> lists = splitList(audios, consumerCount);
//            for (List<byte[]> audio : lists) {
//                futures.add(executor.submit(() -> recognizeAudioRemotely(audio)));
//            }
        } else {
            for (byte[] audio : audios) {
                futures.add(executor.submit(() -> recognizeAudio(audio)));
            }
        }

        // Собираем результаты в порядке исходного списка
        StringBuilder combinedResult = new StringBuilder();
        for (Future<String> future : futures) {
            combinedResult.append(" ").append(future.get());
        }

        return combinedResult.toString().replaceAll("\\s+", " ");
    }

    @SneakyThrows
    public String recognizeAudio(byte[] audio) {
        log.debug("Запущена обработка: {}", audio.length);
        try (Recognizer recognizer = new Recognizer(voskModel, 16000.0f)) {
            recognizer.acceptWaveForm(audio, audio.length);
            return parseVoskJson(recognizer.getFinalResult());
        }
    }

//    @SneakyThrows
//    public String recognizeAudioRemotely(List<byte[]> bytes) {
//        Message messageRq = MessageBuilder
//                .withBody(objectMapper.writeValueAsString(new AudioBatch(bytes)).getBytes())
//                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
//                .build();
//        rabbitTemplate.setReplyTimeout(30_000);
//        rabbitTemplate.setReceiveTimeout(10_000);
//        return String.valueOf(rabbitTemplate.convertSendAndReceive(NAME_QUEUE_RABBITMQ, messageRq));
//    }

    @SneakyThrows
    @RabbitListener(queues = NAME_QUEUE_RABBITMQ)
    public String transcriptionRabbitMq(String message) {
        log.info("Transcription request remotely");
        AudioBatch audioBatch = objectMapper.readValue(message, AudioBatch.class);
        return recognizeAllAudios(audioBatch.getChunks(), false);
    }

//    private int getConsumerCount() {
//        QueueInformation queueInfo = rabbitAdmin.getQueueInfo(NAME_QUEUE_RABBITMQ);
//        if (queueInfo != null) {
//            return queueInfo.getConsumerCount();
//        }
//        return 0;
//    }

    public static <T> List<List<T>> splitList(List<T> list, int parts) {
        int size = list.size();
        if (parts <= 1) {
            return List.of(list);
        }
        int chunkSize = (size + parts - 1) / parts;

        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            int from = i * chunkSize;
            int to = Math.min(from + chunkSize, size);
            if (from < size) {
                result.add(list.subList(from, to));
            } else {
                result.add(Collections.emptyList());
            }
        }
        return result;
    }

    @SneakyThrows
    private String parseVoskJson(String json) {
        return objectMapper.readTree(json).get("text").asText();
    }


}

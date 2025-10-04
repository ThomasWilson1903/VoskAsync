package ru.twilson.voskasync.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import ru.twilson.voskasync.dto.AudioBatch;
import ru.twilson.voskasync.service.AudioProcessor;

import static ru.twilson.voskasync.configuration.RabbitMqConfiguration.NAME_QUEUE_RABBITMQ;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoskRemoteProcessor {

    private final ObjectMapper objectMapper;
    private final AudioProcessor audioProcessor;

    @SneakyThrows
    @RabbitListener(queues = NAME_QUEUE_RABBITMQ)
    public String transcriptionRabbitMq(String message) {
        log.info("Transcription request remotely");
        AudioBatch audioBatch = objectMapper.readValue(message, AudioBatch.class);
        return audioProcessor.recognizeAllAudios(audioBatch.getChunks(), false);
    }
}
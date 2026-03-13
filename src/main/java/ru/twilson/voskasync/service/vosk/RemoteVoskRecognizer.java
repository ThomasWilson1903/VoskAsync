package ru.twilson.voskasync.service.vosk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.twilson.voskasync.configuration.event.VoskAsyncEvent;
import ru.twilson.voskasync.dto.TranscriptionRequest;
import ru.twilson.voskasync.dto.TranscriptionResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class RemoteVoskRecognizer {

    public static final String SERVICE_VPS = "service.vps";
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ApplicationEventPublisher eventPublisher;
    @Value("${topic_response:topic_response}")
    private String topic_response;

    @SneakyThrows
    @KafkaListener(topics = "${topic_response:topic_response}", groupId = "speech-workers")
    public void getResponse(String message) {
        TranscriptionResponse transcriptionResponse = objectMapper.readValue(message, TranscriptionResponse.class);
        eventPublisher.publishEvent(new VoskAsyncEvent(this,
                transcriptionResponse.getId(),
                transcriptionResponse.getFinalResult()));
    }

    public void recognize(Long id, String urlFile) {
        recognize(id, urlFile, topic_response);
    }

    public void recognize(Long id, String urlFile, String topic) {
        if (urlFile == null) {
            throw new NullPointerException("urlFile is null");
        }

        TranscriptionRequest build = TranscriptionRequest.builder()
                .id(id)
                .queue(topic)
                .urlFile(urlFile)
                .build();
        try {
            kafkaTemplate.send(SERVICE_VPS, objectMapper.writeValueAsString(build));
        } catch (JsonProcessingException e) {
            log.error("Request serialization error");
            throw new RuntimeException(e);
        }
    }
}

package ru.twilson.voskasync.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.twilson.voskasync.dto.TranscriptionResponse;
import ru.twilson.voskasync.dto.TranscriptionRequest;
import ru.twilson.voskasync.service.vosk.VoskService;

import static ru.twilson.voskasync.configuration.RabbitMqConfiguration.QUEUE_NAME;


@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vosk.handling.type", havingValue = "LOCAL", matchIfMissing = true)
public class VoskListener {

    private final VoskService voskService;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;


    @SneakyThrows
    @RabbitListener(queues = QUEUE_NAME)
    public void transcriptionRabbitMq(String transcription) {
        log.info("Transcription request: {}", transcription.toString());
        TranscriptionRequest transcriptionRequest = objectMapper.readValue(transcription, TranscriptionRequest.class);

        try {
            String result = voskService.recognize(transcriptionRequest.getUrlFile());
            TranscriptionResponse transcriptionResponse = new TranscriptionResponse(transcriptionRequest.getId(), result);
            rabbitTemplate.send("", transcriptionRequest.getQueue(), MessageBuilder
                    .withBody(objectMapper.writeValueAsString(transcriptionResponse).getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build());

        } catch (Exception e) {
            log.error("Ошибка обработки: {}", e.getMessage());
        }
    }
}

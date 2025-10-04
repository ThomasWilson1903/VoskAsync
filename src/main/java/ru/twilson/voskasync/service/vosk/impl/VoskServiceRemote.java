package ru.twilson.voskasync.service.vosk.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.twilson.voskasync.dto.TranscriptionRequest;
import ru.twilson.voskasync.dto.TranscriptionResponse;
import ru.twilson.voskasync.service.vosk.VoskService;

import static org.springframework.amqp.core.MessageProperties.CONTENT_TYPE_JSON;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "vosk.handling.type", havingValue = "REMOTE")
public class VoskServiceRemote implements VoskService {

    private static final String SERVICE_VPS = "service.vps";
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;

    /**
     * Синхронный запрос на транскрибацию, лучше не использовать
     *
     * @param urlFile
     * @return
     */
    @SneakyThrows
    @Override
    public String recognize(String urlFile) {
        if (urlFile == null) {
            throw new NullPointerException("urlFile is null");
        }
        TranscriptionRequest build = TranscriptionRequest.builder()
                .id(0)
                .queue("")
                .urlFile(urlFile)
                .build();
        rabbitTemplate.setReplyTimeout(30_000);
        rabbitTemplate.setReceiveTimeout(10_000);
        String response = String.valueOf(rabbitTemplate.convertSendAndReceive(SERVICE_VPS, build));
        TranscriptionResponse transcriptionResponse = objectMapper.readValue(response, TranscriptionResponse.class);
        return transcriptionResponse.getFinalResult();
    }

    /**
     * listener {@link ru.twilson.voskasync.configuration.event.VoskAsyncEvent}
     *
     * @param transcriptionRequest Запрос на транскрибацию
     */
    @SneakyThrows
    public void giveRecognize(TranscriptionRequest transcriptionRequest) {
        Message message = MessageBuilder
                .withBody(objectMapper.writeValueAsString(transcriptionRequest).getBytes())
                .setContentType(CONTENT_TYPE_JSON)
                .build();
        rabbitTemplate.send("", SERVICE_VPS, message);
    }

}

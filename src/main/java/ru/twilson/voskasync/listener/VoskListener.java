package ru.twilson.voskasync.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Async;
import ru.twilson.voskasync.dto.ResponseDto;
import ru.twilson.voskasync.dto.TranscriptionDto;
import ru.twilson.voskasync.service.vosk.VoskService;

import static ru.twilson.voskasync.configuration.RabbitMqConfiguration.QUEUE_NAME;


@Slf4j
@RequiredArgsConstructor// todo добавить настойку для принятия запросов
@ConditionalOnBean({RabbitTemplate.class, VoskService.class})
public class VoskListener {

    private final VoskService voskService;
    private final ObjectMapper objectMapper;
    private final RabbitTemplate rabbitTemplate;


    @Async
    @SneakyThrows
    @RabbitListener(queues = QUEUE_NAME)
    public void transcriptionRabbitMq(String transcription) {
        log.info("Transcription request: {}", transcription.toString());
        TranscriptionDto transcriptionDto = objectMapper.readValue(transcription, TranscriptionDto.class);
        try {
            String result = voskService.recognize(transcriptionDto.getUrlFile());
            ResponseDto responseDto = new ResponseDto(transcriptionDto.getId(), result);
            rabbitTemplate.send("", transcriptionDto.getQueue(), MessageBuilder
                    .withBody(objectMapper.writeValueAsString(responseDto).getBytes())
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .build());
        } catch (Exception e) {
            log.error("Ошибка обработки: {}", e.getMessage());
        }
    }
}

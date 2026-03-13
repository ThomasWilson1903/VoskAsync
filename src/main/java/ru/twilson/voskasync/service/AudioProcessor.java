package ru.twilson.voskasync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "voskasync.handling_type", havingValue = "LOCAL", matchIfMissing = true)
public class AudioProcessor {

    private final Model voskModel;
//    private final RabbitAdmin rabbitAdmin;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;
//    private final RabbitTemplate rabbitTemplate;

    public String recognizeAllAudios(List<byte[]> audios, boolean splitEnable) throws InterruptedException, ExecutionException {
        List<Future<String>> futures = new ArrayList<>();
//        int consumerCount = getConsumerCount();
        if (false/*splitEnable && consumerCount > 0*/) {
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
            return objectMapper.readTree(recognizer.getFinalResult()).get("text").asText();
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


//    private int getConsumerCount() {
//        QueueInformation queueInfo = rabbitAdmin.getQueueInfo(NAME_QUEUE_RABBITMQ);
//        return queueInfo.getConsumerCount();
//    }
}

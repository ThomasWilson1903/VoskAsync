package ru.twilson.voskasync.service.vosk.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import ru.twilson.voskasync.service.vosk.VoskService;

@Slf4j
@Service
@ConditionalOnProperty(name = "vosk.handling.type", havingValue = "REMOTE")
public class VoskServiceRemote implements VoskService {

    @PostConstruct
    public void init() {
        log.info("VoskServiceRemote init");
    }

    @Override
    public String recognize(String url) {
        return "";
    }
}

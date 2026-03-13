package ru.twilson.voskasync.configuration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vosk.Model;

@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "vosk.model", name = {"path"})
@ConditionalOnProperty(name = "voskasync.handling_type", havingValue = "LOCAL", matchIfMissing = true)
public class VoskConfiguration {

    @Value("${vosk.model.path}")
    private String pathVoskModel;

    @SneakyThrows
    @Bean(destroyMethod = "close")
    public Model model() {
        log.info("path model: {}", pathVoskModel);
        return new Model(pathVoskModel);
    }
}

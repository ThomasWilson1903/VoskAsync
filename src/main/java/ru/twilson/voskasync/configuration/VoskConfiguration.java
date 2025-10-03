package ru.twilson.voskasync.configuration;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vosk.Model;

@Configuration
@ConditionalOnProperty(prefix = "vosk.model", name = {"path"})
@ConditionalOnProperty(name = "vosk.handling.type", havingValue = "LOCAL", matchIfMissing = true)
public class VoskConfiguration {

    @Value("${vosk.model.path}")
    private String pathVoskModel;

    @SneakyThrows
    @Bean(destroyMethod = "close")
    public Model model() {
        return new Model(pathVoskModel);
    }
}

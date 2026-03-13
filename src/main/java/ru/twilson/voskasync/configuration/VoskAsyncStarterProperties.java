package ru.twilson.voskasync.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "voskasync")
public class VoskAsyncStarterProperties {

    private String host = "localhost";
    private int port = 8080;
    private String handling_type = "REMOTE";
}

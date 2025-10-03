package ru.twilson.voskasync.configuration;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnBean(VoskConfiguration.class)
public class RabbitMqConfiguration {

    public static final String QUEUE_NAME = "service.vps";
    public static final String NAME_QUEUE_RABBITMQ = "service.vps.agent";

    @Bean
    public Queue vpsQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Queue vpsQueueAgent() {
        return new Queue(NAME_QUEUE_RABBITMQ, true);
    }

    @Bean
    public RabbitAdmin rabbitAdmin(RabbitTemplate rabbitTemplate) {
        return new RabbitAdmin(rabbitTemplate);
    }
}

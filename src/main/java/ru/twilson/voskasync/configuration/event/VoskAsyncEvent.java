package ru.twilson.voskasync.configuration.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class VoskAsyncEvent extends ApplicationEvent {

    private final long id;
    private final String result;

    public VoskAsyncEvent(Object source, long id, String result) {
        super(source);
        this.id = id;
        this.result = result;
    }
}

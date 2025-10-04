package ru.twilson.voskasync.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranscriptionRequest {

    private long id = 0;
    private String queue;
    private String urlFile;
}

package ru.twilson.voskasync.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class TranscriptionDto {
    private long id;
    private String queue;
    private String urlFile;
}

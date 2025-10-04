package ru.twilson.voskasync.dto;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Data
@ToString
@RequiredArgsConstructor
public class TranscriptionResponse {
    private final long id;
    private final String finalResult;
}

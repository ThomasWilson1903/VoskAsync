package ru.twilson.voskasync.service.vosk;

import ru.twilson.voskasync.dto.TranscriptionRequest;

public interface VoskService {

    String recognize(String urlFile);

    void giveRecognize(TranscriptionRequest transcriptionRequest);
}


package ru.twilson.voskasync.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.twilson.voskasync.service.vosk.VoskService;

@RestController
@RequiredArgsConstructor
public class RecognizeController {

    private final VoskService voskService;

    @GetMapping("/test")
    public String recognize() {
        voskService.recognize("");
        return "Hello World";
    }
}

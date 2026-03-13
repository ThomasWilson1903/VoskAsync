package ru.twilson.voskasync.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(name = "voskasync.handling_type", havingValue = "LOCAL", matchIfMissing = true)
public class AudioService {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${ffprobe.path}")
    private String ffprobePath;

    /**
     * Разделяет аудио данные на части по паузам в речи
     *
     * @param audioBytes           исходный аудио массив
     * @param format               формат аудио (должен содержать sampleRate, sampleSizeInBits, channels)
     * @param silenceThresholdDb   порог тишины в децибелах (например, -40)
     * @param minSilenceDurationMs минимальная длительность паузы для разделения (мс)
     * @param chunkMinDurationMs   минимальная длительность чанка (мс)
     * @return список аудио чанков
     */
    public List<byte[]> splitBySilence(byte[] audioBytes,
                                       AudioFormat format,
                                       float silenceThresholdDb,
                                       int minSilenceDurationMs,
                                       int chunkMinDurationMs) {

        List<byte[]> chunks = new ArrayList<>();

        // Параметры аудио
        int sampleRate = (int) format.getSampleRate();
        int sampleSizeBytes = format.getSampleSizeInBits() / 8;
        int channels = format.getChannels();
        int frameSize = sampleSizeBytes * channels;

        // Конвертируем порог из dB в линейное значение
        float silenceThreshold = (float) Math.pow(10, silenceThresholdDb / 20);

        // Вычисляем размеры блоков в samples/bytes
        int samplesPerMs = sampleRate / 1000;
        int minSilenceSamples = minSilenceDurationMs * samplesPerMs;
        int minChunkSamples = chunkMinDurationMs * samplesPerMs;

        // Текущее состояние
        int chunkStart = 0;
        boolean inSilence = false;
        int silenceStart = 0;

        // Анализируем аудио блоками по 10мс
        int windowSize = 10 * samplesPerMs * frameSize;

        for (int i = 0; i < audioBytes.length; i += windowSize) {
            int windowEnd = Math.min(i + windowSize, audioBytes.length);
            int windowLength = windowEnd - i;

            // Вычисляем RMS для текущего окна
            double rms = calculateRms(audioBytes, i, windowLength, frameSize);

            if (rms < silenceThreshold) {
                if (!inSilence) {
                    silenceStart = i;
                    inSilence = true;
                }
            } else {
                if (inSilence && (i - silenceStart) >= minSilenceSamples * frameSize) {
                    // Проверяем, что чанк не слишком короткий
                    if ((silenceStart - chunkStart) >= minChunkSamples * frameSize) {
                        chunks.add(Arrays.copyOfRange(audioBytes, chunkStart, silenceStart));
                        chunkStart = i;
                    }
                }
                inSilence = false;
            }
        }

        // Добавляем последний чанк
        if (chunkStart < audioBytes.length) {
            chunks.add(Arrays.copyOfRange(audioBytes, chunkStart, audioBytes.length));
        }

        return chunks;
    }

    /**
     * Вычисляет RMS (Root Mean Square) для аудио данных
     */
    private double calculateRms(byte[] audio, int offset, int length, int frameSize) {
        double sum = 0;
        int sampleCount = 0;

        for (int i = offset; i < offset + length; i += frameSize) {
            if (i + frameSize > audio.length) break;

            // Конвертируем байты в sample (предполагаем 16-bit little-endian)
            short sample = (short) ((audio[i + 1] << 8) | (audio[i] & 0xFF));

            // Нормализуем до [-1, 1]
            double normalized = sample / 32768.0;
            sum += normalized * normalized;
            sampleCount++;
        }

        return sampleCount > 0 ? Math.sqrt(sum / sampleCount) : 0;
    }

    public AudioFormat getAudioFormatFromFile(File voiceFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                ffprobePath,
                "-v", "error",
                "-show_entries", "stream=sample_rate,channels,bits_per_sample",
                "-of", "default=noprint_wrappers=1",
                voiceFile.getAbsolutePath()
        );

        Process process = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        int sampleRate = 16000; // default
        int channels = 1;
        int bitsPerSample = 16;

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("sample_rate=")) {
                sampleRate = Integer.parseInt(line.split("=")[1]);
            } else if (line.startsWith("channels=")) {
                channels = Integer.parseInt(line.split("=")[1]);
            } else if (line.startsWith("bits_per_sample=")) {
                bitsPerSample = Integer.parseInt(line.split("=")[1]);
            }
        }

        return new AudioFormat(
                sampleRate,
                bitsPerSample,
                channels,
                true,  // signed
                false  // little-endian (может зависеть от формата)
        );
    }

    public int detectSilenceThreshold(File audioFile) throws Exception {

        // Получаем аудиоданные
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        AudioFormat format = audioStream.getFormat();
        byte[] audioData = audioStream.readAllBytes();
        audioStream.close();

        // Конвертируем в амплитуды (для 16-битного PCM)
        double[] amplitudes = new double[audioData.length / 2];
        for (int i = 0; i < amplitudes.length; i++) {
            short sample = (short) ((audioData[i * 2 + 1] << 8) | (audioData[i * 2] & 0xFF));
            amplitudes[i] = sample / 32768.0; // Нормализация [-1.0, 1.0]
        }

        // Вычисляем RMS (среднеквадратичное значение)
        double sumSquares = 0;
        for (double amp : amplitudes) {
            sumSquares += amp * amp;
        }
        double rms = Math.sqrt(sumSquares / amplitudes.length);

        // Преобразуем в децибелы (dBFS)
        double dB = 20 * Math.log10(rms);

        // Определяем порог тишины (средний шум + запас)
        int silenceThreshold = (int) Math.round(dB - 10);

        return silenceThreshold;
    }

    @SneakyThrows
    public File convertOggToWav(File fileOgg) {
        log.info("Получен файл для конвертации в формат .ogg");
        File fileWav = Files.createTempFile("converted_", ".wav").toFile();
        Process process = new ProcessBuilder(
                ffmpegPath, "-i", fileOgg.getAbsolutePath(),
                "-acodec", "pcm_s16le", "-ar", "16000", "-ac", "1", "-y",
                fileWav.getAbsolutePath()
        ).start();

        try (InputStream errorStream = process.getErrorStream()) {
            if (process.waitFor() != 0) {
                String error = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                throw new IOException("FFmpeg error: " + error);
            }
            log.info("Файл обработан .ogg");
            return fileWav;
        } catch (Exception e) {
            throw e;
        }
    }

    @SneakyThrows
    public double getAudioDuration(File audioFile) {
        Process process = new ProcessBuilder(
                ffmpegPath, "-i", audioFile.getAbsolutePath()
        ).redirectErrorStream(true).start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Duration:")) {
                    String durationStr = line.split("Duration:")[1].split(",")[0].trim();
                    return parseDuration(durationStr); // HH:MM:SS.mmm
                }
            }
        }
        throw new IOException("Не удалось определить длительность");
    }

    private double parseDuration(String duration) {
        String[] parts = duration.split(":");
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        double seconds = Double.parseDouble(parts[2]);
        double v = hours * 3600 + minutes * 60 + seconds;
        log.info("Длина аудио файла {}, {}", duration, v);
        return v;
    }
}

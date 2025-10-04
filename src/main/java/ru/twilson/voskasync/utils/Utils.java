package ru.twilson.voskasync.utils;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class Utils {

    public static <T> List<List<T>> splitList(List<T> list, int parts) {
        int size = list.size();
        if (parts <= 1) {
            return List.of(list);
        }
        int chunkSize = (size + parts - 1) / parts;

        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < parts; i++) {
            int from = i * chunkSize;
            int to = Math.min(from + chunkSize, size);
            if (from < size) {
                result.add(list.subList(from, to));
            } else {
                result.add(Collections.emptyList());
            }
        }
        return result;
    }
}

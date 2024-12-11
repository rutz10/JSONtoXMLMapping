package org.rutz;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ResourceUtil {

    public static String readResourceFileAsString(String fileName) throws Exception {
        ClassLoader classLoader = ResourceUtil.class.getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream(fileName)) {
            if (is == null) {
                throw new IllegalArgumentException("File not found: " + fileName);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}

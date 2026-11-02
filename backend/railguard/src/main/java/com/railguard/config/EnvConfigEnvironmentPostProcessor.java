package com.railguard.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads environment variables from the root .env file into the Spring Environment
 * during startup if not already provided by host environment variables.
 * Enables seamless local development without hardcoding or exposing sensitive secrets.
 */
public class EnvConfigEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String existingSecret = environment.getProperty("JWT_SECRET");
        if (existingSecret != null && !existingSecret.trim().isEmpty()) {
            return;
        }

        // Search for .env file across potential working directory structures
        String userDir = System.getProperty("user.dir", ".");
        Path userDirPath = Paths.get(userDir);

        Path[] candidates = new Path[]{
                Paths.get(".env"),
                Paths.get("../.env"),
                Paths.get("../../.env"),
                userDirPath.resolve(".env"),
                userDirPath.getParent() != null ? userDirPath.getParent().resolve(".env") : null,
                userDirPath.getParent() != null && userDirPath.getParent().getParent() != null
                        ? userDirPath.getParent().getParent().resolve(".env") : null
        };

        for (Path candidate : candidates) {
            if (candidate != null && Files.exists(candidate) && Files.isRegularFile(candidate)) {
                Map<String, Object> props = loadDotEnv(candidate);
                if (!props.isEmpty()) {
                    environment.getPropertySources().addLast(new MapPropertySource("dotenvProperties", props));
                    // Also seed system properties for components relying on system-level resolution
                    props.forEach((k, v) -> {
                        if (System.getProperty(k) == null && v instanceof String) {
                            System.setProperty(k, (String) v);
                        }
                    });
                    break;
                }
            }
        }
    }

    private Map<String, Object> loadDotEnv(Path path) {
        Map<String, Object> props = new HashMap<>();
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eqIdx = line.indexOf('=');
                if (eqIdx > 0) {
                    String key = line.substring(0, eqIdx).trim();
                    String val = line.substring(eqIdx + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    props.put(key, val);
                }
            }
        } catch (IOException ignored) {
        }
        return props;
    }
}


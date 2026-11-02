package com.railguard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Main Spring Boot application entry point for RailGuard AI.
 * Safely discovers and loads environment variables from root .env if not already set.
 */
@SpringBootApplication
public class RailguardApplication {

	public static void main(String[] args) {
		loadEnvironmentVariables();
		SpringApplication.run(RailguardApplication.class, args);
	}

	private static void loadEnvironmentVariables() {
		Path current = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 4 && current != null; i++) {
			Path envFile = current.resolve(".env");
			if (Files.exists(envFile) && Files.isRegularFile(envFile)) {
				try (BufferedReader reader = Files.newBufferedReader(envFile)) {
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
							if (System.getProperty(key) == null && System.getenv(key) == null) {
								System.setProperty(key, val);
							}
						}
					}
				} catch (IOException ignored) {
				}
				break;
			}
			current = current.getParent();
		}
	}
}

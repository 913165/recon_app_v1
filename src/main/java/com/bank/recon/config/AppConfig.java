package com.bank.recon.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.json.JsonMapper;

@ConfigurationProperties("recon.file")
public record AppConfig(InputPaths input, OutputPaths output, String extension) {

    /** Normalized suffix including leading dot, e.g. {@code .txt} or {@code .dat}. Defaults to {@code .txt} if blank. */
    public String fileExtension() {
        String e = (extension == null || extension.isBlank()) ? "txt" : extension.strip();
        if (e.startsWith(".")) {
            e = e.substring(1);
        }
        return "." + e;
    }

    public record InputPaths(String npciPath, String switchPath, String cbsPath) {
        public Path npciDir() {
            return Path.of(npciPath);
        }

        public Path switchDir() {
            return Path.of(switchPath);
        }

        public Path cbsDir() {
            String p = cbsPath;
            if (p == null || p.isBlank()) {
                p = "data/input/cbs";
            }
            return Path.of(p);
        }
    }

    public record OutputPaths(String path) {
        public Path outputDir() {
            return Path.of(path);
        }
    }

    @Bean
    public JsonMapper jsonMapper() {
        return JsonMapper.builder().findAndAddModules().build();
    }
}

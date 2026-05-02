package com.bank.recon.config;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

import tools.jackson.databind.json.JsonMapper;

@ConfigurationProperties("recon.file")
public record AppConfig(InputPaths input, OutputPaths output) {

    public record InputPaths(String npciPath, String switchPath) {
        public Path npciDir() {
            return Path.of(npciPath);
        }

        public Path switchDir() {
            return Path.of(switchPath);
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

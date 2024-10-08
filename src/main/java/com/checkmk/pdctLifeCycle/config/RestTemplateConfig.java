package com.checkmk.pdctLifeCycle.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate getResttemplate(){
        return new RestTemplate();
    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }
}

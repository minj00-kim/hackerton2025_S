package com.hackerton.hackerton2025.Config;


import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ PythonProps.class })


public class AppConfig {}

package org.likelionhsu.hackathon.imageasset.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableConfigurationProperties(
        ImageAssetCleanupProperties.class
)
public class ImageAssetSchedulingConfig {
}

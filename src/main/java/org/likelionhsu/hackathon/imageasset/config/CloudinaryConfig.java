package org.likelionhsu.hackathon.imageasset.config;

import java.util.HashMap;
import java.util.Map;

import com.cloudinary.Cloudinary;

import org.likelionhsu.hackathon.imageasset.storage.CloudinaryImageStorageAdapter;
import org.likelionhsu.hackathon.imageasset.storage.ImageStoragePort;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(
            CloudinaryProperties properties
    ) {
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", properties.cloudName());
        config.put("api_key", properties.apiKey());
        config.put("api_secret", properties.apiSecret());
        config.put("secure", true);

        return new Cloudinary(config);
    }

    @Bean
    public ImageStoragePort imageStoragePort(
            Cloudinary cloudinary
    ) {
        return new CloudinaryImageStorageAdapter(cloudinary);
    }
}

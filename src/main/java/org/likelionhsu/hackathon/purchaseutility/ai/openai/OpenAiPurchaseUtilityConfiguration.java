package org.likelionhsu.hackathon.purchaseutility.ai.openai;

import org.likelionhsu.hackathon.purchaseutility.ai.PurchaseUtilityExplanationPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@Conditional(OpenAiPurchaseUtilityConfiguration.ApiKeyPresentCondition.class)
public class OpenAiPurchaseUtilityConfiguration {

    @Bean
    PurchaseUtilityExplanationPort openAiPurchaseUtilityExplanationPort(
            ObjectMapper objectMapper,
            @Value("${OPENAI_API_KEY}") String apiKey,
            @Value("${OPENAI_MODEL:gpt-5.6-luna}") String model
    ) {
        return new OpenAiPurchaseUtilityExplanationAdapter(
                objectMapper,
                apiKey,
                model
        );
    }

    static final class ApiKeyPresentCondition implements Condition {

        @Override
        public boolean matches(
                ConditionContext context,
                AnnotatedTypeMetadata metadata
        ) {
            return StringUtils.hasText(
                    context
                            .getEnvironment()
                            .getProperty("OPENAI_API_KEY")
            );
        }
    }
}

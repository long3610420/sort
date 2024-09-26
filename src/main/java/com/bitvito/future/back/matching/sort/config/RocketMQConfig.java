package com.bitvito.future.back.matching.sort.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bitvito.matching.rocket")
public class RocketMQConfig {
    private String groupName = "future";
    private String namespace;
    private String topicPrefix;
    private String topicOrderlyPrefix;
    private Integer batchSize;
}

package com.bitvito.future.back.matching.sort.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@ConfigurationProperties(prefix = "bitvito.matching")
//@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
//@Data
public class MatchingConfig {
    private RocketMQConfig rocket;
    private IdGeneratorConfig idGenerator;
    private MatchingSystemConfig system;
    private PulsarConfig pulsar;

    public RocketMQConfig getRocket() {
        return rocket;
    }

    public void setRocket(RocketMQConfig rocket) {
        this.rocket = rocket;
    }

    public IdGeneratorConfig getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(IdGeneratorConfig idGenerator) {
        this.idGenerator = idGenerator;
    }

    public MatchingSystemConfig getSystem() {
        return system;
    }

    public void setSystem(MatchingSystemConfig system) {
        this.system = system;
    }

    public PulsarConfig getPulsar() {
        return pulsar;
    }

    public void setPulsar(PulsarConfig pulsar) {
        this.pulsar = pulsar;
    }
}

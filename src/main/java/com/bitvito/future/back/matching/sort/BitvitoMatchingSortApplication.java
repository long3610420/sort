package com.bitvito.future.back.matching.sort;

//import com.aex.common.base.nacos.annotation.EnableGlobalNacosCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConfigurationPropertiesScan
@SpringBootApplication
@EnableConfigurationProperties
//@EnableGlobalNacosCache
public class BitvitoMatchingSortApplication {

    public static void main(String[] args) {
        try {
            SpringApplication.run(BitvitoMatchingSortApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

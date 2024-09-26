package com.bitvito.future.back.matching.sort.config;

import lombok.Data;

/**
 * @ClassName PulsarConfig
 * @Description
 * @Author zhangjinying
 * @Date 2020/11/27 11:59 上午
 */
@Data
public class PulsarConfig {
    private Boolean createTopic;
    private String url;
    private String topic;
    private String prodTopic;

}

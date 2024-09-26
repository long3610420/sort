package com.bitvito.future.back.matching.sort.config;

import lombok.Data;

@Data
public class IdGeneratorConfig {
    private Integer dataCenterId;
    private Integer machineId;
    private String zkAddress;


}

package com.bitvito.future.back.matching.sort.config;

import lombok.Data;

@Data
public class MatchingSystemConfig {
    private Boolean syncAck = true;
    private String childNodePath;
    private String parentNodePath;
    private String zkAddress;
    private Integer zkTimeOut;
    private String contractName;

}

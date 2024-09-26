//package com.bitvito.future.back.matching.sort.service;
//
//import com.bitvito.future.back.matching.sort.service.MatchingMsgOrderlyService;
//import com.bitvito.future.back.matching.sort.config.MatchingConfig;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.DependsOn;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//import javax.annotation.Resource;
//
//@Slf4j
//@Service
//@DependsOn({"matchingConfig"})
//public class DeaultMatchingMsgOrderlyService extends MatchingMsgOrderlyService {
//
//
//
//    @PostConstruct
//    public void init() {
//        try {
//            super.init();
//        } catch (Exception e) {
//            log.error("init error", e);
//        }
//    }
//}

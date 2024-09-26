package com.bitvito.future.back.matching.sort.service.impl;

import com.bitvito.future.back.matching.sort.service.IdGeneratorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * @Description 基于Redis分布式ID生成实现
 * @author butterfly
 * @date 2017年8月3日 下午2:35:16
 */
@Service
public class JedisIdGeneratorService implements IdGeneratorService {

    /**
     * JedisClient对象
     */
    @Autowired
    private RedisTemplate redisTemplate;


    /**
     * @param date
     * @return
     * @Description
     * @author butterfly
     */
    private String getOrderPrefix(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int year = c.get(Calendar.YEAR);
        int day = c.get(Calendar.DAY_OF_YEAR); // 今天是第多少天
        int hour = c.get(Calendar.HOUR_OF_DAY);
        String dayFmt = String.format("%1$03d", day); // 0补位操作 必须满足三位
        String hourFmt = String.format("%1$02d", hour);  // 0补位操作 必须满足2位
        StringBuffer prefix = new StringBuffer();
        prefix.append((year - 2000)).append(dayFmt).append(hourFmt);
        return prefix.toString();
    }


    /**
     * @param prefix
     * @return
     * @Description 支持一个小时100w个订单号的生成
     * @author butterfly
     */
    private Long incrOrderId(String biz) {
        Long orderId = null;
        String key = "geese:#{biz}:id:".replace("#{biz}", biz); // 00001
        try {
            orderId = redisTemplate.opsForValue().increment(key);
        } catch (Exception ex) {
            System.out.println("分布式订单号生成失败异常。。。。。");
        }
        return orderId;
    }

    /**
     * @return
     * @Description 生成分布式ID
     * @author butterfly
     */
    @Override
    public Long generatorId(String biz) {
        // 转成数字类型，可排序
        return incrOrderId(biz);
    }
}

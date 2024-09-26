package com.bitvito.future.back.matching.sort.service;

import cn.hutool.core.util.IdUtil;
import com.alibaba.druid.pool.DruidDataSource;
import com.bitvito.future.back.matching.sort.idgen.IDGen;
import com.bitvito.future.back.matching.sort.idgen.common.Result;
import com.bitvito.future.back.matching.sort.idgen.common.Status;
import com.bitvito.future.back.matching.sort.idgen.common.ZeroIDGen;
import com.bitvito.future.back.matching.sort.idgen.segment.SegmentIDGenImpl;
import com.bitvito.future.back.matching.sort.idgen.segment.dao.IDAllocDao;
import com.bitvito.future.back.matching.sort.idgen.segment.dao.impl.IDAllocDaoImpl;
import com.bitvito.future.back.matching.sort.util.InitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class SegmentService {
    private Logger logger = LoggerFactory.getLogger(SegmentService.class);

    private IDGen idGen;
    private DruidDataSource dataSource;
//    @Value("${bitvito.mysql.enable}")
    private String enable;
//    @Value("${bitvito.mysql.url}")
    private String url;
//    @Value("${bitvito.mysql.username}")
    private String username;
//    @Value("${bitvito.mysql.password}")
    private String pwd;

    //@PostConstruct
    void init() throws SQLException, InitException {
        //Properties properties = PropertyFactory.getProperties();
        boolean flag = Boolean.parseBoolean(enable);
        if (flag) {
            // Config dataSource
            dataSource = new DruidDataSource();
            //dataSource.setUrl(properties.getProperty(Constant.LEAF_JDBC_URL));
            //dataSource.setUsername(properties.getProperty(Constant.LEAF_JDBC_USERNAME));
            //dataSource.setPassword(properties.getProperty(Constant.LEAF_JDBC_PASSWORD));
            dataSource.setUrl(url);
            dataSource.setUsername(username);
            dataSource.setPassword(pwd);
            dataSource.init();

            // Config Dao
            IDAllocDao dao = new IDAllocDaoImpl(dataSource);

            // Config ID Gen
            idGen = new SegmentIDGenImpl();
            ((SegmentIDGenImpl) idGen).setDao(dao);
            if (idGen.init()) {
                logger.info("Segment Service Init Successfully");
            } else {
                throw new InitException("Segment Service Init Fail");
            }
        } else {
            idGen = new ZeroIDGen();
            logger.info("Zero ID Gen Service Init Successfully");
        }
    }

    public Result getId(String key) {
        Result result =new Result();
        result.setId(  IdUtil.getSnowflakeNextId());
        result.setStatus(Status.SUCCESS);
        return result;
//        return idGen.get(key);
    }

    public SegmentIDGenImpl getIdGen() {
        if (idGen instanceof SegmentIDGenImpl) {
            return (SegmentIDGenImpl) idGen;
        }
        return null;
    }
}

package io.github.mingjia.mybatis.autocache.test.redissonCache.service.impl;

import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.CityMapper;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.CountryMapper;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.po.Country;
import io.github.mingjia.mybatis.autocache.test.redissonCache.service.TestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mingjia on 19/9/3.
 */
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private CountryMapper countryMapper;

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void batchOpt() {
        for(int i=10;i<13;i++){
            Country c = new Country();
            c.setId(i);
            c.setName("country-"+i);
            countryMapper.insertSelective(c);
        }
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void batchOptWithError() {
        batchOpt();
        throw new RuntimeException("");
    }
}

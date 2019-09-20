package io.github.mingjia.mybatis.autocache.test.interceptor.service.impl;

import io.github.mingjia.mybatis.autocache.test.interceptor.dao.CityMapper;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.CountryMapper;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.po.City;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.po.Country;
import io.github.mingjia.mybatis.autocache.test.interceptor.service.TransactionServiceI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Created by mingjia on 19/8/28.
 */
@Service
public class TransactionServiceImpl implements TransactionServiceI {
    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private CountryMapper countryMapper;

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void insertCountryBatch(List<Country> countryList) {
        if (CollectionUtils.isEmpty(countryList))
            return;
        for (Country c : countryList)
            countryMapper.insertSelective(c);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void insertCityBatch(List<City> cityList) {
        if (CollectionUtils.isEmpty(cityList))
            return;
        for (City c : cityList)
            cityMapper.insertSelective(c);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void insertCityBatchWithError(List<City> cityList) {
        insertCityBatch(cityList);
        throw new RuntimeException("事务异常");
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void updateCountryBatch(List<Country> countryList) {
        if (CollectionUtils.isEmpty(countryList))
            return;
        for (Country c : countryList)
            countryMapper.updateSelective(c);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void updateCityBatch(List<City> cityList) {
        if (CollectionUtils.isEmpty(cityList))
            return;
        for (City c : cityList)
            cityMapper.updateSelective(c);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void updateCityBatchWithError(List<City> cityList) {
        updateCityBatch(cityList);
        throw new RuntimeException("事务异常");
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void selectCountryBatch(List<Country> countryList) {
        if (CollectionUtils.isEmpty(countryList))
            return;
        for (Country c : countryList)
            countryMapper.selectSelective(c);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void selectCityBatch(List<City> cityList) {
        if (CollectionUtils.isEmpty(cityList))
            return;
        for (City c : cityList)
            cityMapper.selectSelective(c);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void selectCityBatchWithError(List<City> cityList) {
        selectCityBatch(cityList);
        throw new RuntimeException("事务异常");
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void multiOperate(City city) {
        cityMapper.insertSelective(city);
        cityMapper.updateSelective(city);
        cityMapper.selectSelective(city);
    }

    @Override
    @Transactional(value = "transactionManager", rollbackFor = Throwable.class)
    public void multiOperateWithError(City city) {
        multiOperate(city);
        multiOperate(city);
        throw new RuntimeException("事务异常");

    }
}

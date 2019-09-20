package io.github.mingjia.mybatis.autocache.test.interceptor.service;


import io.github.mingjia.mybatis.autocache.test.interceptor.dao.po.City;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.po.Country;

import java.util.List;

/**
 * Created by mingjia on 19/8/28.
 */
public interface TransactionServiceI {

    void insertCountryBatch(List<Country> countryList);
    void insertCityBatch(List<City> cityList);
    void insertCityBatchWithError(List<City> cityList);


    void updateCountryBatch(List<Country> countryList);
    void updateCityBatch(List<City> cityList);
    void updateCityBatchWithError(List<City> cityList);


    void selectCountryBatch(List<Country> countryList);
    void selectCityBatch(List<City> cityList);
    void selectCityBatchWithError(List<City> cityList);


    void multiOperate(City city);
    void multiOperateWithError(City city);
}

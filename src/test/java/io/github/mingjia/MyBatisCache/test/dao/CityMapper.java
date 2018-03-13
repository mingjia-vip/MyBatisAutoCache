package io.github.mingjia.MyBatisCache.test.dao;

import io.github.mingjia.MyBatisCache.test.dao.po.City;

import java.util.List;

/**
 * city mapper
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public interface CityMapper {
    List<City> selectSelective(City city);
    List<City> selectCitysByCountryId(int countryid);
    List<City> selectCountryAndCitysByCountryId(int countryid);

    int insertSelective(City city);
    int updateSelective(City city);
    int deleteById(int id);

}

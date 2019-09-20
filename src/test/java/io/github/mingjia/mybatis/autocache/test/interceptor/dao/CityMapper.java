package io.github.mingjia.mybatis.autocache.test.interceptor.dao;

import io.github.mingjia.mybatis.autocache.test.interceptor.dao.po.City;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * city mapper
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public interface CityMapper {

    City selectById(@Param("countryid") Integer countryid, @Param("id") Integer id);

    List<City> selectSelective(City city);

    List<City> selectCitysByCountryId(@Param("countryid") int countryid);

    int insertSelective(City city);

    int updateSelective(City city);

    int deleteById(int id);


}

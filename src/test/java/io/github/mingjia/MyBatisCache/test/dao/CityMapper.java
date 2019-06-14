package io.github.mingjia.MyBatisCache.test.dao;

import io.github.mingjia.MyBatisCache.CacheQuery;
import io.github.mingjia.MyBatisCache.test.dao.po.City;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * city mapper
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public interface CityMapper {

    @CacheQuery(primaries = "id=#param1.id")
    List<City> selectSelective(City city);
    @CacheQuery(disCache = true, tables = {"city"})
    List<City> selectSelectiveByMap(Map cityMap);
    @CacheQuery(tables = {"city"})
    List<City> selectByName(@Param("cityName") String cityName);
    List<City> selectCitysByCountryId(@Param("countryid") int countryid);
    List<City> selectCountryAndCitysByCountryId(Integer countryid);
    List<City> selectByIds(@Param("ids") List<Integer> ids);
    @CacheQuery(primaries = "id=#id")
    City selectById(@Param("id") Integer id);

    int insertSelective(City city);
    int updateSelective(City city);
    int deleteById(int id);


}

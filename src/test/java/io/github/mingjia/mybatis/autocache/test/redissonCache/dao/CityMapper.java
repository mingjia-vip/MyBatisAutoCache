package io.github.mingjia.mybatis.autocache.test.redissonCache.dao;

import io.github.mingjia.mybatis.autocache.annoations.AutoCacheEvict;
import io.github.mingjia.mybatis.autocache.annoations.AutoCacheQuery;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.po.City;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * city mapper
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public interface CityMapper {

    @AutoCacheQuery(shard = "#param1")
    City selectById(@Param("countryid") Integer countryid, @Param("id") Integer id);

    @AutoCacheQuery(shard = "#param1.countryid")
    List<City> selectSelective(City city);

    @AutoCacheQuery(shard = "#param1")
    List<City> selectCitysByCountryId(@Param("countryid") int countryid);

    @AutoCacheEvict(evictShards = {"#param1.countryid"})
    int insertSelective(City city);

    @AutoCacheEvict(evictShards = {"#param1.countryid"})
    int updateSelective(City city);

    int deleteById(int id);


}

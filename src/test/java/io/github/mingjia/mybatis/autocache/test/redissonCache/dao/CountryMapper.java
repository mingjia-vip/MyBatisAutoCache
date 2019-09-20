package io.github.mingjia.mybatis.autocache.test.redissonCache.dao;

import io.github.mingjia.mybatis.autocache.annoations.AutoCacheEvict;
import io.github.mingjia.mybatis.autocache.annoations.AutoCacheQuery;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.po.Country;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * country mapper
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public interface CountryMapper {

    @AutoCacheQuery(shard = "#param1.name")
    List<Country> selectSelective(Country country);

    @AutoCacheQuery(shard = "#param1")
    List<Country> selectByName(String countryName);

    @AutoCacheEvict(evictShards = {"#param1.name"})
    int insertSelective(Country country);

    @AutoCacheEvict(evictShards = {"#param1.name"})
    int updateSelective(Country country);

    int deleteById(@Param("id") int id);

}

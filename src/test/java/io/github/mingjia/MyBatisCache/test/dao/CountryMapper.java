package io.github.mingjia.MyBatisCache.test.dao;

import io.github.mingjia.MyBatisCache.test.dao.po.Country;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * country mapper
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public interface CountryMapper {
    List<Country> selectSelective(Country country);
    List<Country> selectByCountryName(String countryName);
    int insertSelective(Country country);
    int updateSelective(Country country);
    int deleteById(@Param("id") int id);

}

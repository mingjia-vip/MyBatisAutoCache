package io.github.mingjia.MyBatisCache.test.dao;

import io.github.mingjia.MyBatisCache.test.dao.po.Country;
import java.util.List;

/**
 * country mapper
 *
 * @auther GuiBin
 * @create 18/3/13
 */
public interface CountryMapper {
    List<Country> selectSelective(Country country);
    int insertSelective(Country country);
    int updateSelective(Country country);
    int deleteById(int id);

}

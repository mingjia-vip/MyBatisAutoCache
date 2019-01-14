package io.github.mingjia.MyBatisCache.test;

import com.github.pagehelper.PageHelper;
import io.github.mingjia.MyBatisCache.MyBatisCacheConfig;
import io.github.mingjia.MyBatisCache.test.dao.CityMapper;
import io.github.mingjia.MyBatisCache.test.dao.CountryMapper;
import io.github.mingjia.MyBatisCache.test.dao.po.City;
import io.github.mingjia.MyBatisCache.test.dao.po.Country;
import io.github.mingjia.MyBatisCache.test.service.DefaultCacheService;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * SelectCache test
 *
 * @auther GuiBin
 * @create 18/3/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:applicationContext-interceptor.xml"})
public class MyBatisInterceptorTest {
    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private CountryMapper countryMapper;
    /**
     * 插件拦截测试
     */
    @Test
    public void singleTest() {
        City city = new City();
        city.setCountryid(1);
        //增加相同方法的分页缓存
        PageHelper.startPage(1,10);
        cityMapper.selectSelective(city);

    }
}

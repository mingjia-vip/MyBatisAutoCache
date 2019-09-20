package io.github.mingjia.mybatis.autocache.test.interceptor;

import com.github.pagehelper.PageHelper;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.CityMapper;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.CountryMapper;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.po.City;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * AutoCacheQuery test
 *
 * @auther GuiBin
 * @create 18/3/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:interceptor/applicationContext-sequence.xml"})
public class MyBatisInterceptorSequenceTest {
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

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
 * MyBatisCache test
 *
 * @auther GuiBin
 * @create 18/3/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:applicationContext-nocache.xml"})
@ContextConfiguration(locations = {"classpath:applicationContext-cache.xml"})
public class MyBatisCacheTest {
    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private CountryMapper countryMapper;
    @Autowired
    private DefaultCacheService cacheService;

    @Before
    public void showConfig(){
        for(String table:MyBatisCacheConfig.DIS_CACHE_METHOD){
            System.err.println(table);
        }
        Iterator<String> it = MyBatisCacheConfig.TABLE_METHOD.keySet().iterator();
        while(it.hasNext()){
            String table = it.next();
            Set<String> methodCodes = MyBatisCacheConfig.TABLE_METHOD.get(table);
            for(String methodCode:methodCodes){
                System.err.println(table+":"+methodCode+":"+MyBatisCacheConfig.METHOD_DESC.get(methodCode));
            }
        }
    }
    /**
     * 单表验证
     * select触发缓存；
     * 增删改清除缓存；
     */
    @Test
    public void singleTest() {
        City city = new City();
        city.setId(1);
        //首次读取数据库
        List<City> list = cityMapper.selectSelective(city);
        //验证缓存
        Assert.assertEquals(1, cacheService.getAllCache().size());
        printCache();
        //第二次读取缓存
        cityMapper.selectSelective(city);
        Assert.assertEquals(1, cacheService.getAllCache().size());
        //新增清除缓存
        City newCity = new City();
        newCity.setId(7);
        newCity.setCountryid(3);
        newCity.setName("Pattaya");
        cityMapper.insertSelective(newCity);
        //验证缓存
        Assert.assertEquals(0, cacheService.getAllCache().size());
        //再次缓存数据
        cityMapper.selectSelective(city);
        Assert.assertEquals(1, cacheService.getAllCache().size());
        //增加相同方法的缓存
        city.setId(2);
        cityMapper.selectSelective(city);
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        Assert.assertEquals(1, map.size());
        Iterator<String> it = map.keySet().iterator();
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            Assert.assertEquals(2, kvs.size());
            break;
        }
        //增加相同方法的分页缓存
        PageHelper.startPage(1,10);
        cityMapper.selectSelective(city);
        map =  cacheService.getAllCache();
        Assert.assertEquals(1, map.size());
        it = map.keySet().iterator();
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            Assert.assertEquals(3, kvs.size());
            break;
        }
        //增加不同方法的缓存,关联查询country
        cityMapper.selectCitysByCountryId(1);
        map =  cacheService.getAllCache();
        Assert.assertEquals(2, map.size());
        printCache();

    }

    /**
     * 关联验证
     * select触发缓存；
     * 增删改清除缓存；
     */
    @Test
    public void mulityTest() {
        singleTest();
        cityMapper.selectCountryAndCitysByCountryId(1);
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        Assert.assertEquals(3, map.size());
        printCache();
        Country country = new Country();
        country.setId(4);
        country.setName("Japan");
        countryMapper.insertSelective(country);
        map =  cacheService.getAllCache();
        Assert.assertEquals(2, map.size());
        printCache();
    }




    private void printResult(List list) {
        if (list != null && list.size() > 0)
            for (Object o : list)
                System.err.println("result:" + o);
        else
            System.err.println("result is empty");
    }

    private void printCache(){
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        if(map!=null && map.size()>0){

            Iterator<String> it = map.keySet().iterator();
            while(it.hasNext()){
                String method = it.next();
                Map<String,Object> subMap = map.get(method);
                Iterator<String> subIt = subMap.keySet().iterator();
                while (subIt.hasNext()) {
                    String key = subIt.next();
                    Object value = subMap.get(key);
                    System.err.println(method+":"+key+":"+value);
                }
            }
        }else{
            System.err.println("cache is empty");
        }
        System.err.println("");
    }
}

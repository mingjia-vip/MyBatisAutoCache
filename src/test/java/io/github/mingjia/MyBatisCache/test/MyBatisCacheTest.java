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
     * 测试1:相同方法、相同参数
     */
    @Test
    public void Test1() {
        City city = new City();
        city.setId(1);
        //首次读取数据库
        List<City> list = cityMapper.selectSelective(city);
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        //验证缓存
        Assert.assertEquals(1, cacheService.getAllCache().size());
        Iterator<String> it = map.keySet().iterator();
        int cacheKeyCount = 0;
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            cacheKeyCount += kvs.size();
        }
        Assert.assertEquals(1, cacheKeyCount);
        printCache();
        //第二次，读取缓存
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
    }

    /**
     * 测试2:相同方法、不同参数
     */
    @Test
    public void Test2() {
        City city = new City();
        city.setId(1);
        //首次读取数据库
        cityMapper.selectSelective(city);
        city.setId(2);
        cityMapper.selectSelective(city);
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        //验证缓存
        Assert.assertEquals(1, cacheService.getAllCache().size());
        Iterator<String> it = map.keySet().iterator();
        int cacheKeyCount = 0;
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            cacheKeyCount += kvs.size();
        }
        Assert.assertEquals(2, cacheKeyCount);
        printCache();
        //第二次，读取缓存
        city.setId(1);
        cityMapper.selectSelective(city);
        city.setId(2);
        cityMapper.selectSelective(city);

        //新增清除缓存
        City newCity = new City();
        newCity.setId(7);
        newCity.setCountryid(3);
        newCity.setName("Pattaya");
        cityMapper.insertSelective(newCity);
        //验证缓存
        Assert.assertEquals(0, cacheService.getAllCache().size());
    }



    /**
     * 测试3:相同方法、不同参数、相同分页
     */
    @Test
    public void Test3() {
        City city = new City();
        //首次读取数据库
        PageHelper.startPage(1,5);
        city.setId(1);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,5);
        city.setId(2);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,5);
        city.setId(100);
        cityMapper.selectSelective(city);
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        //验证缓存
        Assert.assertEquals(2, cacheService.getAllCache().size());//方法本身＋方法_COUNT
        Iterator<String> it = map.keySet().iterator();
        int cacheKeyCount = 0;
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            cacheKeyCount += kvs.size();
        }
        Assert.assertEquals(5, cacheKeyCount);//3count+2query,最后一次query为执行，应为count＝0
        printCache();

        //第二次，读取缓存
        PageHelper.startPage(1,5);
        city.setId(1);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,5);
        city.setId(2);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,5);
        city.setId(100);
        cityMapper.selectSelective(city);


        //新增清除缓存
        City newCity = new City();
        newCity.setId(7);
        newCity.setCountryid(3);
        newCity.setName("Pattaya");
        cityMapper.insertSelective(newCity);
        printCache();
        //验证缓存
        Assert.assertEquals(0, cacheService.getAllCache().size());

    }

    /**
     * 测试4:相同方法、不同参数、不同分页
     */
    @Test
    public void Test4() {
        City city = new City();
        //首次读取数据库
        PageHelper.startPage(1,5);
        city.setId(1);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,10);
        city.setId(1);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,10);
        city.setId(2);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,10);
        city.setId(100);
        cityMapper.selectSelective(city);
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        //验证缓存
        Assert.assertEquals(2, cacheService.getAllCache().size());//方法本身＋方法_COUNT
        Iterator<String> it = map.keySet().iterator();
        int cacheKeyCount = 0;
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            cacheKeyCount += kvs.size();
        }
        Assert.assertEquals(6, cacheKeyCount);//3count+3query,最后一次query为执行，应为count＝0
        printCache();

        //第二次，读取缓存
        PageHelper.startPage(1,5);
        city.setId(1);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,10);
        city.setId(1);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,10);
        city.setId(100);
        cityMapper.selectSelective(city);


        //新增清除缓存
        City newCity = new City();
        newCity.setId(7);
        newCity.setCountryid(3);
        newCity.setName("Pattaya");
        cityMapper.insertSelective(newCity);
        printCache();
        //验证缓存
        Assert.assertEquals(0, cacheService.getAllCache().size());

    }



    /**
     * 测试5:不同方法
     */
    @Test
    public void Test5() {
        City city = new City();
        //首次读取数据库
        PageHelper.startPage(1,5);
        city.setCountryid(1);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,5);
        cityMapper.selectCitysByCountryId(1);
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        //验证缓存
        Assert.assertEquals(4, cacheService.getAllCache().size());//2方法本身＋2方法_COUNT
        Iterator<String> it = map.keySet().iterator();
        int cacheKeyCount = 0;
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            cacheKeyCount += kvs.size();
        }
        Assert.assertEquals(4, cacheKeyCount);//2count+2query
        printCache();

        //第二次，读取缓存
        PageHelper.startPage(1,5);
        city.setId(1);
        cityMapper.selectSelective(city);
        PageHelper.startPage(1,5);
        cityMapper.selectCitysByCountryId(1);

        //新增清除缓存
        City newCity = new City();
        newCity.setId(7);
        newCity.setCountryid(3);
        newCity.setName("Pattaya");
        cityMapper.insertSelective(newCity);
        printCache();
        //验证缓存
        Assert.assertEquals(0, cacheService.getAllCache().size());

    }


    /**
     * 测试6:不同表，不同方法
     */
    @Test
    public void Test6() {
        //首次读取数据库
        PageHelper.startPage(1,5);
        cityMapper.selectCitysByCountryId(1);
        PageHelper.startPage(1,5);
        cityMapper.selectCountryAndCitysByCountryId(1);
        PageHelper.startPage(1,5);
        countryMapper.selectSelective(new Country());
        ConcurrentMap<String, Map<String,Object>> map =  cacheService.getAllCache();
        //验证缓存
        Assert.assertEquals(6, cacheService.getAllCache().size());//3方法本身＋3方法_COUNT
        Iterator<String> it = map.keySet().iterator();
        int cacheKeyCount = 0;
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            cacheKeyCount += kvs.size();
        }
        Assert.assertEquals(6, cacheKeyCount);//2count+2query
        printCache();

        //第二次，读取缓存
        PageHelper.startPage(1,5);
        cityMapper.selectCitysByCountryId(1);
        PageHelper.startPage(1,5);
        cityMapper.selectCountryAndCitysByCountryId(1);
        PageHelper.startPage(1,5);
        countryMapper.selectSelective(new Country());

        //新增清除缓存
        City newCity = new City();
        newCity.setId(7);
        newCity.setCountryid(3);
        newCity.setName("Pattaya");
        cityMapper.insertSelective(newCity);
        printCache();
        //验证缓存
        Assert.assertEquals(2, cacheService.getAllCache().size());
        it = map.keySet().iterator();
        cacheKeyCount = 0;
        while(it.hasNext()){
            Map<String,Object> kvs = map.get(it.next());
            cacheKeyCount += kvs.size();
        }
        Assert.assertEquals(2, cacheKeyCount);//2count+2query
        printCache();

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

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
 * CacheQuery test
 *
 * @auther GuiBin
 * @create 18/3/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:applicationContext-nocache.xml"})
@ContextConfiguration(locations = {"classpath:applicationContext-cache.xml"})
public class MyBatisCacheTest1 {
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
        Iterator<String> it = MyBatisCacheConfig.TABLE_METHODS.keySet().iterator();
        while(it.hasNext()){
            String table = it.next();
            Set<String> methodCodes = MyBatisCacheConfig.TABLE_METHODS.get(table);
            for(String methodCode:methodCodes){
                System.err.println(table+":"+methodCode+":"+MyBatisCacheConfig.METHOD_DESC.get(methodCode));
            }
        }
    }

    /**
     * 测试1:primaryFild
     */
    @Test
    public void Test1() {
        City city1 = cityMapper.selectById(1);
        City city2 = cityMapper.selectById(2);
        List<City> list1 = cityMapper.selectSelective(city1);
        List<City> list2 = cityMapper.selectSelective(city2);
        printCache();
        //修改city2
        city2.setName(city2.getName()+"2");
        cityMapper.updateSelective(city2);
        printCache();
        city2 = cityMapper.selectById(2);
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

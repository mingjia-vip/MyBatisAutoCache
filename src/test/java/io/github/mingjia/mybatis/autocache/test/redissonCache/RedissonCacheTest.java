package io.github.mingjia.mybatis.autocache.test.redissonCache;


import io.github.mingjia.mybatis.autocache.AutoCacheRuntime;
import io.github.mingjia.mybatis.autocache.AutoCacheServiceI;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.CityMapper;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.CountryMapper;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.po.Country;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * AutoCacheQuery test
 *
 * @auther GuiBin
 * @create 18/3/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:applicationContext-nocache.xml"})
@ContextConfiguration(locations = {"classpath:redissonCache/applicationContext.xml"})
public class RedissonCacheTest {
    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private CountryMapper countryMapper;
    @Autowired
    private AutoCacheServiceI cacheService;

    @Before
    public void showConfig(){
        for(String table: AutoCacheRuntime.DIS_CACHE_METHOD){
            System.err.println(table);
        }
        Iterator<String> it = AutoCacheRuntime.TABLE_METHODS.keySet().iterator();
        while(it.hasNext()){
            String table = it.next();
            Set<String> methodCodes = AutoCacheRuntime.TABLE_METHODS.get(table);
            for(String methodCode:methodCodes){
                System.err.println(table+":"+methodCode+":"+ AutoCacheRuntime.METHOD_DESC.get(methodCode));
            }
        }
    }

    /**
     * 测试1:shards
     */
    @Test
    public void TestShards() {
        String name = "China";
        Country query = new Country();
        query.setName(name);
        List<Country> list1 = countryMapper.selectSelective(query);//普通cache
        printCache();
        list1 = countryMapper.selectSelective(query);//use 普通cache
        List<Country> list2 = countryMapper.selectByName(name);//shard redissonCache
        printCache();
        list2 = countryMapper.selectByName(name);//use shard redissonCache

        //insert
        Country add = new Country();
        add.setName("Japan");
        add.setId(4);
        countryMapper.insertSelective(add);
        printCache();

        //update
        Country update = new Country();
        update.setName(name);
        countryMapper.updateSelective(update);
        printCache();

        //delete
        countryMapper.deleteById(4);
        printCache();


    }


    private void printCache(){
        /*ConcurrentMap<String, Map<String,Object>> map =  cacheService.getCache().getAllCache();
        if(map!=null && map.size()>0){

            Iterator<String> it = map.keySet().iterator();
            while(it.hasNext()){
                String method = it.next();
                Map<String,Object> subMap = map.get(method);
                Iterator<String> subIt = subMap.keySet().iterator();
                while (subIt.hasNext()) {
                    String key = subIt.next();
                    Object value = subMap.get(key);
                    System.err.println(method+"="+key+"="+value);
                }
            }
        }else{
            System.err.println("redissonCache is empty");
        }
        System.err.println("");*/
    }
}

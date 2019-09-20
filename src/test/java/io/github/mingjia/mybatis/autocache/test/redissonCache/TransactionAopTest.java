package io.github.mingjia.mybatis.autocache.test.redissonCache;


import io.github.mingjia.mybatis.autocache.AutoCacheRuntime;
import io.github.mingjia.mybatis.autocache.test.redissonCache.service.TestService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Iterator;
import java.util.Set;

/**
 * AutoCacheQuery test
 *
 * @auther GuiBin
 * @create 18/3/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:redissonCache/applicationContext.xml"})
@WebAppConfiguration
public class TransactionAopTest {

    @Autowired
    private TestService testService;

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
    public void TestTransactionAopInterceptor(){
        //testService.batchOpt();
        testService.batchOptWithError();
    }
}

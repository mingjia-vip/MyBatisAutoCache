package io.github.mingjia.mybatis.autocache.test.redissonCache;


import io.github.mingjia.mybatis.autocache.AutoCacheRuntime;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.CityMapper;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.CountryMapper;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.po.City;
import io.github.mingjia.mybatis.autocache.test.redissonCache.dao.po.Country;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * AutoCacheQuery test
 *
 * @auther GuiBin
 * @create 18/3/13
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:redissonCache/applicationContext.xml"})
public class RedissonCacheLockTest {
    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private CountryMapper countryMapper;

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
    public void TestLock() throws InterruptedException {


        ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 100, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(100), new ThreadPoolExecutor.CallerRunsPolicy());

        ThreadPoolExecutor executor1 = new ThreadPoolExecutor(100, 100, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(100), new ThreadPoolExecutor.CallerRunsPolicy());


        Integer countryId = 1;
        /*for(int i=10;i<30;i++){
            City c = new City();
            c.setId(i);
            c.setCountryid(countryId);
            c.setName("city-"+i);
            executor.submit(new InsertCityTask(c));
            executor1.submit(new SelectCitysTask(c));
        }*/

        City c = new City();
        c.setId(10);
        c.setCountryid(countryId);
        c.setName("city10");
        executor.submit(new InsertCityTask(c));
        executor1.submit(new SelectCitysTask(c));

        //System.out.println("已经开启所有的子线程");
        executor.shutdown();
        executor1.shutdown();
        //System.out.println("shutdown()：启动一次顺序关闭，执行以前提交的任务，但不接受新任务。");
        while(true){
            if(executor.isTerminated() && executor1.isTerminated()){
                System.out.println("所有的子线程都结束了！");
                break;
            }
            Thread.sleep(1000);
        }

        System.out.println("=======================");
        List<City> citys = cityMapper.selectCitysByCountryId(countryId);
        System.out.println("city size:"+citys.size());

        if(!CollectionUtils.isEmpty(citys))
            for(City city:citys)
                System.out.println(city);

    }

    /**
     * 测试1:shards
     */
    @Test
    public void TestShardsLock() throws InterruptedException {


        ThreadPoolExecutor executor = new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(100), new ThreadPoolExecutor.CallerRunsPolicy());

        for(int i=10;i<30;i++){
            Country c = new Country();
            c.setId(i);
            c.setName("country-"+i);
            executor.submit(new InsertCountryTask(c));
            //executor.submit(new SelectTask(c));
        }


        System.out.println("已经开启所有的子线程");
        executor.shutdown();
        System.out.println("shutdown()：启动一次顺序关闭，执行以前提交的任务，但不接受新任务。");
        while(true){
            if(executor.isTerminated()){
                System.out.println("所有的子线程都结束了！");
                break;
            }
            Thread.sleep(1000);
        }

    }

    public class InsertCountryTask implements Runnable{

        private Country country;

        public InsertCountryTask(Country country){
            this.country = country;
        }

        @Override
        public void run() {
            countryMapper.insertSelective(country);
            /*try {
                int s = new Random(10).nextInt();
                TimeUnit.SECONDS.sleep(s);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            System.out.println("create country:"+country);
            countryMapper.selectSelective(country);
            System.out.println("select country:"+country);
        }
    }

    public class InsertCityTask implements Runnable{

        private City city;

        public InsertCityTask(City city){
            this.city = city;
        }

        @Override
        public void run() {

            Long start = new Date().getTime();
            /*try {
                int s = new Random(10).nextInt();
                TimeUnit.SECONDS.sleep(s);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            cityMapper.insertSelective(city);
            System.out.println("insert use "+((new Date().getTime()-start)/1000) +"s");
        }
    }

    public class SelectCitysTask implements Runnable{

        private City city;

        public SelectCitysTask(City city){
            this.city = city;
        }

        @Override
        public void run() {

            Long start = new Date().getTime();
            /*try {
                int s = new Random(10).nextInt();
                TimeUnit.SECONDS.sleep(s);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/

            List<City> citys = cityMapper.selectCitysByCountryId(city.getCountryid());

            System.out.println("select use "+((new Date().getTime()-start)/1000) +"s");
        }
    }
}

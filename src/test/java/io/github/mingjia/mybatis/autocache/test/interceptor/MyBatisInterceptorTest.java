package io.github.mingjia.mybatis.autocache.test.interceptor;

import io.github.mingjia.mybatis.autocache.test.interceptor.dao.CityMapper;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.CountryMapper;
import io.github.mingjia.mybatis.autocache.test.interceptor.dao.po.City;
import io.github.mingjia.mybatis.autocache.test.interceptor.service.TransactionServiceI;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.ArrayList;
import java.util.List;
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
@ContextConfiguration(locations = {"classpath:interceptor/applicationContext-interceptor.xml"})
public class MyBatisInterceptorTest {
    @Autowired
    private CityMapper cityMapper;
    @Autowired
    private CountryMapper countryMapper;
    @Autowired
    private TransactionServiceI transactionService;

    /**
     * methodTest
     */
    @Test
    public void methodTest1() {
        System.err.println("==============开启事务，批量新增");
        List<City> cityList = new ArrayList<>();
        int id = 10;
        for (; id < 12; id++) {
            City city = new City();
            city.setId(id);
            city.setName("city" + id);
            city.setCountryid(1);
            cityList.add(city);
        }
        transactionService.insertCityBatch(cityList);

        System.err.println("==============普通新增");
        id++;
        City city = new City();
        city.setId(id);
        city.setName("city" + id);
        city.setCountryid(1);
        cityMapper.insertSelective(city);

        System.err.println("==============开启事务，批量修改");
        transactionService.updateCityBatch(cityList);

        System.err.println("==============普通修改");
        cityMapper.updateSelective(city);

        System.err.println("==============开启事务，批量查询");
        transactionService.selectCityBatch(cityList);

        System.err.println("==============普通查询");
        cityMapper.selectSelective(city);

    }

    /**
     * methodTest
     */
    @Test
    public void methodTest2() {
        System.err.println("==============开启事务，批量新增");
        List<City> cityList = new ArrayList<>();
        int id = 10;
        for (; id < 12; id++) {
            City city = new City();
            city.setId(id);
            city.setName("city" + id);
            city.setCountryid(1);
            cityList.add(city);
        }
        transactionService.insertCityBatch(cityList);

        System.err.println("==============开启事务，批量新增回滚");
        try {
            transactionService.insertCityBatchWithError(cityList);
        } catch (Exception e) {
            System.err.println("==============事务回滚");
        }

        System.err.println("==============普通新增");
        id++;
        City city = new City();
        city.setId(id);
        city.setName("city" + id);
        city.setCountryid(1);
        cityMapper.insertSelective(city);

        System.err.println("==============开启事务，批量修改");
        transactionService.updateCityBatch(cityList);

        System.err.println("==============开启事务，批量修改回滚");
        try {
            transactionService.updateCityBatchWithError(cityList);
        } catch (Exception e) {
            System.err.println("==============事务回滚");
        }

        System.err.println("==============普通修改");
        cityMapper.updateSelective(city);

        System.err.println("==============开启事务，批量查询");

        transactionService.selectCityBatch(cityList);

        System.err.println("==============开启事务，批量查询回滚");
        try {
            transactionService.selectCityBatchWithError(cityList);
        } catch (Exception e) {
            System.err.println("==============事务回滚");
        }

        System.err.println("==============普通查询");
        cityMapper.selectSelective(city);

    }



    /**
     * methodTest
     */
    @Test
    public void methodTest3() {
        System.err.println("==============开启事务");
        List<City> cityList = new ArrayList<>();
        int id = 10;
        City city = new City();
        city.setId(id);
        city.setName("city" + id);
        city.setCountryid(1);
        transactionService.multiOperate(city);

        System.err.println("==============开启事务，回滚");
        try {
            transactionService.multiOperateWithError(city);
        } catch (Exception e) {
            System.err.println("==============事务回滚");
        }

    }

    @Test
    public void mulityThreadTest(){
        ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 100, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(100), new ThreadPoolExecutor.CallerRunsPolicy());

        for(int i=0;i<2;i++){
            executor.submit(new Task());
        }

        executor.shutdown();
        //System.out.println("shutdown()：启动一次顺序关闭，执行以前提交的任务，但不接受新任务。");
        while(true){
            if(executor.isTerminated()){
                System.out.println("所有的子线程都结束了！");
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public class Task implements Runnable{

        @Override
        public void run() {
            methodTest1();
        }
    }
}

### 介绍

### [MyBatis自动缓存](http://blog.csdn.net/mingjia1987/article/details/79424272)
http://blog.csdn.net/mingjia1987/article/details/79424272


MybatisAutoCache主要意在降低缓存使用的复杂度，通过继承mybatis的SqlSessionFactoryBean的方式即可自动实现数据自动缓存（及更新）：
    
    拦截query类型方法，进行缓存；
    拦截update类型方法，根据操作的表名清除缓存中相关联数据；
    支持分布式，保证并发数据一致性；
    兼容pageHelper分页插件
    支持简单的分区功能；


希望喜欢这个小功能的人多多参与，多多指点改正


#### 使用
基于Spring＋MyBatis的配置，只需指定自动缓存的SqlSessionFactoryBean，并引入spring事务拦截处理即可，如下：
```
<!-- 1，指定自动缓存SqlSessionFactoryBean（继承mybatis的SqlSessionFactoryBean） -->
<bean id="sqlSessionFactory" class="io.github.mingjia.mybatis.autocache.SqlSessionFactoryBean">
    
    <!-- mybatis SqlSessionFactoryBean 配置开始（与使用mybatis一致） -->
    <property name="dataSource" ref="dataSource"/>
    <property name="typeAliasesPackage" value="io.github.mingjia.mybatis.autocache.test.redissonCache.dao.po"/>
    <property name="configLocation" value="classpath:mybatis-config.xml"/>
    <property name="mapperLocations" value="classpath:redissonCache/mapper/*.xml"/>
    <property name="plugins">
        <array>
            <!-- pageHelper v5 -->
            <bean class="com.github.pagehelper.PageInterceptor">
                <property name="properties">
                    <value>
                        helperDialect=mysql
                        IDENTITY=MYSQL
                        notEmpty=true
                        reasonable=true
                        supportMethodsArguments=true
                    </value>
                </property>
            </bean>
        </array>
    </property>
    
    <!-- 自动缓存相关配置 -->
    <!--<property name="isCache" value="true"/> --><!-- 是否开启缓存，默认为true -->
    <!--<property name="configProperties" value="mybatisCache.properties"/>--><!-- 指定配置文件,默认读取autoCache.properties -->
    <!--<property name="cacheService" ref="RedisTemplateCacheService"/>--><!-- 自定义cacheService -->
    <!--<property name="pageHelperCountSuffix" value="_COUNT"/> --><!-- 默认_COUNT -->
</bean>

<!-- 2，引入针对spring-tx的拦截处理，保证缓存数据一致性 -->
<bean id="springTransactionInterceptor" class="io.github.mingjia.mybatis.autocache.transaction.aop.SpringTransactionInterceptor" >
</bean>
<aop:config>
    <!--切入点-->
    <aop:pointcut id="methodPoint" expression="execution(* io.github.mingjia.mybatis.autocache.test.redissonCache.service.*.*(..)) "/>
    <aop:advisor pointcut-ref="methodPoint" advice-ref="springTransactionInterceptor" order="0"/>
</aop:config>
    
```

通过以上配置就开启了自动缓存功能，不用退代码进行修改，默认使用redis作为缓存数据库，支持分布式缓存，也可自定义CacheService实现（实现MybatisCacheServiceI即可）;
MyBatisAutoCache比较适合简单场景，针对多租户场景，提供简单的分区支持，需要使用注解增加到Mapper接口方法中，如下所示：
```
public interface CityMapper {

    @AutoCacheQuery(shard = "#param1") // 指定分区为param1，即参数countryId
    City selectById(@Param("countryid") Integer countryid, @Param("id") Integer id);

    @AutoCacheQuery(shard = "#param1.countryid")// 指定分区为param1.countryid，即参数city.countryId
    List<City> selectSelective(City city);

    // 未分区
    List<City> selectCitysByCountryId(@Param("countryid") int countryid);



    @AutoCacheEvict(evictShards = {"#param1.countryid"})// 指定清除分区为param1.countryid，即参数city.countryId
    int insertSelective(City city);

    @AutoCacheEvict(evictShards = {"#param1.countryid"})
    int updateSelective(City city);
}

```
如上所示，再查询和修改City数据的时候，可以指定城市id做为分区依据，这样清除缓存的时候后就不会清除其他城市id的缓存数据（但是会清除没有指定分区的缓存，保证不会有脏数据）。


#### 注意问题
```
1，对于${}方式引入的sql语句无法处理，可能导致缓存数据正确性；
2，在提取select语句时，是通过空参数来模拟获得方法的sql，无法确保sql的完整性，对于针对参数做不同判断而执行不同sql的方法，建议使用@AutoCacheQuery注解，明确声明涉及到表名；
3，@AutoCacheQuery注解也可指定不需要自动缓存的方法；
4，通过注解可以实现简短的数据分区，提高缓存数据的命中；

```


#### 待完善

基本可以使用，但还有完善之处，诸如：
```
1，效率问题：缓存和清理存在锁阻塞，由于使用redisson不支持事务，最好使用jedis活spring-redis实现，但是CacheServiceI接口抽象就需要修改...
2，集成方式：不够灵活，未实现注解方式配置，为了保住数据一致性引入了拦截器，对spirng的@Transactional进行拦截，并且需要在springTx拦截之前，xml配置方式可以简单指定拦截顺序，但注解方式...
3，缓存方式：目前依赖于redis，太重了，应可选择多种场景模式，如：单jvm内，分布式等...
4，使用场景：不适合中大应用使用，不能满足有针对性的缓存功能要求，比如...
...

```
希望喜欢这个小功能的人多多参与指点改正。

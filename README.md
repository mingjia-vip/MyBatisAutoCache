### 介绍

### [MyBatis自动缓存](http://blog.csdn.net/mingjia1987/article/details/79424272)
http://blog.csdn.net/mingjia1987/article/details/79424272


MybatisAutoCache主要意在降低缓存使用的复杂度，通过继承mybatis的SqlSessionFactoryBean的方式即可自动实现数据自动缓存（及更新）：
    
    拦截query类型方法，进行缓存；
    拦截update类型方法，根据操作的表名清除缓存中相关联数据；
    采用redis作为缓存数据库，保证分布式缓存数据一致性；
    支持简单的分区功能；



#### 配置
基于Spring＋MyBatis的配置，更换SqlSessionFactoryBean，并引入spring事务拦截处理，如下：
```
    <bean id="sqlSessionFactory" class="io.github.mingjia.mybatis.autocache.SqlSessionFactoryBean"><!-- 继承mybatis的SqlSessionFactoryBean -->
        <!-- aotu cache setting -->
        <property name="isCache" value="true"/><!-- 是否开启缓存 -->
        <!--<property name="configProperties" value="mybatisCache.properties"/>--><!-- 指定配置文件,默认读取autoCache.properties -->
        <!--<property name="cacheService" ref="RedisTemplateCacheService"/>--><!-- 自定义cacheService -->
        <!--<property name="pageHelperCountSuffix" value="_COUNT"/> --><!-- 默认_COUNT -->
        
        <!-- mybatis SqlSessionFactoryBean setting -->
        <property name="dataSource" ref="dataSource"/>
        <property name="typeAliasesPackage" value="io.github.mingjia.MyBatisCache.test.dao.po"/>
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <property name="mapperLocations" value="classpath:mapper/*.xml"/>
        <property name="plugins">
            <array>
                <!-- pageHelper v5
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
                </bean>-->
                <!-- pageHelper v4 -->
                <bean class="com.github.pagehelper.PageHelper">
                    <property name="properties">
                        <value>
                            dialect=mysql
                        </value>
                    </property>
                </bean>
            </array>
        </property>
    </bean>
    
    <!-- spring事务拦截处理，保证缓存数据一致 -->
    <bean id="springTransactionInterceptor" class="io.github.mingjia.mybatis.autocache.transaction.aop.SpringTransactionInterceptor" >
    </bean>
    <aop:config>
        <!--切入点-->
        <aop:pointcut id="methodPoint" expression="execution(* io.github.mingjia.mybatis.autocache.test.redissonCache.service.*.*(..)) "/>
        <aop:advisor pointcut-ref="methodPoint" advice-ref="springTransactionInterceptor" order="0"/>
    </aop:config>
    
```
提供了默认使用redis作为缓存数据库，支持分布式缓存，也可自定义CacheService实现（实现MybatisCacheServiceI即可）。


#### 注意问题
```
    1，对于${}方式引入的sql语句无法处理，可能导致缓存数据正确性；
    2，在提取select语句时，是通过空参数来模拟获得方法的sql，无法确保sql的完整性，对于针对参数做不同判断而执行不同sql的方法，建议使用@AutoCacheQuery注解，明确声明涉及到表名；
    3，@AutoCacheQuery注解也可指定不需要自动缓存的方法；
    4，通过注解可以实现简短的数据分区，提高缓存数据的命中；

```


#### 待完善

基本可以使用，但还有很多问题，诸如：
```
    1，效率问题：缓存和清理存在锁阻塞，由于使用redisson不支持事务，最好使用jedis活spring-redis实现，但是CacheServiceI接口抽象就需要修改...
    2，集成方式：未实现注解方式配置，为了保住数据一致性引入了拦截器，对spirng的@Transactional进行拦截，并且需要在springTx拦截之前，xml配置方式可以简单指定拦截顺序，但注解方式...
    3，缓存方式：目前依赖于redis，太重了，应可选择多种场景模式，如：单jvm内，分布式等...
    4，使用场景：不适合中大应用使用，不能满足有针对性的缓存功能要求，比如...

```


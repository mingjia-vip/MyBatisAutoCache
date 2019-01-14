### 介绍

### [MyBatis自动缓存](http://blog.csdn.net/mingjia1987/article/details/79424272)
http://blog.csdn.net/mingjia1987/article/details/79424272


MybatisCache主要意在降低缓存使用的复杂度，通过插件的方式引入即可自动实现数据缓存（及更新）：
    
    拦截query类方法，进行缓存；
    拦截update类方法，根据操作的表名清除缓存中相关联数据；
    自动保证数据一致性；


#### 配置
基于Spring＋MyBatis的配置，更换SqlSessionFactoryBean，并引入MyBatisCacheInterceptor插件，如：
```
    <bean id="sqlSessionFactory" class="io.github.mingjia.MyBatisCache.SqlSessionFactoryBean">
        <property name="pageHelperCountSuffix" value="_COUNT"/><!-- 默认_COUNT -->
        <property name="dataSource" ref="dataSource"/>
        <property name="typeAliasesPackage" value="io.github.mingjia.MyBatisCache.test.dao.po"/>
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <property name="mapperLocations" value="classpath:mapper/*.xml"/>
        <property name="plugins">
            <array>
                <bean class="io.github.mingjia.MyBatisCache.MyBatisCacheInterceptor">
                    <property name="cacheService" ref="cacheService"/>
                </bean>
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
    <bean id="cacheService" class="io.github.mingjia.MyBatisCache.test.service.DefaultCacheService" />
```
提供了默认的Google Guava Cache的cacheService实现，实际使用中建议使用外部cache实现，实现接口（MybatisCacheServiceI）即可。


#### 注意问题
```
    1，对于${}方式引如的sql语句无法处理，可能导致缓存数据正确性；
    2，在提取select语句时，是通过空参数来模拟获得方法的sql，无法确保sql的完整性，对于针对参数做不同判断而执行不同sql的方法，建议使用SelectCache注解，明确声明涉及到表名；
    3，SelectCache注解也可指定不需要自动缓存的方法；

```

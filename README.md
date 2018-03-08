### 介绍

### [DAO层的数据缓存实现](http://blog.csdn.net/mingjia1987/article/details/79424272)
http://blog.csdn.net/mingjia1987/article/details/79424272


MybatisCache主要意在降低缓存使用的复杂度，对Mybatis+pagehelper进行包装，实现mapper方法缓存的拦截逻辑：
    
    拦截query类方法，进行缓存（分页缓存逻辑依赖PageHelper）；
    拦截update类方法，根据操作的表名清除缓存中相关联数据；
    自动保证数据一致性；


#### 常规配置
与Spring＋MyBatis的配置类似，更换SqlSessionFactoryBean和MyBatisCacheInterceptor，如：
```
    <bean id="sqlSessionFactory" class="com.xbniao.uc.dao.mybatisCache.SqlSessionFactoryBean">
        <property name="dataSource" ref="dataSource"/>
        <property name="configLocation" value="classpath:mybatis-config.xml"/>
        <property name="mapperLocations">
            <array>
                <value>classpath*:mapper/*.xml</value>
            </array>
        </property>
        <property name="typeAliasesPackage" value="com.xbniao.uc.dao.po"/>
        <property name="plugins">
            <array>
                <bean class="com.xbniao.uc.dao.mybatisCache.MyBatisCacheInterceptor" >
                    <property name="properties">
                        <value>
                            helperDialect=mysql
                            IDENTITY=MYSQL
                            notEmpty=true
                            reasonable=true
                            supportMethodsArguments=true
                        </value>
                    </property>
                    <!--<property name="cacheService" ref="mybatisCacheService"/> 指定缓存实现类（需要实现MybatisCacheServiceI，默认MybatisCacheServiceI.GUAVA_CACHE）-->
                </bean>
            </array>
        </property>
    </bean>
```



package io.github.mingjia.mybatis.autocache.transaction.aop;


import io.github.mingjia.mybatis.autocache.AutoCacheCleanHolder;
import io.github.mingjia.mybatis.autocache.service.redisson.RedissonConfig;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.HashMap;

@Component
@Aspect
public class SpringTransactionInterceptor implements MethodInterceptor {
    private static Logger logger = LoggerFactory.getLogger(SpringTransactionInterceptor.class);

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        Method method = methodInvocation.getMethod();
        if (method.isAnnotationPresent(Transactional.class)) {
            logger.debug("-----SpringTransactionInterceptor begin-----");
            //事务前
            AutoCacheCleanHolder.set(1, new HashMap<>());
            try {
                Object obj = methodInvocation.proceed();
                if (RedissonConfig.mulityThread)
                    AutoCacheCleanHolder.mulityThreadCleanCache(1);
                else
                    AutoCacheCleanHolder.cleanCache(1);
                logger.debug("-----SpringTransactionInterceptor clean end-----");
                return obj;

            } catch (Exception e) {
                AutoCacheCleanHolder.remove();
                logger.debug("-----SpringTransactionInterceptor remove end-----");
                throw e;
            }

        } else {
            return methodInvocation.proceed();
        }
    }
}

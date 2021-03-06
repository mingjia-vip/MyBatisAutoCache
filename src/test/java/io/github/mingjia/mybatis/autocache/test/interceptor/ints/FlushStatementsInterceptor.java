package io.github.mingjia.mybatis.autocache.test.interceptor.ints;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.plugin.*;

import java.util.Properties;

/**
 * @auther GuiBin
 * @create 18/3/29
 */
@Intercepts({
        @Signature(type = Executor.class, method = "flushStatements", args = {})
})
public class FlushStatementsInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        System.err.print("------- [flushStatements] -------");
        if(invocation==null){
            System.err.println(" [no invocation] ");
        }else{
            System.err.println();
            Object[] args = invocation.getArgs();
            if(args!=null)
                for(Object arg:args)
                    System.err.println("------- "+arg);
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {

    }
}

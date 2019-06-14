package io.github.mingjia.MyBatisCache.expression;

import java.lang.reflect.Method;

public class SimpleExpression extends AbstractExpression {

    public Method getFunctionMethod(String methodName) {
        return (Method) eContext.lookupVariable(methodName);
    }
}

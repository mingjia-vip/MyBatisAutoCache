package io.github.mingjia.mybatis.autocache.expression;

import java.lang.reflect.Method;

public class SimpleExpression extends AbstractExpression {

    public Method getFunctionMethod(String methodName) {
        return (Method) eContext.lookupVariable(methodName);
    }
}

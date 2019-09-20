package io.github.mingjia.mybatis.autocache.expression;

import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelCompilerMode;
import org.springframework.expression.spel.SpelParserConfiguration;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;


public class AbstractExpression {

    protected final StandardEvaluationContext eContext = ScenarioCreator.getEvaluationContext();

    protected final SpelParserConfiguration config = new SpelParserConfiguration(SpelCompilerMode.IMMEDIATE, this.getClass().getClassLoader());

    protected final ExpressionParser parser = new SpelExpressionParser(config);

    public <T> T evaluate(String expression, Class<T> expectedResultType) {
        if(expression==null || expectedResultType==null)
            return null;
        Expression expr = parser.parseExpression(expression);
        Object value = expr.getValue(eContext, expectedResultType);
        return (T) value;
    }

    public <T> T evaluate(String expression, Class<T> expectedResultType, Map<String, Object> var) {
        if(expression==null || expectedResultType==null)
            return null;
        Expression expr = parser.parseExpression(expression);
        eContext.setVariables(var);
        Object value = expr.getValue(eContext, expectedResultType);
        return (T) value;
    }

    public Object evaluate(String expression) {
        if(expression==null)
            return null;
        Expression expr = parser.parseExpression(expression);
        Object value = expr.getValue(eContext);
        return value;
    }

    public Object evaluate(String expression, Map<String, Object> var) {
        if(expression==null)
            return null;
        Expression expr = parser.parseExpression(expression);
        eContext.setVariables(var);
        Object value = expr.getValue(eContext);
        return value;
    }

}

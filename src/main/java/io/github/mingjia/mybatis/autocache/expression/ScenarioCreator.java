package io.github.mingjia.mybatis.autocache.expression;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.expression.spel.support.StandardEvaluationContext;

public class ScenarioCreator {

    public static StandardEvaluationContext getEvaluationContext() {
        StandardEvaluationContext context = new StandardEvaluationContext();
//        setupRootContextObject(context);
//        populateVariables(context);
        populateFunctions(context);
        return context;

    }

    private static void populateVariables(StandardEvaluationContext context) {
    }

    private static void setupRootContextObject(StandardEvaluationContext context) {
    }

    private static void populateFunctions(StandardEvaluationContext context) {
        try {

            context.registerFunction("BEGINS", StringUtils.class.getDeclaredMethod("startsWith", CharSequence.class, CharSequence.class));
            context.registerFunction("CONTAINS", StringUtils.class.getDeclaredMethod("contains", CharSequence.class, CharSequence.class));
            context.registerFunction("FIND", StringUtils.class.getDeclaredMethod("indexOf", CharSequence.class, CharSequence.class));
            context.registerFunction("LEFT", StringUtils.class.getDeclaredMethod("left", String.class, int.class));
            context.registerFunction("LEN", StringUtils.class.getDeclaredMethod("length", CharSequence.class));
            context.registerFunction("LOWER", StringUtils.class.getDeclaredMethod("lowerCase", String.class));
            context.registerFunction("LPAD", StringUtils.class.getDeclaredMethod("leftPad", String.class, Integer.TYPE, String.class));
            context.registerFunction("MID", StringUtils.class.getDeclaredMethod("mid", String.class, Integer.TYPE, Integer.TYPE));
            context.registerFunction("RIGHT", StringUtils.class.getDeclaredMethod("right", String.class, Integer.TYPE));
            context.registerFunction("RPAD", StringUtils.class.getDeclaredMethod("rightPad", String.class, Integer.TYPE, String.class));
            context.registerFunction("SUBSTITUTE", StringUtils.class.getDeclaredMethod("replace", String.class, String.class, String.class));
            context.registerFunction("TRIM", StringUtils.class.getDeclaredMethod("trim", String.class));
            context.registerFunction("UPPER", StringUtils.class.getDeclaredMethod("upperCase", String.class));

            context.registerFunction("ISBLANK", StringUtils.class.getDeclaredMethod("isBlank", CharSequence.class));
            //context.registerFunction("ISNUMBER", StringUtils.class.getDeclaredMethod("isNumeric", CharSequence.class));
            context.registerFunction("ISNUMBER", NumberUtils.class.getDeclaredMethod("isNumber", String.class));
            context.registerFunction("BLANKVALUE", StringUtils.class.getDeclaredMethod("defaultIfBlank", CharSequence.class, CharSequence.class));


        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }


}

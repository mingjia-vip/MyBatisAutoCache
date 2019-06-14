package io.github.mingjia.MyBatisCache.test.expression;


import io.github.mingjia.MyBatisCache.expression.SimpleExpression;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class EvalTest {


    private SimpleExpression expression = new SimpleExpression();

    @Test
    public void testIfThenElse() {

        int x = 0;
        System.out.println(expression.evaluate("2<="+x+"?'已付款':("+x+">0?'部分付款':'未付款')", String.class));
        x = 1;
        System.out.println(expression.evaluate("2<="+x+"?'已付款':("+x+">0?'部分付款':'未付款')", String.class));
        x = 2;
        System.out.println(expression.evaluate("2<="+x+"?'已付款':("+x+">0?'部分付款':'未付款')", String.class));
    }

    @Test
    public void testMathOperator() {

        Double d = 3d;
        System.out.println(d);
        System.out.println(expression.evaluate("3", String.class));
        System.out.println(expression.evaluate("3d", String.class));
        System.out.println(expression.evaluate("3.00d", String.class));
        System.out.println(expression.evaluate("3.01d", String.class));
        System.out.println(expression.evaluate("3.010d", String.class));
        System.out.println(expression.evaluate("3.0102d", String.class));
        System.out.println(expression.evaluate("new java.math.BigDecimal(\"10.0100\")", String.class));

        System.out.println(3e0);
        System.out.println(20.55d-24d);
        System.out.println(new BigDecimal("20.55").subtract(new BigDecimal("24")).toString());
        System.out.println(expression.evaluate("new java.math.BigDecimal(\"20.55\") - new java.math.BigDecimal(\"24\")", Double.class));
        System.out.println(expression.evaluate("20.55f-24", Double.class));
        System.out.println(expression.evaluate("20.55f-24f", Double.class));
        System.out.println(expression.evaluate("20.55d-24", Double.class));
        System.out.println(expression.evaluate("20.55d-24d", Double.class));

        assertEquals(6, expression.evaluate("2 + 4", Integer.class).intValue());
        assertEquals(1, expression.evaluate("5 - 4", Integer.class).intValue());
        assertEquals(6, expression.evaluate("3 * 2", Integer.class).intValue());
        assertEquals(3, expression.evaluate("10 / 3", Integer.class).intValue());
        assertEquals(9, expression.evaluate("3 ^ 2", Integer.class).intValue());

        assertEquals(36, expression.evaluate("(3+1) * 3 ^ 2", Integer.class).intValue());
        assertEquals(6.8d, expression.evaluate("2.5d + 4.3d", Double.class), 0.1);
        assertEquals(1.0d, expression.evaluate("5.0d - 4.0d", Double.class), 0.1);
        assertEquals(6.0d, expression.evaluate("3.0d * 2.0d", Double.class), 0.1);
        assertEquals(3.0d, expression.evaluate("9.0d / 3.0d", Double.class), 0.1);
        assertEquals(9.0d, expression.evaluate("3.0d ^ 2", Double.class), 0.1);
        assertEquals(36.0d, expression.evaluate("(3.0d+1.0d) * 3.0d ^ 2", Double.class), 0.1);

    }

    @Test
    public void testLogicalOperator() {
        assertEquals(true, expression.evaluate("null<-1", Boolean.class));
        assertEquals(false, expression.evaluate("3 == 5", Boolean.class));
        assertEquals(true, expression.evaluate("5 eq 5", Boolean.class));
        assertEquals(false, expression.evaluate("5 != 5", Boolean.class));
        assertEquals(true, expression.evaluate("9 > 7", Boolean.class));
        assertEquals(true, expression.evaluate("9 >= 9", Boolean.class));
        assertEquals(true, expression.evaluate("3 < 9", Boolean.class));
        assertEquals(true, expression.evaluate("3.2d <= 9", Boolean.class));
        assertEquals(false, expression.evaluate("true AND false", Boolean.class));
        assertEquals(true, expression.evaluate("true OR false", Boolean.class));
    }

    @Test
    public void testMathFunctions() {

        assertEquals(1, expression.evaluate("#ABS(-1)", Double.class), 0.1d);
        assertEquals(5, expression.evaluate("#CEIL(4.6)", Double.class), 0.1d);
        assertEquals(54.59d, expression.evaluate("#EXP(4)", Double.class), 0.1d);
        assertEquals(4, expression.evaluate("#FLOOR(4.6)", Double.class), 0.1d);
        assertEquals(2.19d, expression.evaluate("#LN(9)", Double.class), 0.1d);
        assertEquals(0.95d, expression.evaluate("#LOG(9)", Double.class), 0.1d);
        assertEquals(5.7d, expression.evaluate("#MAX(4.1,5.7)", Double.class), 0.1d);
        assertEquals(4.1d, expression.evaluate("#MIN(4.1,5.7)", Double.class), 0.1d);
        assertEquals(4.1f, expression.evaluate("#ROUND(4.1)", Long.class), 1.0d);
        assertEquals(2.0d, expression.evaluate("#SQRT(4.0)", Double.class), 0.1d);
    }

    @Test
    public void testTextFunctions() {

        //assertEquals(true,expression.evaluate("''1''!='1'",Boolean.TYPE));
        assertEquals(true, expression.evaluate("50>1 and 1!='1'", Boolean.TYPE));
        assertEquals(true, expression.evaluate("'签字笔采购'!='1'", Boolean.TYPE));
        assertEquals(true, expression.evaluate("null == null", Boolean.class));
        assertEquals(true, expression.evaluate("#BEGINS('abcdefg','ab')", Boolean.TYPE));
        assertEquals(true, expression.evaluate("#CONTAINS('abcdefg','cde')", Boolean.TYPE));
        assertEquals(false, expression.evaluate("!#CONTAINS('abcdefg','cde')", Boolean.TYPE));
        assertEquals(2, expression.evaluate("#FIND('abcdefg','cde')", Integer.TYPE).intValue());
        assertEquals("ab", expression.evaluate("#LEFT('abcdefg',2)", String.class));
        assertEquals(7, expression.evaluate("#LEN('abcdefg')", Integer.TYPE).intValue());
        assertEquals("sddf", expression.evaluate("#LOWER('SDDF')", String.class));
        assertEquals("abSDDF", expression.evaluate("#LPAD('SDDF',6,'abc')", String.class));
        assertEquals("de", expression.evaluate("#MID('abcdefg',3,2)", String.class));
        assertEquals("fg", expression.evaluate("#RIGHT('abcdefg',2)", String.class));
        assertEquals("abXXefg", expression.evaluate("#SUBSTITUTE('abcdefg','cd','XX')", String.class));
        assertEquals("abcdefg", expression.evaluate("#TRIM(' abcdefg  ')", String.class));
        assertEquals("ABCDEFG", expression.evaluate("#UPPER('abcdefg')", String.class));
        assertEquals(true, expression.evaluate("#ISBLANK('')", Boolean.TYPE));
        assertEquals(true, expression.evaluate("#ISBLANK(null)", Boolean.class));
        assertEquals(true, expression.evaluate("#ISNUMBER('123')", Boolean.class));
        assertEquals(true, expression.evaluate("#ISNUMBER('-123')", Boolean.class));
        assertEquals("abc", expression.evaluate("#BLANKVALUE('','abc')", String.class));
    }


    public class JavaBean {
        private String name = "test";

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
    @Test
    public void testMap() {
        Map foo = new HashMap();
        foo.put("NEW", "VALUE");
        foo.put("new", "value");
        foo.put("T", "TV");
        foo.put("t", "tv");
        foo.put("abc.def", "value");
        foo.put("VALUE", 37);
        foo.put("value", 38);
        foo.put("TV", "new");
        foo.put("time", new Date());
        foo.put("food", "fdsfdsfdsfds");
        foo.put("isnull", null);
        foo.put("param1",new JavaBean());
        assertEquals(false, expression.evaluate("#ISBLANK(#t)", Boolean.class, foo));
        //assertEquals(38, expression.evaluate("#MAX(#VALUE,#value)", Double.class, foo).intValue());
        //assertEquals(true, expression.evaluate("#DATEVALUE('1974-08-24') < #time", boolean.class, foo));
        assertEquals(true, expression.evaluate("#value+1 >= #VALUE", boolean.class, foo));
        assertEquals("ds", expression.evaluate("#ISBLANK(#food)? #LEFT(#food,2):#RIGHT(#food,2)", String.class, foo));
        assertEquals(true, expression.evaluate("#isnull == null", Object.class, foo));

        System.out.println(expression.evaluate("#param1.name", String.class, foo));
    }


}

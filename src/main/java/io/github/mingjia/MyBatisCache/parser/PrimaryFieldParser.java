package io.github.mingjia.MyBatisCache.parser;

import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.*;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mingjia on 19/5/10.
 */
public class PrimaryFieldParser {

    private static final Logger logger = LoggerFactory.getLogger(PrimaryFieldParser.class);

    public static Object parserWhere(SelectBody selectBody, String targetTable, String targetField) {
        if (selectBody instanceof PlainSelect) {
            PlainSelect plainSelect = (PlainSelect) selectBody;
            FromItem fromItem = plainSelect.getFromItem();
            if (fromItem != null) {
                System.out.println(fromItem);
                System.out.println("is SubSelect:" + (fromItem instanceof SubSelect));
                if (fromItem instanceof SubSelect) {
                    parserWhere(((SubSelect) fromItem).getSelectBody(), targetTable, targetField);
                }
                if (fromItem instanceof Table) {
                    Table table = (Table) fromItem;
                    if (table.getName().equals(targetTable)) {
                        Expression where = plainSelect.getWhere();
                        Object value = parserWhereField(where, targetTable, targetField);
                        return value;
                    }
                }
            }


        } else if (selectBody instanceof WithItem) {
            WithItem withItem = (WithItem) selectBody;
            if (withItem.getSelectBody() != null) {
                parserWhere(withItem.getSelectBody(), targetTable, targetField);
            }
        } else {
            SetOperationList operationList = (SetOperationList) selectBody;
            if (operationList.getSelects() != null && operationList.getSelects().size() > 0) {
                List<SelectBody> plainSelects = operationList.getSelects();
                parserWhere(plainSelects.get(plainSelects.size() - 1), targetTable, targetField);
            }
        }
        return null;
    }


    public static Object parserWhereField(Expression expression, String targetTable, String targetField) {
        if (expression != null) {

            System.out.println("expression:" + expression);
            //System.out.print("expression class:"+expression.getClass());
            if (expression instanceof EqualsTo) {
                System.out.println(" is EqualsTo");
                EqualsTo equalsTo = (EqualsTo) expression;
                if (equalsTo.getLeftExpression().toString().toLowerCase().equals(targetField.toLowerCase()) || equalsTo.getLeftExpression().toString().toLowerCase().endsWith("." + targetField.toLowerCase())) {
                    System.out.println("value class: " + equalsTo.getRightExpression().getClass());
                    if (equalsTo.getRightExpression() instanceof SubSelect)
                        return parserWhere(((SubSelect) equalsTo.getRightExpression()).getSelectBody(), targetTable, targetField);
                    else
                        return equalsTo.getRightExpression();
                }

            } else if (expression instanceof BinaryExpression) {
                System.out.println(" is BinaryExpression");
                BinaryExpression binaryExpression = (BinaryExpression) expression;
                Object value = parserWhereField(binaryExpression.getLeftExpression(), targetTable, targetField);
                if (value != null)
                    return value;
                value = parserWhereField(binaryExpression.getRightExpression(), targetTable, targetField);
                if (value != null)
                    return value;

            } else if (expression instanceof Parenthesis) {
                System.out.println(" is Parenthesis");
                Parenthesis parenthesis = (Parenthesis) expression;
                return parserWhereField(parenthesis.getExpression(), targetTable, targetField);

            }

        }
        return null;
    }


    //public static final String KEEP_ORDERBY = "/*keep orderby*/";

    public static Map<String,Object> getSelectPrimarySql(String sql, String targetTable, String[] primaryFields) {
        Map<String,Object> rMap = new HashMap<>(2);
        rMap.put("removeParamCount",0);

        //解析SQL
        Statement stmt = null;
        /*//特殊sql不需要去掉order by时，使用注释前缀
        if(sql.indexOf(KEEP_ORDERBY) >= 0){
            return getSimpleCountSql(sql);
        }*/
        try {
            stmt = CCJSqlParserUtil.parse(sql);
        } catch (Throwable e) {
            //无法解析的用一般方法返回count语句
            return null;
        }
        if (stmt instanceof Delete) {
            Delete delete = (Delete) stmt;
            Table delTable = delete.getTable();
            if (!delTable.getName().toLowerCase().endsWith(targetTable.toLowerCase())) {
                logger.error("getSelectPrimarySql:删除表与目标表不一致");
                return null;
            }
            String alias = delTable.getAlias() == null ? "" : delTable.getAlias().getName()+".";
            StringBuffer result = new StringBuffer("select ");
            for (int i = 0; i < primaryFields.length; i++) {
                if (i != 0)
                    result.append(",");
                result.append(alias).append(primaryFields[i]).append(" ");
            }
            String fromStr = " from ";
            String subSql = delete.toString().substring(delete.toString().toLowerCase().indexOf(fromStr) + fromStr.length()).toLowerCase();
            result.append(subSql);
            //return result.toString();
            rMap.put("stmtType","Delete");
            rMap.put("sql",result.toString());

        } else if (stmt instanceof Update) {
            Update update = (Update) stmt;
            for (Table t : update.getTables()) {
                if(t.getName().toLowerCase().equals(targetTable.toLowerCase())){
                    String alias = t.getAlias() == null ? "" : t.getAlias().getName()+".";
                    StringBuffer result = new StringBuffer("select ");
                    for (int i = 0; i < primaryFields.length; i++) {
                        if (i != 0)
                            result.append(",");
                        result.append(alias).append(primaryFields[i]).append(" ");
                    }
                    String updateStr = "update ";
                    String tableSql = update.toString().substring(update.toString().toLowerCase().indexOf(updateStr) + updateStr.length()).toLowerCase();
                    tableSql = tableSql.substring(0, tableSql.indexOf(" set "));
                    result.append(" from ").append(tableSql).append(" ");
                    result.append(" where ").append(update.getWhere().toString());
                    //return result.toString();
                    rMap.put("stmtType","Update");
                    rMap.put("sql",result.toString());
                    rMap.put("removeParamCount",getCount(update.toString(),"?") - getCount(result.toString(),"?"));
                }

            }


        } else if (stmt instanceof Insert) {
            rMap.put("stmtType","Insert");
        }

        return rMap;
    }


    private static int getCount(String str, String tag) {
        int index = 0;
        int count = 0;
        while ((index = str.indexOf(tag)) != -1 ) {
            str = str.substring(index + tag.length());
            count++;
        }
        return count;
    }
}

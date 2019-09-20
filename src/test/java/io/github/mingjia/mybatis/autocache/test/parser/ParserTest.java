package io.github.mingjia.mybatis.autocache.test.parser;

import io.github.mingjia.mybatis.autocache.parser.PrimaryFieldParser;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;

/**
 * Created by mingjia on 19/5/10.
 */
public class ParserTest {

    public static void main(String[] args) throws JSQLParserException {
        //testParserWhereField();

        String sql = "delete a from xbn_user a,app_user_info b where a.id >10 and a.id=b.user_id";
        Statement statement = CCJSqlParserUtil.parse(sql);
        Delete deleteStatement = (Delete) statement;
        Table delTable = deleteStatement.getTable();

    }


    public static void testParserWhereField() throws JSQLParserException {
        String sql = "select * from xbn_user a where a.id=123";
        //sql = "select a.id from xbn_user a where a.id=123 and a.value<>3 order by a.id";
        //sql = "select a.id from xbn_user a where a.id=(select id from xbn_user where id=123) order by a.id";
        //sql = "select aa.sumv from ( select a.id,sum(a.user_tag) as sumv from xbn_user a where a.aid=100000000000000001 and (a.id=456 or a.bid=789) group by a.id) aa";
        Statement statement = CCJSqlParserUtil.parse(sql);
        Select selectStatement = (Select) statement;
        SelectBody selectBody = selectStatement.getSelectBody();
        Object whereValue = PrimaryFieldParser.parserWhere(selectBody, "xbn_user", "id");
        System.out.println("whereValue:" + whereValue);
        /*TablesNamesFinder tablesNamesFinder = new TablesNamesFinder();
        List<String> tableList = tablesNamesFinder.getTableList(selectStatement);
        for (String tableName : tableList)
            System.out.println(tableName);*/
    }
}

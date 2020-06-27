package snippetGenerationTest;

import snippetAlgorithm.DualCES;
import util.DBUtil;
import util.SnippetUtil;
import util.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class DualCESTest {

    private void generateDualCESSnippet(List<String> keywords, int dataset){
        Connection connection = new DBUtil().conn;
        try {
            DualCES finder = new DualCES(dataset, keywords, connection);
            finder.findSnippet();
            String snippetStr = getResultString(dataset, finder.result);
            String keyword = keywords.get(0);
            for (int i = 1; i < keywords.size(); i++){
                keyword += " " + keywords.get(i);
            }
            SnippetUtil.showSnippet(dataset, snippetStr);
            saveResult(dataset, keyword, snippetStr);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private String getResultString(int dataset, int[] result){
        Connection connection = new DBUtil().conn;
        String snippetStr = "";
        Set<Integer> ids = new HashSet<>();
        String select = "select subject,predicate,object from triple where dataset_local_id=" + dataset +" and triple_id=? limit 1";
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet;
            for (int iter: result){
                selectStatement.setInt(1, iter);
                resultSet = selectStatement.executeQuery();
                resultSet.next();
                int s = resultSet.getInt("subject");
                int p = resultSet.getInt("predicate");
                int o = resultSet.getInt("object");
                ids.add(s);
                ids.add(o);
                snippetStr += s + " " + o + " " + p + ",";
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                connection.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        String snippetStr0 = "";
        for (int iter: ids){
            snippetStr0 += iter + ",";
        }
        snippetStr0 = snippetStr0.substring(0, snippetStr0.length()-1);
        snippetStr0 += ";" + snippetStr;
        return snippetStr0.substring(0, snippetStr0.length()-1);
    }

    private void saveResult(int dataset, String keyword, String snippet) {
        Connection connection = new DBUtil().conn;
        String insert = "insert into snippet(dataset_local_id,algorithm,keyword,snippet) values (?,?,?,?)";
        try {
            PreparedStatement insertStatement = connection.prepareStatement(insert);
            insertStatement.setInt(1, dataset);
            insertStatement.setString(2, "DualCES");
            insertStatement.setString(3, keyword);
            insertStatement.setString(4, snippet);
            insertStatement.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
        } finally {
            try {
                connection.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){
        DualCESTest test = new DualCESTest();
        /**NOTE: 1, you can change the input keywords in following lines,
         * BUT BE SURE to use StringUtil.processKeyword() to get the stem of each keyword in advance.
         * 2, this method includes random sampling, it's POSSIBLE to output different results in different runs*/
        ArrayList<String> keywords = new ArrayList<>();
        keywords.add(StringUtil.processKeyword("London"));
        keywords.add(StringUtil.processKeyword("Berlin"));
        keywords.add(StringUtil.processKeyword("Europe"));
        test.generateDualCESSnippet(keywords, 1);
    }

}

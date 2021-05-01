package snippetGenerationTest;

import beans.weightedTriple;
import snippetAlgorithm.KSDSnippet;
import util.DBUtil;
import util.SnippetUtil;
import util.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KSDSnippetTest {

    private void generateKSDSnippet(List<String> keywords, int dataset){
        KSDSnippet finder = new KSDSnippet(dataset);
        Set<weightedTriple> result = finder.findSnippet(keywords);
        Set<Integer> ids = new HashSet<>();
        String tripleStr = "";
        for (weightedTriple iter: result){
            int sid = iter.getSid();
            int oid = iter.getOid();
            int pid = iter.getPid();
            ids.add(sid);
            ids.add(oid);
            tripleStr += sid+" "+oid+" "+pid+",";
        }
        String idstr = "";
        for (int iter: ids){
            idstr += iter+",";
        }
        String snippetStr = idstr.substring(0, idstr.length()-1)+";"+tripleStr.substring(0, tripleStr.length()-1);
        String keyword = keywords.get(0);
        for (int i = 1; i < keywords.size(); i++){
            keyword += " " + keywords.get(i);
        }
        SnippetUtil.showSnippet(dataset, snippetStr);
        saveResult(dataset, keyword, snippetStr);
    }

    private void saveResult(int dataset, String keyword, String snippet) {
        Connection connection = new DBUtil().conn;
        String insert = "insert into snippet(dataset_local_id,algorithm,keyword,snippet) values (?,?,?,?)";
        try {
            PreparedStatement insertStatement = connection.prepareStatement(insert);
            insertStatement.setInt(1, dataset);
            insertStatement.setString(2, "KSD");
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
        KSDSnippetTest test = new KSDSnippetTest();
        /**NOTE: you can change the input keywords in following lines,
         * BUT BE SURE to use StringUtil.processKeyword() to get the stem of each keyword in advance. */
        ArrayList<String> keywords = new ArrayList<>();
        keywords.add(StringUtil.processKeyword("London"));
        keywords.add(StringUtil.processKeyword("Berlin"));
        keywords.add(StringUtil.processKeyword("Europe"));
        test.generateKSDSnippet(keywords, 1);
    }
}

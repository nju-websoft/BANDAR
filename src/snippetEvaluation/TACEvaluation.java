package snippetEvaluation;

import beans.triple;
import util.DBUtil;
import util.SnippetUtil;
import util.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Set;

public class TACEvaluation {

    private void evaluateTACSnippet(int dataset, ArrayList<String>keywords, ArrayList<String>querywords){
        String keyword = keywords.get(0);
        for (int i = 1; i < keywords.size(); i++){
            keyword += " " + keywords.get(i);
        }
        Connection connection = new DBUtil().conn;
        String select = "select snippet from snippet where algorithm = 'TA+C' and keyword = '" + keyword + "' and dataset_local_id = " + dataset + " limit 1";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(select);
            ResultSet resultSet = preparedStatement.executeQuery();
            String snippetStr = "";
            while (resultSet.next()){
                snippetStr = resultSet.getString("snippet");
            }
            if (snippetStr.equals("")){
                System.out.println("Snippet doesn't exist. (Should be generated first. )");
                return;
            }
            SnippetUtil.showSnippet(dataset, snippetStr);///////////////////////////////////////
            Evaluations eva = new Evaluations(dataset, connection);
            Set<triple> snippet = eva.formSnippet(snippetStr);
            double skmrep, entrep, descrep, linkrep, kwrel, qryrel;
            skmrep = eva.SkmRep(snippet);
            entrep = eva.EntRep(snippet);
            descrep = eva.DescRep(snippet);
            linkrep = eva.LinkRep(snippet);
            kwrel = eva.KwRel(snippet, keywords);
            qryrel = eva.QryRel(snippet, keywords, querywords);
            System.out.println("SkmRep = " + skmrep);
            System.out.println("EntRep = " + entrep);
            System.out.println("DescRep = " + descrep);
            System.out.println("LinkRep = " + linkrep);
            System.out.println("KwRel = " + kwrel);
            System.out.println("QryRel = " + qryrel);
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

    public static void main(String[] args){
        TACEvaluation test = new TACEvaluation();
        ArrayList<String> keywords = new ArrayList<>();
        keywords.add(StringUtil.processKeyword("London"));
        keywords.add(StringUtil.processKeyword("Berlin"));
        keywords.add(StringUtil.processKeyword("Europe"));
        /**NOTE: in practice, words in the original query may not be completely contained in the dataset, in this circumstance,
         * the input keyword is the part of query words contained by the dataset.
         * In this example, all query words are contained in the dataset, so the keywords are identical to the query words. */
        test.evaluateTACSnippet(1, keywords, keywords);
    }
}

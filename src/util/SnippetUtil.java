package util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SnippetUtil {
    public static void showSnippet(int dataset, String snippetStr){
        /**Translate the snippetStr(what is stored in the database) to readable triples*/
        Connection connection = new DBUtil().conn;
        String select = "select uri,id,is_literal from uri_label_id where dataset_local_id = " + dataset;
        HashMap<Integer, String> id2URIMap = new HashMap<>();
        HashSet<Integer> literalSet = new HashSet<>();
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()){
                int id = resultSet.getInt("id");
                String uri = resultSet.getString("uri");
                id2URIMap.put(id, uri);
                if (resultSet.getInt("is_literal") == 1){
                    literalSet.add(id);
                }
            }
            //finish building the map
            System.out.println("Snippet result: ");
            Set<Integer> ids = new HashSet<>();
            String vertex;
            if (snippetStr.contains(";")){
                vertex = snippetStr.substring(0, snippetStr.indexOf(";"));
                String[] triples = snippetStr.substring(snippetStr.indexOf(";") + 1).split(",");
                for (String iter: triples){
                    String[] item = iter.split(" ");
                    int sid = Integer.parseInt(item[0]);
                    int oid = Integer.parseInt(item[1]);
                    int pid = Integer.parseInt(item[2]);
                    ids.add(sid);
                    ids.add(oid);
                    String subject = id2URIMap.get(sid);
                    String object = id2URIMap.get(oid);
                    String predicate = id2URIMap.get(pid);
                    if (subject.startsWith("_:")) System.out.print(subject + " ");
                    else System.out.print("<" + subject + "> ");
                    System.out.print("<" + predicate + "> ");
                    if (literalSet.contains(oid)) System.out.println("\"" + object + "\" .");
                    else System.out.println("<" + object + "> .");
                }
            }
            else vertex = snippetStr;// if there are single nodes in the snippet
            String check = "select subject from triple where subject = ? and dataset_local_id = " +dataset;
            for (String iter: vertex.split(",")){
                int id = Integer.parseInt(iter);
                if (!ids.contains(id)){
                    PreparedStatement checkStatement = connection.prepareStatement(check);
                    checkStatement.setInt(1, id);
                    ResultSet checkResultSet = checkStatement.executeQuery();
                    if (checkResultSet.next()){
                        String subject = id2URIMap.get(id);
                        if (subject.startsWith("_:")) System.out.print(subject + "   .");
                        else System.out.print("<" + subject + ">   .");
                    }
                    else {
                        String object = id2URIMap.get(id);
                        if (literalSet.contains(id)) System.out.println("  \"" + object + "\" .");
                        else System.out.println("  <" + object + "> .");
                    }
                }
            }
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
}

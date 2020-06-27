package snippetGenerationTest;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import snippetAlgorithm.TACSnippet;
import util.DBUtil;
import util.SnippetUtil;
import util.StringUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class TACSnippetTest {

	private void generateTACSnippet(List<String> keywords, int dataset){
		TACSnippet finder = new TACSnippet(dataset);
		Multigraph<Integer, DefaultWeightedEdge> result = finder.findSnippet(keywords);
		String snippetStr = "";
		if(result != null){
			if(result.vertexSet().size() == 0)
				snippetStr = "";
			else {
				StringBuilder sb = new StringBuilder();
				for(Integer v : result.vertexSet())
					sb.append(v+",");
				sb.replace(sb.length()-1, sb.length(), ";");
				for(DefaultWeightedEdge edge : result.edgeSet()){
					int predicate = (int) result.getEdgeWeight(edge);
					if(predicate > 0)
						sb.append(result.getEdgeSource(edge)+" "+result.getEdgeTarget(edge)+" "+predicate+",");
					else
						sb.append(result.getEdgeTarget(edge)+" "+result.getEdgeSource(edge)+" "+(-predicate)+",");
				}
				snippetStr = sb.substring(0, sb.length()-1);
			}
		}
		String keyword = keywords.get(0);
		for(int i = 1; i < keywords.size(); i++){
			keyword += " "+keywords.get(i);
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
			insertStatement.setString(2, "TA+C");
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
		TACSnippetTest test = new TACSnippetTest();
		/**NOTE: you can change the input keywords in following lines,
		 * BUT BE SURE to use StringUtil.processKeyword() to get the stem of each keyword in advance. */
		ArrayList<String> keywords = new ArrayList<>();
		keywords.add(StringUtil.processKeyword("London"));
		keywords.add(StringUtil.processKeyword("Berlin"));
		keywords.add(StringUtil.processKeyword("Europe"));
		test.generateTACSnippet(keywords, 1);
	}

}

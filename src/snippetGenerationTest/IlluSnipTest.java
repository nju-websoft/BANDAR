package snippetGenerationTest;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import snippetAlgorithm.IlluSnip;
import util.DBUtil;
import util.SnippetUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;

public class IlluSnipTest {

	private void generateIllustrativeSnippet(int dataset){
		IlluSnip finder = new IlluSnip(dataset);
		Multigraph<Integer, DefaultWeightedEdge> result = finder.findSnippet(new ArrayList<>());
		String snippetStr = "";
		if(result != null){
			StringBuilder sb = new StringBuilder();
			for(Integer v : result.vertexSet()){
				sb.append(v+",");
			}
			sb.replace(sb.length()-1, sb.length(), ";");
			for(DefaultWeightedEdge edge : result.edgeSet())
				sb.append(result.getEdgeSource(edge)+" "+result.getEdgeTarget(edge)+" "+(int)result.getEdgeWeight(edge)+",");
			snippetStr = sb.substring(0, sb.length()-1);
		}
		SnippetUtil.showSnippet(dataset, snippetStr);
		saveResult(dataset, snippetStr);
	}

	private void saveResult(int dataset, String snippet){
		Connection connection = new DBUtil().conn;
		String insert = "insert into snippet(dataset_local_id,algorithm,snippet) values (?,?,?)";
		try {
			PreparedStatement insertStatement = connection.prepareStatement(insert);
			insertStatement.setInt(1, dataset);
			insertStatement.setString(2, "IlluSnip");
			insertStatement.setString(3, snippet);
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
		IlluSnipTest test = new IlluSnipTest();
		/**NOTE: this method is irrelevant to the keywords, it's only input parameter is the dataset id */
		test.generateIllustrativeSnippet(1);
	}
}

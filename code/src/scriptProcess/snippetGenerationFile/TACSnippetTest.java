package scriptProcess.snippetGenerationFile;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import scriptProcess.snippetAlgorithmFile.TACSnippet;
import util.SnippetUtil;
import util.StringUtil;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class TACSnippetTest {

	public static void generateTACSnippet(List<String> keywords, String folder){
		TACSnippet finder = new TACSnippet();
		Multigraph<Integer, DefaultWeightedEdge> result = finder.findSnippet(keywords, folder);
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
		System.out.println("TA+C result: ");
		SnippetUtil.showSnippet(folder, snippetStr);
		saveResult(folder, keyword, snippetStr);
	}

	private static void saveResult(String folder, String keyword, String snippet) {
		try {
			PrintWriter writer = new PrintWriter(folder + "/tac-record.txt");
			writer.println(snippet);
			writer.println(keyword);
			writer.close();
			System.out.println("Snippet record has been stored in " + folder + "/tac-record.txt");
		}catch (Exception e){
			e.printStackTrace();
		}
	}

//	public static void main(String[] args){
//		/**NOTE: you can change the input keywords in following lines,
//		 * BUT BE SURE to use StringUtil.processKeyword() to get the stem of each keyword in advance. */
//		List<String> keywords = new ArrayList<>();
//		keywords.add(StringUtil.processKeyword("London"));
//		keywords.add(StringUtil.processKeyword("Berlin"));
//		keywords.add(StringUtil.processKeyword("Europe"));
//		String baseFolder = "C:/Users/xiaxiawang/Desktop";
//		generateTACSnippet(keywords, baseFolder);
//	}

}

package scriptProcess.snippetGenerationFile;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import scriptProcess.snippetAlgorithmFile.IlluSnip;
import util.SnippetUtil;

import java.io.PrintWriter;

public class IlluSnipTest {

	public static void generateIllustrativeSnippet(String folder){
		IlluSnip finder = new IlluSnip();
		Multigraph<Integer, DefaultWeightedEdge> result = finder.findSnippet(folder);
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
		System.out.println("IlluSnip result: ");
		SnippetUtil.showSnippet(folder, snippetStr);
		saveResult(folder, snippetStr);
	}

	private static void saveResult(String folder, String snippet){
		try {
			PrintWriter writer = new PrintWriter(folder + "/illusnip-record.txt");
			writer.println(snippet);
			writer.close();
			System.out.println("Snippet record has been stored in " + folder + "/illusnip-record.txt");
		}catch (Exception e){
			e.printStackTrace();
		}
	}

//	public static void main(String[] args){
//		/**NOTE: this method is irrelevant to the keywords, it's only input parameter is the dataset id */
//		String baseFolder = "C:/Users/xiaxiawang/Desktop";
//		generateIllustrativeSnippet(baseFolder);
//	}
}

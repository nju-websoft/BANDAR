package scriptProcess.snippetGenerationFile;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import scriptProcess.snippetAlgorithmFile.PrunedDPSnippet;
import util.ReadFile;
import util.SnippetUtil;

import java.io.PrintWriter;
import java.util.*;

public class PrunedDPGenerateTest {

	public static void generatePrunedDPSnippet(List<String> keywords, String folder){
		PrunedDPSnippet finder = new PrunedDPSnippet();
		Multigraph<Integer, DefaultWeightedEdge> result = finder.findSnippet(keywords, folder);
		String snippetStr = "";
		if(result != null){
			if(result.vertexSet().size() == 0)
				snippetStr = "";
			else {
				StringBuilder sb = new StringBuilder();
				for(Integer v : result.vertexSet()){
					sb.append(v+",");
				}
				sb.replace(sb.length()-1, sb.length(), ";");

				Map<List<Integer>, Integer> predicateMap = new HashMap<>();
				for (List<Integer> iter: ReadFile.readInteger(folder + "/indexes/triple.txt", "\t")) {
					List<Integer> so = new ArrayList<>(Arrays.asList(iter.get(0), iter.get(2)));
					if (!predicateMap.containsKey(so)) {
						predicateMap.put(so, iter.get(1));
					}
				}
				for(DefaultWeightedEdge edge : result.edgeSet()){
					int predicate = predicateMap.getOrDefault(new ArrayList<>(Arrays.asList(result.getEdgeSource(edge), result.getEdgeTarget(edge))), 0);
					if (predicate == 0) {
						predicate = predicateMap.get(new ArrayList<>(Arrays.asList(result.getEdgeTarget(edge), result.getEdgeSource(edge))));
						sb.append(result.getEdgeTarget(edge)+" "+result.getEdgeSource(edge)+" "+ predicate + ",");
					}
					else {
						sb.append(result.getEdgeSource(edge)+" "+result.getEdgeTarget(edge)+" "+predicate+",");
					}
				}
				snippetStr = sb.substring(0, sb.length()-1);
			}
		}
		String keyword = keywords.get(0);
		for(int i = 1; i < keywords.size(); i++){
			keyword += " "+keywords.get(i);
		}
		System.out.println("PrunedDP++ result: ");
		SnippetUtil.showSnippet(folder, snippetStr);
		saveResult(folder, keyword, snippetStr);
	}

	private static void saveResult(String folder, String keyword, String snippet) {
		try {
			PrintWriter writer = new PrintWriter(folder + "/pruneddp-record.txt");
			writer.println(snippet);
			writer.println(keyword);
			writer.close();
			System.out.println("Snippet record has been stored in " + folder + "/pruneddp-record.txt");
		}catch (Exception e){
			e.printStackTrace();
		}
	}

//	public static void main(String[] args) throws Exception {
//		/**NOTE: you can change the input keywords in following lines,
//		 * BUT BE SURE to use StringUtil.processKeyword() to get the stem of each keyword in advance. */
//		List<String> keywords = new ArrayList<>();
//		keywords.add(StringUtil.processKeyword("London"));
//		keywords.add(StringUtil.processKeyword("Berlin"));
//		keywords.add(StringUtil.processKeyword("Europe"));
//		String baseFolder = "C:/Users/xiaxiawang/Desktop";
//		generatePrunedDPSnippet(keywords, baseFolder);
//	}
}

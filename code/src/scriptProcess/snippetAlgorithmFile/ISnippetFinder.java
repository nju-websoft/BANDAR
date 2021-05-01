package scriptProcess.snippetAlgorithmFile;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;

import java.util.List;

public interface ISnippetFinder {
	public void preProcess(int datasetId);
	public Multigraph<Integer, DefaultWeightedEdge> findSnippet(List<String> keywords);
}

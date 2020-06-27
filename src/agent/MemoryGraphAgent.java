package agent;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MemoryGraphAgent implements GraphAgent{
	public int datasetId = 1;
	public Multigraph<Integer, DefaultWeightedEdge> graph;
	public MemoryGraphAgent(Multigraph<Integer, DefaultWeightedEdge> graph, int datasetId){
		this.graph=graph;
		this.datasetId = datasetId;
	}
	@Override
	public Map<Integer, Double> getNeighborInfo(Integer id) {
		Map<Integer, Double> result = new HashMap<>();
		try{
			Set<DefaultWeightedEdge> allEdges =	graph.edgesOf(id);
			for(DefaultWeightedEdge ie : allEdges){
				int neighbor = graph.getEdgeSource(ie);
				if(graph.getEdgeSource(ie) == id){
					neighbor = graph.getEdgeTarget(ie);
				}
				double weight = graph.getEdgeWeight(ie);
				result.put(neighbor, weight);
			}	
		} catch(IllegalArgumentException e){
//			e.printStackTrace();
		}
		return result;
	}

}

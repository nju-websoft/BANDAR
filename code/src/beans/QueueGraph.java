package beans;

import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;

import java.util.Comparator;
import java.util.List;

public class QueueGraph {
	public Multigraph<Integer, DefaultWeightedEdge> graph;
	public List<Integer> hitKeywordIndex;
	public double quality;
	
	public QueueGraph(){}

	public QueueGraph(Multigraph<Integer, DefaultWeightedEdge> graph, List<Integer> hitKeywordIndex, double quality) {
		this.graph = graph;
		this.hitKeywordIndex = hitKeywordIndex;
		this.quality = quality;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		for(Integer v:graph.vertexSet())
			result = prime * result + v;
		return result;
	}

	//is used to removing duplicate, to prevent a graph being added to the queue 1+ times
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueueGraph other = (QueueGraph) obj;
		if (graph == null) {
			if (other.graph != null)
				return false;
		} else{
			if(other.graph.vertexSet().size()!=graph.vertexSet().size() 
					|| other.graph.edgeSet().size()!=graph.edgeSet().size())
				return false;
			for(Integer v:graph.vertexSet())
				if(!other.graph.containsVertex(v))
					return false;
			for(DefaultWeightedEdge e:graph.edgeSet())
				if(!other.graph.containsEdge(graph.getEdgeSource(e), graph.getEdgeTarget(e)))
					return false;
		}
		return true;
	}

	public static Comparator<QueueGraph> cmp = new Comparator<QueueGraph>() {
		
		@Override
		public int compare(QueueGraph o1, QueueGraph o2) {
			if(o1.quality > o2.quality)
				return -1;
			else if(o1.quality < o2.quality)
				return 1;
			return 0;
		}
	};
}

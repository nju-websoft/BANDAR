package prunedp;

import java.util.ArrayList;

public class DTreeInfo {
	public double cost;
	public ArrayList<EdgeDP> edges;
	public DTreeInfo(double cost, ArrayList<EdgeDP> edges){
		this.cost = cost;
		this.edges = new ArrayList<EdgeDP>();
		if (edges == null)
			return;
		for (EdgeDP it : edges)
			this.edges.add(new EdgeDP(it));
	}
}

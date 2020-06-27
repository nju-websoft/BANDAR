package prunedp;

public class EdgeDP implements Comparable<EdgeDP>{
	public int u;
	public int v;
	public double weight;
	public EdgeDP(int u, int v){		
		this.u = u;
		this.v = v;
	}
	
	public EdgeDP(int u, int v, double weight){		
		this.u = u;
		this.v = v;
		this.weight = weight;
		if (weight < 0)
			System.out.println("EdgeDP find negetive edges!");
	}
	
	EdgeDP(EdgeDP e2){
		this.u = e2.u;
		this.v = e2.v;
		this.weight = e2.weight;
	}
	public int compareTo(EdgeDP e2) {		
		int result = this.weight > e2.weight ? 1 : (this.weight < e2.weight ? -1 : 0);
		if (result == 0)
			result = (this.u < e2.u) ? 1 : (this.u == e2.u ? 0 : -1);
		if (result == 0)
			result = (this.v < e2.v) ? 1 : (this.v == e2.v ? 0 : -1);
		return result;
	}

	@Override
	public String toString() {
		return "EdgeDP [u=" + u + ", v=" + v + ", weight=" + weight + "]";
	}
	
}

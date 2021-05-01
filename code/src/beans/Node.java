package beans;

import java.util.ArrayList;
import java.util.Comparator;

public class Node {
	public static Comparator<Node> cmp=new Comparator<Node>(){
		@Override
		public int compare(Node o1, Node o2) {
			return o1.id-o2.id;
		}
	};

	public int id;
	public double weight; //指向父节点的边的权重
	public beans.Node father=null;
	public ArrayList<Node> sons=new ArrayList<Node>();
	
	public Node(){}
	
	public Node(int i){ 
		id=i;
	}
	
	public Node(int id, Node father){
		this.id=id;
		this.father=father;
	}
	
	public Node(int id, double weight, Node father){
		this.id=id;
		this.weight = weight;
		this.father=father;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	public Node clone(){
		Node n = new Node(id);
		n.weight = weight;
		n.father=father;
		n.sons=sons;
		return n;
	}
	
}

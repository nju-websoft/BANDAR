package snippetAlgorithm;

import beans.*;
import org.jgrapht.alg.scoring.PageRank;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.WeightedMultigraph;
import util.DBUtil;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

public class IlluSnip implements ISnippetFinder {

	@Override
	public void preProcess(int datasetId) {
	}

	@Override
	public Multigraph<Integer, DefaultWeightedEdge> findSnippet(List<String> keywords) {
//		System.out.println("---------------dataset "+datasetId+"----------------");
		WeightedMultigraph<Integer, DefaultWeightedEdge> snippet = new WeightedMultigraph<>(DefaultWeightedEdge.class);
//		long createStart = System.currentTimeMillis();
		create();
//		long createTime = System.currentTimeMillis() - createStart;
//		System.out.println("create graph time: "+createTime);
//		long setfamStart = System.currentTimeMillis();
		setFam();
//		long setfamTime = System.currentTimeMillis() - setfamStart;
//		System.out.println("set familiarity time: "+setfamTime);
//		long setcovStart = System.currentTimeMillis();
		setCov();
//		long setcovTime = System.currentTimeMillis() - setcovStart;
//		System.out.println("set coverage time: "+setcovTime);
//		long snippetStart = System.currentTimeMillis();
		FindSnippet();
//		long snippetTime = System.currentTimeMillis() - snippetStart;
//		System.out.println("find snippet time: "+snippetTime);
		if(Thread.interrupted())
			return null;
		if(selectedNodes == null)
			return null;
		for(FullNode node : selectedNodes){
			snippet.addVertex(node.getsid());
			snippet.addVertex(node.getoid());
			DefaultWeightedEdge edge = snippet.addEdge(node.getsid(), node.getoid());
			if(edge != null)
				snippet.setEdgeWeight(edge, node.getpid());
		}
		return snippet;
	}
	
	private int datasetId = 1;
	
	private double alpha = 0.2;
	private double beta = 0.8;
	
	private int Ncount = 0;
	private int Ccount = 0;
	private double K = 5;/////////////the size of snippet
	private double maxScore = 0;

	private List<FullNode> totalNodes;
	private Multigraph<String, DefaultEdge> ERgraph;
	
	private Map<Integer , property> pMap = new HashMap<Integer, property>();
	private Map<Integer , typeClass> cMap = new HashMap<Integer, typeClass>();
	private Map<Integer, String>labelID;
	private List<List<Integer>> vertexTriple;
	public Set<FullNode> selectedNodes;
	private int maxNumber = 21000000;
	
	private DBUtil dbHelper = new DBUtil();
	
	public IlluSnip() {}
	
	public IlluSnip(int datasetId) {
		this.datasetId = datasetId;
	}
	
	public double getMaxScore() {
		return maxScore;
	}
	
	public void create(){
		
		totalNodes = new ArrayList<>();
		ERgraph = new Multigraph<>(DefaultEdge.class);
		
		int num = datasetId;
		int NodeCount = 0;
		
		Statement state;
		labelID = new HashMap<Integer, String>();
		vertexTriple = new ArrayList<>();
		
		try {
			state = dbHelper.conn.createStatement();
			//------------running example---------------
			ResultSet rs = state.executeQuery("select * from triple where dataset_local_id = "+num);
			//------------running example---------------
			while(rs.next()) {
				if(rs.getInt("subject") == rs.getInt("object"))
					continue;
				FullNode n = new FullNode();
				n.setNodeID(NodeCount);
				n.setsid(rs.getInt("subject"));
				n.setpid(rs.getInt("predicate"));
				n.setoid(rs.getInt("object"));
				totalNodes.add(n);
				NodeCount++;
			}
			//------------running example---------------
			rs = state.executeQuery("select * from uri_label_id where dataset_local_id = " + num);
			//------------running example---------------
			while(rs.next()) {
				String s = rs.getString("label");
				labelID.put(rs.getInt("id"), s);
				if(s != null) {
					ERgraph.addVertex(s);
				}
				Ncount++;
			}
			
			for(FullNode iter: totalNodes) {
				int sid = iter.getsid();
				iter.setS(labelID.get(sid));
				int pid = iter.getpid();
				iter.setP(labelID.get(pid));
				int oid = iter.getoid();
				iter.setO(labelID.get(oid));
				if(iter.getP()!=null && iter.getP().equals("type")) {
					if(!cMap.containsKey(oid)) {
						typeClass type = new typeClass();
						type.setName(iter.getO());
						type.add();
						cMap.put(oid, type);
					}
					else {
						typeClass type = cMap.get(oid);
						type.add();
						cMap.put(oid, type);
					}
				}
				else if(iter.getP()!=null) {//property
					if(!pMap.containsKey(pid)) {
						property prop = new property();
						prop.setName(iter.getP());
						prop.add();
						pMap.put(pid, prop);
					}
					else {
						property prop = pMap.get(pid);
						prop.add();
						pMap.put(pid, prop);
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		for(int i=0; i<=Ncount+100; i++)
			vertexTriple.add(new ArrayList<>());
		for(FullNode n : totalNodes){
			if(n.getS() != null){
				vertexTriple.get(n.getsid()).add(n.getNodeID());
			}
			if(n.getO() != null){
				vertexTriple.get(n.getoid()).add(n.getNodeID());
			}
		}
		
		try {
			for(FullNode iter: totalNodes) {
				if(iter.getS()==null||iter.getP()==null||iter.getO()==null ||iter.getP().equals("type")) {
					continue;
				}
				else if(!iter.getS().equals(iter.getO())){
					ERgraph.addEdge(iter.getS(), iter.getO());//ERgraph is used to get the PageRank scores
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public double max(double a, double b) {
		if(a>=b)return a;
		else return b;
	}
	
	public void setFam() {//use ERgraph to get fam(t)
		PageRank<String, DefaultEdge> pr = new PageRank<String, DefaultEdge>(ERgraph, 0.85);
		double maxPR = 0;//maximum PageRank score
		for(String iter:pr.getScores().keySet()) {
			maxPR = max(maxPR, pr.getVertexScore(iter));
		}
		try {
			for(FullNode iter:totalNodes) {
				double temp1 = 0, temp2 = 0;
				if(iter.getP()!=null && !iter.getP().equals("type")) {
					if(iter.getS()!=null) {
						temp1 = pr.getVertexScore(iter.getS());
					}
					if(iter.getO()!=null) {
						temp2 = pr.getVertexScore(iter.getO());
					}
					iter.setVis((temp1+temp2)/2/maxPR);
				}
				else if(iter.getP()!=null){
					if(iter.getS()!=null) {
						temp1 = pr.getVertexScore(iter.getS());
					}
					iter.setVis(temp1/maxPR);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public  void setCov() {//use cMap and pMap to get cov(t)
		for(FullNode iter: totalNodes) {//amount of 'S-TYPE-O' triples
			if(iter.getP()!=null && iter.getP().equals("type")) {
				Ccount++;
			}
		}
		
		for(FullNode iter:totalNodes) {
			if(iter.getP()!=null && iter.getP().equals("type")) {
				double count = cMap.get(iter.getoid()).getCount();
				if(count == 1 && Ccount == 1)
					iter.setCov(1);
				else {
					double b = Math.log(count)/Math.log(Ccount);
					iter.setCov(b);
				}
			}
			else if(iter.getP()!=null) {
				double count = pMap.get(iter.getpid()).getCount();
				double b = Math.log(count)/Math.log(Ncount);
				iter.setCov(b);
			}
			iter.setScore(alpha*iter.getVis()+beta*iter.getCov());
		}
	}
	
	public static void SiftDown(List<FullNode> list, int i) {
		while(2*i+1<list.size()) {
			int j = 2 * i + 1;
			if(j+1<list.size() && list.get(j).getScore()<list.get(j+1).getScore()) {
				j++;
			}
			if(list.get(j).getScore()>list.get(i).getScore()) {
				FullNode t = list.get(j);
				list.set(j, list.get(i));
				list.set(i, t);
			}
			else break;
		}
	}
	
	public void FindSnippet() {
		double q = 0;//current score of the snippet
		int k = 0;//current size of the snippet
		Set<FullNode> selected;
		List<FullNode> candidateSet;

		for(FullNode iter:totalNodes) {
			if(Thread.interrupted())
				return;
			
			boolean[] visited = new boolean[maxNumber];
			selected = new HashSet<FullNode>();
			selected.add(iter.clone());
			visited[iter.getNodeID()] = true;
			q = iter.getScore();
			k = 1;
			
			candidateSet = new ArrayList<FullNode>();
			
			Set<Integer> neighborInds = new HashSet<>();
			List<Integer> striple = vertexTriple.get(iter.getsid());
			neighborInds.addAll(striple);
			List<Integer> otriple = vertexTriple.get(iter.getoid());
			neighborInds.addAll(otriple);
			neighborInds.remove(iter.getNodeID());
			if(neighborInds != null){
				if(iter.getP()!=null && iter.getP().equals("type")) {
					int temp = iter.getoid();
					for(Integer nid : neighborInds) {
						FullNode n = totalNodes.get(nid).clone();
						if(n.getoid()==temp) {
							n.setScore(alpha* n.getVis());
						}
						if(!visited[n.getNodeID()]) {
							candidateSet.add(n);
							visited[n.getNodeID()] = true;
						}
					}
				}
				else if(iter.getP()!=null) {
					int temp = iter.getpid();
					for(Integer nid : neighborInds) {
						FullNode n = totalNodes.get(nid).clone();
						if(n.getpid()==temp) {
							n.setScore(alpha* n.getVis());
						}
						if(!visited[n.getNodeID()]) {
							candidateSet.add(n);
							visited[n.getNodeID()] = true;
						}
					}
				}
			}
			for(int i = (candidateSet.size()-2)/2; i>=0;  i--) {
				SiftDown(candidateSet, i);
			}
			while(k < K && !candidateSet.isEmpty()) {
				FullNode first = candidateSet.remove(0);
				selected.add(first);
				q += first.getScore();
				k++;
				Set<Integer> newneighborInds = new HashSet<>();
				newneighborInds.addAll(vertexTriple.get(first.getsid()));
				newneighborInds.addAll(vertexTriple.get(first.getoid()));
				newneighborInds.remove(iter.getNodeID());
				if(newneighborInds.size() == 0)
					continue;
				for(Integer nid : newneighborInds) {
					if(nid == first.getNodeID() || visited[nid])
						continue;
					FullNode iter1 = totalNodes.get(nid).clone();
//					if(!visited[iter1.getNodeID()]) {
						candidateSet.add(iter1);
						visited[iter1.getNodeID()] = true;
//					}
				}
				if(first.getP()!=null && first.getP().equals("type")) {
					int temp1 = first.getoid();
					for(FullNode n: candidateSet) {
						if(n.getoid()==temp1) {
							n.setScore(alpha* n.getVis());
						}
					}
				}
				else if(first.getP()!=null) {
					int temp1 = first.getpid();
					for(FullNode n: candidateSet) {
						if(n.getpid()==temp1) {
							n.setScore(alpha* n.getVis());
						}
					}
				}
				for(int i = (candidateSet.size()-2)/2; i>=0;  i--) {
					SiftDown(candidateSet, i);
				}
			}
			if(maxScore < q) {
				maxScore = q;
				selectedNodes = selected ;
			}
		}
	}

}

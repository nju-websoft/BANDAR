package snippetAlgorithm;

import dao.UriLabelIdDao;
import prunedp.*;
import util.DBUtil;
import util.StemAnalyzer;
import util.StringUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.WeightedMultigraph;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class PrunedDPSnippet implements ISnippetFinder {

	@Override
	public void preProcess(int datasetId) {
	}

	@Override
	public Multigraph<Integer, DefaultWeightedEdge> findSnippet(List<String> keywords) {
		initKeywordNodeMap(keywords);
		WeightedMultigraph<Integer, DefaultWeightedEdge> snippet = new WeightedMultigraph<>(DefaultWeightedEdge.class);
		if(keywords.size() == 1) {
			if(keywordNodesMap.get(keywords.get(0)) == null || keywordNodesMap.get(keywords.get(0)).size() == 0)
				return snippet;
			snippet.addVertex(keywordNodesMap.get(keywords.get(0)).get(0)+1);
			return snippet;
		}
		for(String keyword : keywords)
			if(keywordNodesMap.get(keyword).size() == 0)
				return snippet;

		Map<Integer, Integer> nodeCount = new HashMap<>();
		for(String keyword : keywords) {
			for(int id : keywordNodesMap.get(keyword))
				nodeCount.put(id, nodeCount.getOrDefault(id, 0)+1);
		}
		for(int id : nodeCount.keySet()) {
			if(nodeCount.get(id) == keywords.size()) {
				snippet.addVertex(id + 1);
				return snippet;
			}
		}
		init();
		kdeal(keywords);
		if(ansDtree == null)
			return snippet;
		for(EdgeDP e : ansDtree.edges) {
			int source = e.u + 1;
			int target = e.v + 1;
			snippet.addVertex(source);
			snippet.addVertex(target);
			DefaultWeightedEdge edge = snippet.addEdge(source, target);
		}
		return snippet;
	}
	
	private int datasetId;
	private int directoryId;
	private int nodeNum = 0;
	private ArrayList<ArrayList<EdgeDP>> graph;
	private int[] neighborNum;
	
	
	public PrunedDPSnippet() {}
	
	public PrunedDPSnippet(int datasetId) {
		this.datasetId = datasetId;
		this.directoryId = datasetId;
	}

	public void createLabelIdIndex() {
		
		DBUtil dbHelper = new DBUtil();
		PreparedStatement psmt;
		try {
			Analyzer analyzer = new StemAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			Directory nodeKwdDir = FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId+"/nodeKwdIndex"));
			IndexWriter writer = new IndexWriter(nodeKwdDir, iwc);
			
			Set<Integer> nodeSet = new HashSet<>();
			String sql;
			//---------running example------------
			sql = "select `subject`,object from triple where dataset_local_id=" + datasetId;
			//---------running example------------
			psmt = dbHelper.conn.prepareStatement(sql);
			ResultSet rs = psmt.executeQuery();
			while(rs.next()) {
				nodeSet.add(rs.getInt(1));
				nodeSet.add(rs.getInt(2));
			}
			//-------------running example-----------------
			sql = "select label,id from uri_label_id where dataset_local_id=" + datasetId + " and label is not null";
			//-------------running example-----------------
			psmt = dbHelper.conn.prepareStatement(sql);
			rs = psmt.executeQuery();
			while(rs.next()){
				String keyword = StringUtil.processLabel(rs.getString(1));
//				String keyword = rs.getString(1);
				if(keyword.length() == 0)
					continue;
				if(!nodeSet.contains(rs.getInt(2)))
					continue;
				Document doc = new Document();
				TextField keywordField = new TextField("keyword", keyword, Field.Store.YES);
				TextField idField = new TextField("id", String.valueOf(rs.getInt(2)), Field.Store.YES);
				doc.add(keywordField);
				doc.add(idField);
				writer.addDocument(doc);
			}
			rs.close();
			psmt.close();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void init() {
		readUriId(datasetId);
		try {
			readGraph(datasetId);
		} catch (IOException e) {
			e.printStackTrace();
		}
//		System.out.println("Init end!");
	}
	
	private void readUriId(int datasetId) {
		nodeNum = UriLabelIdDao.getNodeNum(datasetId);
	}
	
	private void readGraph(int datasetId) throws IOException{
		graph = new ArrayList<>();
		neighborNum = new int[nodeNum];
		for (int i = 0; i < nodeNum; i++)
			graph.add(new ArrayList<EdgeDP>());
		IndexReader reader = DirectoryReader
				.open(FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId + "/trpIndex")));
		for (int i = 0; i < reader.maxDoc(); i++) {
			String pairID = reader.document(i).get("pairID");
			int source = Integer.parseInt(pairID.split(",")[0]) - 1;
			int target = Integer.parseInt(pairID.split(",")[1]) - 1;
			graph.get(source).add(new EdgeDP(source, target));
			graph.get(target).add(new EdgeDP(target, source));
			neighborNum[source]++;
			neighborNum[target]++;
		}
		for(int i=0; i<nodeNum; i++) {
			for(EdgeDP e : graph.get(i)) {
				e.weight = Math.log(1+Math.max(neighborNum[e.u], neighborNum[e.v])) / Math.log(2); // log2(1+max{N(u),N(v)})
//				System.out.println(e.weight);
			}
		}
	}

	private void initKeywordNodeMap(List<String> keywords) {
		keywordNodesMap = new HashMap<>();
		for (String keyword : keywords) {	//record all 
			List<Integer> ids = getKeywordNodes(keyword);
			keywordNodesMap.put(keyword, ids);
			keynode += ids.size();
//			System.out.println(keyword+" "+ids);
		}
	}
	
	Map<String, List<Integer>> keywordNodesMap;
	int keyNum;
	double keynode;
	double dist[][];
	int sp[][];
	boolean visited[];
	double W[][][];	
	public double best;
	BitSet P;
	MST m1;
	PriorityQueue<CandiTree> Qplus;
	Map<DTree, DTreeInfo> Dplus;
	CandiTree ansDtree;
	boolean isConnect = true;
	
	
	public double kdeal(List<String> keywords) {
		keyNum = keywords.size();
		if (keyNum > 30) {	//note that need O(2^k*k^2) space to store all paths, so a large k is unacceptable 
			System.out.println("too much keyword to deal by PrunedDP++");
			return 0;
		}
		double result = PrunedDP(keywords);
		if(ansDtree != null)
			for (EdgeDP it : ansDtree.edges) {
//				System.out.println(it);
			}
		return result;
	}
	
	double PrunedDP(List<String> keywords) {
		
		AllPaths(keywords);	//line 1		
		ansDtree = null;
		if(!isConnect)
			return keynode;

		for (int i = 0; i < (nodeNum + keyNum); i++) visited[i] = false;
		m1 = new MST(nodeNum + keyNum);
		
		best = Double.MAX_VALUE;	//line 2		

		//line 3
		Qplus = new PriorityQueue<CandiTree>();	
		Dplus = new TreeMap<DTree, DTreeInfo>();
		//line 4-6
		for (int i = 0; i < keywords.size(); i++) {
			for (int it : keywordNodesMap.get(keywords.get(i))) {
				CandiTree newct = new CandiTree(it, keyNum, i);
				newct.lb = lb(newct.v, newct.X, newct.weight);
				Qplus.add(newct);
			}
		}
				
		ArrayList<Integer> path = new ArrayList<Integer>();
		while (!Qplus.isEmpty()) {	//line 7
			CandiTree ct = Qplus.poll();	//line 8
			//System.out.println(ct.X + " "+ ct.v+" "+ct.weight + " " + ct.lb);
			if (ct.X.equals(P)) {	//line 9				
				best = ct.weight;
				ansDtree = ct;
//				for (EdgeDP it : ct.edges) System.out.println(it.u + " " + it.v+ " " + it.weight);
				return keynode;
			}
			
			if (ct.lb - Math.pow(0.1, 10) > best) {
				continue;
			}
			//line 10			
			DTree tmpDTree10 = new DTree(ct.v, ct.X);
			if (!Dplus.containsKey(tmpDTree10))
				Dplus.put(tmpDTree10, new DTreeInfo(ct.weight, ct.edges));
			else if (Dplus.containsKey(tmpDTree10)) {
				/*
				if (Dplus.get(tmpDTree10).cost > ct.weight) System.out.println("Nooooo! wrong A*");
				else continue;
				*/
			}			
							
			//System.out.println(ct.v + " " + getloc(ct.X)  + " " + ct.lb);
			BitSet Xbar = (BitSet)P.clone();			
			Xbar.xor(ct.X);
			int v = ct.v;			
			
			//line 12-13
			visited[v] = true;			
			boolean connected = true;
			//generate tree
			ArrayList<EdgeDP> treebar = new ArrayList<EdgeDP>(); 
			for (int i = Xbar.nextSetBit(0); i >= 0; i = Xbar.nextSetBit(i + 1)) {
				if (dist[i][v] < 0) {
					connected = false;
					break;
				}			
				path.add(v);
				int tv = v;
				while (tv != (i + nodeNum)) {
					tv = sp[i][tv];
					path.add(tv);
				}
				
				//merge path to tree
				for (int j = path.size() - 1; j >= 0; j--) {
					if (visited[path.get(j)])						
						break;					
					visited[path.get(j)] = true;					
					treebar.add(new EdgeDP(path.get(j), path.get(j - 1), 
							- dist[i][path.get(j)] + dist[i][path.get(j - 1)]));
				}
				path.clear();
			}
			//clear all visted path
			for (int i = 0; i <treebar.size(); i++) {
				visited[treebar.get(i).u] = false;
				visited[treebar.get(i).v] = false;
			}
			visited[v] = false;					

			if (!connected) continue;			
			
			double newcandi = m1.prim(treebar, ct.edges);	//line 14
			
			if (newcandi < best) { 	//line 15
				best = newcandi;				
//				System.out.println("best changed to : "+best);
			}
			
			if(Thread.interrupted())/////////////////////////
				break;
			
			DTree newdt = new DTree(v, Xbar);
			if (Dplus.containsKey(newdt)) {
				update(v, P, ct.weight + Dplus.get(newdt).cost, ct.lb, edgesMerge(ct.edges, Dplus.get(newdt).edges), Dplus.get(newdt).cost);
				//continue;
			}
			
			//there Theorem 1 can't be used, or right on the right answer may be pruned 
			if (ct.weight < best / 2 + Math.pow(0.1, 10)) {	//line 20						
				for (EdgeDP it : graph.get(v)) {	//line 21				
					update(it.v, ct.X, ct.weight + it.weight, ct.lb, edgesMerge(ct.edges, it), it.weight);	//line22
				}
				
				//line 23-26
				findSubset(ct, ct.X, Xbar, 0);
			}
		}
		return keynode;
	}
	

	private List<Integer> getKeywordNodes(String keyword){
		List<Integer> ids = new ArrayList<>();
		IndexReader reader;
		try {
			reader = DirectoryReader
					.open(FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId+ "/nodeKwdIndex")));
			IndexSearcher searcher = new IndexSearcher(reader);
			Analyzer analyzer = new StemAnalyzer();
			QueryParser parser = new QueryParser("keyword", analyzer);
	        Query query = parser.parse(keyword);
	        TopDocs subgraphs = searcher.search(query, 1000000);
			ScoreDoc[] hits = subgraphs.scoreDocs;
			for(int j=0; j<hits.length; j++){
				Document doc = searcher.doc(hits[j].doc);
		    	int id = Integer.parseInt(doc.get("id")) - 1;
		    	ids.add(id);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ids;
	}
	
	//precompute shortest path from
	private void AllPaths(List<String> keywords){
		for (int i = 0; i < keywords.size(); i++) {
			ArrayList<EdgeDP> vi = new ArrayList<EdgeDP>();
			for (int it : keywordNodesMap.get(keywords.get(i))) {
				//g1.graph.get(it).add(new EdgeDP(it, nodeNum + i, 0));
				vi.add(new EdgeDP(nodeNum + i, it, 0));
			}
			graph.add(vi);
		}
		
		//record P in the form of BitSet 
		P = new BitSet(keyNum);
		P.flip(0, keyNum);
		
		//calculate dist from vi to all other nodes
		int tmpNodeNum = nodeNum + keyNum;
		dist = new double[keyNum][tmpNodeNum];
		sp = new int[keyNum][tmpNodeNum];
		visited = new boolean[tmpNodeNum];
		for (int ii = 0; ii < keyNum; ii++) {
			int realii = ii + nodeNum;
			for (int i = 0; i < tmpNodeNum; i++) dist[ii][i] = -1;
			dist[ii][realii] = 0;
			sp[ii][realii] = realii;
			for (int i = 0; i < tmpNodeNum; i++) visited[i] = false;
			
			PriorityQueue<Hop> nodes = new PriorityQueue<Hop>();		
			nodes.add(new Hop(realii, 0));		
			while (!nodes.isEmpty()) {
				Hop sh = nodes.poll();
				if (visited[sh.v]) continue;				
				visited[sh.v] = true;
				for (EdgeDP edge : graph.get(sh.v)) {					
					if (dist[ii][edge.v] < 0 || edge.weight + dist[ii][sh.v] < dist[ii][edge.v]) {						
						dist[ii][edge.v] = edge.weight + dist[ii][sh.v];						
						sp[ii][edge.v] = sh.v; 
						nodes.add(new Hop(edge.v, dist[ii][edge.v]));
					}
				}
			}
			
			for (int i = 0; i < keyNum; i++) {
				if (i != ii){
					int reali = i + nodeNum;
					for (int it : keywordNodesMap.get(keywords.get(i))) {
						if (dist[ii][it] < 0) continue; 
						if(dist[ii][reali] < 0 || dist[ii][it] < dist[ii][reali]) {
							dist[ii][reali] = dist[ii][it];
							sp[ii][reali] = it;
						}						
					}
				}	
			}
		}
		for(int i=0; i<keyNum; i++) {
			for(int j=0; j<keyNum; j++) {
				if(i == j)
					continue;
				if(dist[i][j+nodeNum] < 0) {
					isConnect = false;
					return;
				}
			}
		}
		
		//real allPath in Algorithm3 AllPaths
		//not exactly implemented as the pesucode
		//don't use Fibonacci heap, don't use D, don't update Q  
		W = new double[keyNum][keyNum][1<<keyNum];
		for (int i = 0; i < keyNum; i++)
			for (int j = 0; j < keyNum; j++)
				for (int k = 0; k < (1<<keyNum); k++)
					W[i][j][k] = -1;
		for (int i = 0; i < keyNum; i++)
			for (int j = 0; j < keyNum; j++) W[i][j][0] = dist[i][j + nodeNum];
		PriorityQueue<Router> Q = new PriorityQueue<Router>();
		for (int i = 0; i < keyNum; i++)	
				Q.add(new Router(i, i, keyNum, i));	//line 5
		while (!Q.isEmpty()) {	//line 6
			Router rt = Q.poll();	//line 7
//			System.out.println(rt.u+" "+rt.v+" "+rt.weight);
			if (W[rt.u][rt.v][getloc(rt.X)]>=0 && W[rt.u][rt.v][getloc(rt.X)] < rt.weight) continue;
			W[rt.u][rt.v][getloc(rt.X)] = rt.weight;	//line 8
			
			//get the complementary of X
			BitSet Xbar = (BitSet)P.clone();
			Xbar.xor(rt.X);
			Xbar.flip(rt.u);	//also need W[rt.u][rt.u]
			
			for (int p = Xbar.nextSetBit(0); p >= 0; p = Xbar.nextSetBit(p + 1)) {//line 10
				Router newrt = new Router(rt.u, p, keyNum, p);				
				newrt.X.or(rt.X);	//line 11			
				newrt.weight = rt.weight + dist[rt.v][p + nodeNum];	//line 12
				//line 13-15 
				if (W[newrt.u][p][getloc(newrt.X)] < 0 || W[newrt.u][p][getloc(newrt.X)] > newrt.weight){
					W[newrt.u][p][getloc(newrt.X)] = newrt.weight;
					Q.add(newrt);
				}			
			}
		}
				
		//remove added nodes and edges
		for (int i = 0; i < keyNum; i++) 			
			graph.remove(graph.size() - 1);		
	}
	
	private int getloc(BitSet X) {
		int ans = 0;
		for (int i = X.nextSetBit(0); i >= 0; i = X.nextSetBit(i + 1))
		      ans += X.get(i) ? (1 << i) : 0;
		return ans;
	}
	
	//line 23-26
	void findSubset(CandiTree ct, BitSet X, BitSet Xbar, int loc) {
		if (Xbar.cardinality() > 0) {
			DTree newdt = new DTree(ct.v, Xbar);
			if (Dplus.containsKey(newdt)) {
				if (ct.weight + Dplus.get(newdt).cost <= (best * 2 / 3) + Math.pow(0.1, 10)) {
					X.xor(Xbar);
					update(ct.v, X, ct.weight + Dplus.get(newdt).cost, ct.lb, edgesMerge(ct.edges, Dplus.get(newdt).edges), Dplus.get(newdt).cost);
					X.xor(Xbar);
				}
			}
		}
		for (int i = Xbar.nextSetBit(loc); i >= 0; i = Xbar.nextSetBit(i + 1)) {
			Xbar.flip(i);
			findSubset(ct, X, Xbar, i + 1);
			Xbar.flip(i);
		}
	}
	
	ArrayList<EdgeDP> edgesMerge(ArrayList<EdgeDP> edges1, ArrayList<EdgeDP> edges2) {
		ArrayList<EdgeDP> edges = new ArrayList<EdgeDP>();
		for (EdgeDP it : edges1) edges.add(it);
		for (EdgeDP it : edges2) edges.add(it);
		return edges;
	}
	
	ArrayList<EdgeDP> edgesMerge(ArrayList<EdgeDP> edges1, EdgeDP edge2) {
		ArrayList<EdgeDP> edges = new ArrayList<EdgeDP>();
		for (EdgeDP it : edges1) edges.add(it);
		edges.add(edge2);
		return edges;
	}
	
	void update(int v, BitSet X, double cost, double lbhat, ArrayList<EdgeDP> edges, double tmpadd) {
		if (Dplus.containsKey(new DTree(v, X)))	//line 29
			return;
		double lowb = lb(v, X, cost);	//line 30
		if (lowb < lbhat - tmpadd) lowb = lbhat - tmpadd;	//line 31		
		//there lowb==best can't be pruned, or right answer may not in Q
		
		//System.out.println("update "+ v + " "+ X + " " +lowb + " " + best);
		if (lowb- Math.pow(0.1,10)> best) {
			//System.out.println(lowb - Math.pow(0.1, 10));
			//System.out.println(best);
			return;
		}	//line 32
		if (X.equals(P))	//line 33
			if (best > cost) best = cost;
		Qplus.add(new CandiTree(v, X, cost, lowb, edges));				
		//System.out.println("update tree:"+ v + " " + X);
		//for (EdgeDP it : edges) System.out.println(it.u + " " + it.v + " " + it.weight);
	}
	
	double lb(int v, BitSet X, double cost) {		
		double lowb = 0;
		BitSet Xbar = (BitSet)P.clone();		
		Xbar.xor(X);
		if (Xbar.cardinality() == 0) {	//line 39			
			return cost;	//line 40
		}
		
		// get lowb1
		double lowb1 = Double.MAX_VALUE;
		double tmplow;
		for (int i = Xbar.nextSetBit(0); i >= 0; i = Xbar.nextSetBit(i + 1))
			if (dist[i][v] >=0)
			for (int j = Xbar.nextSetBit(0); j >= 0; j = Xbar.nextSetBit(j + 1)) {
				if (W[i][j][getloc(Xbar)] < 0) System.out.println("wrong lb1!");
				if (dist[j][v] >= 0 && W[i][j][getloc(Xbar)] >= 0){
					tmplow = dist[i][v] + W[i][j][getloc(Xbar)] + dist[j][v];
					if (tmplow < lowb1) { 
						lowb1 = tmplow;						
					}
				}
			}
		lowb1 = lowb1 / 2;
		
		
		// get lowb2
		double lowb2 = 0;
		
		double mindist = Double.MAX_VALUE;
		for (int i = Xbar.nextSetBit(0); i >= 0; i = Xbar.nextSetBit(i + 1)) {
			tmplow = -1; // W(vi,-X)
			for (int j = Xbar.nextSetBit(0); j >= 0; j = Xbar.nextSetBit(j + 1)) {
				if (W[i][j][getloc(Xbar)] < 0) System.out.println("wrong lb2!");
				if (tmplow < 0 || tmplow > W[i][j][getloc(Xbar)])
					tmplow = W[i][j][getloc(Xbar)];
			}
			if (tmplow >= 0 && dist[i][v] >= 0 && dist[i][v] + tmplow > lowb2)
				lowb2 = dist[i][v] + tmplow;

			if (dist[i][v] >= 0 && dist[i][v] < mindist)
				mindist = dist[i][v];
		}
		lowb2 = (lowb2 + mindist) / 2;
		 
		// line 44		
		lowb = lowb1;		
		if (lowb2 > lowb) lowb = lowb2;
			
		//get one-label lower bound
		for (int i = Xbar.nextSetBit(0); i >= 0; i = Xbar.nextSetBit(i + 1)) 
			if (lowb < dist[i][v])
				lowb = dist[i][v];
		//return cost;
		return cost + lowb;
	}
}

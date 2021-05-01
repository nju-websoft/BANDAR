package snippetAlgorithm;

import agent.GraphAgent;
import agent.MemoryGraphAgent;
import beans.Node;
import beans.QueueGraph;
import dao.TripleDao;
import util.DBUtil;
import util.StemAnalyzer;
import util.StringUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.spanning.PrimMinimumSpanningTree;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.WeightedMultigraph;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;

public class TACSnippet implements ISnippetFinder{
	
	@Override
	public void preProcess(int datasetId) {
	}

	@Override
	public Multigraph<Integer, DefaultWeightedEdge> findSnippet(List<String> keywords) {
		WeightedMultigraph<Integer, DefaultWeightedEdge> totalGraph = generateSimpleGraphByTriples();
		double totalWeight = computeTotalWeight(totalGraph);
		WeightedMultigraph<Integer, DefaultWeightedEdge> snippet = assembleSnippet(keywords, totalWeight);
		return snippet;
	}
	
	
	private int datasetId;
	private int directoryId;
	public double alpha = 0.5;
	private int radius = 1;
	private int maxNodes = 5; //////////////size bound of snippet
	
	private DBUtil dbUtil;
	
	public TACSnippet(){}
	
	public TACSnippet(int datasetId){
		this.datasetId = datasetId;
		this.directoryId = datasetId;
	}

	public void createTripleIndex(){
		DBUtil dbHelper = new DBUtil();
		try {
			Directory tripleDir = FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId+"/trpIndex"));
			Analyzer luceneAnalyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(luceneAnalyzer);
			IndexWriter indexWriter = new IndexWriter(tripleDir, iwc);

			String sql;
			//-------------running example-------------
			sql = "select `subject`,object from triple where dataset_local_id=?";
			//-------------running example-------------------
			PreparedStatement psmt = dbHelper.conn.prepareStatement(sql);
			psmt.setInt(1, datasetId);
			ResultSet rs = psmt.executeQuery();
			Map<String, Integer> edgeCount = new HashMap<>();
			while(rs.next()){
				String pairID = null;
				if(rs.getInt(1) < rs.getInt(2))
					pairID = rs.getInt(1)+","+rs.getInt(2);
				else if(rs.getInt(1) > rs.getInt(2))
					pairID = rs.getInt(2)+","+rs.getInt(1);
				else
					continue;

				if(edgeCount.containsKey(pairID))
					edgeCount.put(pairID, edgeCount.get(pairID)+1);
				else
					edgeCount.put(pairID, 1);
			}
			rs.close();
			psmt.close();

			for(String pairID : edgeCount.keySet()){
				Document doc = new Document();
				TextField pair = new TextField("pairID", pairID, Field.Store.YES);
				int cnt = edgeCount.get(pairID);
				TextField count = new TextField("count", String.valueOf(cnt), Field.Store.YES);
				doc.add(pair);
				doc.add(count);
				indexWriter.addDocument(doc);
			}
			indexWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dbHelper.close();
		}
	}

	public WeightedMultigraph<Integer, DefaultWeightedEdge> generateSimpleGraphByTriples() {
		WeightedMultigraph<Integer, DefaultWeightedEdge> graph = new WeightedMultigraph<>(DefaultWeightedEdge.class);
		try {
			IndexReader reader = DirectoryReader
					.open(FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId + "/trpIndex")));
			for (int i = 0; i < reader.maxDoc(); i++) {
				String pairID = reader.document(i).get("pairID");
				double weight = 1.0 / Integer.parseInt(reader.document(i).get("count"));
				int source = Integer.parseInt(pairID.split(",")[0]);
				int target = Integer.parseInt(pairID.split(",")[1]);
				graph.addVertex(source);
				graph.addVertex(target);
				DefaultWeightedEdge edge = graph.addEdge(source, target);
				if(edge != null)
					graph.setEdgeWeight(edge, weight);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return graph;
	}

	public double computeTotalWeight(Multigraph<Integer, DefaultWeightedEdge> graph){
		double totalWeight = 0;
		for(DefaultWeightedEdge edge:graph.edgeSet()){
			totalWeight += graph.getEdgeWeight(edge);
		}
		return totalWeight;
	}

	public void MRRExtraction(WeightedMultigraph<Integer, DefaultWeightedEdge> graph, int radius){
		List<WeightedMultigraph<Integer, DefaultWeightedEdge>> subgraphs = new ArrayList<>();
		Map<Integer, List<Integer>> vertexGraphindMap = new HashMap<>();
		int count = 0;
		for(int v:graph.vertexSet()){
			count++;
			WeightedMultigraph<Integer, DefaultWeightedEdge> subgraphCandidate = breadthFirstSearch(graph, v, radius);
			subgraphs.add(subgraphCandidate);
			for(Integer ve:subgraphCandidate.vertexSet()){
				List<Integer> graphInds;
				if(vertexGraphindMap.containsKey(ve))
					graphInds = vertexGraphindMap.get(ve);
				else
					graphInds = new ArrayList<>();
				graphInds.add(count-1);
				vertexGraphindMap.put(ve, graphInds);
			}
		}

		TreeSet<Integer> removeIndex = new TreeSet<>();
//		System.out.println("vertexGraphindMap: "+vertexGraphindMap.size());
		int cnt = 0;
		for(Integer v : vertexGraphindMap.keySet()){
			cnt++;
//			if(cnt % 10 == 0)
//				System.out.println(cnt);
			List<Integer> graphInds = vertexGraphindMap.get(v);
			for(int i=0; i<graphInds.size()-1; i++){
				for(int j=i+1; j<graphInds.size(); j++){
					if(removeIndex.contains(graphInds.get(i)) && removeIndex.contains(graphInds.get(j)))
						continue;
					if(removeIndex.contains(graphInds.get(i)) && subgraphs.get(graphInds.get(j)).vertexSet().size()>subgraphs.get(graphInds.get(i)).vertexSet().size())
						continue;
					if(removeIndex.contains(graphInds.get(j)) && subgraphs.get(graphInds.get(i)).vertexSet().size()>subgraphs.get(graphInds.get(j)).vertexSet().size())
						continue;
					WeightedMultigraph<Integer, DefaultWeightedEdge> Gi;
					WeightedMultigraph<Integer, DefaultWeightedEdge> Gj;
					int removeInd = graphInds.get(i);
					if(subgraphs.get(graphInds.get(i)).vertexSet().size() < subgraphs.get(graphInds.get(j)).vertexSet().size()){
						Gi = subgraphs.get(graphInds.get(i));
						Gj = subgraphs.get(graphInds.get(j));
					} else {
						Gi = subgraphs.get(graphInds.get(j));
						Gj = subgraphs.get(graphInds.get(i));
						removeInd = graphInds.get(j);
					}
					boolean isSubset = true;
					for(Integer id:Gi.vertexSet()){
						if(!Gj.vertexSet().contains(id)){
							isSubset = false;
							break;
						}
					}
					if(isSubset)
						removeIndex.add(removeInd);
				}
			}
		}

		List<Integer> removeList = new ArrayList<>();
		removeList.addAll(removeIndex);
		for(int i=removeList.size()-1; i>=0; i--){
			int ind = removeList.get(i);
			subgraphs.remove(ind);
		}

		dbUtil = new DBUtil();
		DecimalFormat df = new DecimalFormat("#.000");
		for(int i=0; i<subgraphs.size(); i++){
			WeightedMultigraph<Integer, DefaultWeightedEdge> mrr = subgraphs.get(i);
			StringBuilder graphToString = new StringBuilder();
			int edgeN = 0;
			for(DefaultWeightedEdge edge:mrr.edgeSet()){
				edgeN++;
				if(edgeN == 1)
					graphToString.append(mrr.getEdgeSource(edge)+":");
				if(mrr.getEdgeWeight(edge) < 1)
					graphToString.append(mrr.getEdgeTarget(edge) + " " + df.format(mrr.getEdgeWeight(edge))+";");
				else
					graphToString.append(mrr.getEdgeTarget(edge) +";");
			}
			PreparedStatement psmt = null;
			//---------------running example---------------
			String sql = "insert into mrrs(dataset_local_id,subgraph,id) values(?,?,?)";
			//----------------running example------------------
			try {
				psmt = dbUtil.conn.prepareStatement(sql);
				psmt.setInt(1, datasetId);
				psmt.setString(2, graphToString.toString());
				psmt.setInt(3, i+1);
				psmt.executeUpdate();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					psmt.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		dbUtil.close();
	}

	public void createAllKeywordIndex() {
		dbUtil = new DBUtil();

		Analyzer analyzer = new StemAnalyzer();
		IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
		Directory kw2sgDir = null;
		IndexWriter kw2sgIndexWriter = null;
		IndexWriterConfig iwc2 = new IndexWriterConfig(analyzer);
		Directory kwsg2eDir = null;
		IndexWriter kwsg2eIndexWriter = null;
		try {
			kw2sgDir = FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId+"/kw2sgIndex"));
			kw2sgIndexWriter = new IndexWriter(kw2sgDir, iwc); //keyword->subgraphID

			kwsg2eDir = FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId+"/kwsg2eIndex"));
			kwsg2eIndexWriter = new IndexWriter(kwsg2eDir, iwc2); //keyword_subgraphID->vertexID

			String sql;
			sql = "select subgraph,id from mrrs where dataset_local_id=?";
			PreparedStatement psmt = dbUtil.conn.prepareStatement(sql);
			psmt.setInt(1, datasetId);
			ResultSet rs = psmt.executeQuery();
			while(rs.next()){
				WeightedMultigraph<Integer, DefaultWeightedEdge> mrr = new WeightedMultigraph<>(DefaultWeightedEdge.class);
				int source = Integer.parseInt(rs.getString(1).split(":")[0]);
				mrr.addVertex(source);
				String[] triples = rs.getString(1).split(":")[1].split(";");
				for(String triple:triples){
					String[] objPred = triple.split(" ");
					int target = Integer.parseInt(objPred[0]);
					mrr.addVertex(target);
					if(objPred.length == 1){
						DefaultWeightedEdge edge = mrr.addEdge(source, target);
						mrr.setEdgeWeight(edge, 1);
					} else {
						double weight = Double.parseDouble(objPred[1]);
						DefaultWeightedEdge edge = mrr.addEdge(source, target);
						mrr.setEdgeWeight(edge, weight);
					}
				}
				int subgraphID = rs.getInt(2);
				//-----------running example-----------------
				sql = "select label from uri_label_id where dataset_local_id=? and id=? and label is not null";
				//-----------running example-----------------
				PreparedStatement psmt2 = dbUtil.conn.prepareStatement(sql);
				psmt2.setInt(1, datasetId);
				for(Integer vertex:mrr.vertexSet()){
					psmt2.setInt(2, vertex);
					ResultSet rs2 = psmt2.executeQuery();
					if(rs2.next()){
						String label = rs2.getString(1);
						label = StringUtil.processLabel(label);
						if(label.length() == 0)
							continue;
						Document doc = new Document();
						TextField kwField = new TextField("keyword", label, Field.Store.YES);
						TextField sgidField = new TextField("subgraphID", String.valueOf(subgraphID), Field.Store.YES);
						doc.add(kwField);
						doc.add(sgidField);
						kw2sgIndexWriter.addDocument(doc);

						Document doc2 = new Document();
						TextField kwField2 = new TextField("keyword", label, Field.Store.YES);
						TextField sgidField2 = new TextField("subgraphID", String.valueOf(subgraphID), Field.Store.YES);
						TextField vertexField = new TextField("vertexID", String.valueOf(vertex), Field.Store.YES);
						doc2.add(kwField2);
						doc2.add(sgidField2);
						doc2.add(vertexField);
						kwsg2eIndexWriter.addDocument(doc2);
//						System.out.println(label+" "+subgraphID+" "+vertex);
					}
					rs2.close();
				}
				psmt2.close();
			}
			rs.close();
			psmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			kw2sgIndexWriter.close();
			kwsg2eIndexWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		dbUtil.close();
	}

	public WeightedMultigraph<Integer, DefaultWeightedEdge> assembleSnippet(List<String> keywords, double totalWeight){

		Queue<QueueGraph> queue = new PriorityQueue<>(QueueGraph.cmp);
		boolean[] checkedSubgraphs = new boolean[1000000];
		for(int i=0; i<keywords.size(); i++){
			String keyword = keywords.get(i);
			try {
				IndexReader reader = DirectoryReader
						.open(FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId + "/kw2sgIndex")));

				IndexSearcher searcher = new IndexSearcher(reader);
				Analyzer analyzer = new StemAnalyzer();
				QueryParser parser = new QueryParser("keyword", analyzer);
				Query query = parser.parse(keyword);
				TopDocs subgraphs = searcher.search(query, 1000000);
				ScoreDoc[] hits = subgraphs.scoreDocs;
//				System.out.println(keywords.get(i)+" "+hits.length);//-----------------------------
				DBUtil dbHelper = new DBUtil();
				String sql;
				PreparedStatement psmt;
				sql = "select subgraph from mrrs where dataset_local_id=? and id=?";
				psmt = dbHelper.conn.prepareStatement(sql);
				psmt.setInt(1, datasetId);
				for(int j=0; j<hits.length; j++){
//					if(j % 10 == 0)
//					System.out.println(j);
					Document doc = searcher.doc(hits[j].doc);
					int subgraphID = Integer.parseInt(doc.get("subgraphID"));
					if(!checkedSubgraphs[subgraphID]){
						checkedSubgraphs[subgraphID] = true;
						int hitKeywordNum = 0;
						List<Integer> hitKeywordIndex = new ArrayList<>();
						List<ArrayList<Integer>> requiredNodes = new ArrayList<>();
						for(int k=0; k<keywords.size(); k++)
							requiredNodes.add(new ArrayList<Integer>());
						for(int k=i; k<keywords.size(); k++){
//			    			String keywordGraphid = keywords.get(k)+"_"+subgraphID;
							IndexReader reader2 = DirectoryReader
									.open(FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+directoryId + "/kwsg2eIndex")));
							IndexSearcher searcher2 = new IndexSearcher(reader2);
							BooleanQuery.Builder builder = new BooleanQuery.Builder();
							QueryParser parser1 = new QueryParser("keyword", analyzer);
							Query query1 = parser1.parse(keywords.get(k));
							builder.add(query1, Occur.MUST);
							QueryParser parser2 = new QueryParser("subgraphID", analyzer);
							Query query2 = parser2.parse(String.valueOf(subgraphID));
							builder.add(query2, Occur.MUST);
							TopDocs entities = searcher2.search(builder.build(), 1000);
							ScoreDoc[] hits2 = entities.scoreDocs;
							if(hits2.length > 0){
								hitKeywordNum++;
								hitKeywordIndex.add(k);
							}
							for(int d=0; d<hits2.length; d++){
								Document doc2 = searcher2.doc(hits2[d].doc);
								int entityID = Integer.parseInt(doc2.get("vertexID"));
								requiredNodes.get(k).add(entityID);
							}
						}
						if(hitKeywordNum == 0)
							continue;
						if(hitKeywordNum == 1){
							WeightedMultigraph<Integer, DefaultWeightedEdge> graph = new WeightedMultigraph<>(DefaultWeightedEdge.class);
							graph.addVertex(requiredNodes.get(i).get(0)); //只有一个点
							QueueGraph qGraph = new QueueGraph(graph, hitKeywordIndex, alpha+(1-alpha)*(1.0/keywords.size()));
							if(!queue.contains(qGraph))
								queue.add(qGraph);
							continue;
						}

						psmt.setInt(2, subgraphID);
						ResultSet rs = psmt.executeQuery();
						if(rs.next()){
							Multigraph<Integer, DefaultEdge> graph = new Multigraph<>(DefaultEdge.class);
							WeightedMultigraph<Integer, DefaultWeightedEdge> wGraph = new WeightedMultigraph<>(DefaultWeightedEdge.class);
							int source = Integer.parseInt(rs.getString(1).split(":")[0]);
							graph.addVertex(source);
							wGraph.addVertex(source);
							String[] triples = rs.getString(1).split(":")[1].split(";");
							for(String triple:triples){
								String[] objPred = triple.split(" ");
								int target = Integer.parseInt(objPred[0]);
								graph.addVertex(target);
								graph.addEdge(source, target);
								wGraph.addVertex(target);
								if(objPred.length == 1){
									DefaultWeightedEdge edge = wGraph.addEdge(source, target);
									wGraph.setEdgeWeight(edge, 1);
								} else {
									double weight = Double.parseDouble(objPred[1]);
									DefaultWeightedEdge edge = wGraph.addEdge(source, target);
									wGraph.setEdgeWeight(edge, weight);
								}
							}
							Multigraph<Integer, DefaultWeightedEdge> minSpanningTree = findMinSpanningTree(graph, wGraph, requiredNodes);
							if(queue.contains(new QueueGraph(minSpanningTree, hitKeywordIndex, 0)))
								continue;
							double weight = 0;
							for (DefaultWeightedEdge edge : minSpanningTree.edgeSet())
								weight += minSpanningTree.getEdgeWeight(edge);
							double quality = alpha * (1 - weight / totalWeight)
									+ (1-alpha)*((double)hitKeywordNum/keywords.size());
//							System.out.println("minSpanTree: "+minSpanningTree);
//			    			System.out.println(hitKeywordIndex);
//			    			System.out.println(quality);
							queue.add(new QueueGraph(minSpanningTree, hitKeywordIndex, quality));
						}
						rs.close();
					}
				}
				psmt.close();
				dbHelper.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		WeightedMultigraph<Integer, DefaultWeightedEdge> snippet = new WeightedMultigraph<>(DefaultWeightedEdge.class);
		boolean[] isHitKeyword = new boolean[keywords.size()];
		while(snippet.vertexSet().size() < maxNodes && !queue.isEmpty()){
			QueueGraph qGraph = queue.poll();
			Multigraph<Integer, DefaultWeightedEdge> subSnippet = qGraph.graph;
//			System.out.println("poll: "+subSnippet+" "+qGraph.quality);
			List<Integer> hitKeywordIndex = qGraph.hitKeywordIndex;
			Set<Integer> newVertexSet = new HashSet<>();
			newVertexSet.addAll(snippet.vertexSet());
			newVertexSet.addAll(subSnippet.vertexSet());
			if(newVertexSet.size() > maxNodes)
				continue;
			for(Integer vertex:subSnippet.vertexSet())
				snippet.addVertex(vertex);
			for(DefaultWeightedEdge edge:subSnippet.edgeSet()){
				if(!snippet.containsEdge(edge)){
					DefaultWeightedEdge e = snippet.addEdge(subSnippet.getEdgeSource(edge), subSnippet.getEdgeTarget(edge));
					snippet.setEdgeWeight(e, subSnippet.getEdgeWeight(edge));
				}
			}
			for(Integer ind:hitKeywordIndex)
				isHitKeyword[ind] = true;
			Queue<QueueGraph> temp = new PriorityQueue<>(QueueGraph.cmp);
			while(!queue.isEmpty()){
				QueueGraph qGraph2 = queue.poll();
				Multigraph<Integer, DefaultWeightedEdge> subSnippet2 = qGraph2.graph;
				List<Integer> hitKeywordIndex2 = qGraph2.hitKeywordIndex;
				double quality = 0;
				for(Integer ind:hitKeywordIndex2){
					if(!isHitKeyword[ind])
						quality += (1-alpha) * (1.0 / keywords.size());
				}
				for(DefaultWeightedEdge e:subSnippet2.edgeSet()){
					if(!snippet.containsEdge(e))
						quality -= alpha * subSnippet2.getEdgeWeight(e) / totalWeight;
				}
				temp.add(new QueueGraph(subSnippet2, hitKeywordIndex2, quality));
			}
			queue = temp;
		}
		for(DefaultWeightedEdge edge : snippet.edgeSet()){
			int source = snippet.getEdgeSource(edge);
			int target = snippet.getEdgeTarget(edge);
			int predicate = TripleDao.getPredicate(datasetId, source, target);
			snippet.setEdgeWeight(edge, predicate);
//			System.out.println("["+source+","+target+"]"+"edge weight: "+snippet.getEdgeWeight(edge));
		}
		return snippet;
	}

	public WeightedMultigraph<Integer, DefaultWeightedEdge> findMinSpanningTree(Multigraph<Integer, DefaultEdge> graph, WeightedMultigraph<Integer, DefaultWeightedEdge> wGraph, List<ArrayList<Integer>> requiredNodes) throws IOException{
		WeightedMultigraph<Integer, DefaultWeightedEdge> minWeightGraph = null;
		double minWeight = Double.MAX_VALUE;
		int beginIndex = 0;
		for(int i=0; i<requiredNodes.size(); i++){
			if(requiredNodes.get(i).size()>0){
				beginIndex = i;
				break;
			}
		}
		for(Integer v1:requiredNodes.get(beginIndex)){
			WeightedMultigraph<Integer, DefaultWeightedEdge> tempGraph = new WeightedMultigraph<>(DefaultWeightedEdge.class);
			for(int i=beginIndex+1; i<requiredNodes.size(); i++){
				if(requiredNodes.get(i).size() == 0)
					continue;
				List<DefaultEdge> minPath = null;
				int minLength = Integer.MAX_VALUE;
				for(Integer vi:requiredNodes.get(i)){
					List<DefaultEdge> path = DijkstraShortestPath.findPathBetween(graph, v1, vi).getEdgeList(); ////////////
					if(path.size() < minLength){
						minLength = path.size();
						minPath = path;
					}
				}
				if(minPath.size() == 0)
					tempGraph.addVertex(v1);
				for(DefaultEdge edge:minPath){
					int source = graph.getEdgeSource(edge);
					int target = graph.getEdgeTarget(edge);
					tempGraph.addVertex(source);
					tempGraph.addVertex(target);
					DefaultWeightedEdge weightEdge = tempGraph.addEdge(source, target);
					tempGraph.setEdgeWeight(weightEdge, wGraph.getEdgeWeight(wGraph.getEdge(source, target)));
				}
				double tempWeight = 0;
				for(DefaultWeightedEdge edge:tempGraph.edgeSet())
					tempWeight += tempGraph.getEdgeWeight(edge);
				if(tempWeight < minWeight){
					minWeight = tempWeight;
					minWeightGraph = tempGraph;
				}
			}
		}
		if(minWeightGraph.edgeSet().size() == 0)
			return minWeightGraph;
		PrimMinimumSpanningTree<Integer, DefaultWeightedEdge> minSpanningTree = new PrimMinimumSpanningTree<>(minWeightGraph);
		WeightedMultigraph<Integer, DefaultWeightedEdge> minSpanGraph = new WeightedMultigraph<>(DefaultWeightedEdge.class);
		for(Integer vertex:minWeightGraph.vertexSet())
			minSpanGraph.addVertex(vertex);
		for(DefaultWeightedEdge edge:minSpanningTree.getSpanningTree().getEdges())
			minSpanGraph.addEdge(minWeightGraph.getEdgeSource(edge), minWeightGraph.getEdgeTarget(edge));
		return minSpanGraph;
	}


	private WeightedMultigraph<Integer, DefaultWeightedEdge> breadthFirstSearch(WeightedMultigraph<Integer, DefaultWeightedEdge> graph, int v, int radius) {
		GraphAgent graphAgent = new MemoryGraphAgent(graph, datasetId);

		WeightedMultigraph<Integer, DefaultWeightedEdge> result = new WeightedMultigraph<>(DefaultWeightedEdge.class);

		List<Node> iterateSet = new ArrayList<>();
		iterateSet.add(new Node(v, 0, null));
		result.addVertex(v);
		for(int pathLength=1; pathLength<=radius; pathLength++){
			List<Node> iterateTempSet = new ArrayList<>();
			for(Node tempNode : iterateSet){
				int currentNodeId = tempNode.id;
				Map<Integer, Double> allEdges =	graphAgent.getNeighborInfo(currentNodeId);
				for(Map.Entry<Integer, Double> ie:allEdges.entrySet()){
					int neighbor = ie.getKey();
					double weight = ie.getValue();
					if(isDuplicate(neighbor, tempNode))
						continue;
					Node nextNeighborNode = new Node(neighbor, weight, tempNode);
					tempNode.sons.add(nextNeighborNode);
					iterateTempSet.add(nextNeighborNode);

					result.addVertex(neighbor);
					DefaultWeightedEdge edge = result.addEdge(currentNodeId, neighbor);
					result.setEdgeWeight(edge, weight);
				}
			}
			iterateSet = iterateTempSet;
		}
		return result;
	}

	private boolean isDuplicate(int id,Node node){
		Node temp = node;
		while(temp!=null){
			if(temp.id==id) return true;
			temp=temp.father;
		}
		return false;
	}

}

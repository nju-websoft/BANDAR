package snippetEvaluation;

import beans.triple;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Multigraph;
import snippetAlgorithm.Parameter;
import util.StemAnalyzer;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class Evaluations {
    private Connection connection;
    private int typeID;//the id whose uri is 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
    private int dataset;
    Map<String, Set<Integer>>kw2ids; /**kw2ids: keyword -> S/P/O containing it. init in kwRel, used in QryRel*/
    double cokNEW;
    Set<Integer> entitySet;/**set of entities, init in EntRep, used in EntRep, DescRep*/
    Set<Integer> entWithEDP;/**set of entities with full description pattern in the snippet, init in DescRep, used in LinkRep*/
    public Evaluations(int dataset, Connection connection){
        this.dataset = dataset;
        this.connection = connection;
        typeID = getTypeID(dataset);
    }

    public int getTypeID(int dataset){
        String select = "select type_id from dataset_info where dataset_local_id = " + dataset;
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) typeID = resultSet.getInt("type_id");
        }catch (Exception e){
            e.printStackTrace();
        }
        return typeID;
    }

    public Set<triple> formSnippet(String arg){
        /**isolated nodes are presented in three ways:
         *  1, subject-0-0 (entity and appears as subject in the dataset)
         *  2, class：0-(-1)-object (-1 is used to suggest 'type')
         *  3, entity/literal：0-0-object*/
        Set<triple>snippet = new HashSet<>();
        Set<Integer>ids = new HashSet<>();/**id appeared in triples*/
        if (arg.contains(";")){
            String triples = arg.substring(arg.indexOf(";") + 1);
            String[] triple = triples.split(",");
            for (String iter: triple){
                String[] item = iter.split(" ");
                beans.triple temp = new triple();
                int s = Integer.parseInt(item[0]);
                int o = Integer.parseInt(item[1]);
                int p = Integer.parseInt(item[2]);
                temp.setSid(s);
                temp.setOid(o);
                temp.setPid(p);
                snippet.add(temp);
                ids.add(s);
                ids.add(o);
                ids.add(p);
            }
        }
        try {
            String vertex;
            if (arg.contains(";")) vertex = arg.substring(0, arg.indexOf(";"));
            else vertex = arg;
            for (String iter: vertex.split(",")){
                int id = Integer.parseInt(iter);
                if (!ids.contains(id)){ /**if predicate == -1: object is a class*/
                    triple temp = new triple();
                    String check = "select triple_id from triple where dataset_local_id=" + dataset +" and subject=?";
                    PreparedStatement checkStatement = connection.prepareStatement(check);
                    checkStatement.setInt(1, id);
                    ResultSet resultSet = checkStatement.executeQuery();
                    if (resultSet.next()){
                        temp.setSid(id);
                        snippet.add(temp);
                    }
                    else {
                        String recheck = "select predicate from triple where dataset_local_id=" + dataset + " and object=?";
                        checkStatement = connection.prepareStatement(recheck);
                        checkStatement.setInt(1, id);
                        resultSet = checkStatement.executeQuery();
                        if (resultSet.next()){
                            if (resultSet.getInt("predicate") == typeID){
                                temp.setPid(-1);
                                temp.setOid(id);
                            } else {
                                temp.setOid(id);
                            }
                        }
                        snippet.add(temp);
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return snippet;
    }

    public double SkmRep(Set<triple> snippet){
        Set<Integer>snipPrp = new HashSet<>();//set of properties in the snippet
        Set<Integer>snipCls = new HashSet<>();//set of class in the snippet
        double ttlTriple = 0, ttlCls = 0;/**amount of triple / class instances*/
        double frqCls = 0, frqPrp = 0;
        for(triple iter: snippet) {
            if(iter.getPid() > 0) snipPrp.add(iter.getPid());
            if((typeID != 0 && iter.getPid() == typeID) || iter.getPid() == -1){//孤立点
                snipCls.add(iter.getOid());
            }
        }
        String select = "select triple_count from dataset_info where dataset_local_id = " + dataset;
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet = selectStatement.executeQuery();
            if(resultSet.next()) ttlTriple = resultSet.getInt(1);
            if (typeID != 0){
                select = "select count(*) from triple where dataset_local_id = " + dataset + " and predicate = ?";
                selectStatement = connection.prepareStatement(select);
                selectStatement.setInt(1, typeID);
                resultSet = selectStatement.executeQuery();
                if(resultSet.next()) {
                    ttlCls = resultSet.getInt(1);
                }
            }
            select = "select count(*) from triple where dataset_local_id = " + dataset + " and predicate = ?";
            for(int iter: snipPrp) {
                selectStatement = connection.prepareStatement(select);
                selectStatement.setInt(1, iter);
                resultSet = selectStatement.executeQuery();
                if(resultSet.next()) {
                    frqPrp += resultSet.getInt(1);//amount of property instances
                }
            }
            select = "select count(*) from triple where dataset_local_id = " + dataset + " and predicate = ? and object = ?";
            for(int iter: snipCls) {
                selectStatement = connection.prepareStatement(select);
                selectStatement.setInt(1, typeID);
                selectStatement.setInt(2, iter);
                resultSet = selectStatement.executeQuery();
                if(resultSet.next()) {
                    frqCls += resultSet.getInt(1);//amount of class instances
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(ttlTriple == 0) {
            System.out.print("Empty Dataset. ");
            frqPrp = 0;
        }
        else frqPrp = frqPrp/ttlTriple;
        if(ttlCls == 0) frqCls = 0;
        else frqCls = frqCls/ttlCls;
//        System.out.println("frqPrp: "+frqPrp+";  "+"frqCls: "+frqCls);
//        FrqPrp = frqPrp; FrqCls = frqCls;
        if (typeID == 0){
            return frqPrp;
        }
        else if( frqPrp+frqCls != 0){
            return 2*frqPrp*frqCls/(frqPrp+frqCls);
        }
        else return 0;
    }

    public double EntRep(Set<triple> snippet){
        entitySet = new HashSet<>();
        String select = "select id from uri_label_id where is_literal = 0 and " +
                "dataset_local_id = " + dataset + " and (";
        for (triple iter: snippet){
            select += (" id = " + iter.getSid() + " OR ");
            if (typeID == 0 || (iter.getPid() != typeID && iter.getPid() != -1))select += (" id = " + iter.getOid() + " OR ");
        }
        select = select.substring(0, select.lastIndexOf("OR"));
        select += ")";
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet = selectStatement.executeQuery();
            while(resultSet.next()) {
                entitySet.add(resultSet.getInt("id"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        /**finish entitySet*/
        if (entitySet.isEmpty()){
            return 0;
        }
        double outTotal = 0;
        double inTotal = 0;
        int maxout = 0, maxin = 0;//can be 0
        select = "select max_out_degree, max_in_degree from dataset_info where dataset_local_id = "+dataset;
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet= selectStatement.executeQuery();
            if(resultSet.next()) {
                maxout = resultSet.getInt("max_out_degree");
                maxin = resultSet.getInt("max_in_degree");
            }
            String select1 = "select count(*) from triple where dataset_local_id = " + dataset + " and subject = ?";
            String select2 = "select count(*) from triple where dataset_local_id = " + dataset + " and object = ?";
            for(int iter: entitySet) {//查每个entity的出度
                selectStatement = connection.prepareStatement(select1);
                selectStatement.setInt(1, iter);
                resultSet = selectStatement.executeQuery();
                if(resultSet.next()) {
                    outTotal += Math.log((double)resultSet.getInt(1) +1);
                }
                selectStatement = connection.prepareStatement(select2);
                selectStatement.setInt(1, iter);
                resultSet = selectStatement.executeQuery();
                if(resultSet.next()) {
                    inTotal += Math.log((double)resultSet.getInt(1) +1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        System.out.println("maxout = "+maxout);
//        System.out.println("maxin = "+maxin);
        if(maxout == 0) outTotal = 0;
        else outTotal = outTotal/(entitySet.size() * Math.log((double)maxout +1));
        if(maxin == 0) inTotal = 0;
        else inTotal = inTotal/(entitySet.size() * Math.log((double)maxin +1));
        double coDat = 0;
        if (maxin == 0){
            coDat = outTotal;
        }
        else if(inTotal + outTotal != 0) {
            coDat = 2*(inTotal * outTotal)/(inTotal + outTotal); //harmonic mean
        }
        return coDat;
    }

    public double DescRep(Set<triple> snippet){
        Map<Integer, Set<Integer>> patternMap = new HashMap<>();//entity_id -> EDP
        Map<Integer, Integer> patternCount = new HashMap<>();//entity_id -> relative frequency of its EDP
        Set<Set<Integer>>containedPattern = new HashSet<>();//set of EDP contained in the snippet
        Map<Integer, Set<Integer>> snippetMap = new HashMap<>();//entity_id -> pattern in snippet(can be an incomplete EDP)
        String path = Parameter.directory+"/dataset"+dataset+"/EDPIndex";
        try {
            Directory directory = FSDirectory.open(Paths.get(path));
            IndexReader indexReader = DirectoryReader.open(directory);
            for (int i = 0; i < indexReader.maxDoc(); i++){
                int id = Integer.parseInt(indexReader.document(i).get("id"));
                String[] pattern = indexReader.document(i).get("pattern").split(" ");
                Set<Integer> temp = new HashSet<>();
                for (String iter: pattern){
                    temp.add(Integer.parseInt(iter));
                }
                patternMap.put(id, temp);
                int count = Integer.parseInt(indexReader.document(i).get("count"));
                patternCount.put(id, count);
            }
            for (triple iter: snippet){
                int sid = iter.getSid();
                int pid = iter.getPid();
                int oid = iter.getOid();
                if (entitySet.contains(sid)){
                    Set<Integer> temp = snippetMap.getOrDefault(sid, new HashSet<>());
                    if (typeID != 0 && pid == typeID){
                        temp.add(oid);
                        snippetMap.put(sid, temp);
                    }
                    else if (pid != 0){
                        temp.add(pid);
                        snippetMap.put(sid, temp);
                    }
                }
                if (entitySet.contains(oid) && pid != 0){
                    Set<Integer> temp = snippetMap.getOrDefault(oid, new HashSet<>());
                    temp.add(-pid);
                    snippetMap.put(oid, temp);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        entWithEDP = new HashSet<>();
        int total = patternMap.size();
        int count = 0;
//        System.out.print("\nfull entity: ");
        for (Integer iter: snippetMap.keySet()){
            Set<Integer> candidate = snippetMap.get(iter);
            Set<Integer> reference = patternMap.get(iter);
            if (candidate.equals(reference)){
                entWithEDP.add(iter);
//                System.out.print(iter+" ");
                if (!containedPattern.contains(candidate)){
                    containedPattern.add(candidate);
                    count += patternCount.get(iter);
                }
            }
        }
//        System.out.println("\ntotal: "+total);
//        System.out.println("count: "+count);
        return (double)count/total;
    }

    public double LinkRep(Set<triple> snippet){
        if (entWithEDP.isEmpty())return 0;
        String path = Parameter.directory+"/dataset"+dataset+"/LPIndex";
        Map<String, Double>triple2freq = new HashMap<>();//LP的Map
        Map<String, Integer>triple2id = new HashMap<>();//
        try {
            Directory directory = FSDirectory.open(Paths.get(path));
            IndexReader indexReader = DirectoryReader.open(directory);
            for (int i = 0; i < indexReader.maxDoc(); i++){
                String triple = indexReader.document(i).get("triple");
                Double freq = Double.parseDouble(indexReader.document(i).get("frequency"));
                int id = Integer.parseInt(indexReader.document(i).get("id"));
                triple2freq.put(triple, freq);
                triple2id.put(triple, id);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        Set<Integer> containedLP = new HashSet<>();
        double ans = 0;
        for (triple iter: snippet){
            int sid = iter.getSid();
            int pid = iter.getPid();
            int oid = iter.getOid();
            if (!entWithEDP.contains(sid) || !entWithEDP.contains(oid))continue;
            String temp = sid+" "+pid+" "+oid;
            int LPid = triple2id.get(temp);
            if (!containedLP.contains(LPid)){
                ans += triple2freq.get(temp);
                containedLP.add(LPid);
            }
        }
        return ans;
    }

    public double KwRel(Set<triple> snippet, List<String>kws){
        int count = 0;
        Set<Integer>ids = new HashSet<>();
        for(triple iter: snippet) {
            ids.add(iter.getSid());
            ids.add(iter.getPid());
            ids.add(iter.getOid());
        }
        String path = Parameter.directory+"/dataset"+dataset+"/labelIDIndex";
        Analyzer analyzer = new StemAnalyzer();
        try {
            Directory directory = FSDirectory.open(Paths.get(path));
            IndexReader indexReader = DirectoryReader.open(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            QueryParser parser = new QueryParser("label", analyzer);
            kw2ids = new HashMap<>();
            for (String iter: kws){
                Set<Integer> hitSet = new HashSet<>();
                Query query = parser.parse(iter);
                TopDocs topDocs = indexSearcher.search(query, 10000000);
                if (topDocs != null){
                    for (ScoreDoc sdoc: topDocs.scoreDocs){
                        hitSet.add(Integer.parseInt(indexSearcher.doc(sdoc.doc).get("id")));
                    }
                }
                for (Integer id: ids){
                    if (hitSet.contains(id)){
                        count++;
                        break;
                    }
                }
                Set<Integer> temp = new HashSet<>();
                for (Integer id: ids){
                    if (hitSet.contains(id)){
                        temp.add(id);
                    }
                }
                kw2ids.put(iter, temp);
            }
            indexReader.close();
            directory.close();
        }catch (Exception e){
            e.printStackTrace();
        }
        cokNEW = (double)count/kws.size();
        return cokNEW;
    }

    public double QryRel(Set<triple> snippet, List<String> kws, List<String>qws){
        String path = Parameter.directory+"/dataset"+dataset+"/componentIndex";
        List<List<String>> wordPair = new ArrayList<>();
        try {
            IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(path))));
            QueryParser parser = new QueryParser("component", new StemAnalyzer());
            for (int i = 0; i < qws.size()-1; i++){
                String word1 = qws.get(i);
                String word2 = qws.get(i + 1);
                if (!kws.contains(word1))continue;
                if (!kws.contains(word2)){
                    i++;
                    continue;
                }
                Set<Integer> hitSet1 = new HashSet<>();
                Query query = parser.parse(word1);
                TopDocs topDocs = indexSearcher.search(query, 10000000);
                if (topDocs != null){
                    for (ScoreDoc sdoc: topDocs.scoreDocs){
                        hitSet1.add(Integer.parseInt(indexSearcher.doc(sdoc.doc).get("id")));
                    }
                }
                Set<Integer> hitSet2 = new HashSet<>();
                query = parser.parse(word2);
                topDocs = indexSearcher.search(query, 10000000);
                if (topDocs != null){
                    for (ScoreDoc sdoc: topDocs.scoreDocs){
                        hitSet2.add(Integer.parseInt(indexSearcher.doc(sdoc.doc).get("id")));
                    }
                }
                for (int iter: hitSet1){
                    if (hitSet2.contains(iter)){
                        List<String> pair = new ArrayList<>();
                        pair.add(word1);
                        pair.add(word2);
                        wordPair.add(pair);
                        break;
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if (wordPair.isEmpty()){
            return cokNEW;
        }
        Multigraph<Integer, DefaultEdge> graph = new Multigraph<>(DefaultEdge.class);
        for (triple iter: snippet){
            int s = iter.getSid();
            int p = iter.getPid();
            int o = iter.getOid();
            if (s > 0) graph.addVertex(s);
            if (p > 0) graph.addVertex(p);
            if (o > 0) graph.addVertex(o);
            if (graph.containsVertex(s) && graph.containsVertex(p) && s != p) graph.addEdge(s, p);
            if (graph.containsVertex(p) && graph.containsVertex(o) && p != o) graph.addEdge(p, o);
        }
        ConnectivityInspector<Integer, DefaultEdge> inspector = new ConnectivityInspector<>(graph);
        List<Set<Integer>> components = inspector.connectedSets();
        int count = 0;
        for (List<String> iter: wordPair){
            Set<Integer> ids1 = kw2ids.get(iter.get(0));
            Set<Integer> ids2 = kw2ids.get(iter.get(1));
            int flag = 0;
            for (int i1: ids1){
                for (int i2: ids2){
                    for (Set<Integer> set: components){
                        if (set.contains(i1) && set.contains(i2)){
                            count++;
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 1)break;
                }
                if (flag == 1)break;
            }
        }
        return ((double)count)/wordPair.size();
    }
}

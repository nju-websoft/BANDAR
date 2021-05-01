package snippetAlgorithm;

import beans.weightedTriple;
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
import util.DBUtil;
import util.StemAnalyzer;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class KSDSnippet {
    Connection connection;

    public KSDSnippet(int dataset){
        connection = new DBUtil().conn;
        this.dataset = dataset;
        getBasicInfo(dataset);
    }

    public void close(){
        try {
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Set<weightedTriple> result = new HashSet<>();

    private int dataset;
    private static int MAX_SIZE = 5; //snippet size bound
    private int typeID;//0: no type, no class
    private double logMaxin, logMaxout;
    private List<weightedTriple>heap = new ArrayList<>();
    private Map<Integer, Integer>propertyCount = new HashMap<>(); //property -> count
    private Map<Integer, Integer>classCount = new HashMap<>(); //class -> count
    private Map<Integer, Integer>outDegreeMap = new HashMap<>();//entity -> its out degree
    private Map<Integer, Integer>inDegreeMap = new HashMap<>();//entity -> its in degree
    private Set<Integer>literalSet = new HashSet<>();
    private Map<String, Set<Integer>> kws2id = new HashMap<>();/**kws -> Set of IDs containing it*/
    private int T,C;/**triple_count，class_count*/
    boolean[] iscovered;

    private void getBasicInfo(int dataset){
        //---------------running example----------
        String select = "select type_id,max_out_degree,max_in_degree from dataset_info where dataset_local_id = " + dataset;
        //---------------running example----------
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()){
                typeID = resultSet.getInt("type_id");
                logMaxout = Math.log(resultSet.getInt("max_out_degree")+1.0);
                logMaxin = Math.log(resultSet.getInt("max_in_degree")+1.0);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Set<weightedTriple>findSnippet(List<String>kws){
        createTriples();
        setInitialWeight(kws);
        getSnippet(kws);
        return result;
    }

    private void createTriples(){
        //---------------running example----------
        String selectLiteral = "select id from uri_label_id where dataset_local_id=" + dataset + " and is_literal=1";
        String select = "select subject,predicate,object from triple where dataset_local_id=" + dataset;
        //---------------running example----------
        try {
            PreparedStatement selectStatement = connection.prepareStatement(selectLiteral);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()){
                literalSet.add(resultSet.getInt("id"));
            }
            /**finish Literal Set*/
            selectStatement = connection.prepareStatement(select);
            resultSet = selectStatement.executeQuery();
            while (resultSet.next()){
                int s = resultSet.getInt("subject");
                int p = resultSet.getInt("predicate");
                int o = resultSet.getInt("object");
                weightedTriple triple = new weightedTriple();
                triple.setSid(s);
                triple.setPid(p);
                triple.setOid(o);
                heap.add(triple);
                int pCount = propertyCount.getOrDefault(p, 0);
                propertyCount.put(p, pCount+1);//property
                int sOut = outDegreeMap.getOrDefault(s, 0);
                outDegreeMap.put(s, sOut+1);//subject-out degree
                if (p == typeID){//object is class
                    int cCount = classCount.getOrDefault(o, 0);
                    classCount.put(o, cCount+1);
                }
                else if (!literalSet.contains(o)){
                    int oIn = inDegreeMap.getOrDefault(o, 0);
                    inDegreeMap.put(o, oIn+1);
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        T = heap.size();
        if (typeID != 0){
            for (int iter: classCount.keySet()){
                C += classCount.get(iter);
            }
        }
    }

    private void setInitialWeight(List<String>kws){
        //-------------running example---------
        String path = Parameter.directory+"/dataset"+dataset+"/labelIDIndex";
        //-------------running example------------
        Analyzer analyzer = new StemAnalyzer();
        try {
            Directory directory = FSDirectory.open(Paths.get(path));
            IndexReader indexReader = DirectoryReader.open(directory);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            QueryParser parser = new QueryParser("label", analyzer);
            for (String iter: kws){
                Set<Integer> hitSet = new HashSet<>();
                Query query = parser.parse(iter);
                TopDocs topDocs = indexSearcher.search(query, 10000000);
                if (topDocs != null){
                    for (ScoreDoc sdoc: topDocs.scoreDocs){
                        hitSet.add(Integer.parseInt(indexSearcher.doc(sdoc.doc).get("id")));
                    }
                }
                kws2id.put(iter, hitSet);
            }
            /**finish kws2id：each of kws -> id containing it*/
//            System.out.println(kws2id);
            for (weightedTriple iter: heap){
                int s = iter.getSid();
                int p = iter.getPid();
                int o = iter.getOid();
                int count = 0;
                for (String siter: kws){
                    Set<Integer> temp = kws2id.get(siter);
                    if (temp.contains(s)||temp.contains(p)||temp.contains(o)){
                        count++;
                    }
                }
                iter.kwsW = (double)count/kws.size();
                iter.prpW = (double)propertyCount.get(p)/T;
                double wOut = Math.log(outDegreeMap.get(s)+1.0)/logMaxout;
                double wIn = Math.log(inDegreeMap.getOrDefault(s, 0)+1.0)/logMaxin;
                if (classCount.containsKey(o)){
                    iter.clsW = (double)classCount.get(o)/C;
                }
                else {
                    wOut += Math.log(outDegreeMap.getOrDefault(o, 0)+1.0)/logMaxout;
                    wIn += Math.log(inDegreeMap.getOrDefault(o, 0)+1.0)/logMaxin;
                }
                iter.outW = wOut;
                iter.inW = wIn;
                iter.setW();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private Set<weightedTriple> getSnippet(List<String>kws){
        iscovered = new boolean[kws.size()];
        boolean[] covering = new boolean[kws.size()];
        Set<Integer>ids = new HashSet<>();/**subject / object in current snippet*/
        for (int i = 1; i <= MAX_SIZE; i++){
            if (heap.size() == 0)break;
            Collections.sort(heap);
            weightedTriple top = heap.get(0);
            result.add(top);
            heap.remove(0);
            int s = top.getSid();
            int p = top.getPid();
            int o = top.getOid();
            ids.add(s);
            ids.add(o);
            if (top.kwsW != 0){
                for (int j = 0; j < kws.size(); j++){
                    Set<Integer>temp = kws2id.get(kws.get(j));
                    if (!iscovered[j] && (temp.contains(s) || temp.contains(p) || temp.contains(o))){
                        covering[j] = true;
                        iscovered[j] = true;
                    }
                }
            }
            for (weightedTriple iter: heap){
                int ss = iter.getSid();
                int pp = iter.getPid();
                int oo = iter.getOid();
                if (top.kwsW != 0 && iter.kwsW != 0){
                    int count = 0;
                    for (int j = 0; j < kws.size(); j++){
                        if (!covering[j])continue;
                        Set<Integer>temp = kws2id.get(kws.get(j));
                        if (temp.contains(ss) || temp.contains(pp) || temp.contains(oo)){
                            count++;
                        }
                    }
                    iter.kwsW -= (double)count/kws.size();
                }
                if (top.prpW > 0 && pp == p) iter.prpW = 0;
                if (top.clsW > 0 && oo == o) iter.clsW = 0;
                if ((top.outW > 0||top.inW > 0) && (ss == s || oo == o)){
                    if (ids.contains(ss) && ids.contains(oo)){
                        iter.outW = 0;
                        iter.inW = 0;
                        iter.setW();
                        continue;
                    }
                    double wOut = 0;
                    double wIn = 0;
                    if (!ids.contains(ss)){
                        wOut = Math.log(outDegreeMap.get(ss)+1.0)/logMaxout;
                        wIn = Math.log(inDegreeMap.getOrDefault(ss, 0)+1.0)/logMaxin;
                    }
                    if (!ids.contains(oo)){
                        wOut += Math.log(outDegreeMap.getOrDefault(oo, 0)+1.0)/logMaxout;
                        wIn += Math.log(inDegreeMap.getOrDefault(oo, 0)+1.0)/logMaxin;
                    }
                    iter.outW = wOut;
                    iter.inW = wIn;
                }
                iter.setW();
            }
        }
        return result;
    }
}

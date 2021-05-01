package scriptProcess.snippetAlgorithmFile;

import beans.weightedTriple;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import util.ReadFile;
import util.StemAnalyzer;

import java.nio.file.Paths;
import java.util.*;

public class KSDSnippet {

    public KSDSnippet(){
    }

    private String folder;

    public Set<weightedTriple> result = new HashSet<>();

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

    private void getBasicInfo(){
        List<Integer> info = ReadFile.readInteger(folder + "/indexes/dataset_info.txt");
        typeID = info.get(0);
        logMaxout = Math.log(info.get(3) + 1.0);
        logMaxin = Math.log(info.get(2) + 1.0);
    }

    public Set<weightedTriple>findSnippet(List<String>kws, String folder){
        this.folder = folder;
        getBasicInfo();
        createTriples();
        setInitialWeight(kws);
        getSnippet(kws);
        return result;
    }

    private void createTriples(){
        try {
            for (List<String> iter: ReadFile.readString(folder + "/indexes/uri_label.txt", "\t")) {
                if (Integer.parseInt(iter.get(3)) == 1) {
                    literalSet.add(Integer.parseInt(iter.get(0)));
                }
            }
            for (List<Integer> iter: ReadFile.readInteger(folder + "/indexes/triple.txt", "\t")) {
                int s = iter.get(0);
                int p = iter.get(1);
                int o = iter.get(2);
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
        String path = folder + "/indexes/labelIDIndex";
        try {
            IndexSearcher indexSearcher = new IndexSearcher(DirectoryReader.open(FSDirectory.open(Paths.get(path))));
            QueryParser parser = new QueryParser("label", new StemAnalyzer());
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

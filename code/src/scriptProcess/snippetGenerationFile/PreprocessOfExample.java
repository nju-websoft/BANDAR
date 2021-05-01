package scriptProcess.snippetGenerationFile;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.WeightedMultigraph;
import scriptProcess.snippetAlgorithmFile.DualCES;
import scriptProcess.snippetAlgorithmFile.PrunedDPSnippet;
import scriptProcess.snippetAlgorithmFile.TACSnippet;
import util.ReadFile;
import util.StemAnalyzer;
import util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.*;

public class PreprocessOfExample {

    private static void ReadDataset(String datasetFile) {
        try {
            String fileBase = datasetFile.substring(0, datasetFile.lastIndexOf("/"));
            String indexFolder = fileBase + "/indexes";
            File index = new File(indexFolder);
            if (index.exists()) {
                for (File f: index.listFiles()) {
                    f.delete();
                }
            }
            index.mkdirs();
            PrintWriter tripleWriter = new PrintWriter(indexFolder + "/triple.txt");
            PrintWriter labelWriter = new PrintWriter(indexFolder + "/uri_label.txt");
            Map<String, Integer> uri2id = new HashMap<>();
            Map<String, String> uri2label = new HashMap<>();
            Set<String> literal = new HashSet<>();
            List<List<String>> triples = ReadFile.readString(datasetFile, "\\s+");
            int count = 0;
            for (List<String> t: triples) {
                String s = t.get(0);
                if (s.startsWith("<")) {
                    s = s.substring(1, s.length() - 1);
                }
                if (!uri2id.containsKey(s)) {
                    count++;
                    uri2id.put(s, count);
                    if (s.contains("#")) {
                        uri2label.put(s, s.substring(s.lastIndexOf("#") + 1));
                    }
                    else if (s.contains("/")) {
                        uri2label.put(s, s.substring(s.lastIndexOf("/") + 1));
                    }
                    else {
                        uri2label.put(s, s);
                    }
                    labelWriter.println(count + "\t" + s + "\t" + uri2label.get(s) + "\t" + 0);
                }
                String p = t.get(1);
                if (p.startsWith("<")) {
                    p = p.substring(1, p.length() - 1);
                }
                if (!uri2id.containsKey(p)) {
                    count++;
                    uri2id.put(p, count);
                    if (p.contains("#")) {
                        uri2label.put(p, p.substring(p.lastIndexOf("#") + 1));
                    }
                    else if (p.contains("/")) {
                        uri2label.put(p, p.substring(p.lastIndexOf("/") + 1));
                    }
                    else {
                        uri2label.put(p, p);
                    }
                    labelWriter.println(count + "\t" + p + "\t" + uri2label.get(p) + "\t" + 0);
                }
                String o = t.get(2);
                if (o.startsWith("<")) {
                    o = o.substring(1, o.length() - 1);
                }
                else {
                    literal.add(o);
                }
                if (!uri2id.containsKey(o)) {
                    count++;
                    uri2id.put(o, count);
                    if (literal.contains(o)) {
                        uri2label.put(o, o);
                        labelWriter.println(count + "\t" + o + "\t" + uri2label.get(o) + "\t" + 1);
                    }
                    else {
                        if (o.contains("#")) {
                            uri2label.put(o, o.substring(o.lastIndexOf("#") + 1));
                        }
                        else if (o.contains("/")) {
                            uri2label.put(o, o.substring(o.lastIndexOf("/") + 1));
                        }
                        else {
                            uri2label.put(o, o);
                        }
                        labelWriter.println(count + "\t" + o + "\t" + uri2label.get(o) + "\t" + 0);
                    }
                }
                tripleWriter.println(uri2id.get(s) + "\t" + uri2id.get(p) + "\t" + uri2id.get(o));
            }
            tripleWriter.close();
            labelWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void tacPreprocess(String indexFolder){
        TACSnippet snippet = new TACSnippet();
        snippet.createTripleIndex(indexFolder);
        WeightedMultigraph<Integer, DefaultWeightedEdge> totalGraph = snippet.generateSimpleGraphByTriples(indexFolder);
        snippet.MRRExtraction(totalGraph, 1, indexFolder);//radius = 1
        snippet.createAllKeywordIndex(indexFolder);
    }

    private static void prunedpPreprocess(String indexFolder){
        PrunedDPSnippet snippet = new PrunedDPSnippet();
        snippet.createLabelIdIndex(indexFolder);
    }

    private static void dualcesPreprocess(String indexFolder){
        DualCES snippet = new DualCES();
        snippet.createSentenceIndex(indexFolder);
    }

    private static void recordDatasetInfo(String indexFolder){
        try {
            PrintWriter writer = new PrintWriter(indexFolder + "/dataset_info.txt");
            int typeId = 0;
            for (List<String> iter: ReadFile.readString(indexFolder + "/uri_label.txt", "\t")) {
                if (iter.get(1).equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                    typeId = Integer.parseInt(iter.get(0));
                    break;
                }
            }
            Set<Integer> literal = new HashSet<>();
            for (List<String> iter: ReadFile.readString(indexFolder + "/uri_label.txt", "\t")) {
                if (Integer.parseInt(iter.get(3)) == 1) {
                    literal.add(Integer.parseInt(iter.get(0)));
                }
            }
            writer.println(typeId);
            List<List<Integer>> triple = ReadFile.readInteger(indexFolder + "/triple.txt", "\t");
            writer.println(triple.size());
            Map<Integer, Integer> outDeg = new HashMap<>();
            Map<Integer, Integer> inDeg = new HashMap<>();
            Set<Integer> predicate = new HashSet<>();
            Set<Integer> classes = new HashSet<>();
            for (List<Integer> iter: triple) {
                int s = iter.get(0);
                int p = iter.get(1);
                int o = iter.get(2);
                int out = outDeg.getOrDefault(s, 0);
                outDeg.put(s, (out + 1));
                if (p != typeId && !literal.contains(o)) {
                    int in = inDeg.getOrDefault(o, 0);
                    inDeg.put(o, (in + 1));
                }
                predicate.add(iter.get(1));
                if (iter.get(1) == typeId) {
                    classes.add(iter.get(2));
                }
            }
            int tempIn = 0;
            for (Map.Entry<Integer, Integer> entry: inDeg.entrySet()) {
                if (tempIn < entry.getValue()) {
                    tempIn = entry.getValue();
                }
            }
            writer.println(tempIn);
            int tempOut = 0;
            for (Map.Entry<Integer, Integer> entry: outDeg.entrySet()) {
                if (tempOut < entry.getValue()) {
                    tempOut = entry.getValue();
                }
            }
            writer.println(tempOut);
            writer.println(predicate.size());
            writer.println(classes.size());
            writer.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createLabelIDIndex(String indexFolder){
        try {
            Map<Integer, String>idLabel = new TreeMap<>();
            for (List<String> iter: ReadFile.readString(indexFolder + "/uri_label.txt", "\t")) {
                int id = Integer.parseInt(iter.get(0));
                idLabel.put(id, StringUtil.processLabel(iter.get(2)));
            }
            File file = new File(indexFolder + "/labelIDIndex");
            if (file.exists()) {
                for (File f: file.listFiles()) {
                    f.delete();
                }
            }
            Directory directory = FSDirectory.open(Paths.get(indexFolder + "/labelIDIndex"));
            IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(new StemAnalyzer()));
            for (Map.Entry<Integer, String> iter: idLabel.entrySet()){
                Document document = new Document();
                document.add(new StringField("id", String.valueOf(iter.getKey()), Field.Store.YES));
                document.add(new TextField("label", iter.getValue(), Field.Store.YES));
                indexWriter.addDocument(document);
            }
            indexWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void createComponentIndex(String indexFolder){
        try {
            Multigraph<Integer, DefaultEdge> graph = new Multigraph<>(DefaultEdge.class);
            for (List<Integer> triple: ReadFile.readInteger(indexFolder + "/triple.txt", "\t")) {
                int subject = triple.get(0);
                int predicate = triple.get(1);
                int object = triple.get(2);
                graph.addVertex(subject);
                graph.addVertex(predicate);
                graph.addVertex(object);
                if (subject != predicate) graph.addEdge(subject, predicate);
                if (predicate != object) graph.addEdge(predicate, object);
            }
            ConnectivityInspector<Integer, DefaultEdge> inspector = new ConnectivityInspector<>(graph);
            List<Set<Integer>> components = inspector.connectedSets();
            /**finish the graph*/
            Map<Integer, String> idLabelMap = new HashMap<>();
            for (List<String> iter: ReadFile.readString(indexFolder + "/uri_label.txt", "\t")) {
                int id = Integer.parseInt(iter.get(0));
                String label = iter.get(2);
                if (label == null || label.equals(""))continue;
                idLabelMap.put(id, StringUtil.processLabel(label));
            }
            File file = new File(indexFolder + "/componentIndex");
            if (file.exists()) {
                for (File f: file.listFiles()) {
                    f.delete();
                }
            }
            IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(indexFolder + "/componentIndex")), new IndexWriterConfig(new StemAnalyzer()));
            int count = 0;
            for (Set<Integer> component: components){
                count ++;
                StringBuilder text = new StringBuilder();
                for (int iter: component){
                    text.append(idLabelMap.getOrDefault(iter, "") + " ");
                }
                if (text.toString().trim().equals(""))continue;
                Document document = new Document();
                document.add(new StringField("id", String.valueOf(count), Field.Store.YES));
                document.add(new TextField("component", text.toString().trim(), Field.Store.YES));
                indexWriter.addDocument(document);
            }
            indexWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void createEDPIndex(String indexFolder){
        try {
            int typeID = ReadFile.readInteger(indexFolder + "/dataset_info.txt").get(0);
            Set<Integer> literal = new HashSet<>();
            for (List<String> iter: ReadFile.readString(indexFolder + "/uri_label.txt", "\t")) {
                if (Integer.parseInt(iter.get(3)) == 1) {
                    literal.add(Integer.parseInt(iter.get(0)));
                }
            }
            Map<Integer, Set<Integer>> id2KP = new HashMap<>();/**entity -> pattern*/
            for (List<Integer> iter: ReadFile.readInteger(indexFolder + "/triple.txt", "\t")) {
                int sid = iter.get(0);
                int pid = iter.get(1);
                int oid = iter.get(2);
                if (!id2KP.containsKey(sid)){
                    Set<Integer>temp = new HashSet<>();
                    if (typeID != 0 && pid == typeID){//S-TYPE-C
                        temp.add(oid);
                    }
                    else {
                        temp.add(pid);
                    }
                    id2KP.put(sid, temp);
                }
                else {
                    Set<Integer>temp = id2KP.get(sid);
                    if (typeID != 0 && pid == typeID){//S-TYPE-C
                        temp.add(oid);
                    }
                    else {
                        temp.add(pid);
                    }
                    id2KP.put(sid, temp);
                }
                if ((typeID == 0 || pid != typeID) && !literal.contains(oid)){//object is an entity
                    if (!id2KP.containsKey(oid)){
                        Set<Integer>temp = new HashSet<>();
                        temp.add(-pid);
                        id2KP.put(oid, temp);
                    }
                    else {
                        Set<Integer>temp = id2KP.get(oid);
                        temp.add(-pid);
                        id2KP.put(oid, temp);
                    }
                }
            }

            List<Integer> info = ReadFile.readInteger(indexFolder + "/dataset_info.txt");
            info.add(id2KP.keySet().size());
            Map<Set<Integer>, Integer>pattern2Count = new HashMap<>(); /**pattern -> count*/
            for (Map.Entry<Integer, Set<Integer>> iter: id2KP.entrySet()){
                Set<Integer> pattern = iter.getValue();
                int temp = pattern2Count.getOrDefault(pattern, 0);
                pattern2Count.put(pattern, temp + 1);
            }
            info.add(pattern2Count.keySet().size());
            PrintWriter writer = new PrintWriter(indexFolder + "/dataset_info.txt");
            for (int iter: info) {
                writer.println(iter);
            }
            writer.close();

            File file = new File(indexFolder + "/EDPIndex");
            if (file.exists()) {
                for (File f: file.listFiles()) {
                    f.delete();
                }
            }
            Directory directory = FSDirectory.open(Paths.get(indexFolder + "/EDPIndex"));
            IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(new StandardAnalyzer()));
            for (int iter: id2KP.keySet()){
                Document document = new Document();
                document.add(new StringField("id", String.valueOf(iter), Field.Store.YES));
                Set<Integer>temp = id2KP.get(iter);
                int count = pattern2Count.get(temp);
                document.add(new StringField("count", String.valueOf(count), Field.Store.YES));
                String pstring = "";
                for (int j: temp){
                    pstring += (j + " ");
                }
                pstring = pstring.substring(0, pstring.length()-1);
                document.add(new StringField("pattern", pstring, Field.Store.YES));
                indexWriter.addDocument(document);
            }
            indexWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static void createLPIndex(String indexFolder){
        Map<Integer, Set<Integer>> ent2pattern = new HashMap<>();//get all entity_id -> EDP from the index
        Map<Set<Integer>, Integer> patternID = new HashMap<>();//EDP -> its id
        try {
            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexFolder + "/EDPIndex")));
            if (indexReader.maxDoc() == 0)return;
            int edpIter = 0;
            for (int j = 0; j < indexReader.maxDoc(); j++){
                int id = Integer.parseInt(indexReader.document(j).get("id"));
                String[] pattern = indexReader.document(j).get("pattern").split(" ");
                Set<Integer> temp = new HashSet<>();
                for (String iter: pattern){
                    temp.add(Integer.parseInt(iter));
                }
                ent2pattern.put(id, temp);
                if (!patternID.containsKey(temp)){
                    edpIter++;
                    patternID.put(temp, edpIter);//EDP's id
                }
            }

            Map<List<Integer>, List<Integer>>triple2LP  = new HashMap<>();//triple -> EDP1,p,EDP2
            Map<List<Integer>, Integer>LPcount = new HashMap<>();//EDP1,p,EDP2 -> count
            int LPtotal = 0;
            for (List<Integer> iter: ReadFile.readInteger(indexFolder + "/triple.txt", "\t")) {
                int sid = iter.get(0);
                int pid = iter.get(1);
                int oid = iter.get(2);
                if (!ent2pattern.containsKey(oid))continue;
                List<Integer> tempkey = new ArrayList<>();
                tempkey.add(sid);
                tempkey.add(pid);
                tempkey.add(oid);
                List<Integer> tempvalue = new ArrayList<>();
                tempvalue.add(patternID.get(ent2pattern.get(sid)));
                tempvalue.add(pid);
                tempvalue.add(patternID.get(ent2pattern.get(oid)));
                triple2LP.put(tempkey, tempvalue);
                int count = LPcount.getOrDefault(tempvalue, 0);
                count++;
                LPcount.put(tempvalue, count);
            }
            Map<List<Integer>, Integer>LP2id = new HashMap<>();//EDP1,p,EDP2 -> LP's id
            int id = 0;
            for (List<Integer> iter: LPcount.keySet()){
                LPtotal += LPcount.get(iter);
                id++;
                LP2id.put(iter, id);
            }
            List<Integer> info = ReadFile.readInteger(indexFolder + "/dataset_info.txt");
            info.add(LPtotal);
            info.add(LPcount.keySet().size());
            PrintWriter writer = new PrintWriter(indexFolder + "/dataset_info.txt");
            for (int iter: info) {
                writer.println(iter);
            }
            writer.close();

            File file = new File(indexFolder + "/LPIndex");
            if (file.exists()) {
                for (File f: file.listFiles()) {
                    f.delete();
                }
            }
            Directory directory2 = FSDirectory.open(Paths.get(indexFolder + "/LPIndex"));
            IndexWriter indexWriter = new IndexWriter(directory2, new IndexWriterConfig(new StandardAnalyzer()));
            for (List<Integer> iter: triple2LP.keySet()){
                List<Integer> LP = triple2LP.get(iter);//EDP1,p,EDP2
                int count = LPcount.get(LP);
                int LPid = LP2id.get(LP);
                Document document = new Document();
                document.add(new StringField("triple", iter.get(0)+" "+iter.get(1)+" "+iter.get(2), Field.Store.YES));
                document.add(new StringField("frequency", String.valueOf((double)count/LPtotal), Field.Store.YES));
                document.add(new StringField("id", String.valueOf(LPid), Field.Store.YES));
                indexWriter.addDocument(document);
            }
            indexWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void preprocess(String datasetFile) {
        File file = new File(datasetFile);
        if (!file.exists()) {
            System.out.println("Dataset file not found. ");
            return;
        }
        try {
            ReadDataset(datasetFile);
            String baseFolder = file.getCanonicalPath().replaceAll("\\\\", "/");
            baseFolder = baseFolder.substring(0, baseFolder.lastIndexOf("/"));
            tacPreprocess(baseFolder + "/indexes");
            prunedpPreprocess(baseFolder + "/indexes");
            dualcesPreprocess(baseFolder + "/indexes");
            recordDatasetInfo(baseFolder + "/indexes");
            createLabelIDIndex(baseFolder + "/indexes");
            createComponentIndex(baseFolder + "/indexes");
            createEDPIndex(baseFolder + "/indexes");
            createLPIndex(baseFolder + "/indexes");
            System.out.println("Finish preprocess for " + datasetFile + ". Indexes are stored in " + baseFolder + "/indexes/. ");
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public static void main(String[] args){
//        String baseFolder = "C:/Users/xiaxiawang/Desktop";
//        ReadDataset(baseFolder + "/dataset.nt");
//        tacPreprocess(baseFolder + "/indexes");
//        prunedpPreprocess(baseFolder + "/indexes");
//        dualcesPreprocess(baseFolder + "/indexes");
//        recordDatasetInfo(baseFolder + "/indexes");
//        createLabelIDIndex(baseFolder + "/indexes");
//        createComponentIndex(baseFolder + "/indexes");
//        createEDPIndex(baseFolder + "/indexes");
//        createLPIndex(baseFolder + "/indexes");
//    }
}

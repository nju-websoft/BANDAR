package snippetGenerationTest;

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
import snippetAlgorithm.DualCES;
import snippetAlgorithm.Parameter;
import snippetAlgorithm.PrunedDPSnippet;
import snippetAlgorithm.TACSnippet;
import util.DBUtil;
import util.StemAnalyzer;
import util.StringUtil;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class PreprocessOfExample {
    Connection connection = new DBUtil().conn;

    private void tacPreprocess(int dataset){
        TACSnippet snippet = new TACSnippet(dataset);
        snippet.createTripleIndex();
        WeightedMultigraph<Integer, DefaultWeightedEdge> totalGraph = snippet.generateSimpleGraphByTriples();
        snippet.MRRExtraction(totalGraph, 1);//radius = 1
        snippet.createAllKeywordIndex();
    }

    private void prunedpPreprocess(int dataset){
        PrunedDPSnippet snippet = new PrunedDPSnippet(dataset);
        snippet.createLabelIdIndex();
    }

    private void dualcesPreprocess(int dataset){
        DualCES snippet = new DualCES(dataset);
        snippet.createSentenceIndex();
        snippet.createTfIndex();
    }

    private void recordDatasetInfo(int dataset){
        String select = "select count(*) from triple where dataset_local_id = " + dataset;
        String insert = "insert into dataset_info (triple_count, dataset_local_id) values(?,?)";
        int triple;
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet = selectStatement.executeQuery();
            resultSet.next();
            triple = resultSet.getInt(1);
            PreparedStatement insertStatement = connection.prepareStatement(insert);
            insertStatement.setInt(1, triple);
            insertStatement.setInt(2, dataset);
            insertStatement.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
        }
        //==================================================
        select = "select id from uri_label_id where dataset_local_id = " + dataset +
                " and uri = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'";
        String update = "update dataset_info set type_id = ? where dataset_local_id = " + dataset;
        int typeID = 0;
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            PreparedStatement updateStatement = connection.prepareStatement(update);
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()){
                typeID = resultSet.getInt(1);
            }
            updateStatement.setInt(1, typeID);
            updateStatement.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
        }
        //====================================================
        String selectProperty = "SELECT COUNT(DISTINCT predicate) FROM triple WHERE dataset_local_id = " + dataset;
        String selectClass = "SELECT COUNT(DISTINCT object) FROM triple WHERE predicate = " + typeID + " AND dataset_local_id = " + dataset;
        update = "update dataset_info set property_count = ?, class_count = ? where dataset_local_id = " + dataset;
        int pcount = 0, ccount = 0;
        try {
            PreparedStatement getProperty = connection.prepareStatement(selectProperty);
            PreparedStatement getClass = connection.prepareStatement(selectClass);
            PreparedStatement updateStatement = connection.prepareStatement(update);
            ResultSet resultSet = getProperty.executeQuery();
            while (resultSet.next()){
                pcount = resultSet.getInt(1);
            }
            resultSet = getClass.executeQuery();
            while (resultSet.next()){
                ccount = resultSet.getInt(1);
            }
            updateStatement.setInt(1, pcount);
            updateStatement.setInt(2, ccount);
            updateStatement.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
        }
        //====================================================
        /**get the'max_in_degree' and 'max_out_degree' of all entities in dataset_info
         * Entity：subject or object, not class，not literal
         * @DATE: 20200312*/
        String selectLiteral = "select id from uri_label_id where is_literal=1 and dataset_local_id = " + dataset;
        String selectTriple = "select subject,predicate,object from triple where dataset_local_id = " + dataset;
        String selectOut = "select count(*) from triple where subject=? and dataset_local_id = " + dataset;
        String selectIn = "select count(*) from triple where object=? and dataset_local_id = " + dataset;
        update = "update dataset_info set max_out_degree=?, max_in_degree=? where dataset_local_id = " + dataset;
        try {
            PreparedStatement updateStatement = connection.prepareStatement(update);
            PreparedStatement selectLiteralStatement = connection.prepareStatement(selectLiteral);
            PreparedStatement selectTripleStatement = connection.prepareStatement(selectTriple);
            PreparedStatement selectOutStatement = connection.prepareStatement(selectOut);
            PreparedStatement selectInStatement = connection.prepareStatement(selectIn);
            Set<Integer> literalSet = new HashSet<>();
            ResultSet resultSet = selectLiteralStatement.executeQuery();
            while (resultSet.next()){
                literalSet.add(resultSet.getInt("id"));
            }
            /**finish Literal Set*/
            Set<Integer> entitySet = new HashSet<>();
            resultSet = selectTripleStatement.executeQuery();
            while (resultSet.next()){
                entitySet.add(resultSet.getInt("subject"));
                if (resultSet.getInt("predicate") == typeID)continue;
                int object = resultSet.getInt("object");
                if (!literalSet.contains(object)){
                    entitySet.add(object);
                }
            }
            /**finish Entity Set*/
            int maxout = 0, maxin = 0;/**record maximum out/in degreeo: out-subject，in-object*/
            for (int iter: entitySet){
                selectOutStatement.setInt(1, iter);
                resultSet = selectOutStatement.executeQuery();
                if (resultSet.next()){
                    int temp = resultSet.getInt(1);
                    if (maxout < temp){
                        maxout = temp;
                    }
                }
                selectInStatement.setInt(1, iter);
                resultSet = selectInStatement.executeQuery();
                if (resultSet.next()){
                    int temp = resultSet.getInt(1);
                    if (maxin < temp){
                        maxin = temp;
                    }
                }
            }
            updateStatement.setInt(1, maxout);
            updateStatement.setInt(2, maxin);
            updateStatement.executeUpdate();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void createLabelIDIndex(int dataset){
        String select = "select id, label from uri_label_id where dataset_local_id = " + dataset;
        try {
            Map<Integer, String>idLabel = new TreeMap<>();
            PreparedStatement selectStatement = connection.prepareStatement(select);
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String label = resultSet.getString("label");
                if (label == null) {
                    idLabel.put(id, "");
                    continue;
                }
                idLabel.put(id, StringUtil.processLabel(label));
            }
            String path = Parameter.directory+"/dataset"+dataset+"/labelIDIndex";
            Directory directory = FSDirectory.open(Paths.get(path));
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StemAnalyzer());
            IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
            for (Integer iter2: idLabel.keySet()){
                Document document = new Document();
                document.add(new StringField("id", String.valueOf(iter2), Field.Store.YES));
                document.add(new TextField("label", idLabel.get(iter2), Field.Store.YES));
                indexWriter.addDocument(document);
            }
            indexWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void createComponentIndex(int dataset){
        /**for each dataset as an RDF graph, compute all its connected components, and store all the words appeared in each component, respectively.
         * the index is used in Evaluations.QryRel()
         * @DATE: 20200615
         * */
        String select = "select subject,predicate,object from triple where dataset_local_id = " + dataset;
        String selectLabel = "select label, id from uri_label_id where dataset_local_id = " + dataset;
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            PreparedStatement selectLabelStatement = connection.prepareStatement(selectLabel);
            ResultSet tripleSet = selectStatement.executeQuery();
            Multigraph<Integer, DefaultEdge> graph = new Multigraph<>(DefaultEdge.class);
            while (tripleSet.next()){
                int subject = tripleSet.getInt("subject");
                int predicate = tripleSet.getInt("predicate");
                int object = tripleSet.getInt("object");
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
            ResultSet resultSet = selectLabelStatement.executeQuery();
            while (resultSet.next()){
                int id = resultSet.getInt("id");
                String label = resultSet.getString("label");
                if (label == null || label.equals(""))continue;
                idLabelMap.put(id, StringUtil.processLabel(label));
            }
            String path = Parameter.directory+"/dataset"+dataset+"/componentIndex";
            IndexWriter indexWriter = new IndexWriter(FSDirectory.open(Paths.get(path)), new IndexWriterConfig(new StemAnalyzer()));
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

    private void createEDPIndex(int dataset){
        /**Fields: id(entity), pattern, count(#instances of pattern)*/
        String getType = "select type_id from dataset_info where dataset_local_id = " + dataset;
        String getLiteral = "select id from uri_label_id where is_literal = 1 and dataset_local_id = " + dataset;
        String select = "select subject, predicate, object from triple where dataset_local_id = " + dataset;
        String update = "update dataset_info set edp_count = ?, distinct_edp = ? where dataset_local_id = " + dataset;
        try {
            PreparedStatement getTypeStatement = connection.prepareStatement(getType);
            PreparedStatement getLiteralStatement = connection.prepareStatement(getLiteral);
            PreparedStatement selectStatement = connection.prepareStatement(select);
            PreparedStatement updateStatement = connection.prepareStatement(update);
            ResultSet resultSet = getTypeStatement.executeQuery();
            resultSet.next();
            int typeID = resultSet.getInt("type_id");
            Set<Integer> Literal = new HashSet<>();
            resultSet = getLiteralStatement.executeQuery();
            while (resultSet.next()){
                Literal.add(resultSet.getInt("id"));
            }
            resultSet = selectStatement.executeQuery();
            Map<Integer, Set<Integer>> id2KP = new HashMap<>();/**entity -> pattern*/
            while (resultSet.next()){
                int sid = resultSet.getInt("subject");
                int pid = resultSet.getInt("predicate");
                int oid = resultSet.getInt("object");
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
                if ((typeID == 0 || pid != typeID) && !Literal.contains(oid)){//object is an entity
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
            }/**finish entity -> pattern Map*/
            int total = id2KP.keySet().size();/**total numbers of patterns/entities*/
            updateStatement.setInt(1, total);
            Map<Set<Integer>, Integer>pattern2Count = new HashMap<>(); /**pattern -> count*/
            for (int iter: id2KP.keySet()){
                Set<Integer> pattern = id2KP.get(iter);
                if (pattern2Count.containsKey(pattern)){
                    int temp = pattern2Count.get(pattern);
                    pattern2Count.put(pattern, temp+1);
                }
                else {
                    pattern2Count.put(pattern, 1);
                }
            }
            updateStatement.setInt(2, pattern2Count.keySet().size());
            updateStatement.executeUpdate();
            /**finish patternMap*/
            String path = Parameter.directory+"/dataset"+dataset+"/EDPIndex";
            Directory directory = FSDirectory.open(Paths.get(path));
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig);
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
                if (indexWriter.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
                    indexWriter.addDocument(document);
                }
            }
            indexWriter.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void createLPIndex(int dataset){
        /**field: triple, frequency, id
         *  triple：the 'link'
         *  frequency：link's relevant frequency in the dataset
         *  id：link's id
         * @DATE: 20200401*/
        String select = "select subject,predicate,object from triple where dataset_local_id = " + dataset;
        String update = "update dataset_info set lp_count = ?, distinct_lp = ? where dataset_local_id = " + dataset;
        Map<Integer, Set<Integer>> ent2pattern = new HashMap<>();//get all entity_id -> EDP from the index
        Map<Set<Integer>, Integer> patternID = new HashMap<>();//EDP -> its id
        try {
            PreparedStatement selectStatement = connection.prepareStatement(select);
            PreparedStatement updateStatement = connection.prepareStatement(update);
            String path = Parameter.directory+"/dataset"+dataset+"/EDPIndex";
            IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(path)));
            if (indexReader.maxDoc() == 0)return;//empty
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
            /**finish Map*/
            Map<ArrayList<Integer>, ArrayList<Integer>>triple2LP  = new HashMap<>();//triple -> EDP1,p,EDP2
            Map<ArrayList<Integer>, Integer>LPcount = new HashMap<>();//EDP1,p,EDP2 -> count
            int LPtotal = 0;
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()){
                int sid = resultSet.getInt("subject");
                int pid = resultSet.getInt("predicate");
                int oid = resultSet.getInt("object");
                if (!ent2pattern.containsKey(oid))continue;
                ArrayList<Integer> tempkey = new ArrayList<>();
                tempkey.add(sid);
                tempkey.add(pid);
                tempkey.add(oid);
                ArrayList<Integer> tempvalue = new ArrayList<>();
                tempvalue.add(patternID.get(ent2pattern.get(sid)));
                tempvalue.add(pid);
                tempvalue.add(patternID.get(ent2pattern.get(oid)));
                triple2LP.put(tempkey, tempvalue);
                int count = LPcount.getOrDefault(tempvalue, 0);
                count++;
                LPcount.put(tempvalue, count);
            }
            Map<ArrayList<Integer>, Integer>LP2id = new HashMap<>();//EDP1,p,EDP2 -> LP's id
            int id = 0;
            for (ArrayList<Integer> iter: LPcount.keySet()){
                LPtotal += LPcount.get(iter);
                id++;
                LP2id.put(iter, id);
            }
            updateStatement.setInt(1, LPtotal);
            updateStatement.setInt(2, LPcount.keySet().size());
            updateStatement.executeUpdate();
            String path2 = Parameter.directory+"/dataset"+dataset+"/LPIndex";
            Directory directory2 = FSDirectory.open(Paths.get(path2));
            IndexWriterConfig indexWriterConfig = new IndexWriterConfig(new StandardAnalyzer());
            indexWriterConfig.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            IndexWriter indexWriter = new IndexWriter(directory2, indexWriterConfig);
            for (ArrayList<Integer> iter: triple2LP.keySet()){
                ArrayList<Integer> LP = triple2LP.get(iter);//EDP1,p,EDP2
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

    public static void main(String[] args){
        PreprocessOfExample test = new PreprocessOfExample();
        test.tacPreprocess(1);
        test.prunedpPreprocess(1);
        test.dualcesPreprocess(1);
        test.recordDatasetInfo(1);
        test.createLabelIDIndex(1);
        test.createComponentIndex(1);
        test.createEDPIndex(1);
        test.createLPIndex(1);
    }
}

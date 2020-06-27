package snippetAlgorithm;

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
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import util.DBUtil;
import util.StemAnalyzer;
import util.StringUtil;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DualCES {
    /**Dual-CES
     * @DATE: 20200506*/
    Connection connection;

    public DualCES(int dataset){
        this.dataset = dataset;
    }

    public DualCES(int dataset, List<String>keywords, Connection connection){
        this.connection = connection;
        this.dataset = dataset;
        this.keywords = keywords;
    }

    public void close(){
        try {
            connection.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**hyper-parameter*/
    public final int N = 1000;//amount of samples in each round，N = 10,000 in the original paper
    private final int EpochNum = 100; //upper bound number of rounds
    private final double Epsilon = 0.01;
    public final double Alpha = 0.7;
    private final int Miu = 1000;
    double percent = 0.01;//前1%

    private int MAX_SIZE = 5; //output snippet size (triple)
    private int ReferenceSize = 6 * MAX_SIZE;//6 times of the output snippet
    private int DocumentSize = 100 * MAX_SIZE;//sentence pruning size
    public final int frequentWordsSize = 100;//get the top 100 words from the reference

    /**variables*/
    private int dataset;
    List<String> keywords;
    Map<String, Integer> queryTF = new HashMap<>();
    private double preGamma1;//iteration bound for the first CE
    private double curGamma1;
    private double preGamma2;//iteration bound for the second CE
    private double curGamma2;
    private int documentLength;
    public int[] tripleID;//compute in sentences pruning
    public String[] document;//compute in sentences pruning
    public int[] reference;
    public double[] selectPolicy;//for document sentences
    Map<String, Integer> documentTfMap;//word -> tf

    public int[] result = new int[MAX_SIZE];

    public long findSnippet(){
        long t0 = System.currentTimeMillis();
        sentencePruning();
//        long t1 = System.currentTimeMillis();
//        for (int iter: tripleID) {
//            System.out.print(iter + " ");
//        }
//        System.out.println();
//        System.out.println("Step 1: " + (t1-t0) + "ms");
        generateReference();
//        long t2 = System.currentTimeMillis();
//        for (int iter: reference){
//            System.out.print(iter + " ");
//        }
//        System.out.println();
//        System.out.println("Step 2: " + (t2-t1) + "ms");
        selectSnippet();
        long t3 = System.currentTimeMillis();
//        for (int iter: result){
//            System.out.print(iter + " ");
//        }
//        System.out.println();
//        System.out.println("Step 3: " + (t3-t2) + "ms");
        return t3 - t0;
    }

    public void createSentenceIndex(){
        DBUtil dbHelper = new DBUtil();
        String sql;
        //-------running example-----------------
        sql = "select label, id from uri_label_id where dataset_local_id=? and label is not null";
        //-------running example-----------------
        PreparedStatement psmt;
        try {
            psmt = dbHelper.conn.prepareStatement(sql);
            psmt.setInt(1, dataset);
            ResultSet rs = psmt.executeQuery();
            Map<Integer, String> idLabelMap = new HashMap<>();
            while(rs.next()){
                String keyword = StringUtil.processLabel(rs.getString(1));
                if(keyword.length() == 0)
                    continue;
                idLabelMap.put(rs.getInt(2), keyword);
            }
            rs.close();
            psmt.close();

            Analyzer analyzer = new StemAnalyzer();
            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            Directory trpDir = FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+dataset+"/trpKwdIndex"));
            IndexWriter trpWriter = new IndexWriter(trpDir, iwc);
            //-------running example-----------------
            sql = "select `subject`, predicate, object, triple_id from triple where dataset_local_id=?";
            //-------running example-----------------
            psmt = dbHelper.conn.prepareStatement(sql);
            psmt.setInt(1, dataset);
            rs = psmt.executeQuery();
            while(rs.next()){
                StringBuilder sentence = new StringBuilder();
                int subject = rs.getInt(1);
                int predicate = rs.getInt(2);
                int object = rs.getInt(3);
                if(idLabelMap.containsKey(subject))
                    sentence.append(idLabelMap.get(subject)+" ");
                if(idLabelMap.containsKey(predicate))
                    sentence.append(idLabelMap.get(predicate)+" ");
                if(idLabelMap.containsKey(object))
                    sentence.append(idLabelMap.get(object));
                String triple = sentence.toString().trim();
                if(triple.length() == 0)
                    continue;
                Document doc = new Document();
                TextField sentenceField = new TextField("sentence", triple, Field.Store.YES);
                TextField idField = new TextField("id", String.valueOf(rs.getInt(4)), Field.Store.YES);
                doc.add(sentenceField);
                doc.add(idField);
                trpWriter.addDocument(doc);
            }
            trpWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createTfIndex(){
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        try {
            Directory tfDir = FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+dataset+"/tfIndex"));
            IndexWriter tfWriter = new IndexWriter(tfDir, iwc);
            Map<String, Integer> wordTfMap = new HashMap<>();
            IndexReader tripleReader = DirectoryReader.open(FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+dataset+"/trpKwdIndex")));
            IndexReader tfReader = DirectoryReader.open(FSDirectory.open(Paths.get(Parameter.directory+"/dataset"+dataset+"/nodeKwdIndex")));
            IndexSearcher tfSearcher = new IndexSearcher(tfReader);
            Analyzer tfAnalyzer = new StemAnalyzer();
            for(int i = 0; i < tripleReader.maxDoc(); i++){
//                if(i % 10000 == 0)
//                    System.out.println(i);
                String[] words = tripleReader.document(i).get("sentence").split("\\s+");
                for(String word : words){
                    if(wordTfMap.containsKey(word))
                        continue;
                    QueryParser parser = new QueryParser("word", tfAnalyzer);
                    Query query = parser.parse(word);
//                    TopDocs dfdocs = dfSearcher.search(query, 1);
//                    ScoreDoc[] dfhits = dfdocs.scoreDocs;
//                    if(dfhits.length == 0)
//                        continue;
                    QueryParser parser2 = new QueryParser("keyword", tfAnalyzer);
                    Query query2 = parser2.parse(word);
                    TopDocs tfdocs = tfSearcher.search(query2, 1000000);
                    ScoreDoc[] tfhits = tfdocs.scoreDocs;
                    int tf = tfhits.length;
//                    int df = Integer.parseInt(dfSearcher.doc(dfhits[0].doc).get("df"));
//			        double idf = Math.log10((docCount + 1) / (df + 1));
//			        System.out.println("[A]"+word+" "+tf+" "+idf);
                    wordTfMap.put(word, tf);
                }
            }
            for(Map.Entry<String, Integer> entry : wordTfMap.entrySet()){
                Document doc = new Document();
                TextField pair = new TextField("word", entry.getKey(), Field.Store.YES);
                int tf = entry.getValue();
                TextField tfField = new TextField("tf", String.valueOf(tf), Field.Store.YES);
                doc.add(pair);
                doc.add(tfField);
                tfWriter.addDocument(doc);
            }
            tfWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void computeDocumentTf(){
        documentTfMap = new HashMap<>();
        for (String iter: document){
            String[] words = iter.split("\\s+");
            documentLength += words.length;
            for (String word: words){
                documentTfMap.put(word, documentTfMap.getOrDefault(word, 0)+1);
            }
        }
    }

    private void sentencePruning(){
        String path = Parameter.directory+"/dataset"+dataset+"/trpKwdIndex";
        int QueryLen = keywords.size();
        queryTF = new HashMap<>();
        for (String iter: keywords){
            queryTF.put(iter, queryTF.getOrDefault(iter, 0)+1);
        }
        try {
            IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(path)));
            if (reader.maxDoc() <= DocumentSize) { //if the dataset itself is smaller than this size, return all sentences
                DocumentSize = reader.maxDoc();
                tripleID = new int[reader.maxDoc()];
                document = new String[reader.maxDoc()];
                for (int i = 0; i < reader.maxDoc(); i++){
                    tripleID[i] = Integer.parseInt(reader.document(i).get("id"));
                    document[i] = reader.document(i).get("sentence");
                }
                return;
            }
            double[] sim = new double[reader.maxDoc()];
            String[] sentences = new String[reader.maxDoc()];
            int[]id = new int[reader.maxDoc()];
            for (int i = 0; i < reader.maxDoc(); i++){
                id[i] = Integer.parseInt(reader.document(i).get("id"));
                sentences[i] = reader.document(i).get("sentence");
                List<String> words = new ArrayList<>(Arrays.asList(sentences[i].split("\\s+")));
                Map<String, Integer>sentenceTF = new HashMap<>();
                for (String iter: words){
                    sentenceTF.put(iter, sentenceTF.getOrDefault(iter, 0)+1);
                }
                for (String iter: keywords){
                    if (sentenceTF.containsKey(iter)){
                        sim[i] += Math.sqrt((double)queryTF.get(iter)/QueryLen * sentenceTF.get(iter)/words.size());
                    }
                }
            }
            reader.close();
            List<Integer>sortedIndex = getSortedIndex(sim);
            tripleID = new int[DocumentSize];
            document = new String[DocumentSize];
            for (int i = 0; i < DocumentSize; i++){
                tripleID[i] = id[sortedIndex.get(i)];
                document[i] = sentences[sortedIndex.get(i)];
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private List<Integer> getSortedIndex(double[] value){
        List<Integer> sortedIndex = new ArrayList<>();
        for (int i = 0; i < value.length; i++){
            sortedIndex.add(i);
        }
        Collections.sort(sortedIndex, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                if (value[o2] > value[o1]) return 1;
                else if(value[o2] < value[o1]) return -1;
                else return 0;
            }
        });
        return sortedIndex;
    }

    private void generateReference(){
        /**first CE, to generate the reference summary*/
        computeDocumentTf();
        double documentVectorLen = 0;
        for(String word : documentTfMap.keySet()){
            int tf = documentTfMap.get(word);
            documentVectorLen += tf * tf;
        }
        documentVectorLen = Math.sqrt(documentVectorLen);
        if (ReferenceSize >= DocumentSize){
            ReferenceSize = DocumentSize;
            reference = new int[ReferenceSize];
            for (int i = 0; i < DocumentSize; i++){
                reference[i] = i;
            }
            return;
        }
        reference = new int[ReferenceSize];
        selectPolicy = new double[DocumentSize];//the whole document
        Arrays.fill(selectPolicy, 0.5);//initiation
        preGamma1 = 0;
        curGamma1 = 1;
        for (int epoch = 1; epoch <= EpochNum; epoch++){
            if((curGamma1 - preGamma1)/preGamma1 < Epsilon && epoch > 1) break; //Convergence
            List<List<Integer>> refSampleSets = new ArrayList<>(N);
            int count = 0;
            while (refSampleSets.size() < N){
                count++;
                List<Integer> singleSample = new ArrayList<>();
                for (int j = 0; j < DocumentSize; j++){
                    double rand = Math.random(); // [0, 1)
                    if(rand <= selectPolicy[j]){
                        singleSample.add(j);
                    }
                }
//                if (singleSample.size() > 0){
//                    refSampleSets.add(singleSample);
//                }
                refSampleSets.add(singleSample);
            }
//            System.out.println("sample end");
            double[] coverage = new double[N];
            double[] length = new double[N];
            double[] asycov = new double[N];
            double[] focus = new double[N];
            double[] total = new double[N];
            for (int i = 0; i < N; i++){
                /**compute the scores of every predictor*/
                List<Integer> singleSample = refSampleSets.get(i);
                if (singleSample.isEmpty()) continue;
                Map<String, Integer> wordSampleTF = new HashMap<>();
                int len = 0;
                for (Integer iter : singleSample) {
                    String[] words = document[iter].split("\\s+");
                    len += words.length;//length
                    for (String word : words) {
                        wordSampleTF.put(word, wordSampleTF.getOrDefault(word, 0) + 1);
                    }
                }
                for (String word : keywords) {//focus
//                    focus[i] += 1.0 * (wordSampleTF.getOrDefault(word, 0) + 0.1) / (len + 0.1);
                    focus[i] += ((double)wordSampleTF.getOrDefault(word, 0))/len;
                }
                double sampleVectorLen = 0;
                double asyPower = 0;
                for (String word : wordSampleTF.keySet()){//coverage, asycov
                    if(!documentTfMap.containsKey(word)) continue;
                    int sampleTf = wordSampleTF.get(word);
                    double documentTf = documentTfMap.get(word);
                    coverage[i] += sampleTf * documentTf;
                    sampleVectorLen += sampleTf * sampleTf;
                    ///////////////////////////////
                    asyPower -= ((double)sampleTf)/len * Math.log((((double)sampleTf)/len)/(documentTf /documentLength));
//                    System.out.println("sampleTF = " + sampleTf);
//                    System.out.println("len = " + len);
//                    System.out.println("documentTF = " + documentTf);
//                    System.out.println("documentLength = " + documentLength);
//                    System.out.println("sampleTF/len = " + 1.0*sampleTf/len);
//                    System.out.println("log value = " + Math.log((1.0 * sampleTf/len)/(1.0*documentTf/documentLength)));
//                    System.out.println("\n\n");
                }
                asycov[i] = Math.exp(asyPower);
                sampleVectorLen = Math.sqrt(sampleVectorLen);
                if(documentVectorLen != 0 && sampleVectorLen != 0)
                    coverage[i] /= documentVectorLen * sampleVectorLen;
                else {
                    coverage[i] = 0.001;
                }
                length[i] = 1.0 * len / singleSample.size();
                total[i] = coverage[i] * length[i] * asycov[i] * focus[i] * 100;
            }
            List<Integer> sortedIndex = getSortedIndex(total);
            int top = (int) (N * percent);
            preGamma1 = curGamma1;
            curGamma1 = total[sortedIndex.get(top)];
            List<List<Integer>> selectSets = new ArrayList<>();
            for(int i = 0; i < top; i++){
                selectSets.add(refSampleSets.get(sortedIndex.get(i)));
            }
            int[] containAmount = new int[DocumentSize + 1];
            for(List<Integer> set : selectSets) {
                for(int tripleId : set){
                    containAmount[tripleId]++;
                }
            }
            for(int i = 0; i < DocumentSize; i++){
                selectPolicy[i] = Alpha * selectPolicy[i] + (1 - Alpha) * containAmount[i] / top;
            }
        }
        List<Integer> sortedRefIndex = getSortedIndex(selectPolicy);
        for (int i = 0; i < ReferenceSize; i++){
            reference[i] = sortedRefIndex.get(i);
        }
    }

    private void selectSnippet(){
        /**第second CE, to generate the result snippet*/
        if (MAX_SIZE >= DocumentSize){
            result = tripleID;
            return;
        }
        Map<String, Integer> wordsCount = new HashMap<>();
        for (int iter: reference){
            String[] words = document[iter].split("\\s+");
            for (String word: words){
                wordsCount.put(word, wordsCount.getOrDefault(word, 0)+1);
            }
        }
        String[] refWords = new String[wordsCount.size()];
        double[] counts = new double[wordsCount.size()];
        int k = 0;
        for (Map.Entry<String, Integer> iter: wordsCount.entrySet()){
            refWords[k] = iter.getKey();
            counts[k]  = (double)iter.getValue();
            k++;
        }
        List<Integer>sortedCountIndex = getSortedIndex(counts);
        Set<String> frequentWords = new HashSet<>();
        if (sortedCountIndex.size() > frequentWordsSize){
            for (int i = 0; i < frequentWordsSize; i++){
                frequentWords.add(refWords[sortedCountIndex.get(i)]);
            }
        }
        else {
            frequentWords.addAll(Arrays.asList(refWords));
        }
        selectPolicy = new double[DocumentSize];//the whole document
        Arrays.fill(selectPolicy, 0.5);
        preGamma2 = 0;
        curGamma2 = 1;
        for (int epoch = 1; epoch <= EpochNum; epoch++){
            if((curGamma2 - preGamma2)/preGamma2 < Epsilon && epoch > 1) break;
//            System.out.println("Epoch "+epoch);
            List<List<Integer>> sampleSets = new ArrayList<>(N);
            int count = 0;
            while (sampleSets.size() < N){
                count++;
                List<Integer> singleSample = new ArrayList<>();
                for (int j = 0; j < DocumentSize; j++){
                    double rand = Math.random(); // [0, 1)
                    if(rand <= selectPolicy[j]){
                        singleSample.add(j);
                    }
                }
//                if (singleSample.size() > 0){
//                    refSampleSets.add(singleSample);
//                }
                sampleSets.add(singleSample);
            }
//            System.out.println("sample end");
            double[] sim1 = new double[N];
            double[] sim2 = new double[N];
            double[] relevance = new double[N];
            double[] discov = new double[N];
            double[] total = new double[N];
            for (int i = 0; i < N; i++){
                List<Integer> singleSample = sampleSets.get(i);
                if (singleSample.isEmpty())continue;
                Set<String> bow = new HashSet<>();
                Map<String, Integer> wordSampleTF = new HashMap<>();
                int sampleLen = 0;
                for (Integer iter : singleSample) {
                    String[] words = document[iter].split("\\s+");
                    sampleLen += words.length;
                    bow.addAll(Arrays.asList(words));
                    for (String word : words) {
                        wordSampleTF.put(word, wordSampleTF.getOrDefault(word, 0) + 1);
                    }
                }
                double queryVectorLen = 0;
                for (String word : keywords) {//sim1, sim2
                    if(!wordSampleTF.containsKey(word)) continue;
                    int keywordTF = queryTF.get(word);
                    int sampleTf = wordSampleTF.get(word);
                    sim1[i] += Math.sqrt(((double) keywordTF)/keywords.size() * sampleTf/sampleLen);
                    sim2[i] += keywordTF * sampleTf;
                    queryVectorLen += keywordTF * keywordTF;
                }
                double sampleVectorLen = 0;
                for (String word : wordSampleTF.keySet()){
                    int sampleTf = wordSampleTF.get(word);
                    sampleVectorLen += sampleTf * sampleTf;
                }
                queryVectorLen = Math.sqrt(queryVectorLen);
                sampleVectorLen = Math.sqrt(sampleVectorLen);
                if(queryVectorLen != 0 && sampleVectorLen != 0)
                    sim2[i] /= queryVectorLen * sampleVectorLen;
                else {
                    sim2[i] = 0.001;
                }
                relevance[i] = Math.sqrt(sim1[i] * sim2[i]);
                for (String word: frequentWords){
                    if (bow.contains(word)){
                        discov[i] += 1;
                    }
                }
                total[i] = relevance[i] * discov[i];
            }
            List<Integer> sortedSampleIndex = getSortedIndex(total);
            int top = (int) (N * percent);
            preGamma2 = curGamma2;
            curGamma2 = total[sortedSampleIndex.get(top)];
            List<List<Integer>> selectSets = new ArrayList<>();
            for(int i = 0; i < top; i++){
                selectSets.add(sampleSets.get(sortedSampleIndex.get(i)));
            }
            int[] containAmount = new int[DocumentSize + 1];
            for(List<Integer> set : selectSets) {
                for(int tripleId : set){
                    containAmount[tripleId]++;
                }
            }
            for(int i = 0; i < DocumentSize; i++){
                selectPolicy[i] = Alpha * selectPolicy[i] + (1 - Alpha) * containAmount[i] / top;
            }
        }
        List<Integer> sortedResultIndex = getSortedIndex(selectPolicy);
        for (int i = 0; i < MAX_SIZE; i++){
            result[i] = tripleID[sortedResultIndex.get(i)];
        }
    }

    public void handleInterrupt(){
        List<Integer> sortedResultIndex = getSortedIndex(selectPolicy);
        for (int i = 0; i < MAX_SIZE; i++){
            int id = tripleID[sortedResultIndex.get(i)];
            if (id != 0) result[i] = id;
        }
    }
}

package scriptProcess.snippetGenerationFile;

import beans.weightedTriple;
import scriptProcess.snippetAlgorithmFile.KSDSnippet;
import util.SnippetUtil;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class KSDSnippetTest {

    public static void generateKSDSnippet(List<String> keywords, String folder){
        KSDSnippet finder = new KSDSnippet();
        Set<weightedTriple> result = finder.findSnippet(keywords, folder);
        Set<Integer> ids = new HashSet<>();
        String tripleStr = "";
        for (weightedTriple iter: result){
            int sid = iter.getSid();
            int oid = iter.getOid();
            int pid = iter.getPid();
            ids.add(sid);
            ids.add(oid);
            tripleStr += sid+" "+oid+" "+pid+",";
        }
        String idstr = "";
        for (int iter: ids){
            idstr += iter+",";
        }
        String snippetStr = idstr.substring(0, idstr.length()-1)+";"+tripleStr.substring(0, tripleStr.length()-1);
        String keyword = keywords.get(0);
        for (int i = 1; i < keywords.size(); i++){
            keyword += " " + keywords.get(i);
        }
        SnippetUtil.showSnippet(folder, snippetStr);
        saveResult(folder, keyword, snippetStr);
    }

    private static void saveResult(String folder, String keyword, String snippet) {
        try {
            PrintWriter writer = new PrintWriter(folder + "/ksd-record.txt");
            writer.println(snippet);
            writer.println(keyword);
            writer.close();
            System.out.println("Snippet record has been stored in " + folder + "/ksd-record.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

//    public static void main(String[] args){
//        /**NOTE: you can change the input keywords in following lines,
//         * BUT BE SURE to use StringUtil.processKeyword() to get the stem of each keyword in advance. */
//        List<String> keywords = new ArrayList<>();
//        keywords.add(StringUtil.processKeyword("London"));
//        keywords.add(StringUtil.processKeyword("Berlin"));
//        keywords.add(StringUtil.processKeyword("Europe"));
//        String baseFolder = "C:/Users/xiaxiawang/Desktop";
//        generateKSDSnippet(keywords, baseFolder);
//    }
}

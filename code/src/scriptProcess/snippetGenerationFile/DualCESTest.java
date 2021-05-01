package scriptProcess.snippetGenerationFile;

import scriptProcess.snippetAlgorithmFile.DualCES;
import util.ReadFile;
import util.SnippetUtil;

import java.io.PrintWriter;
import java.util.*;

public class DualCESTest {

    public static void generateDualCESSnippet(List<String> keywords, String folder){
        try {
            DualCES finder = new DualCES();
            finder.findSnippet(keywords, folder);
            String snippetStr = getResultString(finder.result, folder);
            String keyword = keywords.get(0);
            for (int i = 1; i < keywords.size(); i++){
                keyword += " " + keywords.get(i);
            }
            System.out.println("DualCES result: ");
            SnippetUtil.showSnippet(folder, snippetStr);
            saveResult(folder, keyword, snippetStr);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String getResultString(int[] result, String folder){
        String snippetStr = "";
        Set<Integer> ids = new HashSet<>();
        Map<Integer, List<Integer>> id2triple = new HashMap<>();
        int count = 0;
        for (List<Integer> t: ReadFile.readInteger(folder + "/indexes/triple.txt", "\t")) {
            count++;
            id2triple.put(count, t);
        }
        try {
            for (int iter: result){
                List<Integer> t = id2triple.get(iter);
                int s = t.get(0);
                int p = t.get(1);
                int o = t.get(2);
                ids.add(s);
                ids.add(o);
                snippetStr += s + " " + o + " " + p + ",";
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        String snippetStr0 = "";
        for (int iter: ids){
            snippetStr0 += iter + ",";
        }
        snippetStr0 = snippetStr0.substring(0, snippetStr0.length()-1);
        snippetStr0 += ";" + snippetStr;
        return snippetStr0.substring(0, snippetStr0.length()-1);
    }

    private static void saveResult(String folder, String keyword, String snippet) {
        try {
            PrintWriter writer = new PrintWriter(folder + "/dualces-record.txt");
            writer.println(snippet);
            writer.println(keyword);
            writer.close();
            System.out.println("Snippet record has been stored in " + folder + "/dualces-record.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

//    public static void main(String[] args){
//        /**NOTE: 1, you can change the input keywords in following lines,
//         * BUT BE SURE to use StringUtil.processKeyword() to get the stem of each keyword in advance.
//         * 2, this method includes random sampling, it's POSSIBLE to output different results in different runs*/
//        ArrayList<String> keywords = new ArrayList<>();
//        keywords.add(StringUtil.processKeyword("London"));
//        keywords.add(StringUtil.processKeyword("Berlin"));
//        keywords.add(StringUtil.processKeyword("Europe"));
//        String baseFolder = "C:/Users/xiaxiawang/Desktop";
//        generateDualCESSnippet(keywords, baseFolder);
//    }

}

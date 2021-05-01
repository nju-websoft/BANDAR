package scriptProcess.snippetEvaluationFile;

import beans.triple;
import util.ReadFile;
import util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class IlluSnipEvaluation {

    private static void evaluateIlluSnip(String folder, List<String> keywords, List<String>querywords){
        try {
            File file = new File(folder + "/illusnip-snippet.txt");
            if (!file.exists()) {
                System.out.println("Snippet doesn't exist. (Should be generated first. )");
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String snippetStr = ReadFile.readString(folder + "/illusnip-snippet.txt").get(0);
        if (snippetStr.equals("")){
            System.out.println("Snippet doesn't exist. (Should be generated first. )");
            return;
        }
        Evaluations eva = new Evaluations(folder);
        Set<triple> snippet = eva.formSnippet(snippetStr);
        double skmrep, entrep, descrep, linkrep, kwrel, qryrel;
        skmrep = eva.SkmRep(snippet);
        entrep = eva.EntRep(snippet);
        descrep = eva.DescRep(snippet);
        linkrep = eva.LinkRep(snippet);
        kwrel = eva.KwRel(snippet, keywords);
        qryrel = eva.QryRel(snippet, keywords, querywords);
        System.out.println("SkmRep = " + skmrep);
        System.out.println("EntRep = " + entrep);
        System.out.println("DescRep = " + descrep);
        System.out.println("LinkRep = " + linkrep);
        System.out.println("KwRel = " + kwrel);
        System.out.println("QryRel = " + qryrel);
    }

//    public static void main(String[] args){
//        List<String> keywords = new ArrayList<>();
//        keywords.add(StringUtil.processKeyword("London"));
//        keywords.add(StringUtil.processKeyword("Berlin"));
//        keywords.add(StringUtil.processKeyword("Europe"));
//        /**NOTE: in practice, words in the original query may not be completely contained in the dataset, in this circumstance,
//         * the input keyword is the part of query words contained by the dataset.
//         * In this example, all query words are contained in the dataset, so the keywords are identical to the query words. */
//        String baseFolder = "C:/Users/xiaxiawang/Desktop";
//        evaluateIlluSnip(baseFolder, keywords, keywords);
//    }
}

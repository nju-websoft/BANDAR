package scriptProcess;

import scriptProcess.snippetEvaluationFile.Evaluations;
import scriptProcess.snippetGenerationFile.*;
import util.ReadFile;
import util.SnippetUtil;
import util.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class scriptAgent {

    public static void main(String[] args) {
        String[] param = args[0].split("\\s+");
        if (param[0].equals("-p")) {
            PreprocessOfExample.preprocess(param[1]);
        }
        else if (param[0].equals("-g")) {
            try {
                File file = new File(param[1]);
                String baseFolder = file.getCanonicalPath().replaceAll("\\\\", "/");
                baseFolder = baseFolder.substring(0, baseFolder.lastIndexOf("/"));
                File indexFolder = new File(baseFolder + "/indexes");
                if (!indexFolder.exists()) {
                    System.out.println("Preprocess should be done first. (enter -p \"your dataset file\" )");
                    return;
                }
                if (param[2].equals("-illusnip")) {
                    IlluSnipTest.generateIllustrativeSnippet(baseFolder);
                }
                else if (param[2].equals("-tac")) {
                    if (param.length == 3) {
                        System.out.println("At least one keyword should be entered. ");
                        return;
                    }
                    List<String> keywords = new ArrayList<>();
                    for (String iter: param[3].split(",")) {
                        keywords.add(StringUtil.processKeyword(iter));
                    }
                    TACSnippetTest.generateTACSnippet(keywords, baseFolder);
                }
                else if (param[2].equals("-pruneddp")) {
                    if (param.length == 3) {
                        System.out.println("At least one keyword should be entered. ");
                        return;
                    }
                    List<String> keywords = new ArrayList<>();
                    for (String iter: param[3].split(",")) {
                        keywords.add(StringUtil.processKeyword(iter));
                    }
                    PrunedDPGenerateTest.generatePrunedDPSnippet(keywords, baseFolder);
                }
                else if (param[2].equals("-ksd")) {
                    if (param.length == 3) {
                        System.out.println("At least one keyword should be entered. ");
                        return;
                    }
                    List<String> keywords = new ArrayList<>();
                    for (String iter: param[3].split(",")) {
                        keywords.add(StringUtil.processKeyword(iter));
                    }
                    KSDSnippetTest.generateKSDSnippet(keywords, baseFolder);
                }
                else if (param[2].equals("-dualces")) {
                    if (param.length == 3) {
                        System.out.println("At least one keyword should be entered. ");
                        return;
                    }
                    List<String> keywords = new ArrayList<>();
                    for (String iter: param[3].split(",")) {
                        keywords.add(StringUtil.processKeyword(iter));
                    }
                    DualCESTest.generateDualCESSnippet(keywords, baseFolder);
                }
                else {
                    System.out.println("Paramater error. ");
                    return;
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (param[0].equals("-e")) {
            if (param.length != 5) {
                System.out.println("Paramater error. ");
                return;
            }
            String datasetPath = param[1];
            File file = new File(datasetPath);
            if (!file.exists()) {
                System.out.println("No such dataset file: " + datasetPath);
                return;
            }
            String snippetPath = param[2];
            File file1 = new File(snippetPath);
            if (!file1.exists()) {
                System.out.println("No Snippet file. (Should be generated first. )");
                return;
            }
            String snippetStr = ReadFile.readString(snippetPath).get(0);
            List<String> keywords = new ArrayList<>();
            for (String iter: param[3].split(",")) {
                keywords.add(StringUtil.processKeyword(iter));
            }
            List<String> queryWords = new ArrayList<>();
            for (String iter: param[4].split(",")) {
                queryWords.add(StringUtil.processKeyword(iter));
            }
            try {
                String baseFolder = file.getCanonicalPath().replaceAll("\\\\", "/");
                baseFolder = baseFolder.substring(0, baseFolder.lastIndexOf("/"));
                Evaluations.evaluateSnippet(snippetStr, baseFolder, keywords, queryWords);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (param[0].equals("-t")) {
            String datasetPath = param[1];
            File file = new File(datasetPath);
            if (!file.exists()) {
                System.out.println("No such dataset file: " + datasetPath);
                return;
            }
            String snippetPath = param[2];
            File file1 = new File(snippetPath);
            if (!file1.exists()) {
                System.out.println("No Snippet file: " + snippetPath);
                return;
            }
            String snippetStr = ReadFile.readString(snippetPath).get(0);
            try {
                String baseFolder = file.getCanonicalPath().replaceAll("\\\\", "/");
                baseFolder = baseFolder.substring(0, baseFolder.lastIndexOf("/"));
                SnippetUtil.showSnippet(baseFolder, snippetStr);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            System.out.println("Command error: the first parameter should be one of \"-p\", \"-g\", \"-e\" or \"-t\". ");
        }
    }
}

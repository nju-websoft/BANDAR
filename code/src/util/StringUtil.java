package util;

import org.tartarus.snowball.ext.PorterStemmer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtil {

	public static String processKeyword(String keyword) {
		/**Used on the input keywords, to get the stem from each keyword*/
		keyword = keyword.replaceAll("\"|#", "");
		keyword = keyword.replaceAll("_|-|\\\\|/|\\(|\\)|\\.\\.\\.|:|,|;|\\?|!|\\+|~|&|\\$|%|\\^|@|\\*|=|<|>|\\[|\\]|\\{|\\}|\\|", " ");
		keyword = keyword.replaceAll("u00..", " ").trim();
		String[] labelSplit = keyword.split("\\s+");
		if (labelSplit.length == 0)
			return "";
		Set<String> set = new HashSet<>();
		List<String> list = new ArrayList<>();
		for (String ls : labelSplit) {
			if(Pattern.matches("[a-z]*[[A-Z]+[a-z]*]+", ls)) {
				Pattern pattern = Pattern.compile("[a-z]+|[A-Z]+[a-z]*");
				Matcher matcher = pattern.matcher(ls);
				while(matcher.find())
					list.add(matcher.group());
			} else
				list.add(ls);
		}
		for(String ls : list) {
			PorterStemmer stemmer = new PorterStemmer();
			stemmer.setCurrent(ls.toLowerCase());
			stemmer.stem();
			set.add(stemmer.getCurrent()); //delete duplicate
		}
		StringBuilder sb = new StringBuilder();
		for(String s : set)
			sb.append(s + " ");
		keyword = sb.toString().trim();
		return keyword;
	}

	public static String processLabel(String label){
		/**Used on each label before indexing the dataset triples*/
		String keyword = label.replaceAll("\"|#", "");
		keyword = keyword.replaceAll("_|-|\\\\|/|\\(|\\)|\\.\\.\\.|:|,|;|\\?|!|\\+|~|&|\\$|%|\\^|@|\\*|=|<|>|\\[|\\]|\\{|\\}|\\|", " ");
		keyword = keyword.replaceAll("u00..", " ").trim();
		String[] labelSplit = keyword.split("\\s+");
		if(labelSplit.length == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		List<String> list = new ArrayList<>();
		for (String ls : labelSplit) {
			if(Pattern.matches("[a-z]*[[A-Z]+[a-z]*]+", ls)) {
				Pattern pattern = Pattern.compile("[a-z]+|[A-Z]+[a-z]*");
				Matcher matcher = pattern.matcher(ls);
				while(matcher.find())
					list.add(matcher.group());
			} else
				list.add(ls);
		}
		for(String ls : list) {
			PorterStemmer stemmer = new PorterStemmer();
			stemmer.setCurrent(ls.toLowerCase());
			stemmer.stem();
			sb.append(stemmer.getCurrent()+" ");
		}
		keyword = sb.toString().trim();
		return keyword;
	}
}

package illinoisParser;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;

/**
 * A CCGbankReader is responsible for reading AUTO and PARG files in CCGbank and storing 
 * them as String fields in a Sentence object.
 * 
 * (The CCGbankTrainer class is the one responsible for e.g. turning an AUTO parse string into a coarse 
 * structure of recursively-linked ChartItems) 
 * 
 * @author ramusa2
 *
 */
public class CCGbankReader {


	public static Collection<Sentence> getSentencesToParseFromMultiplePosFiles(String... posFiles) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		for(String posFile : posFiles) {
			sens.addAll(getSentencesToParseFromPosFile(posFile));
		}
		return sens;
	}
	
	public static Collection<Sentence> getSentencesToParseFromPosFile(String posFile) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		File file = new File(posFile);
		try {
			BufferedReader posBR = getFileReader(file);
			String posLine;
			while ((posLine = posBR.readLine().trim()) != null) {	
				if (!posLine.isEmpty()) {
					posLine = processParse(posLine);

					Sentence sen = new Sentence();
					for(String tok : posLine.split("\\s+")) {
						String[] wordPos = tok.split("_");
						String word = wordPos[0];
						String pos = "NULL";
						if(wordPos.length > 1) {
							pos = wordPos[1];
						}
						sen.addLexicalItem(new LexicalToken(word, pos));
					}
					sens.add(sen);
				}
			}
			posBR.close();
		}
		catch(IOException e) {
			// TODO: handle this exception
			e.printStackTrace();
		}
		return sens;
	}

	public static Collection<Sentence> getSentencesFromMultipleAutoFiles(String... autoFiles) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		for(String autoFile : autoFiles) {
			sens.addAll(getSentencesFromAutoFile(autoFile));
		}
		return sens;
	}
	
	public static Collection<Sentence> getSentencesFromAutoFile(String autoFile) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		File file = new File(autoFile);
		try {
			BufferedReader autoBR = getFileReader(file);
			String autoLine;
			while ((autoLine = autoBR.readLine()) != null) {	
				if (!autoLine.startsWith("ID=")
						&& !autoLine.trim().isEmpty()) {
					autoLine = processParse(autoLine);
					Sentence current_sentence = getWord_POS(autoLine.split("[<>]"));
					current_sentence.addCCGbankParse(autoLine);
					sens.add(current_sentence);
				}
			}
			autoBR.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		return sens;
	}
	
	public static Collection<Sentence> getCCGbankData(int lowSec, int highSec, String autoDir) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		for(int s=lowSec; s<=highSec; s++) {
			Util.logln("Reading section "+s);
			sens.addAll(CCGbankReader.readSection(s, autoDir));
		}
		int index = 1;
		for(Sentence sen : sens) {
			sen.setID(index++);
		}
		Util.logln("Read "+sens.size()+" sentences");
		return sens;
	}

	public static Collection<Sentence> getCCGbankData(int[] sections, String autoDir) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		for(int s : sections) {
			Util.logln("Reading section "+s);
			sens.addAll(CCGbankReader.readSection(s, autoDir));
		}
		int index = 1;
		for(Sentence sen : sens) {
			sen.setID(index++);
		}
		Util.logln("Read "+sens.size()+" sentences");
		return sens;
	}
	
	public static Collection<Sentence> getCCGbankDataOmittingSections(int lowSec, int highSec, 
			String autoDir, int... omit) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		for(int s=lowSec; s<=highSec; s++) {
			if(Util.arrayContains(omit, s)) {
				System.out.println("Skipping section "+s);
				continue;
			}
			Util.logln("Reading section "+s);
			sens.addAll(CCGbankReader.readSection(s, autoDir));
		}
		int index = 1;
		for(Sentence sen : sens) {
			sen.setID(index++);
		}
		Util.logln("Read "+sens.size()+" sentences");
		return sens;
	}

	private static Collection<Sentence> readSection(int s, String autoDirPath) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		String autoDir = String.format("%s/%02d/", autoDirPath, s);
		File sect = new File(autoDir);
		File[] autoDocs = sect.listFiles(new FilenameFilter() {
			public boolean accept(File file, String name) {
				return name.matches("wsj_\\d\\d\\d\\d\\.auto\\.gz") || name.contains(".auto");
			}
		});
		Arrays.sort(autoDocs, new Comparator<File>() {
			public int compare(File f1, File f2) {
				return f1.getName().compareTo(f2.getName());
			}
		});
		for(File autoDoc : autoDocs) {
			int autoCount = 0;
			int usIndex = autoDoc.getName().indexOf('_');
			String docID = autoDoc.getName().substring(usIndex+1, usIndex+5);
			try {
				BufferedReader autoBR = getFileReader(autoDoc);
				String autoLine;
				while ((autoLine = autoBR.readLine()) != null) {					
					if (!autoLine.startsWith("ID=")) {
						autoLine = processParse(autoLine);
						Sentence current_sentence = getWord_POS(autoLine.split("[<>]"));
						current_sentence.addCCGbankParse(autoLine);
						sens.add(current_sentence);
					}
					else {
						autoCount = Integer.parseInt(autoLine.split("[\\\\.\\s]")[1]);
					}
				}
				autoBR.close();
			}
			catch(IOException e) {
				e.printStackTrace();
				Util.Error("Failed to open file: "+autoDoc.getAbsolutePath());
			}
		}
		return sens;
	}

	private static BufferedReader getFileReader(File file) throws FileNotFoundException, IOException {
		if(file.getName().endsWith("gz")) {
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
			return new BufferedReader(new InputStreamReader(gzip));
		}
		InputStream reg = new BufferedInputStream(new FileInputStream(file));
		return new BufferedReader(new InputStreamReader(reg));
	}

	private static Sentence getWord_POS(String[] nodes) { 
		Sentence ret = new Sentence();
		for (int k = 1; k < nodes.length; k += 2) {
			String[] params = nodes[k].split(" ");
			boolean isLeaf = params[0].equals("L");
			if (isLeaf) {
				if (params[3].contains("null")) {
					params[3] = "CD";
				}
				String category = params[1];
				
				// TODO: toggle between PTBbank and CCG POS tags
				//String tag = params[3]; // PTB tags
				String tag = params[2]; // CCGbank tags?
				
				
				String word =  params[4];
				if (params[3].equals("CD")) {
					//word = TAGSET.convertNumber(params[4]);
				} else if (params[4].equals("_")) {
					word = "UNDERSCORE";
				}
				LexicalToken lt = new LexicalToken(word, tag, category);
				ret.addLexicalItem(lt);
			}
		}
		return ret;
	}

	private static String processParse(String parse) {
		if (!Configuration.ignorePunctuation) {
			return parse;
		}
		// If we're ignoring punctuation, we need to adjust the parse tree as well
		try {
			return processParseRecurse(parse.trim());
		} catch (Exception e) {
			e.printStackTrace();
			//System.err.println(e.getMessage());
			Util.Error("\nBad data format: " + parse);
			return null;
		}
	}

	private static String processParseRecurse(String old_parse) throws Exception {
		if (old_parse.charAt(0) != '('
				|| old_parse.charAt(old_parse.length() - 1) != ')') {
			throw new Exception("Bad input data.");
		}
		String parse = old_parse.substring(1, old_parse.length() - 1);
		if (parse.startsWith("<L")) {
			if (TAGSET.Punct(new POS(parse.split("\\s+")[3]))) {
				return null;
			}
			return old_parse;
		}
		ArrayList<String> children = new ArrayList<String>();
		String buffer = "";
		char cur;
		int stack = 0;
		boolean inLabel = false;
		for (int i = parse.indexOf('>') + 2; i < parse.length(); i++) {
			cur = parse.charAt(i);
			if (!inLabel) {
				if (cur == '(') {
					stack++;
				}
				if (cur == ')') {
					stack--;
				}
			}
			if (cur == '<') {
				inLabel = true;
			}
			if (cur == '>') {
				inLabel = false;
			}
			buffer += cur;
			if (stack == 0) {
				children.add(processParseRecurse(buffer));
				i++;
				buffer = "";
			}
		}

		String[] params = parse.split("[<>]")[1].split("\\s+");
		String category = params[1];
		int headInt;
		if(params[2].equals("LEFT")) {
			headInt = 0;
		}
		else if (params[2].equals("RIGHT")) {
			headInt = 1;
		}
		else {
			headInt = Integer.valueOf(params[2]);
		}

		int oldNumChildren = Integer.valueOf(params[3]);
		int newNumChildren = 0;
		String childrenString = "";
		for (int i = 0; i < children.size(); i++) {
			String s = children.get(i);
			if (s != null) {
				newNumChildren++;
				childrenString += s + " ";
			}
		}
		childrenString = childrenString.trim();
		if (newNumChildren == 0) {
			return null;
		}
		if (newNumChildren < oldNumChildren) {
			if (newNumChildren != 1) {
				throw new Exception("Not a binary tree!");
			}
			// went from a binary rule to a unary rule
			// check to see if the category of the remaining child matches; if so,
			// just return it
			String rootString = "T " + category + " 0 1";
			return getYoungestCopy(rootString, childrenString);
			// if it doesn't match, then adjust the indices of the parent
			// we'll infer a new unary rule type during training
			// headInt = 0;
		}
		// otherwise, number of kids stayed the same
		String rootString = "<T " + category
				+ " " + headInt
				+ " " + newNumChildren + ">";
		String new_parse = "(" + rootString + " " + childrenString + " )";
		return new_parse;
	}

	private static String getYoungestCopy(String rootLabel, String childrenString) {
		String rootCat = getCategory(rootLabel);
		if (rootCat.equals(getCategory(getRootLabel(childrenString)))) {
			return childrenString;
		}
		String ret = "(<" + rootLabel + "> " + childrenString + " )";
		String candidate = childrenString;
		while ((candidate = getXthGeneration(candidate, 1)) != null) {
			if (rootCat.equals(getCategory(getRootLabel(candidate)))) {
				ret = candidate;
			}
		}
		return ret;
	}

	private static String getXthGeneration(String parse, int genX) {
		if (genX <= 0) {
			return parse;
		}
		// return null if no such unary generation exists
		String[] params = getRootLabel(parse).split("\\s+");
		if (params[0].equals("L") /* if its a leaf */
				|| Integer.parseInt(params[3]) != 1 /* or a binary node */) {
			return null;
		}
		String child = parse.substring(parse.indexOf(">") + 2,
				parse.lastIndexOf(")")).trim();
		return getXthGeneration(child, genX - 1);
	}

	private static String getRootLabel(String parse) {
		return parse.split("[<>]")[1];
	}

	private static String getCategory(String s) {
		return s.split("\\s+")[1];
	}
}

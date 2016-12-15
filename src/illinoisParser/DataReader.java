package illinoisParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;
import java.io.File;

public class DataReader {

	public static Collection<Sentence> readFromFiles(String autoFile, String pargFile) {
		Collection<Sentence> sens = new ArrayList<Sentence>();
		try {
			Scanner parg = new Scanner(new File(pargFile));
			Scanner auto = new Scanner(new File(autoFile));
			String autoLine, pargLine;
			int count = 1;
			while (auto.hasNextLine()) {	
				autoLine = processParse(auto.nextLine());
				Sentence current_sentence = getWord_POS(autoLine.split("[<>]"));
				current_sentence.addCCGbankParse(autoLine);
				current_sentence.setID(count);
				count++;
				sens.add(current_sentence);
			}
			parg.close();
			auto.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			Util.Error("Failed to read sentences from raw Parg/AUTO files");
		}
		return sens;
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
				String tag = params[3];
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
			System.err.println(e.getMessage());
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
		int headInt = Integer.valueOf(params[2]);

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

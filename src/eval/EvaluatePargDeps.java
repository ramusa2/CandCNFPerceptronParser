package eval;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

public class EvaluatePargDeps {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String goldFile = "data/auto/captions.parg";
		//String pargFile = "baseline_sec23_lexcat_max200.parg";
		//String pargFile = "newtopcat_sec23_lexcat_max200.parg";
		//String pargFile = "newconj_sec23_lexcat_max200.parg";
		String pargFile = "data/auto/captions_test_output_from_web40.parg";
		//String pargFile = "data/auto/captions_test_output.parg";
		DepSetReader goldReader = new DepSetReader(new Scanner(new File(goldFile)));
		DepSetReader pargReader = new DepSetReader(new Scanner(new File(pargFile)));

		DepSet gold = null;
		DepSet parg = null;
		double sens = 0.0;
		double parsedSens = 0.0;
		double goldDepsTotal = 0.0;
		double parserDepsTotal = 0.0;
		double matchedUnlabeled = 0.0;
		double matchedLabeled = 0.0;
		boolean prevWasFailure = false;
		while((gold = goldReader.next()) != null) {
			if(!prevWasFailure) {
				parg = pargReader.next();
			}
			if(parg == null || !gold.matches(parg)) {
				/*
				System.out.println("Gold:");
				System.out.println(gold.toString());
				System.out.println("Parser:");
				System.out.println(parg.toString());
				System.out.println("Warning: Gold and Parser sentences don't match.");
				*/
				System.out.println("WARNING: ignoring gold sentence; parse failure?");
				System.out.println("Adding gold deps to count.");
				goldDepsTotal += gold.deps.size();
				prevWasFailure = true;
			}
			else {
				prevWasFailure = false;
			}
			sens++;
			if(parg.deps.size() > 0) {
				goldDepsTotal += gold.deps.size();
				parsedSens++;
				parserDepsTotal+=parg.deps.size();
				matchedUnlabeled += parg.numMatchedUnlabeled(gold);
				matchedLabeled += parg.numMatchedLabeled(gold);
			}
		}

		// Print results
		System.out.println();
		System.out.println("Number of sentences seen: "+sens);
		System.out.println("Number of sentences parsed: "+parsedSens);
		System.out.println("Number of gold deps: "+goldDepsTotal);
		System.out.println("Number of parser deps: "+parserDepsTotal);
		System.out.println("Matched unlabeled deps: "+matchedUnlabeled);
		System.out.println("Matched labeled deps: "+matchedLabeled);
		double unlabeledRecall = 100*matchedUnlabeled/goldDepsTotal;
		double unlabeledPrecision = 100*matchedUnlabeled/parserDepsTotal;
		double unlabeledF1 = (2 * unlabeledPrecision * unlabeledRecall) / (unlabeledPrecision + unlabeledRecall);
		double labeledRecall = 100*matchedLabeled/goldDepsTotal;
		double labeledPrecision = 100*matchedLabeled/parserDepsTotal;
		double labeledF1 = (2 * labeledPrecision * labeledRecall) / (labeledPrecision + labeledRecall);
		System.out.println(format("UR: ", unlabeledRecall));
		System.out.println(format("UP: ", unlabeledPrecision));
		System.out.println(format("UF1: ", unlabeledF1));
		System.out.println(format("LR: ", labeledRecall));
		System.out.println(format("LP: ", labeledPrecision));
		System.out.println(format("LF1: ", labeledF1));
	}

	private static String format(String label, double score) {
		return String.format("%-6s %2.3f", label, score);
	}
}

/**
 * Dep-set, dep-set, dep-set, dep-set..... 
 * @author ramusa2
 *
 */
class DepSetReader {

	Scanner sc;

	DepSetReader(Scanner scanner) {
		sc = scanner;
	}

	DepSet next() {
		DepSet deps = null;
		while(sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			if(line.startsWith("<s>")) {
				deps = new DepSet(Integer.parseInt(line.substring(4)));
			}
			else if(line.startsWith("<\\s>") || line.startsWith("</s>")) {
				break;
			}
			else if(!line.isEmpty()) {
				deps.addDep(new Dep(line));
				String[] toks = line.split("\\s+");
				deps.addWord(Integer.parseInt(toks[0]), toks[4]);
				deps.addWord(Integer.parseInt(toks[1]), toks[5]);
			}
		}
		return deps;
	}

}

class PartialSentence {
	private HashMap<Integer, String> words;

	public PartialSentence() {
		words = new HashMap<Integer, String>();
	}

	public String getWord(Integer index) {
		return words.get(index);
	}

	public void addWord(Integer index, String word) {
		words.put(index, word);
	}

	public boolean matches(PartialSentence s) {
		boolean match = true;
		HashSet<Integer> wordList = new HashSet<Integer>();
		wordList.addAll(words.keySet());
		wordList.addAll(s.words.keySet());
		for(Integer index : wordList) {
			String w = s.words.get(index);
			String w2 = words.get(index);
			if(w != null && w2 != null && !w.contains("X") && !w2.contains("X")) {
				match = match && w2.equals(w);
				/*
				if(!match) {
					System.out.println(w+" "+w2);
				}
				 */
			}
		}
		return match;
	}
}

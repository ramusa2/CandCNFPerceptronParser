package util;

import illinoisParser.CCGbankReader;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.LexicalToken;
import illinoisParser.ParseResult;
import illinoisParser.Sentence;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.Tree;
import illinoisParser.Util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import perceptron.parser.PerceptronParser;
import supertagger.lewissteedman.LSSupertagger;

public class TestSupertaggerHack {
	
	private static int K = 500;

	private static char BETA = '\u03B2';
	
	private static String lexiconFile = "wsj2-21.auto";

	private static double beta = 0.075;

	private static String taggerDir = "tagger";

	private static String parserFile = "final_parser";

	private static String grammarDir = "grammar_candc";

	private static String sentenceFile = "data/eval.auto";

	private static String outputDir = "results";
	private static String outputFilePrefix = "eval";

	public static void main(String[] args) {
		parseArgs(args);
		printArgs();
		try {
			HashMap<String, HashSet<String>> lexicon = getRestrictedLexicon(lexiconFile);
			Collection<Sentence> sentences = CCGbankReader.getSentencesFromAutoFile(sentenceFile);
			Grammar grammar = Grammar.load(new File(grammarDir));
			LSSupertagger tagger = LSSupertagger.load(new File(taggerDir));
			SupervisedParsingConfig c = SupervisedParsingConfig.getDefaultConfig();
			PerceptronParser parser = PerceptronParser.load(grammar, c, parserFile);
			runEvaluation(sentences, tagger, parser, lexicon);
		}
		catch(Exception e) {
			System.err.println("Failed to evaluate parser; aborting.");
			System.out.println("Error:");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void runEvaluation(Collection<Sentence> sentences,
			LSSupertagger tagger, PerceptronParser parser, HashMap<String, HashSet<String>> lexicon) throws IOException {
		File dir = new File(outputDir);
		dir.mkdirs();
		PrintWriter pw = new PrintWriter(new File(outputDir+File.separator+outputFilePrefix)); // output file
		PrintWriter apw = new PrintWriter(new File(outputDir+File.separator+outputFilePrefix+".auto")); // output file
		int maxLength = 100;
		int count = 0;
		for(Sentence sen : sentences) {
			if(count != 0) {
				pw.println();
			}
			pw.println(sen.toString());
			pw.println(sen.getCCGbankParse());
			if(sen.length() <= maxLength) {					
				Tree<? extends FineChartItem> vit = parser.adaptiveSupertaggingHackParse(sen, tagger, lexicon, beta);
				if(vit == null) {
					Util.logln("Failed to build feature structure: "+sen);
					pw.println("PARSE_FAILURE");
					apw.println("PARSE_FAILURE");
				}
				else {
					System.out.println("Score of viterbi parse: "+vit.probability());
					ParseResult pr = new ParseResult(sen, vit, false);
					StringBuilder sb = new StringBuilder();
					Util.buildAUTORecurse(sb, sen, parser.grammar(), pr.getViterbiParse());
					apw.println(sb.toString().trim());
					//tpw.println();
					pw.println(pr.viterbiCCGDependencies());
					System.out.println(sen);
					System.out.println(pr.viterbiCCGDependencies());
				}
			}
			else {
				pw.println("TOO_LONG");
				apw.println("TOO_LONG");
				System.out.println("Skipping too-long sentence: "+sen.asWords());
			}	
			pw.print("###");
			count++;
			if(count % 100 == 0) {
				System.out.println("Parsed "+count+" out of "+sentences.size()+" sentences.");
			}
			count++;
		}
		System.out.println("Finished parsing.");
		pw.close();
		apw.close();
	}

	private static void printArgs() {
		System.out.println("Using arguments: "
				+"\n  tagger="+taggerDir
				+"\n  beta="+beta
				+"\n  K="+K
				+"\n  lexicon="+lexiconFile
				+"\n  parser="+parserFile
				+"\n  grammar="+grammarDir
				+"\n  sentences="+sentenceFile
				+"\n  outputDir="+outputDir
				+"\n  outputFilePrefix="+outputFilePrefix);
	}

	private static void parseArgs(String[] args) {
		try {
			for(String arg : args) {
				try {
					parseArg(arg);
				}
				catch(Exception e) {
					System.err.println("Failed to parse arg: "+arg);					
					throw e;
				}
			}			

		}
		catch(Exception e) {
			System.err.println("Failed to parse arguments; aborting.");
			System.out.println("Accepted arguments are: "
					+"\n  tagger=PATH_TO_SUPERTAGGER_DIRECTORY"
					+"\n  beta=BEAM_WIDTH"
					+"\n  K=FREQUENCY_THRESHOLD_TO_USE_TAG_LEXICON"
					+"\n  lexicon=PATH_TO_AUTO_FILE_THAT_DEFINES_LEXICON"
					+"\n  parser=PATH_TO_PARSER_DIRECTORY"
					+"\n  grammar=PATH_TO_GRAMMAR_DIRECTORY"
					+"\n  sentences=PATH_TO_AUTO_FILE"
					+"\n  outputDir=PATH_TO_OUTPUT_DIR"
					+"\n  outputFilePrefix=OUTPUT_FILENAME_PREFIX");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void parseArg(String arg) {
		String[] arr = arg.split("=");
		String var = arr[0];
		String val = arr[1];
		if(checkArg(var, "model", "parser", "modelDir", "parserDir", "m", "p")) {
			parserFile = val;
		}
		else if(checkArg(var, "grammar", "grammarDir", "g")) {
			grammarDir = val;
		}
		else if(checkArg(var, "s", "sentences", "text", "data", "auto", "sentenceFile", "autoFile")) {
			sentenceFile = val;
		}
		else if(checkArg(var, "t", "tagger", "st", "supertagger", "taggerDir", "supertaggerDir")) {
			taggerDir = val;
		}
		else if(checkArg(var, "o", "output", "out", "outputDir")) {
			outputDir = val;
		}
		else if(checkArg(var, "of", "outputFilePrefix", "outFile", "outputFile")) {
			outputFilePrefix = val;
		}
		else if(checkArg(var, "beta", "b")) {
			beta = Double.parseDouble(val);
		}
		else if(checkArg(var, "K")) {
			K = Integer.parseInt(val);
		}
		else if(checkArg(var, "lexiconFile")) {
			lexiconFile = val;
		}
	}

	private static boolean checkArg(String key, String... matches) {
		for(String m : matches) {
			if(key.trim().equalsIgnoreCase(m.trim())) {
				return true;
			}
		}
		return false;
	}

	private static HashMap<String, HashSet<String>> getRestrictedLexicon(String trainingFile) {
		HashMap<String, Integer> wordCounts = new HashMap<String, Integer>();
		HashMap<String, HashSet<String>> temp = new HashMap<String, HashSet<String>>();
		Collection<Sentence> trainingData = CCGbankReader.getSentencesFromAutoFile(trainingFile);
		// Build lexicon
		for(Sentence sen : trainingData) {
			for(LexicalToken tok : sen.getTokens()) {
				HashSet<String> cats = temp.get(tok.getWord());
				if(cats == null) {
					cats = new HashSet<String>();
					temp.put(tok.getWord(), cats);
				}
				cats.add(tok.getCategory());
			}
		}
		
		HashMap<String, HashSet<String>> lexicon = new HashMap<String, HashSet<String>>();
		for(String w : temp.keySet()) {
			Integer f = wordCounts.get(w);
			if(f != null && f >= K) {
				lexicon.put(w,  temp.get(f));
			}
		}
		return lexicon;
	}

}

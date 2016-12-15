package training;

import illinoisParser.CCGbankReader;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.LexicalToken;
import illinoisParser.Parse;
import illinoisParser.ParseResult;
import illinoisParser.Sentence;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.Tree;
import illinoisParser.Util;
import illinoisParser.models.BaselineModel;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import eval.ConfusionMatrix;
import eval.DepSet;
import eval.PredArgDepResults;

import perceptron.parser.PerceptronParser;

import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;

public class ParserTestHarness {

	private static double beta = 0.1;

	private static String taggerDir = "tagger";

	private static String parserFile = "final_parser";

	private static String grammarDir = "grammar_candc";

	private static String sentenceFile = "data/eval.auto";

	private static String outputDir = "parser_results";
	private static String outputFilePrefix = "eval";

	private static String goldAuto = "data/wsj0.auto";
	private static String vitAuto = "results/iter6/beta0.005/wsj0.parsed.auto";
	//private static String goldAuto = "data/eval.auto";
	//private static String vitAuto = "results/eval.auto";

	public static void main(String[] args) {
		parseArgs(args);
		printArgs();
		try {
			Collection<Sentence> trainingData = CCGbankReader.getCCGbankData(2,  21, "data/CCGbank/AUTO");
			WordCatFreqMap trainingMap = new WordCatFreqMap();
			for(Sentence sen : trainingData) {
				for(LexicalToken tok : sen.getTokens()) {
					trainingMap.add(tok.getWord(), tok.getCategory());
				}
			}
			Grammar grammar = Grammar.load(new File(grammarDir));
			model = new BaselineModel(grammar);
			ArrayList<Parse> gold = getParses(goldAuto);
			//LSSupertagger tagger = LSSupertagger.load(new File(taggerDir));

			//getGoldCategoryStats(gold, tagger, trainingMap);
			int iter = 6;
			double[] betas = new double[]{0.1, 0.05, 0.025, 0.01, 0.005};
			//evaluateIterBeam(gold, "results/iter"+iter, tagger, trainingMap, betas);
			evaluateAdaptiveAllIterParses(gold, betas); 
		}
		catch(Exception e) {
			System.err.println("Failed to evaluate parser; aborting.");
			System.out.println("Error:");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void evaluateAdaptiveAllIterParses(ArrayList<Parse> gold,
			double[] betas) {
		int[] iters = new int[]{1, 2, 3, 4, 5, 6, 7, 8};
		double tokens = 0.0;
		double deps = 0.0;
		double[] alc = new double[iters.length];
		double[] adep = new double[iters.length];
		//double[] olc = new double[iters.length];
		//double[] odep = new double[iters.length];
		for(Parse p : gold) {
			if(p.getSentence().length() <=100) {
				tokens += p.getSentence().length();
				deps += p.getPredArgDeps().size();
			}
		}
		for(int i=0; i<iters.length; i++) {
			System.out.println("Starting processing iter "+iters[i]);
			ArrayList<ArrayList<Parse>> parses = new ArrayList<ArrayList<Parse>>();
			for(double beta : betas) {
				parses.add(getParses("results/iter"+(iters[i])+"/beta"+beta+"/wsj0.parsed.auto"));
			}
			for(int j=0; j<gold.size(); j++) {
				Parse g = gold.get(j);
				DepSet gDeps = g.getPredArgDeps();
				if(g.getSentence().length() <=100) {
					boolean wasParsed = false;
					for(int b=0; b<betas.length; b++) {
						double beta = betas[b];
						Parse p = parses.get(b).get(j);
						if(!p.isFailure()) {
							DepSet pDeps = p.getPredArgDeps();
							adep[i] += pDeps.numMatchedLabeled(gDeps);
							double matchedCats = 0.0;
							Sentence pSen = p.getSentence();
							Sentence gSen = g.getSentence();
							for(int k=0; k<gSen.length(); k++) {
								if(gSen.get(k).getCategory().equals(pSen.get(k).getCategory())) {
									matchedCats++;
								}
							}
							alc[i] += matchedCats;
							break;
						}
					}
				}
			}				
		}
		System.out.println("Iteration,Labeled Dep. Acc.,Lexcat Acc.");
		for(int i=0; i<iters.length; i++) {
			System.out.println(iters[i]+","+(adep[i]/deps)+","+(alc[i]/tokens));
		}
	}

	private static void evaluateIterBeam(ArrayList<Parse> gold, String iterResultDir, LSSupertagger tagger, WordCatFreqMap map, double... betas) {
		HashMap<Double, ArrayList<Parse>> guesses = new HashMap<Double, ArrayList<Parse>>();
		for(double beta : betas) {
			guesses.put(beta, getParses(iterResultDir+File.separator+"beta"+beta+File.separator+"wsj0.parsed.auto"));
		}
		ArrayList<ParserSentenceResult> results = new ArrayList<ParserSentenceResult>();
		for(int i=0; i<gold.size(); i++) {
			Parse g = gold.get(i);
			if(g.getSentence().length() <=100) {
				ParserSentenceResult res = new ParserSentenceResult(g);
				for(double beta : betas) {
					res.addParse(beta, guesses.get(beta).get(i));
				}
				results.add(res);
			}
		}

		double totalTokens = 0.0;
		double totalSens = gold.size();
		double totalFailures = 0;
		double[] failures = new double[betas.length];
		double totalDeps = 0.0;
		double oracleDeps = 0.0;
		double oracleLexcats = 0.0;
		double[] beamDeps = new double[betas.length];
		double[] beamLexcats = new double[betas.length];
		double[] beamUsed = new double[betas.length];
		double adaptDeps = 0.0;
		double adaptLexcats = 0.0;
		for(ParserSentenceResult res : results) {
			totalTokens += res.sentence.length();
			SupertagAssignment tagged = tagger.tagSentence(res.sentence);
			DepSet gDeps =res.gold.getPredArgDeps(); 
			totalDeps += gDeps.size();
			double maxOracleDeps = 0.0;
			double maxOracleLexcats = 0.0;
			boolean wasParsed = false;
			for(int b=0; b<betas.length; b++) {
				double beta = betas[b];
				Parse p = res.parse(beta);
				if(p.isFailure()) {
					failures[b]++;
				}
				else {
					DepSet pDeps = p.getPredArgDeps();
					double matched = pDeps.numMatchedLabeled(gDeps);
					maxOracleDeps = Math.max(maxOracleDeps, matched);
					double matchedCats = 0.0;
					Sentence pSen = p.getSentence();
					Sentence gSen = res.gold.getSentence();
					for(int i=0; i<gSen.length(); i++) {
						if(gSen.get(i).getCategory().equals(pSen.get(i).getCategory())) {
							matchedCats++;
						}
					}
					maxOracleLexcats = Math.max(maxOracleLexcats, matchedCats);
					beamDeps[b] += matched;
					beamLexcats[b] += matchedCats;
					if(!wasParsed) {
						adaptDeps += matched;
						adaptLexcats += matchedCats;
						beamUsed[b]++;
						wasParsed = true;
					}
				}
			}
			if(!wasParsed) {
				totalFailures++;
			}
			oracleDeps += maxOracleDeps;
			oracleLexcats += maxOracleLexcats;		
		}
		System.out.println("Results for files in "+iterResultDir+"\n");	
		System.out.println("                Total tokens: "+totalTokens);
		System.out.println("             Total sentences: "+totalSens+"\n");
		System.out.println("     Adaptive parse failures: "+(totalFailures));	
		System.out.println("       Adaptive dep accuracy: "+adaptDeps+" "+(adaptDeps/totalDeps));		
		System.out.println("    Adaptive lexcat accuracy: "+adaptLexcats+" "+(adaptLexcats/totalTokens)+"\n");
		System.out.println("   Max (oracle) dep accuracy: "+oracleDeps+" "+(oracleDeps/totalDeps));		
		System.out.println("Max (oracle) lexcat accuracy: "+oracleLexcats+" "+(oracleLexcats/totalTokens)+"\n");
		System.out.println("           Beams widths used: ");
		for(int b=0; b<betas.length; b++) {
			System.out.println("                        "+betas[b]+": \t"+beamUsed[b]);
		}
		System.out.println();
		for(int b=0; b<betas.length; b++) {
			double beta = betas[b];
			System.out.println("Results for only beam width "+beta+":");
			System.out.println("        Parse failures: "+(failures[b]));
			System.out.println("       Lexcat accuracy: "+(beamLexcats[b]/totalTokens));
			System.out.println("          Dep accuracy: "+(beamDeps[b]/totalDeps));
		}
	}

	private static void getGoldCategoryStats(ArrayList<Parse> gold,
			LSSupertagger tagger, WordCatFreqMap map) {
		int max = -1;
		double[] betas = new double[]{0.1, 0.05, 0.025, 0.01, 0.005};
		HashMap<Integer, double[]> bins = new HashMap<Integer, double[]>();
		HashMap<Double, HashMap<Integer, double[]>> beamBins = new HashMap<Double, HashMap<Integer, double[]>>();

		for(double beta : betas) {
			beamBins.put(beta, new HashMap<Integer, double[]>());
		}
		for(Parse parse : gold) {
			Sentence sen = parse.getSentence();
			SupertagAssignment tagged = tagger.tagSentence(sen);
			LexicalToken[] toks = sen.getTokens();
			for(int i=0; i<sen.length(); i++) {
				LexicalToken tok = toks[i];
				String w = tok.getWord();
				String c = tok.getCategory();
				int f = map.wordFreq(w);
				//int f = map.catFreq(c);
				max = Math.max(max, f);
				double[] temp = bins.get(f);
				if(temp == null) {
					temp = new double[2];
					bins.put(f, temp);
				}
				if(map.observed(w, c)) {
					temp[0] += 1.0;
				}
				else {
					temp[1] += 1.0;
				}
				LexicalCategoryEntry[] cats = tagged.getAll(i);
				Arrays.sort(cats);
				for(double beta : betas) {
					temp = beamBins.get(beta).get(f);
					if(temp == null) {
						temp = new double[2];
						beamBins.get(beta).put(f, temp);
					}
					double cutoff = cats[0].score()*beta;
					for(LexicalCategoryEntry e : cats) {
						if(e.score() < cutoff) {
							break;
						}
						if(map.observed(w, e.category())) {
							temp[0] += 1.0;
						}
						else {
							temp[1] += 1.0;
						}
					}
				}
			}	
		}
		try {
			PrintWriter pw = new PrintWriter(new File("results/gold_cat_bins.csv"));
			for(int i=0; i<=max; i++) {
				double[] temp = bins.get(i);
				if(temp != null) {
					double pct = temp[0]/(temp[0]+temp[1]);
					pw.println(i+","+pct+","+(temp[0]+temp[1])+","+temp[0]);
				}
			}
			pw.close();
			pw = new PrintWriter(new File("results/beam_cat_bins.csv"));
			for(int i=0; i<=max; i++) {
				if(bins.get(i) != null) {
					pw.print(i);
					double total = (bins.get(i)[0]+bins.get(i)[1]);
					for(double beta : betas) {
						double[] temp = beamBins.get(beta).get(i);
						if(temp != null) {
							double pct = temp[0]/(temp[0]+temp[1]);
							pw.print(","+temp[0]+","+(temp[0]+temp[1]));
						}
					}
					pw.println(","+total);
				}
			}
			pw.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static BaselineModel model;

	private static ArrayList<Parse> getParses(String autoFile) {
		ArrayList<Sentence> sens = (ArrayList<Sentence>) CCGbankReader.getSentencesFromAutoFile(autoFile);
		ArrayList<Parse> parses = new ArrayList<Parse>();
		for(Sentence sen : sens) {
			Parse parse;
			if(sen.getCCGbankParse().equals("TOO_LONG")
					|| sen.getCCGbankParse().equals("PARSE_FAILURE")) {
				parse = new Parse(model); // do nothing
			}
			else {
				parse = new Parse(sen.getCCGbankParse(), model, sen);
			}
			parses.add(parse);
		}
		return parses;
	}


	private static ConfusionMatrix lexcats;
	private static PredArgDepResults deps;

	private static void runEvaluation(ArrayList<Parse> goldParses, ArrayList<Parse> vitParses,
			LSSupertagger tagger, PerceptronParser parser) throws IOException {
		lexcats = new ConfusionMatrix();
		deps = new PredArgDepResults();
		File dir = new File(outputDir);
		dir.mkdirs();
		PrintWriter pw = new PrintWriter(new File(outputDir+File.separator+outputFilePrefix+".summary.txt")); // output file
		int maxLength = 10000;
		int numFailures = 0;
		for(int s=0; s<goldParses.size(); s++) {
			Sentence sen = goldParses.get(s).getSentence();
			if(sen.length() <= maxLength) {
				Parse gold = goldParses.get(s);
				Parse guess = vitParses.get(s);
				if(guess.isFailure()) {
					numFailures++;
					// Lexcats
					for(String lexcat : gold.getLexicalCategories()) {
						lexcats.add(lexcat, "FAILURE");
					}
					// Pred-arg deps
					deps.addResult(gold.getPredArgDeps(), new DepSet(0));
				}
				else {
					// Lexcats
					String[] goldLCs = gold.getLexicalCategories();
					String[] guessLCs = guess.getLexicalCategories();
					for(int i=0; i<goldLCs.length; i++) {
						if(i < guessLCs.length) {
							lexcats.add(goldLCs[i], guessLCs[i]);
						}
						else {
							lexcats.add(goldLCs[i], "FAILURE");
						}
					}
					// Pred-arg deps
					deps.addResult(gold.getPredArgDeps(), guess.getPredArgDeps());
				}
			}
		}
		pw.close();
		System.out.println(deps.getSummaryString());
		System.out.println();
		System.out.println(lexcats.getSummaryString());
		System.out.println("Failures: "+numFailures);
	}

	private static void printArgs() {
		System.out.println("Using arguments: "
				+"\n  beta="+beta
				+"\n  tagger="+taggerDir
				+"\n  parser="+parserFile
				+"\n  grammar="+grammarDir
				+"\n  sentences="+sentenceFile
				+"\n  outputDir="+outputDir
				+"\n  outputFilePrefix="+outputFilePrefix
				+"\n  goldAuto="+goldAuto
				+"\n  vitAuto="+vitAuto);
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
					+"\n  parser=PATH_TO_PARSER_DIRECTORY"
					+"\n  grammar=PATH_TO_GRAMMAR_DIRECTORY"
					+"\n  sentences=PATH_TO_AUTO_FILE"
					+"\n  outputDir=PATH_TO_OUTPUT_DIR"
					+"\n  outputFilePrefix=OUTPUT_FILENAME_PREFIX"
					+"\n  goldAuto=GOLD_AUTO_FILE"
					+"\n  vitAuto=VITERBI_AUTO_FILE");
			e.printStackTrace();
			System.exit(0);
		}
	}

	private static void parseArg(String arg) {
		String[] arr = arg.split("=");
		String var = arr[0];
		String val = arr[1];
		if(checkArg(var, "beta", "b", "beamwidth")) {
			beta = Double.parseDouble(val);
		}
		else if(checkArg(var, "model", "parser", "modelDir", "parserDir", "m", "p")) {
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
		else if(checkArg(var, "ga", "goldAuto")) {
			goldAuto = val;
		}
		else if(checkArg(var, "va", "vitAuto")) {
			vitAuto = val;
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
}

class ParserSentenceResult {
	Sentence sentence;
	Parse gold;
	HashMap<Double, Parse> parses;

	ParserSentenceResult(Parse goldParse) {
		gold = goldParse;
		sentence = gold.getSentence();
		parses = new HashMap<Double, Parse>();
	}

	void addParse(double beta, Parse parse) {
		parses.put(beta, parse);
	}

	Parse parse(double beta) {
		return parses.get(beta);
	}


}

class WordCatFreqMap {

	private HashMap<String, HashMap<String, Integer>> map;
	private HashMap<String, Integer> wordFreqs, catFreqs;;

	WordCatFreqMap() {
		this.map = new HashMap<String, HashMap<String, Integer>>();
		this.wordFreqs = new HashMap<String, Integer>();
		this.catFreqs = new HashMap<String, Integer>();
	}

	void add(String word, String cat) {
		HashMap<String, Integer> temp = this.map.get(word);
		if(temp == null) {
			temp = new HashMap<String, Integer>();
			map.put(word, temp);
		}
		addToMap(temp, cat);
		addToMap(wordFreqs, word);
		addToMap(catFreqs, cat);
	}

	private static void addToMap(HashMap<String, Integer> temp, String key) {
		Integer old = temp.get(key);
		if(old == null) {
			old = 0;
		}
		temp.put(key, old+1);
	}

	int count(String word, String cat) {
		HashMap<String, Integer> temp = this.map.get(word);
		if(temp != null) {
			Integer count = temp.get(cat);
			if(count != null) {
				return count;
			}
		}
		return 0;
	}

	boolean observed(String word, String cat) {
		return this.count(word, cat) > 0;
	}

	int wordFreq(String word) {
		Integer count = this.wordFreqs.get(word);
		if(count != null) {
			return count;
		}
		return 0;
	}

	int catFreq(String cat) {
		Integer count = this.catFreqs.get(cat);
		if(count != null) {
			return count;
		}
		return 0;
	}

	Collection<String> cats() {
		return this.catFreqs.keySet();
	}

	Collection<String> words() {
		return this.wordFreqs.keySet();
	}
}

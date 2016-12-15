package perceptron;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;

import illinoisParser.CCGbankReader;
import illinoisParser.Configuration;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Normal_Form;
import illinoisParser.ParseResult;
import illinoisParser.Sentence;
import illinoisParser.TAGSET;
import illinoisParser.Tree;
import illinoisParser.Util;
import perceptron.parser.PerceptronParser;
import perceptron.parser.training.PPTrainer;

/**
 * RunPerceptronParser is the entry class for training or evaluating a discriminative
 * parser such as the C&C {@link perceptron.parser.ccnormalform.NormalFormPerceptronParser C&C NormalForm}
 * parser.
 * 
 * @author ramusa2
 *
 */
public abstract class RunPerceptronParser {

	/**
	 * Entry point for training or evaluating a perceptron-trained discriminative parser.
	 * 
	 * @param args	the arguments for the current task; different subtasks take different arguments, 
	 * 				but the first two elements must be specified in all cases:
	 * 				0) Target directory for this perceptron parsing task
	 * 				1) Action command specifying the subtask (e.g. makeFolds, processFold, etc.)
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		long startTime = System.nanoTime();  		 	
		setFields(); 
		String directory = args[0];   
		String ACTION = args[1];  		
		if(ACTION.equalsIgnoreCase("makeFolds")) {
			createNewTrainingFolds(directory, args);
		}	
		else if(args[1].equalsIgnoreCase("processAllFolds")) {
			preprocessAllFolds(directory, args);		
		}
		else if(args[1].equalsIgnoreCase("processFold")) {
			preprocessSingleFold(directory, args);
		}
		else if(args[1].equalsIgnoreCase("extractFeatures")) {
			extractFeatureSpace(directory, args);
		}		
		else if(args[1].equalsIgnoreCase("buildForests")) {
			buildPackedFeatureForestsForFold(directory, args);	
		}
		else if(args[1].equalsIgnoreCase("learnWeights")) {
			learnWeights(directory, args);
		}
		else if(args[1].equalsIgnoreCase("debugEvaluate")
				|| args[1].equalsIgnoreCase("evaluate")
				|| args[1].equalsIgnoreCase("test")
				|| args[1].equalsIgnoreCase("parse")) {
			parseUnseenSentences(directory, args);
		} 
		long estimatedTime = System.nanoTime() - startTime;
		System.out.println("Job took "+estimatedTime/Math.pow(10, 9)+" seconds.");
	}

	/**
	 * Specify static parsing fields such as the normal form constraints and the POS tagset.
	 * @throws IOException 
	 */
	private static void setFields() throws IOException {
		Configuration.NF = Normal_Form.Eisner_Orig;
		new TAGSET("data/POS/english.txt"); // necessary for verb distance features
	}


	/**
	 * Divides an AUTO file in a set of folds for training.
	 * 
	 * @param dir	the target directory for the new parsing experiment
	 * @param args	stores a list of arguments:
	 * 				0) Target directory for training
	 * 				1) Action command
	 * 				2) Number of folds
	 * 				3) AUTO file
	 * 				4) Grammar directory
	 */
	protected static void createNewTrainingFolds(String dir, String[] args) {
		System.out.println("Creating "+args[2]+" folds read from ./"+args[3]+", and writing results to ./"+args[0]);
		Grammar grammar;
		if(args.length >= 5) {
			System.out.println("Loading grammar from: ./"+args[4]);
			grammar = Grammar.load(new File(args[4]));
		}
		else {
			System.out.println("Loading grammar from default location: ./grammar/");
			grammar = Grammar.load(new File("grammar"));
		}
		int numFolds = Integer.parseInt(args[2]);
		String filename = args[3];
		Collection<Sentence> sentences = CCGbankReader.getSentencesFromAutoFile(filename);
		PPTrainer trainer = PPTrainer.create(dir, grammar, sentences);
		trainer.createFolds(numFolds);		
	}

	/**
	 * Pre-processes the training data; i.e., performs multitagging and coarsing on each fold,
	 * then extracts the parser's feature space, and finally constructs a packed feature forest
	 * for each training sentence.
	 * 
	 * @param dir	the target directory for the new parsing experiment
	 * @param args	stores a list of arguments:
	 * 				0) Target directory for training
	 * 				1) Action command
	 * 				2) Multitagging beam (beta)
	 * 				3) Feature frequency cutoff
	 */
	protected static void preprocessAllFolds(String dir, String[] args) {
		double beta = Double.parseDouble(args[2]);
		int featureFreqCutoff = Integer.parseInt(args[3]);
		PPTrainer trainer = PPTrainer.loadTrainer(dir);
		trainer.multitag(beta);
		System.out.println("Finished multitagging.");
		trainer.coarseParse();
		System.out.println("Finished coarse parsing.");		
		trainer.getFeatureSpace(featureFreqCutoff);
		System.out.println("Derived model features.");	
		trainer.buildTrainingForests();
		System.out.println("Constructed training forests.");
	}

	/**
	 * Multitags and coarse-parses a single training fold (used for parallel training).
	 * N.B.: Must extract feature space ({@link #extractFeatureSpace}) and create 
	 * packed feature forests ({@link #buildPackedFeatureForests}) before learning weights ({@link #learnWeights}).
	 * 
	 * @param dir	the target directory for the new parsing experiment
	 * @param args	stores a list of arguments:
	 * 				0) Target directory for training
	 * 				1) Action command
	 * 				2) Number of folds
	 * 				3) AUTO file
	 * 				4) Grammar directory
	 */
	protected static void preprocessSingleFold(String dir, String[] args) {
		int foldNum = Integer.parseInt(args[2]);
		double beta = Double.parseDouble(args[3]);
		System.out.println("Processing fold "+foldNum+" with a multitagging beam of "+beta);
		PPTrainer trainer = PPTrainer.loadTrainer(dir);
		trainer.multitagFold(foldNum, beta);
		System.out.println("Finished multitagging.");
		trainer.coarseParseFold(foldNum);	
		System.out.println("Finished coarse parsing.");		
	}
	
	/**
	 * Defines the parser's feature space by applying a frequency cutoff to features 
	 * that are observed in the gold parses.
	 * 
	 * @param dir	the target directory for the new parsing experiment
	 * @param args	stores a list of arguments:
	 * 				0) Target directory for training
	 * 				1) Action command
	 * 				2) Feature frequency cutoff
	 */
	protected static void extractFeatureSpace(String dir, String[] args) {
		int featureFreqCutoff = Integer.parseInt(args[2]);
		System.out.println("Extracting feature space with a frequency cutoff of "+featureFreqCutoff);
		PPTrainer trainer = PPTrainer.loadTrainer(dir);
		trainer.getFeatureSpace(featureFreqCutoff);
		System.out.println("Finished extracting features.");	
	}

	/**
	 * Builds a packed feature forest for each sentence in a specified training fold
	 * 
	 * @param dir	the target directory for the new parsing experiment
	 * @param args	stores a list of arguments:
	 * 				0) Target directory for training
	 * 				1) Action command
	 * 				2) Index of the fold for which to generate packed feature forests
	 */
	protected static void buildPackedFeatureForestsForFold(String directory,
			String[] args) {
		int foldNum = Integer.parseInt(args[2]);
		System.out.println("Constructing packed feature forests for fold "+foldNum);
		PPTrainer trainer = PPTrainer.loadTrainer(directory);
		trainer.buildTrainingForestsForFold(foldNum);
		System.out.println("Constructed training forests for fold "+foldNum+".");	
	}
	
	/**
	 * Uses perceptron updates on packed feature forests to learn the parser weights.
	 * 
	 * @param dir	the target directory for the new parsing experiment
	 * @param args	stores a list of arguments:
	 * 				0) Target directory for training
	 * 				1) Action command
	 */
	protected static void learnWeights(String dir, String[] args) {
		PPTrainer trainer = PPTrainer.loadTrainer(dir);
		System.out.println("Starting to learn weights");
		trainer.trainParser();
		System.out.println("Finished learning weights.");	
	}

	/**
	 * Given an AUTO file containing sentences to parse, parses them and writes the output to file.
	 * 
	 * @param dir	the target directory for the new parsing experiment
	 * @param args	stores a list of arguments:
	 * 				0) Target directory for training
	 * 				1) Action command
	 * 				2) AUTO file containing sentences to parse
	 * 				3) Label (serves as prefix for output files)
	 * 				(4) Optional max sentence length (default = 100) 
	 * @throws IOException 
	 */
	protected static void parseUnseenSentences(String directory, String[] args) throws IOException {
		Collection<Sentence> sentences = CCGbankReader.getSentencesFromAutoFile(args[2]);
		
		PrintWriter pw = new PrintWriter(new File(args[3])); // output file
		PrintWriter apw = new PrintWriter(new File(args[3]+".auto")); // output file
		PrintWriter bpw = new PrintWriter(new File(args[3]+".bracket.txt")); // output file
		PrintWriter tpw = new PrintWriter(new File(args[3]+".tex")); // output file
		createTeXHeader(tpw, false);
		int maxLength = 100;
		if(args.length >= 5) {
			maxLength = Integer.parseInt(args[4]);
		}
		PPTrainer trainer = PPTrainer.loadTrainer(directory);
		PerceptronParser parser = trainer.loadFinalParserForTesting();
		int count = 0;
		for(Sentence sen : sentences) {
			if(count != 0) {
				pw.println();
			}
			pw.println(sen.toString());
			pw.println(sen.getCCGbankParse());
			if(sen.length() <= maxLength) {					
				Tree<? extends FineChartItem> vit = parser.parse(sen, trainer.getSupertagger());
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
		bpw.close();
		closeTeXFile(tpw, false);
		tpw.close();
	}

	/**
	 * Begins the LaTeX document
	 * 
	 * @param writer		pointer to the output file
	 * @param isChinese		true iff language is Chinese
	 * @throws IOException
	 */
	private static void createTeXHeader(PrintWriter writer, boolean isChinese) throws IOException {
		writer.print("\\documentclass[11pt]{beamer}\n");
		writer.print("\\usetheme{default}\n");
		writer.print("\\usepackage{ccg}\n");
		writer.print("\\usepackage[utf8]{inputenc}\n");
		writer.print("\\usepackage[T1]{fontenc}\n");
		if (isChinese) {
			writer.print("\\usepackage{CJK}\n");
			writer.print("\\newcommand{\\chinese}{\\begin{CJK}{UTF8}{gbsn}}\n");
			writer.print("\\newcommand{\\stopchinese}{\\end{CJK}}\n");
		}
		writer.print("\\geometry{top=1mm, bottom=1mm, left=1mm, right=1mm}\n");
		writer.print("\\usepackage{adjustbox}\n");
		writer.print("\\begin{document}\n");
	}

	/**
	 * Ends the LaTeX document
	 * 
	 * @param writer		pointer to the output file
	 * @param isChinese		true iff language is Chinese
	 * @throws IOException
	 */
	private static void closeTeXFile(PrintWriter writer, boolean isChinese) throws IOException {
		writer.print("\\end{document}");
	}
}

package perceptron.parser.training;

import illinoisParser.BackPointer;
import illinoisParser.Binary;
import illinoisParser.Cell;
import illinoisParser.CoarseChartItem;
import illinoisParser.CoarseLexicalCategoryChartItem;
import illinoisParser.FineBackPointer;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Sentence;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.variables.ConditioningVariables;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Map.Entry;

import perceptron.core.representations.parsing.PackedFeatureForest;
import perceptron.parser.PerceptronChart;
import perceptron.parser.PerceptronParser;
import perceptron.parser.SupertaggedSentence;
import perceptron.parser.SupertaggedTrainingData;
import supertagger.CandCSupertaggerWrapper;
import supertagger.lewissteedman.LSSupertagger;
import util.serialization.CompressionType;
import util.serialization.SerializedData;
import util.serialization.SerializedFSTDiskData;

/**
 * The PPTrainer class manages the {@link perceptron.parser.PerceptronParser} training pipeline. In particular, it
 * implicitly defines the file structure of the intermediate data structures and allows for
 * saving and loading trained models. 
 * 
 * @author ramusa2
 *
 */
public class PPTrainer {

	/** Pointer to configuration file (stores parameters) **/
	private static String CONFIG_FILE_NAME = "config";

	/** Pointer to status file (stores position in pipeline) **/
	private static String STATUS_FILE_NAME = "status";

	/** Static/final label for compressed sentences AUTO file **/
	private static String SENTENCES_FILE_NAME = "training_data.auto.gz";

	/** Static/final label for directory that stores grammar files **/
	private static String GRAMMAR_DIR_NAME = "grammar";

	/** Static/final label for directory that stores supertagger files **/
	private static String SUPERTAGGER_DIR_NAME = "supertagger";

	/** Static/final label for directory that stores data/training files **/
	private static String DATA_DIR_NAME = "data";

	/** Static/final label for data sub-directory that stores fold directories **/
	private static String DATA_FOLDS_DIR_NAME = "folds";

	/** Static/final label for directory that contains intermediate models and packed feature forests **/
	private static String TRAINING_DIR_NAME = "training";

	/** Static/final label for training sub-directory that stores intermediate models **/
	private static String INTERMEDIATE_MODEL_DIR_NAME = "intermediate_models";

	/** Static/final label for training sub-directory that stores packed feature forests **/
	private static String PRUNED_FORESTS_DIR_NAME = "training_forests";

	/** Static/final label for directory that stores saved parsing models **/
	private static String MODEL_DIR_NAME = "final_model";

	/** Static/final label for untrained parsing model (defines the feature space)**/
	private static String TRAINING_PARSER_FILE_NAME = "training_parser";

	/** Static/final label for default learned parsing model**/
	private static String FINAL_PARSER_FILE_NAME = "final_parser";

	/** File system pointer to top-level directory **/
	private File directory;

	/** Stores the Grammar object loaded from disk **/
	private Grammar grammar;

	/** Interface with EasyCCG supertagger **/
	private LSSupertagger supertagger;

	/** Stores the pipeline's state **/
	private PPTrainerStatus status;

	/** Stores the training parameters **/
	private PPTrainerConfig config;

	/** Stores a pointer to each training fold **/
	private ArrayList<PPTrainingFold> folds;

	/**
	 * Constructor for loading an existing perceptron parser
	 * (fields are set in factory method)
	 * 
	 * @param dir	top-level directory from which to load parser
	 */
	private PPTrainer(String dir) {
		this.directory = new File(dir);
	}

	/**
	 * Constructor for creating a new perceptron parser trainer
	 * 
	 * @param dir		top-level directory from which to load parser
	 * @param gr		grammar (defines rules, nonterminals, and lexicon for the new parser)
	 * @param sentences	training data
	 */
	private PPTrainer(String dir, Grammar gr, Collection<Sentence> sentences) {
		this.directory = new File(dir);
		this.directory.mkdirs();
		this.grammar = gr;
		this.buildNewFileStructure(sentences);
	}

	/**
	 * Initializes the directory structure for a new parser
	 * 
	 * @param sentences	training data
	 */
	private void buildNewFileStructure(Collection<Sentence> sentences) {
		// Save grammar
		this.grammar.save(createDir(GRAMMAR_DIR_NAME));
		// Set (default) status
		this.status = new PPTrainerStatus();
		this.status.save(createFile(STATUS_FILE_NAME));
		// Set (default) config
		this.config = PPTrainerConfig.getDefaultConfig();
		this.config.save(createFile(CONFIG_FILE_NAME));
		// Create sub-directories
		createDir(SUPERTAGGER_DIR_NAME);
		createDir(MODEL_DIR_NAME);
		createDir(TRAINING_DIR_NAME);
		createDir(DATA_DIR_NAME);
		// Write sentences to .auto.gz file in data directory
		Sentence.writeToGZIPFile(getFile(DATA_DIR_NAME, SENTENCES_FILE_NAME), 
				sentences);
		// Create empty directory for storing training data folds
		createDir(DATA_DIR_NAME, DATA_FOLDS_DIR_NAME);
		// Create empty directory for storing pruned forests for training
		createDir(TRAINING_DIR_NAME, PRUNED_FORESTS_DIR_NAME);
		// Create empty directory for storing intermediate training parsers
		createDir(TRAINING_DIR_NAME, INTERMEDIATE_MODEL_DIR_NAME);
	}

	/**
	 * Concatenates an array of strings to produce a single path (ie, adds separators)
	 * and returns the corresponding File object. Creates any previously non-existent
	 * parent directories along the way.
	 * 
	 * @param filenames		the path to the file to create
	 * @return				the created file
	 */
	private File createFile(String... filenames) {
		File file = getFile(filenames);
		file.getParentFile().mkdirs();
		return file;
	}

	/**
	 * Concatenates an array of strings to produce a single path (ie, adds separators)
	 * and returns the corresponding File object (which will be a directory).
	 * 
	 * @param filenames		the path to the directory to create
	 * @return				the created directory
	 */
	private File createDir(String... filenames) {
		File dir = getFile(filenames);
		dir.mkdirs();
		return dir;
	}

	/**
	 * Concatenates an array of strings to produce a single filename (ie, adds separators)
	 * and returns the corresponding File object.
	 * 
	 * @param filenames		the path to the file to create
	 * @return				the created file
	 */
	private File getFile(String... filenames) {
		String path = this.directory.getPath();
		for(String filename : filenames) {
			path += File.separator + filename;
		}
		return new File(path);
	}

	/**
	 * Loads an existing parser/training pipeline from disk.
	 * 
	 * @param dir	top-level directory for this pipeline
	 * @return		the loaded pipeline trainer
	 */
	public static PPTrainer loadTrainer(String dir) {
		PPTrainer trainer = new PPTrainer(dir);
		try {
		trainer.loadFields();
		}
		catch(Exception e) {
			System.err.println("Exception while loading fields.");
			e.printStackTrace();
		}
		return trainer;
	}
	
	public LSSupertagger getSupertagger() {
		return this.supertagger;
	}

	/**
	 * Initializes fields for a newly-loaded pipeline based on existing files.
	 * @throws Exception 
	 */
	private void loadFields() throws Exception {
		this.status = PPTrainerStatus.load(getFile(STATUS_FILE_NAME));
		this.config = PPTrainerConfig.load(getFile(CONFIG_FILE_NAME));
		this.grammar = Grammar.load(getFile(GRAMMAR_DIR_NAME));

		// TODO: load supertagger from directory, not default location
		this.supertagger = new LSSupertagger(7, 60, 50, getCatList());
		this.supertagger.loadWeights(this.getFile("tagger"));

		this.folds = new ArrayList<PPTrainingFold>();
		if(getFile(DATA_DIR_NAME, DATA_FOLDS_DIR_NAME).exists()) {
			File[] foldDirs = getFile(DATA_DIR_NAME, DATA_FOLDS_DIR_NAME).listFiles();
			Arrays.sort(foldDirs);
			for(File foldDir : foldDirs) {
				this.folds.add(new PPTrainingFold(foldDir));
			}
		}
	}

	private static ArrayList<String> getCatList() throws Exception {
		Scanner sc = new Scanner(new File("categories"));
		ArrayList<String> cats = new ArrayList<String>();
		while(sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			if(!line.isEmpty()) {
				cats.add(line);
			}
		}
		sc.close();
		return cats;
	}

	/**
	 * Creates a new training pipeline.
	 * 
	 * @param dir		top-level directory
	 * @param gr		grammar
	 * @param sentences	training data
	 * @return
	 */
	public static PPTrainer create(String dir, Grammar gr, Collection<Sentence> sentences) {
		(new File(dir)).mkdirs();
		PPTrainer trainer = new PPTrainer(dir, gr, sentences);
		// TODO: set default fields
		return trainer;
	}

	/**
	 * Splits the training data into the specified number of folds
	 * 
	 * @param numFolds	number of folds to create
	 */
	public void createFolds(int numFolds) {
		Collection<Sentence> sentences = Sentence.readFromGZIPFile(getFile(DATA_DIR_NAME, SENTENCES_FILE_NAME));
		this.folds = new ArrayList<PPTrainingFold>();
		File foldsDir = this.getFile(DATA_DIR_NAME, DATA_FOLDS_DIR_NAME);
		// Erase any existing folds
		for(File child : foldsDir.listFiles()) {
			child.delete();
		}
		int sensTotal = sentences.size();
		int sensPerFold = sensTotal/numFolds;
		int remFolds = sensTotal % numFolds;
		int processed = 0;
		int curFold = 0;
		Iterator<Sentence> iter = sentences.iterator();
		while(processed < sensTotal) {
			int curSens = 0;
			int toProcess = (curFold < remFolds) ? sensPerFold+1 : sensPerFold;
			String foldName = "fold_"+curFold;
			File foldDir = createDir(DATA_DIR_NAME, DATA_FOLDS_DIR_NAME, foldName);
			ArrayList<Sentence> foldSens = new ArrayList<Sentence>();
			while(curSens < toProcess) {
				foldSens.add(iter.next());
				curSens++;
				processed++;
			}
			PPTrainingFold fold = new PPTrainingFold(foldDir);
			fold.setSentences(foldSens);
			this.folds.add(fold);
			curFold++;
		}
	}

	/**
	 * Multitags sentences in all folds in the training data.
	 * @param beta	multitagger beam width
	 */
	public void multitag(double beta) {
		for(int f=0; f<this.folds.size(); f++) {
			this.multitagFold(f, beta);
			System.out.println("Multitagged fold "+f+".");
		}
	}

	/**
	 * Coarse-parses sentences in all folds in the training data.
	 */
	public void coarseParse() {
		for(int f=0; f<this.folds.size(); f++) {
			this.coarseParseFold(f);
			System.out.println("Coarse parsed fold "+f+".");
		}
	}

	@Deprecated
	public void extractFeaturesUsingOldCutoff() {
		// Note: may be able to use this code for Dependency model feature extraction
		for(int f=0; f<this.folds.size(); f++) {
			this.extractFeaturesForFold(f);
			System.out.println("Extracted features for fold "+f+".");
		}
	}

	/**
	 * Runs perceptron training to learn the parsing model's feature weights.
	 */
	public void trainParser() {
		int numIterations = this.config.getNumTrainingIterations();		
		int maxLength = this.config.getMaxSentenceLength();
		PerceptronParser finalParser = this.loadFinalParserForTraining();		
		SerializedData<PackedFeatureForest> finalForests = this.loadFinalForests();
		finalParser.trainOnPackedForests(finalForests, numIterations, maxLength);
		finalParser.save(FINAL_PARSER_FILE_NAME);
	}

	/**
	 * Loads the parsing model for training.
	 * 
	 * @return	a PerceptronParser object with the correct feature space (no weights yet)
	 */
	private PerceptronParser loadFinalParserForTraining() {
		return PerceptronParser.loadIntermediateParser(this.getFinalModelDir(), 
				TRAINING_PARSER_FILE_NAME, true, this.grammar);
	}

	/**
	 * Loads the packed feature forests for training from disk.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private SerializedData<PackedFeatureForest> loadFinalForests() {
		return (SerializedData<PackedFeatureForest>) SerializedFSTDiskData.useExisting(this.getPrunedForestsDir(), CompressionType.LZ4);
	}

	/**
	 * Loads an intermediate parsing model
	 * @param iteration	specifies which iteration of training to load the parser from
	 * @return	the loaded intermediate model
	 */
	public PerceptronParser loadIterationParser(int iteration) {
		// TODO: don't load features with value 0.0
		Grammar g = this.grammar;
		SupervisedParsingConfig c = SupervisedParsingConfig.getDefaultConfig();
		String filename = "iter_"+iteration+"_saved_parser.txt";
		return PerceptronParser.load(g, c, this.getFinalModelDir(), filename, true);
	}

	/**
	 * Loads the final parsing model
	 * @return	a PerceptronParser object using the final weights from training
	 */
	public PerceptronParser loadFinalParserForTesting() {
		// TODO: don't load features with value 0.0
		Grammar g = this.grammar;
		SupervisedParsingConfig c = SupervisedParsingConfig.getDefaultConfig();
		return PerceptronParser.load(g, c, this.getFinalModelDir(), FINAL_PARSER_FILE_NAME, true);
	}

	/**
	 * Multitag sentences in the specified fold using the specified beam.
	 * 
	 * @param foldNum 	index of fold to multitag
	 * @param beta		multitagger beam
	 */
	public void multitagFold(int foldNum, double beta) {
		this.folds.get(foldNum).multitag(this.supertagger, beta);
	}

	/**
	 * Coarse-parse sentences in the specified fold.
	 * 
	 * @param foldNum	index of fold to coarse-parse
	 */
	public void coarseParseFold(int foldNum) {
		this.folds.get(foldNum).coarseParse(this.grammar);
	}

	@Deprecated
	public void extractFeaturesForFold(int foldNum) {
		SupervisedParsingConfig config = SupervisedParsingConfig.getDefaultConfig();
		PerceptronParser newParser = this.config.getNewParser(this.grammar, config);
		this.folds.get(foldNum).extractFeatures(newParser);
	}

	/**
	 * Returns the path to the directory containing the saved parsing models.
	 * 
	 * @return	model directory path
	 */
	private String getFinalModelDirPath() {
		return this.directory.getPath() + File.separator + MODEL_DIR_NAME; 
	}

	/**
	 * Return a File object pointing to the saved model directory.
	 * 
	 * @return	model directory
	 */
	private File getFinalModelDir() {
		return new File(this.getFinalModelDirPath());
	}

	/**
	 * Return a File object pointing to the directory containing the packed 
	 * feature forests for training.
	 * 
	 * @return	packed feature forests directory
	 */
	private File getPrunedForestsDir() {
		return new File(this.directory.getPath() + File.separator + TRAINING_DIR_NAME
				+ File.separator + PRUNED_FORESTS_DIR_NAME);
	}

	/**
	 * Debug method for processing all folds.
	 * 
	 * @param beta			multitagger beam
	 * @param freqCutoff	feature frequency cutoff
	 */
	public void debug(double beta, int freqCutoff) {
		File debugDir = new File(this.directory+File.separator+"debug_output");
		debugDir.mkdirs();
		for(int f=0; f<this.folds.size(); f++) {
			this.debugFold(debugDir, f, beta, freqCutoff);
			System.out.println("Debugged fold "+f+".");
		}
	}


	/**
	 * Debug method for processing a single fold.
	 * 
	 * @param debugDir		output directory
	 * @param foldNum		index of fold to debug
	 * @param beta			multitagger beam
	 * @param freqCutoff	feature frequency cutoff
	 */
	private void debugFold(File debugDir, int foldNum, double beta, int freqCutoff) {
		PrintWriter summary = null;
		try {
			File dir = new File(debugDir.getPath()+File.separator+"fold_"+foldNum);
			dir.mkdir();
			summary = new PrintWriter(new File(dir.getPath()+File.separator+"summary.txt"));
			PPTrainingFold fold = this.folds.get(foldNum);
			fold.multitag(this.supertagger, beta);
			SupertaggedTrainingData stSens = fold.getSupertaggedSentences();
			writeDebugMultitaggingSummary(summary, stSens, dir.getPath()+File.separator+"supertagging_summary.txt");
			fold.coarseParse(this.grammar);
			SerializedData<PerceptronChart> coarseForests = fold.getCoarseForests();
			writeCoarseParsingSummary(summary, coarseForests, dir.getPath()+File.separator+"coarseparse_summary.txt");

			PerceptronParser newParser = this.config.getNewParser(this.grammar, SupervisedParsingConfig.getDefaultConfig());
			fold.extractFeatures(newParser);
			writeFineParsingSummary(newParser, summary, coarseForests, dir.getPath()+File.separator+"fineparse_summary.txt");
			summary.close();
		}
		catch(Exception e) {
			System.out.println("Error while debugging fold "+foldNum);
			e.printStackTrace();
			if(summary != null) {
				summary.close();
			}
		}
	}

	/**
	 * Debug helper method -- writes summary of final fine parsing.
	 * 
	 * @throws FileNotFoundException
	 */
	private void writeFineParsingSummary(PerceptronParser parser, PrintWriter summary,
			SerializedData<PerceptronChart> coarseForests, String fpFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(fpFile));
		pw.println("Fine parsing summary:");
		summary.println("Fine parsing summary:");
		PerceptronChart chart = null;
		while((chart = coarseForests.next()) != null) {
			chart.fineParseWithPerceptronModel(parser, false);
			summary.println("\n---\n");
			pw.println("\n---\n");
			Sentence sen = chart.getSentence();
			int numCoarseItems = 0;
			int numFineItems = 0;
			int numFineBP = 0;
			int numCoarseBP = 0;
			for(int s=0; s<sen.length(); s++) {
				for(int i=0; i+s<sen.length(); i++) {
					int x=i;
					int y=i+s;
					Cell cell = chart.getCoarseCell(x, y);
					numCoarseItems += cell.values().size();
					for(CoarseChartItem ci : cell.values()) {
						numCoarseBP += ci.children.size();
						numFineItems += ci.fineItems().size();
						for(FineChartItem fci :  ci.fineItems()) {
							numFineBP += fci.children().size();
						}
					}
				}
			}
			pw.println("Sentence:\n\t"+sen.asWords()+"\n");
			pw.println("Total coarse items:\n\t"+numCoarseItems+"\n");
			pw.println("Total coarse backpointers:\n\t"+numCoarseBP+"\n");
			pw.println("Total fine items:\n\t"+numFineItems+"\n");
			pw.println("Total fine backpointers:\n\t"+numFineBP+"\n");

			pw.println("Chart:");
			summary.println("Sentence:\n\t"+sen.asWords()+"\n");
			summary.println("Total coarse items:\n\t"+numCoarseItems+"\n");
			summary.println("Total coarse backpointers:\n\t"+numCoarseBP+"\n");
			summary.println("Total fine items:\n\t"+numFineItems+"\n");
			summary.println("Total fine backpointers:\n\t"+numFineBP+"\n");

			summary.println("Chart:");
			for(int s=0; s<sen.length(); s++) {
				for(int i=0; i+s<sen.length(); i++) {
					int x=i;
					int y=i+s;
					Cell cell = chart.getCoarseCell(x, y);
					numCoarseItems += cell.values().size();
					int fineItems = 0;
					int numFBP = 0;
					for(CoarseChartItem ci : cell.values()) {
						fineItems += ci.fineItems().size();
						for(FineChartItem fci :  ci.fineItems()) {
							numFBP += fci.children().size();
						}
					}
					summary.println("\n\t"+span(cell)+": "+sen.asWords(x, y)+"  ("+cell.values().size()+" coarse, "+fineItems+" fine items)");
					pw.println("\n\t"+span(cell)+": "+sen.asWords(x, y)+"  ("+cell.values().size()+" coarse items, "+fineItems+" fine items, "+numFBP+" fine bp)");
					for(CoarseChartItem ci : cell.values()) {
						if(ci instanceof CoarseLexicalCategoryChartItem) {
							pw.println("\t\tCoarse: "+ci.toString(grammar)+" (lexical)");
							summary.println("\t\tCoarse: "+ci.toString(grammar)+" (lexical)");
							for(FineChartItem fci : ci.fineItems()) {
								pw.println("\t\t\tFine: "+fci.toString(grammar, "\t\t\t\t")+" (lexical)\n");
								summary.println("\t\t\tFine: "+fci.toString(grammar, "\t\t\t\t")+" (lexical)\n");
							}
						}
					}
					for(CoarseChartItem ci : cell.values()) {
						if(!(ci instanceof CoarseLexicalCategoryChartItem)) {
							Collection<FineChartItem> fineCI =  ci.fineItems();
							pw.println("\t\tCoarse: "+ci.toString(grammar)+" ("+fineCI.size()+" fine):");
							summary.println("\t\tCoarse: "+ci.toString(grammar)+" ("+fineCI.size()+" fine):");
							for(FineChartItem fci : fineCI) {
								pw.println("\t\t\tFine: \t"+fci.toString(grammar,"\t\t\t\t")+" ("+fci.children().size()+" bp):\n");
								summary.println("\t\t\tFine: \t"+fci.toString(grammar, "\t\t\t\t")+" ("+fci.children().size()+" bp):\n");
								for(FineBackPointer bp : fci.children()) {
									String rule = grammar.prettyRule(bp.rule());
									pw.println("\t\t\t\t\t"+rule+"\n");
									pw.println("\t\t\t\t\t\t"+bp.B().toString(grammar, "\t\t\t\t\t")+" "+span(bp.B().coarseItem().cell)+"\n");
									summary.println("\t\t\t\t\t"+rule+"\n");
									summary.println("\t\t\t\t\t\t"+bp.B().toString(grammar, "\t\t\t\t\t")+" "+span(bp.B().coarseItem().cell)+"\n");
									if(bp.rule() instanceof Binary) {
										pw.println("\t\t\t\t\t\t"+bp.C().toString(grammar, "\t\t\t\t\t")+" "+span(bp.C().coarseItem().cell)+"\n");
										summary.println("\t\t\t\t\t\t"+bp.C().toString(grammar, "\t\t\t\t\t")+" "+span(bp.C().coarseItem().cell)+"\n");
									}
								}
							}
							pw.println();
							summary.println();
						}
					}
				}
			}
		}
		pw.close();
	}

	/**
	 * Debug helper method -- writes summary of final coarse parsing.
	 * 
	 * @throws FileNotFoundException
	 */
	private void writeCoarseParsingSummary(PrintWriter summary,
			SerializedData<PerceptronChart> coarseForests, String cpFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(cpFile));
		pw.println("Coarse parsing summary:");
		summary.println("Coarse parsing summary:");
		PerceptronChart chart = null;
		while((chart = coarseForests.next()) != null) {
			summary.println("\n---\n");
			pw.println("\n---\n");
			Sentence sen = chart.getSentence();
			int numCoarseItems = 0;
			for(int s=0; s<sen.length(); s++) {
				for(int i=0; i+s<sen.length(); i++) {
					int x=i;
					int y=i+s;
					Cell cell = chart.getCoarseCell(x, y);
					numCoarseItems += cell.values().size();
				}
			}
			pw.println("Sentence:\n\t"+sen.asWords()+"\n");
			pw.println("Total coarse items:\n\t"+numCoarseItems+"\n");
			pw.println("Chart:");
			summary.println("Sentence:\n\t"+sen.asWords()+"\n");
			summary.println("Total coarse items:\n\t"+numCoarseItems+"\n");
			summary.println("Chart:");
			for(int s=0; s<sen.length(); s++) {
				for(int i=0; i+s<sen.length(); i++) {
					int x=i;
					int y=i+s;
					Cell cell = chart.getCoarseCell(x, y);
					numCoarseItems += cell.values().size();
					summary.println("\n\t"+span(cell)+": "+sen.asWords(x, y)+"  ("+cell.values().size()+" items)");
					pw.println("\n\t"+span(cell)+": "+sen.asWords(x, y)+"  ("+cell.values().size()+" items)");
					for(CoarseChartItem ci : cell.values()) {
						if(ci instanceof CoarseLexicalCategoryChartItem) {
							pw.println("\t\t"+ci.toString(grammar)+" (lexical)\n");
							summary.println("\t\t"+ci.toString(grammar)+" (lexical)\n");
						}
					}
					for(CoarseChartItem ci : cell.values()) {
						if(!(ci instanceof CoarseLexicalCategoryChartItem)) {
							pw.println("\t\t"+ci.toString(grammar)+" ("+ci.children.size()+" bp):");
							summary.println("\t\t"+ci.toString(grammar)+" ("+ci.children.size()+" bp):");
							for(BackPointer bp : ci.children) {
								String rule = grammar.prettyRule(bp.r);
								pw.println("\t\t\t"+rule);
								pw.println("\t\t\t\t"+bp.B().toString(grammar)+" "+span(bp.B().cell));
								summary.println("\t\t\t"+rule);
								summary.println("\t\t\t\t"+bp.B().toString(grammar)+" "+span(bp.B().cell));
								if(bp.r instanceof Binary) {
									pw.println("\t\t\t\t"+bp.C().toString(grammar)+" "+span(bp.C().cell));
									summary.println("\t\t\t\t"+bp.C().toString(grammar)+" "+span(bp.C().cell));
								}
							}
							pw.println();
							summary.println();
						}
					}
				}
			}
		}
		pw.close();
		coarseForests.reset();
	}
	
	/**
	 * Returns a pretty string indicating the span of a cell
	 */
	private String span(Cell cell) {
		return "["+cell.X()+", "+cell.Y()+"]";
	}

	/**
	 * Debug helper method -- writes summary of final multitagging.
	 * 
	 * @throws FileNotFoundException
	 */
	private void writeDebugMultitaggingSummary(PrintWriter summary,
			SupertaggedTrainingData stSens, String stFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(new File(stFile));
		int totalWords = 0;
		int totalTags = 0;
		for(SupertaggedSentence sen : stSens.getData()) {
			int words = sen.sentence().length();
			int tags = 0;
			for(int i=0; i<sen.sentence().length(); i++) {
				tags += sen.tags().getAll(i).length;
			}
			pw.println(sen.sentence().asWords());
			pw.println("  Words: "+words);
			pw.println("  Tags:  "+tags);
			pw.println("  Avg.:  "+((tags+0.0)/words));
			pw.println("---");
			totalWords += words;
			totalTags += tags;
		}

		summary.println("Supertagging summary:");
		summary.println("  # sentences:     "+stSens.size());
		summary.println("  Total words:     "+totalWords);
		summary.println("  Total tags:      "+totalTags);
		summary.println("  Avg. tags/word:  "+((totalTags+0.0)/totalWords));
		summary.println();

		pw.println("# sentences:     "+stSens.size());
		pw.println("Total words:     "+totalWords);
		pw.println("Total tags:      "+totalTags);
		pw.println("Avg. tags/word:  "+((totalTags+0.0)/totalWords));

		pw.close();
	}

	/**
	 * Defines the feature space that the model should use; a feature is included
	 * only if it appears at least k times in the gold parses
	 * for the training data.
	 * 
	 * @param featureFreqCutoff 	feature frequency cutoff (k)
	 */
	public void getFeatureSpace(int featureFreqCutoff) {
		HashMap<ConditioningVariables, Integer> featureFreqs = 
				new HashMap<ConditioningVariables, Integer>();

		SupervisedParsingConfig config = SupervisedParsingConfig.getDefaultConfig();
		PerceptronParser newParser = this.config.getNewParser(this.grammar, config);
		for(PPTrainingFold fold : this.folds) {
			SerializedData<PerceptronChart> coarseForests = fold.getCoarseForests();
			PerceptronChart chart;
			while((chart = coarseForests.next()) != null) {
				PackedFeatureForest goldDerivation = new PackedFeatureForest(chart, newParser, true);
				HashSet<Integer> activeFeatures = goldDerivation.getActiveFeatures();
				for(Integer f : activeFeatures) {
					ConditioningVariables vars = newParser.getFeature(f);
					Integer oldFreq = featureFreqs.get(vars);
					if(oldFreq == null) {
						oldFreq = 0;
					}
					featureFreqs.put(vars, oldFreq+1);
				}
			}
			coarseForests.reset();
		}
		PerceptronParser finalParser = this.config.getNewParser(this.grammar, null);
		for(Entry<ConditioningVariables, Integer> entry : featureFreqs.entrySet()) {
			if(entry.getValue() >= featureFreqCutoff) {
				int index = newParser.getFeatureIndex(entry.getKey());
				finalParser.addFeature(entry.getKey(), index);
			}
		}
		System.out.println("Final feature space has "+finalParser.features().size()+" features.");
		finalParser.initializeWeightVector();
		finalParser.setSaveDirectory(this.getFinalModelDirPath());
		finalParser.save(TRAINING_PARSER_FILE_NAME);
	}

	/**
	 * Creates the packed feature forests for all folds in the training data.
	 */
	public void buildTrainingForests() {
		for(int f=0; f<this.folds.size(); f++) {
			this.buildTrainingForestsForFold(f);
		}
	}

	/**
	 * Creates the packed feature forests for sentences in a particular fold.
	 * 
	 * @param foldNum	index of that fold
	 */
	public void buildTrainingForestsForFold(int foldNum) {
		@SuppressWarnings("unchecked")
		SerializedFSTDiskData<PackedFeatureForest> finalForests = 
				(SerializedFSTDiskData<PackedFeatureForest>) SerializedFSTDiskData.createNew(this.getPrunedForestsDir(), 
				CompressionType.LZ4, "fold_"+foldNum+"_");

		PerceptronParser parser = 
				PerceptronParser.loadIntermediateParser(this.getFinalModelDir(), 
						TRAINING_PARSER_FILE_NAME, true, this.grammar);
		this.folds.get(foldNum).buildTrainingForests(parser, finalForests);
	}

}

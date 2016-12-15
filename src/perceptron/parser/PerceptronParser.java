package perceptron.parser;

import illinoisParser.AutoDecoder;
import illinoisParser.BackPointer;
import illinoisParser.Chart;
import illinoisParser.CoarseChartItem;
import illinoisParser.CoarseLexicalCategoryChartItem;
import illinoisParser.FineBackPointer;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Sentence;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.Tree;
import illinoisParser.variables.ConditioningVariables;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import perceptron.core.representations.parsing.FeatureTree;
import perceptron.core.representations.parsing.FeatureTreeBackPointer;
import perceptron.core.representations.parsing.PackedFeatureForest;
import perceptron.parser.ccnormalform.NormalFormChartItem;
import perceptron.parser.ccnormalform.NormalFormPerceptronParser;
import perceptron.parser.training.PPFeatureExtractor;

import eval.DepSet;

import supertagger.CandCSupertaggerWrapper;
import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;
import util.serialization.SerializedData;

/**
 * The abstract PerceptronParser class defines methods for CKY parsing during training and
 * evaluation that are shared by the discriminative parsing models that extend this class. 
 * 
 * @author ramusa2
 *
 */
public abstract class PerceptronParser {
	
	/** Every subclass must set this label (used to identify model type in save file)**/
	protected final String NAME;

	/** Default beta values used for multitagging **/
	private static final double[] betas = new double[]{0.1, 0.05, 0.025, 0.01, 0.005};
	
	/** Default learning rate (shouldn't affect training without regularization) **/
	private static final double LR = 1.0;

	/** The grammar used by this parser **/
	protected Grammar grammar;

	/** Configuration storing the values for this parser's learning parameters **/
	private SupervisedParsingConfig parserConfig;

	/** Maps feature descriptions to the corresponding index in the feature vector **/
	private HashMap<ConditioningVariables, Integer> featureIndices;
	
	/** Stores feature descriptions at the index corresponding to the weight vector **/
	private ArrayList<ConditioningVariables> featureList;

	/** Stores the model's feature weights **/
	private double[] weights;
	
	/** Stores the accumulated weights used in training the averaged perceptron **/
	private double[] accumulatedWeights;
	
	/** Stores the number of examples seen during training **/
	private double count;

	/** Indicates the directory this model would be saved to **/
	private String saveDir;

	/**
	 * Constructor called by subclasses
	 * 
	 * @param g						the grammar used by the parser
	 * @param coarseParsingConfig	the configuration storing coarse parsing parameters
	 * @param parserName			a label identifying the type of model (set of features)
	 */
	protected PerceptronParser(Grammar g, SupervisedParsingConfig coarseParsingConfig, String parserName) {
		this.NAME = parserName;
		this.grammar = g;
		this.parserConfig = coarseParsingConfig;
		this.featureIndices = new HashMap<ConditioningVariables, Integer>();
		this.featureList = new ArrayList<ConditioningVariables>();
		this.saveDir = "";
		this.weights = null; // not set until we know how many (and which) features to use
	}

	/**
	 * Given a sentence, uses adaptive supertagging to find a set of lexical categories that
	 * produces at least one complete parse (if one exists), then returns the Viterbi parse 
	 * tree according to the current feature weights.
	 * 
	 * @param sentence		the sentence to parse
	 * @param supertagger	EasyCCG supertagger
	 * @return				a Tree object representing the Viterbi parse
	 */
	public Tree<? extends FineChartItem> parse(Sentence sentence,
			LSSupertagger supertagger) {
		// Coarse parse with adaptive supertagging
		PerceptronChart chart = null;
		try {
			for(int b=0; b<betas.length; b++) {
				chart = new PerceptronChart(sentence, grammar);
				SupertagAssignment tags = supertagger.tagSentence(sentence, betas[b]);	
				chart.coarseParseWithSupertags(grammar, parserConfig, tags, false);
				if(chart.successfulCoarseParse()) {
					break;
				}
			}
			if(chart != null && chart.successfulCoarseParse()) {
				chart.fineParseWithPerceptronModel(this, true);
				if(chart.successfulFineParse()) {
					return chart.getViterbiParse();
				}
				else {
					System.out.println("Unsuccessful fine parse");
				}
			}
			else {
				System.out.println("Unsuccessful coarse parse");
			}
		}catch (Exception e) {
			System.out.println("Failed to parse sentence: "+sentence);
			e.printStackTrace();
		}
		return null;
	}
	

	public Tree<? extends FineChartItem> parse(Sentence sentence,
			LSSupertagger supertagger, double beta) {
		// Coarse parse with adaptive supertagging
		PerceptronChart chart = null;
		try {

			chart = new PerceptronChart(sentence, grammar);
			SupertagAssignment tags = supertagger.tagSentence(sentence, beta);	
			chart.coarseParseWithSupertags(grammar, parserConfig, tags, false);
			if(chart.successfulCoarseParse()) {
				if(chart != null && chart.successfulCoarseParse()) {
					chart.fineParseWithPerceptronModel(this, true);
					if(chart.successfulFineParse()) {
						return chart.getViterbiParse();
					}
					else {
						System.out.println("Unsuccessful fine parse");
					}
				}
				else {
					System.out.println("Unsuccessful coarse parse");
				}
			}
			
			for(int b=0; b<betas.length; b++) {
				if(betas[b] >= beta) {
					continue;
				}
				chart = new PerceptronChart(sentence, grammar);
				tags = supertagger.tagSentence(sentence, betas[b]);	
				chart.coarseParseWithSupertags(grammar, parserConfig, tags, false);
				if(chart.successfulCoarseParse()) {
					break;
				}
			}
			if(chart != null && chart.successfulCoarseParse()) {
				chart.fineParseWithPerceptronModel(this, true);
				if(chart.successfulFineParse()) {
					return chart.getViterbiParse();
				}
				else {
					System.out.println("Unsuccessful fine parse");
				}
			}
			else {
				System.out.println("Unsuccessful coarse parse");
			}
		}catch (Exception e) {
			System.out.println("Failed to parse sentence: "+sentence);
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Given a sentence, uses adaptive supertagging with C&C supertagger to find a set of 
	 * lexical categories that
	 * produces at least one complete parse (if one exists), then returns the Viterbi parse 
	 * tree according to the current feature weights.
	 * 
	 * @param sentence	the sentence to parse
	 * @return			a Tree object representing the Viterbi parse
	 */
	public Tree<? extends FineChartItem> parse(Sentence sentence) {
		// Coarse parse with adaptive supertagging
		PerceptronChart chart = null;
		try {
			for(int b=0; b<betas.length; b++) {
				chart = new PerceptronChart(sentence, grammar);
				SupertagAssignment tags = CandCSupertaggerWrapper.multi(sentence, betas[b]);
				chart.coarseParseWithSupertags(grammar, parserConfig, tags, false);
				if(chart.successfulCoarseParse()) {
					break;
				}
			}
			if(chart != null && chart.successfulCoarseParse()) {
				chart.fineParseWithPerceptronModel(this, true);
				if(chart.successfulFineParse()) {
					return chart.getViterbiParse();
				}
				else {
					System.out.println("Unsuccessful fine parse");
				}
			}
			else {
				System.out.println("Unsuccessful coarse parse");
			}
		}catch (Exception e) {
			System.out.println("Failed to parse sentence: "+sentence);
			e.printStackTrace();
		}
		return null;
	}

	public Tree<? extends FineChartItem> parse(SupertagAssignment tagged) {
		// Coarse parse with adaptive supertagging
		Sentence sentence = tagged.sentence();
		PerceptronChart chart = null;
		try {
			for(int b=0; b<betas.length; b++) {
				chart = new PerceptronChart(sentence, grammar);
				SupertagAssignment tags = filterTags(tagged, betas[b]);	
				chart.coarseParseWithSupertags(grammar, parserConfig, tags, false);
				if(chart.successfulCoarseParse()) {
					break;
				}
			}
			if(chart != null && chart.successfulCoarseParse()) {
				chart.fineParseWithPerceptronModel(this, true);
				if(chart.successfulFineParse()) {
					return chart.getViterbiParse();
				}
				else {
					System.out.println("Unsuccessful fine parse");
				}
			}
			else {
				System.out.println("Unsuccessful coarse parse");
			}
		}catch (Exception e) {
			System.out.println("Failed to parse sentence: "+sentence);
			e.printStackTrace();
		}
		return null;
	}

	private SupertagAssignment filterTags(SupertagAssignment full, double beta) {
		SupertagAssignment tags = new SupertagAssignment(full.sentence());
		for(int i=0; i<full.sentence().length(); i++) {
			double cutoff = full.getBest(i).score()*beta;;
			for(LexicalCategoryEntry entry : full.getAll(i)) {
				if(entry.score() >= cutoff) {
					tags.addLexcat(i, entry);
				}
				else {
					break;
				}
			}
		}
		return tags;
	}

	/**
	 * Given a sentence,  returns the Viterbi parse tree according to the current feature weights.
	 * Uses all lexical categories licensed by the grammar.
	 * 
	 * @param sentence	the sentence to parse
	 * @return			a Tree object representing the Viterbi parse
	 */
	public Tree<? extends FineChartItem> parseWithoutST(Sentence sentence) {
		// Coarse parse with adaptive supertagging
		PerceptronChart chart = null;
		try {
			chart = new PerceptronChart(sentence, grammar);
			chart.coarseParse(grammar, parserConfig);
			if(chart != null && chart.successfulCoarseParse()) {
				chart.fineParseWithPerceptronModel(this, true);
				if(chart.successfulFineParse()) {
					return chart.getViterbiParse();
				}
				else {
					System.out.println("Unsuccessful fine parse");
				}
			}
			else {
				System.out.println("Unsuccessful coarse parse: "+chart.getSentence());
			}
		}catch (Exception e) {
			System.out.println("Failed to parse sentence: "+sentence);
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Saves the model to disk after averaging the weight vector.
	 * 
	 * @param filename	target output file
	 * @param c			the number of seen examples
	 */
	private void saveAveragedWeights(String filename, double c) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(this.saveDir+File.separator+filename));
			pw.println(this.NAME);
			pw.println(this.weights == null ? -1 : this.weights.length);
			for(ConditioningVariables f : this.featureIndices.keySet()) {
				Integer featureIndex = this.featureIndices.get(f);
				if(this.accumulatedWeights != null) {
					double weight = this.weights[featureIndex] -( this.accumulatedWeights[featureIndex] / c);
					pw.println(featureIndex+"  "+weight+"  "+f);
				}
				else {
					pw.println(featureIndex+"  "+0.0+"  "+f);
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save parser (file not found exception)");
			e.printStackTrace();
		}
		if(pw != null) {
			pw.close();
		}
	}

	/**
	 * Uses perceptron updates to learn the model's feature weights. Saves the final
	 * model to disk; also saves a copy of the model after each training iteration to disk.
	 * Both averaged and unaveraged models are saved.
	 * 
	 * @param data				serialized packed feature forests forests (training data)
	 * @param numIterations		the number of iterations through the data
	 * @param maxLength			length of longest sentence to use in training
	 */
	public void trainOnPackedForests(SerializedData<PackedFeatureForest> data,
			int numIterations, int maxLength) {
		this.accumulatedWeights = new double[this.weights.length];
		this.count = 0.0;
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(this.saveDir+File.separator+"update_indices.txt"));
		}
		catch(Exception e) {
			System.out.println("Failed to open update index log.");
		}
		for(int T=0; T<numIterations; T++) {
			long startTime = System.nanoTime();    
			int j=0;
			PackedFeatureForest packed = null;
			while((packed = data.next()) != null) {
				this.count++;
				boolean updated = this.update(packed, this.count);
				if(updated) {
					pw.println(this.count);
				}
				j++;
				if(j%100 == 0) {
					System.out.println("Iteration "+(T+1)+": finished "+j+" sentences out of "+data.size());
				}
			}
			System.out.println("Finished iteration "+(T+1));
			this.save("iter_"+(T+1)+"_saved_parser.txt");
			this.saveAveragedWeights("iter_"+(T+1)+"_saved_parser.averaged.txt", count);

			long estimatedTime = System.nanoTime() - startTime;
			System.out.println("Iteration took "+estimatedTime/Math.pow(10, 9)+" seconds.");
			System.out.println("Saved perceptron model");
			data.reset();
		}
		pw.close();
		data.close();
	}

	/**
	 * Uses the current feature weights to obtain a Viterbi parse, then checks 
	 * whether that parse is equal to the gold parse. If not, upweight features
	 * that are active in the gold parses and downweight features active in the
	 * Viterbi parse.
	 * 
	 * @param forest	packed feature forests for this training example
	 * @param c			number of examples seen so far
	 * @return			true iff the weight vector was changed
	 */
	private boolean update(PackedFeatureForest forest, double c) {	
		FeatureTree viterbi = forest.getViterbiTree(this);
		if(viterbi == null) {
			return false;
		}
		FeatureTree gold = forest.getGoldTree();
		if(!gold.matches(viterbi)) {
			this.updateWeightsFromFeatureTree(viterbi, -LR, c);
			this.updateWeightsFromFeatureTree(gold, LR, c);
			return true;
		}
		return false;
	}

	/**
	 * Recurses down a parse tree and increments (or decrements) the
	 * weight for every active feature; if a feature occurs multiple times in the tree,
	 * its weight will be adjusted multiple times. 
	 * 
	 * @param tree		parse tree
	 * @param delta		value to change the weight be (increment if delta is positive, decrement if negative)
	 * @param c			number of examples seen so far
	 */
	private void updateWeightsFromFeatureTree(FeatureTree tree, double delta, double c) {
		this.updateWeights(tree.features(), delta, c);
		if(!tree.isLeaf()) {
			FeatureTreeBackPointer bp = tree.backpointer();
			this.updateWeights(bp.features(), delta, c);
			this.updateWeightsFromFeatureTree(bp.leftChild(), delta, c);
			if(!bp.isUnary()) {
				this.updateWeightsFromFeatureTree(bp.rightChild(), delta, c);
			}
		}
	}

	/**
	 * Helper method for changing weights.
	 * 
	 * @param feats		list of features to change
	 * @param delta		value to change weight by
	 * @param c			number of examples seen
	 */
	private final void updateWeights(int[] feats, double delta, double c) {
		for(int f : feats) {
			this.weights[f] += delta;
			this.accumulatedWeights[f] += delta*c;
		}
	}

	/**
	 * Returns the sum of the weights for a collection of features (allows repeats).
	 * 
	 * @param feats		list of features to score
	 * @return			the score of a list of features
	 */
	public double score(ArrayList<ConditioningVariables> feats) {
		double sc = 0.0;
		for(ConditioningVariables f : feats) {
			sc += this.weight(f);
		}
		return sc;
	}

	/**
	 * Returns the current weight for a single feature.
	 * 
	 * @param feature	the description of the feature to look up
	 * @return			that feature's current weight
	 */
	public double weight(ConditioningVariables feature) {
		Integer featureIndex = this.featureIndices.get(feature);
		if(featureIndex == null) {
			return 0.0;
		}
		return this.weight(featureIndex);
	}

	/**
	 * Returns the current weight for a single feature specified by its index.
	 * 
	 * @param featureIndex	the index of the feature to look up
	 * @return				the weight of the feature 
	 */
	public double weight(int featureIndex) {
		if(this.weights == null
				|| featureIndex >= this.weights.length) {
			return 0.0;
		}
		return this.weights[featureIndex];
	}

	/**
	 * Scores a lexical chart item's features and sets the item's Viterbi 
	 * score to be that value (a lexical chart item has no children).
	 * 
	 * @param fineLexicalCI		the chart item to score
	 */
	public void setScoreOfLexicalItem(FineChartItem fineLexicalCI) {
		fineLexicalCI.setViterbiProb(this.score(
				getLexicalFeatures((NormalFormChartItem)fineLexicalCI)), null);
	}

	/**
	 * Returns a list of descriptions for the features active at a lexical chart item.
	 * 
	 * @param fineLexicalCI		the lexical chart item
	 * @return					a list of active features
	 */
	protected abstract ArrayList<ConditioningVariables> getLexicalFeatures(NormalFormChartItem fineLexicalCI);

	/**
	 * Scores a unary parent chart item's features and returns the sum 
	 * (in logspace) of that value and the child's Viterbi score.
	 * 
	 * @param fineParentCI	the parent chart item
	 * @param fineChildCI	the child chart item
	 * @return		the score of the parent
	 */
	public double getScoreOfUnaryChartItem(FineChartItem fineParentCI,
			FineChartItem fineChildCI) {
		return this.score(
				this.getUnaryFeatures((NormalFormChartItem) fineParentCI, 
						(NormalFormChartItem) fineChildCI))
						+ fineChildCI.getViterbiProb();
	}

	/**
	 * Returns a list of descriptions for the features active at a unary parent chart item.
	 * 
	 * @param fineParentCI	the parent chart item
	 * @param fineChildCI	the child chart item
	 */
	protected abstract ArrayList<ConditioningVariables> getUnaryFeatures(NormalFormChartItem fineParentCI,
			NormalFormChartItem fineChildCI);

	/**
	 * Scores a binary parent chart item's features and returns the sum 
	 * (in logspace) of that value and the children's Viterbi scores.
	 * 
	 * @param parentCI	parent chart item
	 * @param fineBP	backpointer to the children
	 * @return			the score of the parent
	 */
	public double getScoreOfBinaryChartItem(FineChartItem parentCI,
			FineBackPointer fineBP) {
		return this.score(
				this.getBinaryFeatures((NormalFormChartItem) parentCI, 
						fineBP))
						+ fineBP.B().getViterbiProb()
						+ fineBP.C().getViterbiProb();
	}


	/**
	 * Returns a list of descriptions for the features active at a binary parent chart item.
	 * 
	 * @param fineParentCI	the parent chart item
	 * @param fineBP		backpointer to the children
	 * @return
	 */
	protected abstract ArrayList<ConditioningVariables> getBinaryFeatures(
			NormalFormChartItem fineParentCI, FineBackPointer fineBP);

	/**
	 * 
	 * @param ci
	 * @param chart
	 * @return
	 */
	public abstract FineChartItem getFineLexicalChartItem(
			CoarseLexicalCategoryChartItem ci, Chart chart);

	public abstract FineChartItem getFineUnaryChartItem(CoarseChartItem ci,
	FineChartItem fineChildCI);

	public abstract NormalFormChartItem getFineBinaryChartItem(CoarseChartItem ci,
			BackPointer bp, FineChartItem fineLeftChildCI,
			FineChartItem fineRightChildCI);

	public abstract FineChartItem getFineRootChartItem(CoarseChartItem coarseRoot);

	public ArrayList<ConditioningVariables> collateFeatures(NormalFormChartItem item) {
		FineBackPointer bp = item.getViterbiBP();
		if(bp == null) {
			return getLexicalFeatures(item);
		}
		else if(bp.isUnary()) {
			NormalFormChartItem child = (NormalFormChartItem) bp.B();
			ArrayList<ConditioningVariables> feats = collateFeatures(child);
			feats.addAll(getUnaryFeatures(item, child));
			return feats;
		}
		else {
			NormalFormChartItem leftChild = (NormalFormChartItem) bp.B();
			NormalFormChartItem rightChild = (NormalFormChartItem) bp.C();
			ArrayList<ConditioningVariables> feats = collateFeatures(leftChild);
			feats.addAll(collateFeatures(rightChild));
			feats.addAll(getBinaryFeatures(item, bp));
			return feats;
		}
	}	

	public Grammar grammar() {
		return grammar;
	}

	public NormalFormChartItem scoreAutoParse(Sentence sen) {
		AutoDecoder auto = new AutoDecoder(sen, sen.getCCGbankParse());
		Chart chart = auto.getFineChart(this);
		NormalFormChartItem root = (NormalFormChartItem) chart.fineRoot();
		return root;
	}

	public DepSet readDepsFromAuto(Sentence sen) {
		AutoDecoder auto = new AutoDecoder(sen, sen.getCCGbankParse());
		Chart chart = auto.getFineChart(this);
		Tree<? extends FineChartItem> tree = chart.getViterbiParse();
		return DepSet.getDepSetFromPargEntry(tree.buildPargString(sen));
	}

	public void save(String filename) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(this.saveDir+File.separator+filename));
			pw.println(this.NAME);
			pw.println(this.weights == null ? -1 : this.weights.length);
			for(ConditioningVariables f : this.featureIndices.keySet()) {
				Integer featureIndex = this.featureIndices.get(f);
				if(this.weights != null) {
					double weight = this.weights[featureIndex];
					pw.println(featureIndex+"  "+weight+"  "+f);
				}
				else {
					pw.println(featureIndex+"  "+0.0+"  "+f);
				}
			}
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save parser (file not found exception)");
			e.printStackTrace();
		}
		if(pw != null) {
			pw.close();
		}
		if(this.accumulatedWeights != null && count != 0.0) {
			String avgFilename = filename+".averaged";
			this.saveAveragedWeights(avgFilename, count);
		}
	}

	public static PerceptronParser load(Grammar g, SupervisedParsingConfig coarseParsingConfig, 
			File directory, String filename, boolean cacheVariables) {
		Scanner sc = null;
		try {
			sc = new Scanner(new File(directory.getPath()+File.separator+filename));
			String parserName = sc.nextLine();
			PerceptronParser parser;
			if(parserName.equals(NormalFormPerceptronParser.NF_NAME)) {
				parser = new NormalFormPerceptronParser(g, coarseParsingConfig);
			}
			else {
				System.err.println("Unrecognized parser name "+parserName+
						"; failed to instantiate a new parser");
				if(sc != null) {
					sc.close();
				}
				return null;
			}
			parser.setSaveDirectory(directory.getPath());
			int numFeatures = Integer.parseInt(sc.nextLine().trim());
			if(numFeatures >= 0) {
				parser.weights = new double[numFeatures];
				parser.featureList = new ArrayList<ConditioningVariables>(numFeatures);
			}
			String line;
			String[] toks;
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					if(parser.weights == null) {
						if(cacheVariables) {
							parser.addFeature(ConditioningVariables.loadFromString(line, cacheVariables));
						}
					}
					else {
						toks = line.split("\\s+");
						int index = Integer.parseInt(toks[0]);
						double w = Double.parseDouble(toks[1]);
						String cond = line.substring((index+"  "+w).length()).trim();
						if(cacheVariables) {
							parser.addFeature(
									ConditioningVariables.loadFromString(cleanVariableString(cond), cacheVariables), index);
						}
						parser.weights[index] = w;
					}
				}
			}
			if(sc != null) {
				sc.close();
			}
			return parser;	
		} catch (Exception e) {
			System.out.println("Failed to load parser (possible malformed parser file).");
			e.printStackTrace();
		}
		if(sc != null) {
			sc.close();
		}
		return null;
	}
	


	public static PerceptronParser load(Grammar g, SupervisedParsingConfig coarseParsingConfig, 
			String weightFileName) {
		Scanner sc = null;
		try {
			sc = new Scanner(new File(weightFileName));
			String parserName = sc.nextLine();
			PerceptronParser parser;
			if(parserName.equals(NormalFormPerceptronParser.NF_NAME)) {
				parser = new NormalFormPerceptronParser(g, coarseParsingConfig);
			}
			else {
				System.err.println("Unrecognized parser name "+parserName+
						"; failed to instantiate a new parser");
				if(sc != null) {
					sc.close();
				}
				return null;
			}
			int numFeatures = Integer.parseInt(sc.nextLine().trim());
			if(numFeatures >= 0) {
				parser.weights = new double[numFeatures];
				parser.featureList = new ArrayList<ConditioningVariables>(numFeatures);
			}
			String line;
			String[] toks;
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					if(parser.weights != null) {
						toks = line.split("\\s+");
						int index = Integer.parseInt(toks[0]);
						double w = Double.parseDouble(toks[1]);
						String cond = line.substring((index+"  "+w).length()).trim();
						parser.addFeature(
								ConditioningVariables.loadFromString(cleanVariableString(cond), true), index);
						parser.weights[index] = w;
					}
				}
			}
			if(sc != null) {
				sc.close();
			}
			return parser;	
		} catch (Exception e) {
			System.out.println("Failed to load parser (possible malformed parser file).");
			e.printStackTrace();
		}
		if(sc != null) {
			sc.close();
		}
		return null;
	}

	public static PerceptronParser loadIntermediateParser(
			File directory, String filename, boolean cacheVariables) {
		return loadIntermediateParser(directory, filename, cacheVariables, null);
	}

	public static PerceptronParser loadIntermediateParser(
			File directory, String filename, boolean cacheVariables, Grammar grammar) {
		Scanner sc = null;
		try {
			sc = new Scanner(new File(directory.getPath()+File.separator+filename));
			String parserName = sc.nextLine();
			PerceptronParser parser;
			if(parserName.equals(NormalFormPerceptronParser.NF_NAME)) {
				parser = new NormalFormPerceptronParser(grammar, null);
			}
			else {
				System.err.println("Unrecognized parser name "+parserName+
						"; failed to instantiate a new parser");
				if(sc != null) {
					sc.close();
				}
				return null;
			}
			parser.setSaveDirectory(directory.getPath());
			int numFeatures = Integer.parseInt(sc.nextLine().trim());
			parser.featureList = new ArrayList<ConditioningVariables>();
			if(numFeatures >= 0) {
				parser.weights = new double[numFeatures];
			}
			String line;
			String[] toks;
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					toks = line.split("\\s+");
					int index = Integer.parseInt(toks[0]);
					double w = Double.parseDouble(toks[1]);
					String cond = line.substring((index+"  "+w).length());
					cond = cleanVariableString(cond);
					parser.addFeature(ConditioningVariables.loadFromString(cond, cacheVariables), index);
					if(parser.weights != null) {
						parser.weights[index] = w;
					}
				}
			}
			sc.close();
			return parser;	
		} catch (Exception e) {
			System.out.println("Failed to load parser (possible malformed parser file).");
			e.printStackTrace();
		}
		if(sc != null) {
			sc.close();
		}
		return null;
	}

	private static String cleanVariableString(String vars) {
		if(vars.trim().equals("null")) {
			return "";
		}
		return vars.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll(",", "").trim();

	}

	public void addFeature(ConditioningVariables feature, int index) {
		this.featureIndices.put(feature,  index);
		if(this.featureList.size() <= index) {
			while(this.featureList.size() < index) {
				this.featureList.add(null);
			}
			this.featureList.add(feature);
		}
		else {
			this.featureList.set(index, feature);
		}
	}

	public int addFeature(ConditioningVariables feature) {
		Integer f = this.featureIndices.get(feature);
		if(f == null) {
			f = this.featureList.size();
			this.featureIndices.put(feature, f);
			this.featureList.add(feature);
		}
		return f;
	}

	public void setSaveDirectory(String dir) {
		try {
			(new File(dir)).mkdirs();
			this.saveDir = dir;
			if(!this.saveDir.endsWith("/")) {
				this.saveDir += "/";
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	public double score(int[] features) {
		double score = 0.0;
		for(int f : features) {
			score += weights[f];
		}
		return score;
	}

	abstract public int[] getFeaturesForItem(FineChartItem item, boolean addFeaturesToParser);

	abstract public int[] getFeaturesForBackPointer(FineChartItem item,
			FineBackPointer bp, boolean addFeaturesToParser);

	protected int[] registerFeatures(ArrayList<ConditioningVariables> feats) {
		int[] featIDs = new int[feats.size()];
		for(int f=0; f<featIDs.length; f++) {
			featIDs[f] = this.addFeature(feats.get(f));
		}
		return featIDs;
	}

	public PackedFeatureForest extractAndAddFeatures(PerceptronChart chart, PPFeatureExtractor extractor,
			boolean addNewFeaturesToParser) {
		PackedFeatureForest forest = getUnscoredPackedFeatureForest(chart, addNewFeaturesToParser);
		HashSet<Integer> active = forest.getActiveFeatures();
		for(Integer f : active) {
			extractor.incrementFeatureCount(this.featureList.get(f));
		}
		return forest;
	}

	private PackedFeatureForest getUnscoredPackedFeatureForest(PerceptronChart chart,
			boolean addNewFeaturesToParser) {
		chart.fineParseWithPerceptronModel(this, false);
		return new PackedFeatureForest(chart, this, addNewFeaturesToParser);
	}

	public Collection<ConditioningVariables> features() {
		return this.featureIndices.keySet();
	}

	public Integer getFeatureIndex(ConditioningVariables feature) {
		return this.featureIndices.get(feature);
	}

	public ConditioningVariables getFeature(int index) {
		return this.featureList.get(index);
	}

	public void initializeWeightVector() {
		this.weights = new double[this.featureList.size()];
	}

	public int[] filterActiveFeatures(ArrayList<ConditioningVariables> feats) {
		ArrayList<Integer> activeFeats = new ArrayList<Integer>();
		for(ConditioningVariables f : feats) {
			Integer id = this.featureIndices.get(f);
			if(id != null) {
				activeFeats.add(id);
			}
		}
		int[] featIDs = new int[activeFeats.size()];
		for(int f=0; f<featIDs.length; f++) {
			featIDs[f] = activeFeats.get(f);
		}
		return featIDs;
	}
	


	public String getCandCString(ConditioningVariables feature) {
		return "C&C string not implemented";
	}

	public String getPrettyFeatureString(ConditioningVariables feature) {
		return "Pretty feature string not implemented";
	}

	public String getPrettyFeatureTree(FeatureTree tree) {
		return "Pretty feature tree not implemented";
	}

	public static PerceptronParser loadForParsing(Grammar g,
			LSSupertagger tagger, File file) {
		// TODO Auto-generated method stub
		return null;
	}

	public Tree<? extends FineChartItem> adaptiveSupertaggingOracleParse(Sentence sentence,
			LSSupertagger supertagger, int numTagsMax) {
		// Coarse parse with adaptive supertagging
		PerceptronChart chart = null;
		try {

			chart = new PerceptronChart(sentence, grammar);
			SupertagAssignment tags = supertagger.tagSentenceOracle(sentence);	
			chart.coarseParseWithSupertags(grammar, parserConfig, tags, false);
			if(chart.successfulCoarseParse()) {
				if(chart != null && chart.successfulCoarseParse()) {
					chart.fineParseWithPerceptronModel(this, true);
					if(chart.successfulFineParse()) {
						return chart.getViterbiParse();
					}
					else {
						System.out.println("Unsuccessful fine parse");
					}
				}
				else {
					System.out.println("Unsuccessful coarse parse");
				}
			}
		}catch (Exception e) {
			System.out.println("Failed to parse sentence: "+sentence);
			e.printStackTrace();
		}
		return null;
	}

	public Tree<? extends FineChartItem> adaptiveSupertaggingHackParse(Sentence sentence,
			LSSupertagger supertagger, HashMap<String, HashSet<String>> lexicon, double beta) {
		// Coarse parse with adaptive supertagging
		PerceptronChart chart = null;
		try {

			chart = new PerceptronChart(sentence, grammar);
			SupertagAssignment tags = supertagger.tagSentenceRestrictLexicon(sentence, lexicon, beta);	
			chart.coarseParseWithSupertags(grammar, parserConfig, tags, false);
			if(chart.successfulCoarseParse()) {
				if(chart != null && chart.successfulCoarseParse()) {
					chart.fineParseWithPerceptronModel(this, true);
					if(chart.successfulFineParse()) {
						return chart.getViterbiParse();
					}
					else {
						System.out.println("Unsuccessful fine parse");
					}
				}
				else {
					System.out.println("Unsuccessful coarse parse");
				}
			}
		}catch (Exception e) {
			System.out.println("Failed to parse sentence: "+sentence);
			e.printStackTrace();
		}
		return null;
	}
	


	public void setFeatureWeights(ArrayList<ConditioningVariables> featuresToSet,
			ArrayList<Double> weightsToSet) {
		int numFeatures = featuresToSet.size();
		this.weights = new double[numFeatures];
		this.featureIndices = new HashMap<ConditioningVariables, Integer>();
		this.featureList = new ArrayList<ConditioningVariables>(numFeatures);
		for(int f=0; f<featuresToSet.size(); f++) {
			this.addFeature(featuresToSet.get(f));
			this.weights[f] = weightsToSet.get(f);
		}
	}
}

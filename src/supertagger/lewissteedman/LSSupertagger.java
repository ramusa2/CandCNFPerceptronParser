package supertagger.lewissteedman;

import illinoisParser.Grammar;
import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Scanner;

// TODO: should move to neuralnet.simplelayers.LinearLayer (and update that class accordingly)
import neuralnet.deprecated.LinearLayer;

import org.jblas.DoubleMatrix;

import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;
import supertagger.nn.modules.LSLookupTableLayer;
import supertagger.nn.modules.LookupTableLayer;
import supertagger.nn.modules.SoftMaxLayer;

/**
 * This class defines the neural net used in the Lewis and Steedman supertagger
 * (specifically, this class does NOT use the additional hard-tanh hidden layer).
 * 
 * @author ramusa2
 *
 */
public class LSSupertagger {

	/** Size of context window (including word to tag) **/
	private int CONTEXT_WINDOW_SIZE;

	/** Dimensionality for non-embedding lookup features, i.e. capitalization and 2-character suffix **/
	private static final int K = 5;

	private LSLookupTableLayer lookupLayer;
	private LinearLayer firstLinearLayer;
	private SoftMaxLayer outputLayer;
	private int numLabels;

	private ArrayList<String> catList;
	private HashMap<String, Integer> catToID;

	/**
	 * Constructs a new supertagger.
	 * 
	 * @param contextWindowSize		total number of words to consider during a tagging decision
	 * @param numberOfFeaturesPerVariable	number of features each word maps to in the lookup table
	 * @param embeddingDimension	dimensionality of the pre-trained word embeddings
	 * @param listOfCats			set of lexical categories to predict
	 */
	public LSSupertagger(int contextWindowSize, int numberOfFeaturesPerVariable, 
			int embeddingDimension, ArrayList<String> listOfCats) {
		this.CONTEXT_WINDOW_SIZE = contextWindowSize;
		this.lookupLayer = new LSLookupTableLayer(this.CONTEXT_WINDOW_SIZE, numberOfFeaturesPerVariable, embeddingDimension, this.K);
		this.numLabels = listOfCats.size();
		//this.firstLinearLayer = new LinearLayer(this.lookupLayer.getNumberOfOutputs(), this.numLabels);
		
		this.outputLayer = new SoftMaxLayer(this.lookupLayer.getNumberOfOutputs(), this.numLabels);
		
		this.catList = listOfCats;
		this.catToID = new HashMap<String, Integer>();
		for(int i=0; i<this.catList.size(); i++) {
			this.catToID.put(this.catList.get(i), i);
		}
	}

	public void initializeLookupFeatures(WordEmbeddings embeddings) {
		this.lookupLayer.initializeEmbeddings(embeddings);
	}

	/**
	 * Adjusts the network parameters based on the gradient of the cross-entropy loss function
	 * for probabilities assigned by the network (based on the specified active variables) and
	 * the target correct label.
	 *  
	 * The gradient is back-propagated through the network, and the parameters at each layer
	 * are adjusted using a step scaled by the learning rate.
	 */
	public void trainOn(LSVariableEntry[] context, int correctLabel, double learningRate) {
		/*
		DoubleMatrix lookupOutput = this.lookupLayer.output(context);
		DoubleMatrix linearOutput = this.firstLinearLayer.output(lookupOutput);
		DoubleMatrix prediction = this.outputLayer.output(linearOutput);
		
		DoubleMatrix costGrad = this.outputLayer.getGradientOfCostFunction(correctLabel, this.numLabels);
		DoubleMatrix linGrad = 
				this.firstLinearLayer.calculateGradientWithRespectToInput(lookupOutput, costGrad);
		this.firstLinearLayer.updateParameters(costGrad, learningRate);
		this.lookupLayer.updateParameters(linGrad, learningRate);
		*/
		
		// TODO: verify correctness of this step
		DoubleMatrix lookupOutput = this.lookupLayer.output(context);
		DoubleMatrix prediction = this.outputLayer.predict(lookupOutput);
		
		DoubleMatrix costGrad = this.outputLayer.getBackpropagatedGradient(lookupOutput, correctLabel);
		this.lookupLayer.updateParameters(costGrad, learningRate);
	}

	/**
	 * Returns a column vector where the i'th element is the probability of the i'th label,
	 * according to the network's current parameters.
	 */
	public DoubleMatrix predict(LSVariableEntry[] context) {
		//return this.outputLayer.predict(this.firstLinearLayer.output(this.lookupLayer.output(context)));
		// TODO: verify correctness
		return this.outputLayer.predict(this.lookupLayer.output(context));
	}

	private DoubleMatrix getRandomGaussianNoise(int dimension) {
		DoubleMatrix gaussian = new DoubleMatrix(dimension);
		Random random = new Random();
		for(int f=0; f<dimension; f++) {
			gaussian.put(f, random.nextGaussian());
		}
		return gaussian;
	}


	public SupertagAssignment tagSentence(Sentence sentence) {
		SupertagAssignment assign = new SupertagAssignment(sentence);
		for(int w=0; w<sentence.length(); w++) {
			DoubleMatrix prediction = this.predict(sentence, w);
			for(int c=0; c<prediction.length; c++) {
				assign.addLexcat(w, this.catList.get(c), prediction.get(c));
			}
		}
		return assign;
	}


	public SupertagAssignment tagSentence(Sentence sentence, double beta) {
		SupertagAssignment assign = new SupertagAssignment(sentence);
		for(int w=0; w<sentence.length(); w++) {
			DoubleMatrix prediction = this.predict(sentence, w);
			double best = -1.0;
			for(int c=0; c<prediction.length; c++) {
				best = Math.max(best, prediction.get(c));
			}
			double cutoff = best*beta;
			for(int c=0; c<prediction.length; c++) {
				if(prediction.get(c) >= cutoff) {
					assign.addLexcat(w, this.catList.get(c), prediction.get(c));
				}
			}
		}
		return assign;
	}

	public void train(Collection<Sentence> data, int numIterations, double learningRate) {
		train(data, numIterations, learningRate, "");
	}

	public void train(Collection<Sentence> data, int numIterations, double learningRate, String saveDir) {
		train(data, numIterations, learningRate, saveDir, 0);
	}
	
	public void train(Collection<Sentence> data, int numIterations, double learningRate, String saveDir, int startIter) {
		ArrayList<LSVariableEntry[]> words = new ArrayList<LSVariableEntry[]>();
		ArrayList<Integer> labels = new ArrayList<Integer>();
		for(Sentence sentence : data) {
			for(int w=0; w<sentence.length(); w++) {
				Integer label = this.catToID.get(sentence.get(w).getCategory());
				if(label != null) {
					words.add(this.lookupLayer.extractContext(sentence, w, this.CONTEXT_WINDOW_SIZE));
					labels.add(label);
				}
			}
		}
		for(int T=startIter; T<numIterations; T++) {
			for(int t=0; t<words.size(); t++) {
				this.trainOn(words.get(t), labels.get(t), learningRate);
				if(t>0 && t%100000==0) {
					System.out.println("Iteration "+(T+1)+": trained on "+t+" out of "+words.size()+" words.");
				}
			}
			this.save(new File(saveDir+File.separator+"tagger__iter="+(T+1)));
		}
	}

	public void save(File dir) {
		dir.mkdirs();
		File lookupWeights = new File(dir.getPath()+File.separator+"lookup");
		lookupWeights.mkdir();
		this.lookupLayer.saveWeightsToFile(lookupWeights);
		File linearWeights = new File(dir.getPath()+File.separator+"linear");
		this.firstLinearLayer.saveWeightsToFile(linearWeights);
		try {
			File config = new File(dir.getPath()+File.separator+"config");
			this.writeConfig(config);
			File categories = new File(dir.getPath()+File.separator+"categories");
			this.writeCategories(categories);
		}
		catch(Exception e) {
			System.err.println("Failed to save supertagger.");
			e.printStackTrace();
		}
	}

	private void writeCategories(File catFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(catFile);
		for(String cat : this.catList) {
			pw.println(cat);
		}
		pw.close();
	}

	private void writeConfig(File config) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(config);
		pw.println("contextWindowSize="+this.CONTEXT_WINDOW_SIZE);
		pw.println("featuresPerVariable="+this.lookupLayer.getNumberOfFeaturesPerInput());
		pw.println("embeddingDimension="+this.lookupLayer.getEmbeddingDimension());
		pw.close();
	}

	public void loadWeights(File dir) {
		/*
		File lookupWeights = new File(dir.getPath()+File.separator+"lookup");
		this.lookupLayer.loadWeightsFromFile(lookupWeights);
		File linearWeights = new File(dir.getPath()+File.separator+"linear");
		this.firstLinearLayer.loadWeightsFromFile(linearWeights);
		*/
		
		// TODO: verify correctness
		File lookupWeights = new File(dir.getPath()+File.separator+"lookup");
		this.lookupLayer.loadWeightsFromFile(lookupWeights);
		File linearWeights = new File(dir.getPath()+File.separator+"linear");
		this.outputLayer.loadWeightsFromFile(linearWeights);
		
	}

	public static LSSupertagger load(File dir) {
		try {
			File categoriesFile = new File(dir.getPath()+File.separator+"categories");
			ArrayList<String> categories = readCategories(categoriesFile);
			File config = new File(dir.getPath()+File.separator+"config");
			LSSupertagger tagger = readFromConfig(config, categories);
			tagger.loadWeights(dir);
			return tagger;
		}
		catch(Exception e) {
			System.err.println("Failed to save supertagger.");
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<String> readCategories(File file) throws FileNotFoundException {
		ArrayList<String> categories = new ArrayList<String>();
		Scanner sc = new Scanner(file);
		while(sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			if(!line.isEmpty()) {
				categories.add(line);
			}
		}
		sc.close();
		return categories;
	}

	private static LSSupertagger readFromConfig(File file, ArrayList<String> categories) throws FileNotFoundException {
		Scanner sc = new Scanner(file);
		int contextWindowSize = Integer.parseInt(sc.nextLine().trim().split("=")[1]);
		int featuresPerVariable = Integer.parseInt(sc.nextLine().trim().split("=")[1]);
		int embeddingDimension = Integer.parseInt(sc.nextLine().trim().split("=")[1]);
		sc.close();
		return new LSSupertagger(contextWindowSize, featuresPerVariable, embeddingDimension, categories);
	}

	public SupertagAssignment tagSentenceOracle(Sentence sentence) {
		SupertagAssignment assign = new SupertagAssignment(sentence);
		for(int w=0; w<sentence.length(); w++) {
			DoubleMatrix prediction = this.predict(sentence, w);
			double goldProb = -1.0;
			for(int c=0; c<prediction.length; c++) {
				if(this.catList.get(c).equals(sentence.get(w).getCategory())) {
					goldProb = prediction.get(c);
				}
			}
			if(goldProb >= 0.0) {
				for(int c=0; c<prediction.length; c++) {
					if(prediction.get(c) >= goldProb) {
						assign.addLexcat(w, this.catList.get(c), prediction.get(c));
					}
				}
			}
			else {
				for(int c=0; c<prediction.length; c++) {
					assign.addLexcat(w, this.catList.get(c), prediction.get(c));
				}
			}
		}
		return assign;
	}


	public SupertagAssignment tagSentenceRestrictLexicon(Sentence sentence, HashMap<String, HashSet<String>> lexicon, double beta) {
		SupertagAssignment assign = new SupertagAssignment(sentence);
		for(int w=0; w<sentence.length(); w++) {
			HashSet<String> allowedCats = lexicon.get(sentence.get(w).getWord());
			DoubleMatrix prediction = this.predict(sentence, w);
			double best = -1.0;
			for(int c=0; c<prediction.length; c++) {
				if(allowedCats == null || allowedCats.contains(this.catList.get(c))) {
					best = Math.max(best, prediction.get(c));
				}
			}
			double cutoff = best*beta;
			for(int c=0; c<prediction.length; c++) {
				if(prediction.get(c) >= cutoff && (allowedCats == null || allowedCats.contains(this.catList.get(c)))) {
					assign.addLexcat(w, this.catList.get(c), prediction.get(c));
				}
			}
		}
		return assign;
	}


	public ArrayList<String> getCategoryList() {
		return this.catList;
	}

	public LinearLayer getHiddenLayer() {
		return this.firstLinearLayer;
	}

	public DoubleMatrix getHiddenLayerOutput(Sentence sentence, int i) {
		return this.firstLinearLayer.output(
				this.lookupLayer.output(
						this.getContext(sentence, i)));

	}

	private LSVariableEntry[] getContext(Sentence sentence, int i) {
		return this.lookupLayer.extractContext(sentence, i, this.CONTEXT_WINDOW_SIZE);
	}

	public DoubleMatrix predict(Sentence sentence, int i) {
		//return this.outputLayer.predict(getHiddenLayerOutput(sentence, i));
		// TODO: verify correctness
		return this.outputLayer.predict(this.lookupLayer.output(
				this.getContext(sentence, i)));
	}

	public int getCategoryIndex(String category) {
		Integer id = this.catToID.get(category);
		if(id == null) {
			return -1;
		}
		return id;
	}

	public HashMap<String, Integer> getCatIDMap() {
		return this.catToID;
	}

	public ArrayList<LexicalCategoryEntry> tagWord(Sentence sen, int i, double beta) {
		DoubleMatrix prediction = this.predict(sen, i);
		double best = -1.0;
		for(int c=0; c<prediction.length; c++) {
			best = Math.max(best, prediction.get(c));
		}
		double cutoff = best*beta;
		ArrayList<LexicalCategoryEntry> tags = new ArrayList<LexicalCategoryEntry>();
		for(int c=0; c<prediction.length; c++) {
			if(prediction.get(c) >= cutoff) {
				tags.add(new LexicalCategoryEntry(this.catList.get(c), prediction.get(c)));
			}
		}
		Collections.sort(tags);
		return tags;
	}
}

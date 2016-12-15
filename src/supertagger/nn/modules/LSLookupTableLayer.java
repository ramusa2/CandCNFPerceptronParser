package supertagger.nn.modules;

import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.jblas.DoubleMatrix;

import supertagger.lewissteedman.LSVariableEntry;
import supertagger.lewissteedman.WordEmbeddings;

/**
 * The LSLookupTableLayer class adds the discrete features used
 * by the EasyCCG supertagger.
 * 
 * @author ramusa2
 *
 */
public class LSLookupTableLayer extends LookupTableLayer {

	/** Filename for embeddings file in save directory **/
	private static final String EMBEDDINGS_FILE = "embeddings";
	/** Filename for capitalization file in save directory **/
	private static final String CAPITALIZATION_FILE = "capitalization";
	/** Filename for suffix file in save directory **/
	private static final String SUFFIX_FILE = "suffix";

	/** *START* symbol for buffer before the sentence **/
	private static final String START = "*START*";	
	/** ID for *START* symbol **/
	private static final int START_ID = -1;
	/** Cached feature ID vector for START symbol **/
	private final LSVariableEntry START_ENTRY;

	/** *END* symbol for buffer after the sentence **/
	private static final String END = "*END*";
	/** ID for *END* symbol **/
	private static final int END_ID = -2;
	/** Cached feature ID vector for END symbol **/
	private final LSVariableEntry END_ENTRY;

	/** Unknown word token for capitalized unseen words **/
	private static final String UNK_UPPER = "*UNK_UPPER*";	
	/** ID for *UNK_UPPER* symbol **/
	private static final int UNK_UPPER_ID = -3;

	/** Unknown word token for capitalized unseen words **/
	private static final String UNK_LOWER = "*UNK_LOWER*";	
	/** ID for *UNK_LOWER* symbol **/
	private static final int UNK_LOWER_ID = -4;

	/** Unknown word token for unseen words that do not start with a letter **/
	private static final String UNK_SPECIAL = "*UNK_SPECIAL*";	
	/** ID for *UNK_SPECIAL* symbol **/
	private static final int UNK_SPECIAL_ID = -5;

	/** Marker for capitalization feature for Capitalized Words **/
	private final static Integer CAPS_UPPER_ID = -1;	

	/** Marker for capitalization feature for uncapitalized words 
	 * (as well as words that begin with a non-alphabetic character) **/
	private final static Integer CAPS_LOWER_ID = -2;	

	/** Marker for capitalization feature for START/END symbols**/
	private final static Integer CAPS_START_END_ID = -3;	

	/** Shared feature ID for suffix feature for START/END symbols **/
	private final static int SUFFIX_START_END = -1;	

	/** Shared feature ID for unknown suffix feature (used when suffix hasn't been seen before) **/
	private final static int SUFFIX_UNSEEN = -2;	

	/** Number of nodes to add for each discrete feature **/
	private final int discreteK;

	/** Cache map for discrete suffix feature **/
	private final ConcurrentHashMap<String, Integer> suffixIDLookupTable;

	/** Cache map for discrete suffix feature **/
	private final ConcurrentHashMap<Integer, double[]> suffixFeatureLookupTable;

	/** Cache map for discrete capitalization feature **/
	private final ConcurrentHashMap<Integer, double[]> capsFeatureLookupTable;

	/** Caches context (not necessary for testing, but speeds up training) **/
	private LSVariableEntry[] cachedTrainingContext;

	/** Dimensionality of the pre-trained word embeddings **/
	private final int wordEmbeddingDimension;

	/**
	 * Creates a LookupTableLayer of the specified dimensions; feature weights
	 * are initialized using Gaussian noise, with a mean of zero and standard 
	 * deviation of one.
	 * 
	 * @param numberOfInputVariables		size of the input variable space
	 * @param dimensionsPerInput	number of features to match each variable to
	 * @param embeddingDimension	dimensionality of the pre-rtained word embeddings
	 * @param K						number of nodes to add for each discrete feature
	 */
	public LSLookupTableLayer(int numberOfInputVariables, int dimensionsPerInput, int embeddingDimension, int K) {
		super(numberOfInputVariables, dimensionsPerInput);
		this.wordEmbeddingDimension = embeddingDimension;
		this.discreteK = K;
		this.suffixIDLookupTable = new ConcurrentHashMap<String, Integer>();
		this.suffixFeatureLookupTable = new ConcurrentHashMap<Integer, double[]>();
		this.capsFeatureLookupTable = new ConcurrentHashMap<Integer, double[]>();
		// Update ID maps
		this.stringToIndex.put(START, START_ID);
		this.stringToIndex.put(END, END_ID);
		this.stringToIndex.put(UNK_UPPER, UNK_UPPER_ID);
		this.stringToIndex.put(UNK_LOWER, UNK_LOWER_ID);
		this.stringToIndex.put(UNK_SPECIAL, UNK_SPECIAL_ID);
		// Set flag tokens
		this.START_ENTRY = new LSVariableEntry(START, START_ID, CAPS_START_END_ID, SUFFIX_START_END);
		this.END_ENTRY = new LSVariableEntry(END, END_ID, CAPS_START_END_ID, SUFFIX_START_END);
		// Initialize discrete feature vectors
		this.capsFeatureLookupTable.put(CAPS_UPPER_ID, super.getWeightVectorOfGaussianNoise(discreteK));
		this.capsFeatureLookupTable.put(CAPS_LOWER_ID, super.getWeightVectorOfGaussianNoise(discreteK));
		this.capsFeatureLookupTable.put(CAPS_START_END_ID, super.getWeightVectorOfGaussianNoise(discreteK));
		this.suffixFeatureLookupTable.put(SUFFIX_UNSEEN, super.getWeightVectorOfGaussianNoise(discreteK));
		this.suffixFeatureLookupTable.put(SUFFIX_START_END, super.getWeightVectorOfGaussianNoise(discreteK));
	}

	/**
	 * Returns the weight vector for a word's embedding.
	 */
	private double[] getWordEmbeddingWeights(Integer embeddingID) {
		return this.wordEmbeddingsLookupTable.get(embeddingID);
	}

	/**
	 * Returns the weight vector for a word's capitalization feature.
	 */
	private double[] getCapitalizationWeights(Integer capitalizationID) {
		return this.capsFeatureLookupTable.get(capitalizationID);
	}

	/**
	 * Returns the weight vector for a word's suffix feature.
	 */
	private double[] getSuffixWeights(Integer suffixID) {
		return this.suffixFeatureLookupTable.get(suffixID);
	}

	/**
	 * Lower-cases word and replaces all numbers with the # sign
	 */
	private String normalize(String word) {
		return word.toLowerCase().replaceAll("[0-9]", "#");
	}

	/**
	 * Given a list of indices indicating the active variables, return the output
	 * signal (a linear combination of the current weights for those active features).
	 * 
	 * Note: overwrites any existing cached active variable indices, storing activeVariables instead.

	public DoubleMatrix output(int[] activeVariables) {	
		System.out.print
	}
	 */
	
	public int getEmbeddingDimension() {
		return this.wordEmbeddingDimension;
	}

	/**
	 * Caches the feature IDs for the context window of width C centered at position i,
	 * and returns a DoubleMatrix vector with the concatenated current weights.
	 */
	public DoubleMatrix output(Sentence sentence, int i, int C) {	
		this.cachedTrainingContext = extractContext(sentence, i, C);
		double[] vec = new double[this.numOutputs];
		int vecIndex = 0;
		for(int c=0; c<C; c++) {
			double[] entryVec = this.buildContextVectorForEntry(cachedTrainingContext[c]);
			System.arraycopy(entryVec, 0, vec, vecIndex, entryVec.length);
		}
		return buildDoubleMatrix(vec);
	}

	/**
	 * Caches the feature IDs for the context window of width C centered at position i,
	 * and returns a DoubleMatrix vector with the concatenated current weights.
	 */
	public DoubleMatrix output(LSVariableEntry[] context) {	
		this.cachedTrainingContext = context;
		double[] vec = new double[this.numOutputs];
		int vecIndex = 0;
		for(int c=0; c<context.length; c++) {
			double[] entryVec = this.buildContextVectorForEntry(cachedTrainingContext[c]);
			System.arraycopy(entryVec, 0, vec, vecIndex, entryVec.length);
			vecIndex += entryVec.length;
		}
		return buildDoubleMatrix(vec);
	}

	private double[] buildContextVectorForEntry(LSVariableEntry entry) {
		double[] vec = new double[this.numFeaturesPerInputVariable];
		double[] embeddingVec = this.getWordEmbeddingWeights(entry.getEmbeddingIndex());
		double[] capsVec = this.getCapitalizationWeights(entry.getCapitalizationIndex());
		double[] suffixVec = this.getSuffixWeights(entry.getSuffixIndex());
		System.arraycopy(embeddingVec, 0, vec, 0, embeddingVec.length);
		System.arraycopy(capsVec, 0, vec, embeddingVec.length, capsVec.length);
		System.arraycopy(suffixVec, 0, vec, embeddingVec.length+capsVec.length, suffixVec.length);		
		return vec;
	}

	/**
	 * Extracts ID fields for words in the context window of (total) width C centered at position i. 
	 */
	public LSVariableEntry[] extractContext(Sentence sentence, int i, int C) {
		LSVariableEntry[] cxt = new LSVariableEntry[C];
		int width = C/2;
		for(int j=-width; j<=width; j++) {
			cxt[j+width] = constructEntry(sentence, i+j);
		}
		return cxt;
	}

	/**
	 * Constructs an LSVariableEntry storing the feature/embedding indices for
	 * the word at position i in a sentence.
	 */
	public LSVariableEntry constructEntry(Sentence sentence, int i) {
		if(i < 0) {
			return this.START_ENTRY;
		}
		else if(i >= sentence.length()) {
			return this.END_ENTRY;
		}
		String word = sentence.wordAt(i);
		int embeddingID = getEmbeddingID(word);
		int capitalizationID = getCapitalizationID(word);
		int suffixID = getSuffixID(word);
		return new LSVariableEntry(word, embeddingID, capitalizationID, suffixID);
	}

	@Override
	public Integer getEmbeddingID(String word) {
		Integer id = this.stringToIndex.get(normalize(word));
		if(id == null) {
			char c = word.charAt(0);
			boolean isLower = 'a' <= c && c <= 'z';
			boolean isUpper = 'A' <= c && c <= 'Z';
			if (isLower) {
				return UNK_LOWER_ID;
			} else if (isUpper) {
				return UNK_UPPER_ID;
			} else {
				return UNK_SPECIAL_ID;
			}
		}
		return id;
	}

	@Override
	public DoubleMatrix getWordEmbedding(String word) {
		return this.buildDoubleMatrix(this.getWordEmbeddingWeights(this.getEmbeddingID(word)));
	}

	private Integer getCapitalizationID(String word) {
		if(word.equals(START) || word.equals(END)) {
			return CAPS_START_END_ID;
		}
		char c = word.charAt(0);
		if ('A' <= c && c <= 'Z') {
			return CAPS_UPPER_ID;
		}
		return CAPS_LOWER_ID;
	}

	private int checkOrAddSuffixID(String word) {
		String suffix = null;
		word = word.toLowerCase();
		if (word.length() > 1) { 
			suffix = word.substring(word.length()-2);
		} else {
			suffix = "_" + word;
		}
		Integer result = this.suffixIDLookupTable.get(suffix);
		if (result == null) {
			result = this.suffixIDLookupTable.size();
			this.suffixIDLookupTable.put(suffix, result);
		}
		return result;
	}

	private int getSuffixID(String word) {
		String suffix = null;
		word = word.toLowerCase();
		if (word.length() > 1) { 
			suffix = word.substring(word.length()-2);
		} else {
			suffix = "_" + word;
		}
		Integer result = this.suffixIDLookupTable.get(suffix);
		if (result == null) {
			if (word.equals(START) || word.equals(END)) {
				return SUFFIX_START_END;
			}
			return SUFFIX_UNSEEN;
		}
		return result;
	}

	/**
	 * Update the feature weights for the cached active variables based on
	 * the back-propagated gradient.
	 */
	public void updateParameters(DoubleMatrix outputGradient, double learningRate) {
		// Scale gradient by the learning rate
		outputGradient.muli(learningRate);
		// Update weights
		int offset = 0;
		for(LSVariableEntry entry : this.cachedTrainingContext) {
			// Update word embeddings
			double[] embeddings = this.getWordEmbeddingWeights(entry.getEmbeddingIndex());
			for(int w=0; w<embeddings.length; w++) {
				embeddings[w] += outputGradient.get(offset);
				offset++;
			}
			// Update capitalization feature weights
			double[] caps = this.getCapitalizationWeights(entry.getCapitalizationIndex());
			for(int c=0; c<caps.length; c++) {
				caps[c] += outputGradient.get(offset);
				offset++;
			}			
			// Update suffix feature weights
			double[] suff = this.getSuffixWeights(entry.getSuffixIndex());
			for(int s=0; s<suff.length; s++) {
				suff[s] += outputGradient.get(offset);
				offset++;
			}		
		}
	}

	/**
	 * Saves the lookup table weights to file
	 */
	public void saveWeightsToFile(File saveDir) {
		saveDir.mkdir();
		PrintWriter pw = null;
		try {
			// Embeddings
			pw = new PrintWriter(new File(saveDir.getPath()+File.separator+EMBEDDINGS_FILE));
			for(String word : this.stringToIndex.keySet()) {
				int id = this.stringToIndex.get(word);
				pw.println(word+" "+id+" "+arrToString(this.wordEmbeddingsLookupTable.get(id)));
			}
			pw.close();
			// Capitalization
			pw = new PrintWriter(new File(saveDir.getPath()+File.separator+CAPITALIZATION_FILE));
			pw.println("*CAPS_UPPER* "+CAPS_UPPER_ID+" "+arrToString(this.getCapitalizationWeights(CAPS_UPPER_ID)));
			pw.println("*CAPS_LOWER* "+CAPS_LOWER_ID+" "+arrToString(this.getCapitalizationWeights(CAPS_LOWER_ID)));
			pw.println("*CAPS_START_END* "+CAPS_START_END_ID+" "+arrToString(this.getCapitalizationWeights(CAPS_START_END_ID)));
			pw.close();
			// Suffix
			pw = new PrintWriter(new File(saveDir.getPath()+File.separator+SUFFIX_FILE));
			for(String suff : this.suffixIDLookupTable.keySet()) {
				int id = this.suffixIDLookupTable.get(suff);
				pw.println(suff+" "+id+" "+arrToString(this.suffixFeatureLookupTable.get(id)));
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("Failed to save lookup layer weights to: "+saveDir.getPath());
		}
	}

	private String arrToString(double[] arr) {
		String ret = "";
		for(double d : arr) {
			ret += d+" ";
		}
		return ret.trim();
	}

	private final static double[] stringToDoubleArr(String input) {
		return stringToDoubleArr(input, 0);
	}

	private final static double[] stringToDoubleArr(String input, int startPos) {
		String[] toks = input.trim().split("\\s+");
		double[] arr = new double[toks.length - startPos];
		for(int i=startPos; i<toks.length; i++) {
			arr[i-startPos] = Double.parseDouble(toks[i]);
		}
		return arr;
	}

	/**
	 * Loads lookup table weights from file.
	 * Precondition: weight vectors in file match dimensionality of this object.
	 */
	public void loadWeightsFromFile(File loadDir) {
		// Read embeddings
		Scanner sc = null;
		try {
			sc = new Scanner(new File(loadDir.getPath()+File.separator+EMBEDDINGS_FILE));
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String[] toks = line.trim().split("\\s+");
					if(toks.length == this.wordEmbeddingDimension+2) {
						String var = toks[0];
						Integer index = Integer.parseInt(toks[1]);
						this.stringToIndex.put(var, index);
						this.wordEmbeddingsLookupTable.put(index,  stringToDoubleArr(line, 2));
					}
					else {
						System.out.println("Malformed weights file; dimensionality does not match " +
								"(expected "+this.wordEmbeddingDimension+" features, " +
								"read "+(toks.length-2)+" features).");
					}
				}
			}
			sc.close();
			// Read caps feature weights
			sc = new Scanner(new File(loadDir.getPath()+File.separator+CAPITALIZATION_FILE));
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String[] toks = line.split("\\s+");
					Integer index = Integer.parseInt(toks[1]);
					this.capsFeatureLookupTable.put(index,  stringToDoubleArr(line, 2));
				}
			}
			sc.close();
			// Read suffix feature weights
			sc = new Scanner(new File(loadDir.getPath()+File.separator+SUFFIX_FILE));
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String[] toks = line.split("\\s+");
					String suff = toks[0];
					Integer index = Integer.parseInt(toks[1]);
					this.suffixIDLookupTable.put(suff, index);
					this.suffixFeatureLookupTable.put(index,  stringToDoubleArr(line, 2));
				}
			}
			sc.close();	
		}
		catch(FileNotFoundException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Constructs a DoubleMatrix vector from an underlying array.
	 */
	protected DoubleMatrix buildDoubleMatrix(double[] vec) {
		return new DoubleMatrix(vec.length, 1, vec);
	}

	/**
	 * Given a set of pre-trained word embeddings, load them into this model.
	 * Unknown word tokens that don't appear in the embeddings file are 
	 * initialized with Gaussian noise.
	 */
	public void initializeEmbeddings(WordEmbeddings embeddings) {
		// Add all saved embeddings
		for(String word : embeddings.getRawWords()) {
			super.setEmbeddingForVariable(word, embeddings.lookup(word).data);
			int suffID = this.checkOrAddSuffixID(word);
			if(!this.suffixFeatureLookupTable.containsKey(suffID)) {
				this.suffixFeatureLookupTable.put(suffID, 
						super.getWeightVectorOfGaussianNoise(this.discreteK));
			}
		}		
		// Check for START/END/UNK tokens, add them if not already
		int[] unkTokenIDs = new int[]{START_ID, END_ID, UNK_LOWER_ID, UNK_UPPER_ID, UNK_SPECIAL_ID};
		for(int unkID : unkTokenIDs) {
			if(!this.wordEmbeddingsLookupTable.containsKey(unkID)) {
				this.wordEmbeddingsLookupTable.put(unkID, 
						super.getWeightVectorOfGaussianNoise(this.wordEmbeddingDimension));
			}
		}
	}	
}


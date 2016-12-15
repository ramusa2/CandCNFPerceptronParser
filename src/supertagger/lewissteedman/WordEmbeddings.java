package supertagger.lewissteedman;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

import org.jblas.DoubleMatrix;

/**
 * Class to store lookup table for word embeddings (e.g. Turian, Mikolov, etc.). 
 * 
 * @author ramusa2
 *
 */
public class WordEmbeddings {
	
	/** Stores UNKNOWN word string **/
	private static final String UNK = "*UNKNOWN*";
	
	/** Lookup table for getting embeddings **/
	private ConcurrentHashMap<String, DoubleMatrix> lookupTable;
	
	/** Stores unformatted words **/
	private ArrayList<String> rawWords;
	
	/** Dimensionality of the embeddings **/
	private int D;
	
	public WordEmbeddings(int numDimensions, boolean randomlyGenerateUnkVector) {
		this.lookupTable = new ConcurrentHashMap<String, DoubleMatrix>();
		this.D = numDimensions;
		this.rawWords = new ArrayList<String>();
		if(randomlyGenerateUnkVector) {
			this.lookupTable.put(UNK, getGaussianNoise(numDimensions));
		}
	}
	
	public DoubleMatrix lookup(String word) {
		String formatted = this.format(word);
		DoubleMatrix vec = this.lookupTable.get(formatted);
		if(vec != null) {
			return vec;
		}
		int lastHyphen = formatted.lastIndexOf("-");
		if(lastHyphen > -1 && lastHyphen < formatted.length()-1) {
			String prefix = formatted.substring(lastHyphen+1);
			if(this.lookupTable.containsKey(prefix)) {
				return this.lookupTable.get(prefix);
			}
		}
		return this.lookupTable.get(UNK);
	}
	
	/**
	 * Lowercases word and replaces all numbers with 0
	 */
	public String format(String word) {
		String formatted = word.toLowerCase();
		formatted.replaceAll("[0-9]", "0");
		return formatted;
	}

	/**
	 * Returns a vector of D dimensions where each element is generated
	 * by a pseudorandom draw from a Gaussian distribution with mean 0.0 
	 * and standard deviation 1.0
	 */
	private static DoubleMatrix getGaussianNoise(int D) {
		DoubleMatrix vec = new DoubleMatrix(D);
		Random r = new Random();
		for(int d=0; d<D; d++) {
			vec.put(d, r.nextGaussian());
		}
		return vec;
	}
	
	public void loadEmbeddingsFromFile(File file) {
		Scanner sc = null;
		try {
			sc = new Scanner(file);
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String[] toks = line.split("\\s+");
					if(toks.length == this.D+1) {
						String var = toks[0];
						this.rawWords.add(var);
						DoubleMatrix weights = new DoubleMatrix(this.D);
						for(int i=1; i<toks.length; i++) {
							weights.put(i-1, Double.parseDouble(toks[i]));
						}
						String formatted = this.format(var);
						if(!this.lookupTable.containsKey(formatted)) {
							// Only use most-frequent entry if multiple words map to same formatted string
							this.lookupTable.put(formatted,  weights);
						}
					}
					else {
						System.out.println("Malformed embeddings file; dimensionality does not match " +
								"(expected "+this.D+" dimensions, " +
										"read "+(toks.length-1)+" dimensions for word "+toks[0]+").");
					}
				}
			}
			sc.close();
			if(!this.lookupTable.containsKey(UNK)) {
				this.lookupTable.put(UNK, getGaussianNoise(D));
			}
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save lookup layer weights to: "+file.getPath());
		}
	}
	
	public ArrayList<String> getRawWords() {
		return this.rawWords;
	}

	public String getUnknownWord() {
		return UNK;
	}

	public int dimensionality() {
		return this.D;
	}

	public int numberOfWords() {
		return this.rawWords.size();
	}
}

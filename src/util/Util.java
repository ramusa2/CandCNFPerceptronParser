package util;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.jblas.DoubleMatrix;
import org.jblas.Eigen;
import org.jblas.ranges.Range;
import org.jblas.ranges.RangeUtils;

import supertagger.lewissteedman.LSSupertagger;
import supertagger.nn.StringEmbeddings;

public class Util {

	public static final String AUTO_DIR = "data/CCGbank/AUTO";

	public static String rpad(String s, int n) {
		return String.format("%1$-" + n + "s", s);  
	}

	public static String lpad(String s, int n) {
		return String.format("%1$" + n + "s", s);  
	}

	public static ArrayList<String> getCatList() {
		ArrayList<String> cats = new ArrayList<String>();
		try {
			Scanner sc = new Scanner(new File("categories"));
			while(sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					cats.add(line);
				}
			}
			sc.close();
			return cats;
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println("Failed to load category list.");
			return cats;
		}
	}

	public static LSSupertagger loadTagger() throws Exception {
		LSSupertagger net = new LSSupertagger(7, 60, 50, getCatList());
		net.loadWeights(new File("tagger"));
		return net;
	}

	public static StringEmbeddings loadPretrainedWordEmbeddings() {
		StringEmbeddings we = StringEmbeddings.loadFromFile(new File("embeddings/turian/embeddings-scaled.EMBEDDING_SIZE=50.txt"));
		we.setDefault("*UNKNOWN*");
		System.out.println("Loaded initial word embeddings.");
		return we;
	}

	public static StringEmbeddings loadLearnedWordEmbeddings() {
		StringEmbeddings we = StringEmbeddings.loadFromFile(new File("embeddings/word_embeddings_learned_from_supertagger_training.50.txt"));
		we.setDefault("*UNKNOWN*");
		System.out.println("Loaded learned word embeddings.");
		return we;
	}

	public static StringEmbeddings loadWord2VecCategoryEmbeddings() {
		StringEmbeddings ce = StringEmbeddings.loadFromFile(new File("embeddings/word2vec_category_embeddings.50.txt"));
		System.out.println("Loaded word2vec category embeddings.");
		return ce;
	}

	public static StringEmbeddings loadLearnedCategoryEmbeddings100() {
		StringEmbeddings embeddings = new StringEmbeddings();
		try {
			ArrayList<String> categories = getCatList();
			Scanner sc = new Scanner(new File("tagger/linear"));
			int id = 0;
			while(sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String cat = categories.get(id);
					DoubleMatrix vec = stringToDoubleMatrix(line);
					embeddings.addEmbedding(cat, vec);
					id++;
				}
			}
			sc.close();
		}
		catch(Exception e) {
			System.err.println("Failed to load category embeddings.");
			e.printStackTrace();
		}
		System.out.println("Loaded learned category embeddings.");
		return embeddings;
	}

	private final static DoubleMatrix stringToDoubleMatrix(String input, int startPos) {
		String[] toks = input.trim().split("\\s+");
		double[] arr = new double[toks.length - startPos];
		for(int i=startPos; i<toks.length; i++) {
			arr[i-startPos] = Double.parseDouble(toks[i]);
		}
		DoubleMatrix vec = new DoubleMatrix(arr.length, 1);
		for(int i=0; i<vec.length; i++) {
			vec.put(i, arr[i]);
		}
		return vec;
	}

	private final static DoubleMatrix stringToDoubleMatrix(String input) {
		return stringToDoubleMatrix(input, 0);
	}

	public static StringEmbeddings loadLearnedCategoryEmbeddings(int dimension) {
		return getStringEmbeddingsByPCA(loadLearnedCategoryEmbeddings100(), dimension);
	}

	public static StringEmbeddings getStringEmbeddingsByPCA(StringEmbeddings embeddings, int dimension) {
		ArrayList<String> keys = new ArrayList<String>();
		HashMap<String, DoubleMatrix> original = embeddings.getAllVectors();
		keys.addAll(original.keySet());
		if(keys.size() == 0 || original.get(keys.get(0)).length == dimension) {
			return embeddings;
		}
		int originalDimension = original.get(keys.get(0)).length;
		DoubleMatrix mat = new DoubleMatrix(keys.size(), originalDimension);
		for(int r=0; r<keys.size(); r++){
			mat.putRow(r, original.get(keys.get(r)).transpose());
		}
		DoubleMatrix reduced = reduceByPCA(mat, dimension);
		StringEmbeddings newEmbeddings = new StringEmbeddings();
		for(int r=0; r<keys.size(); r++){
			newEmbeddings.addEmbedding(keys.get(r), reduced.getRow(r).transpose());
		}
		return newEmbeddings;
	}

	public static <T> void increment(HashMap<T, Integer> freqs, T key) {
		Integer f = freqs.get(key);
		if(f == null) {
			f = 0;
		}
		freqs.put(key, f+1);
	}

	public static DoubleMatrix initializeRandomRowVector(int rows) {
		return initializeRandomMatrix(rows, 1);
	}

	public static DoubleMatrix initializeRandomColumnVector(int cols) {
		return initializeRandomMatrix(1, cols);
	}
	
	public static DoubleMatrix initializeRandomMatrix(int rows, int cols) {
		DoubleMatrix mat = new DoubleMatrix(rows, cols);
		for(int i=0; i<mat.data.length; i++) {
			mat.data[i] = (Math.random() - 0.5)/cols;
		}
		return mat;
	}
	
	public static DoubleMatrix reduceByPCA(DoubleMatrix mat, int newDim) {
		int rows = mat.rows;
		int cols = mat.columns;
		DoubleMatrix[] decomposition = Eigen.symmetricEigenvectors(mat.transpose().mmul(mat));
		DoubleMatrix E = decomposition[0];
		if(newDim <= rows) {
			return mat.mmul(E).get(RangeUtils.interval(0, rows), RangeUtils.interval(cols-newDim, cols));
		}
		DoubleMatrix padded = new DoubleMatrix(rows, newDim);
		padded.put(RangeUtils.interval(0, rows), RangeUtils.interval(0, cols), mat.mmul(E));
		return padded;
	}
	
	public static void main(String[] args) {
		DoubleMatrix mat = initializeRandomMatrix(10, 2);
		System.out.println(printMatrix(mat)+"\n");
		DoubleMatrix reduced = reduceByPCA(mat, 1);
		System.out.println(printMatrix(reduced)+"\n");
	}
	
	public static String printMatrix(DoubleMatrix mat) {
		StringBuilder str = new StringBuilder();
		for(int r=0; r<mat.rows; r++) {
			for(int c=0; c<mat.columns; c++) {
				str.append(mat.get(r, c));
				str.append(" ");
			}
			str.append("\n");
		}
		return str.toString();
	}

}

package supertagger.nn;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Scanner;

import org.jblas.DoubleMatrix;

public class StringEmbeddings implements Serializable {

	private HashMap<String, DoubleMatrix> vectors;
	
	private String DEFAULT;

	public StringEmbeddings() {
		this.vectors = new HashMap<String, DoubleMatrix>();
	}
	
	public void setDefault(String key) {
		this.DEFAULT = key;
	}
	
	public void setDefault(String key, DoubleMatrix vec) {
		this.DEFAULT = key;
		this.addEmbedding(key, vec);
	}

	public static StringEmbeddings loadFromFile(File file) {
		try {
			StringEmbeddings e = new StringEmbeddings();
			Scanner sc = new Scanner(file);
			while(sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String[] toks = line.split("\\s+");
					String cat = toks[0];
					DoubleMatrix vec = new DoubleMatrix(toks.length-1, 1);
					for(int i=1; i<toks.length; i++) {
						vec.put(i-1, Double.parseDouble(toks[i]));
					}
					e.addEmbedding(cat, vec);
				}
			}
			sc.close();
			return e;
		}
		catch(FileNotFoundException e) {
			System.err.println("Failed to load embeddings.");
			return null;
		}
	}
	
	public void saveToFile(File file) {
		try {
			PrintWriter pw = new PrintWriter(file);
			for(String cat : this.vectors.keySet()) {
				DoubleMatrix vec = this.vectors.get(cat);
				pw.print(cat);
				for(int i=0; i<vec.length; i++) {
					pw.print(" "+vec.get(i));
				}
				pw.println();
			}
			pw.close();
		}
		catch(Exception e) {
			System.err.println("Failed to save embeddings.");
		}
	}

	public void addEmbedding(String cat, DoubleMatrix vec) {
		this.vectors.put(cat, vec);
	}
	
	public DoubleMatrix getVec(String cat) {
		DoubleMatrix vec = this.vectors.get(cat);
		if(vec == null) {
			vec = this.vectors.get(DEFAULT);
		}
		return vec;
	}
	
	public HashMap<String, DoubleMatrix> getAllVectors() {
		return this.vectors;
	}

}

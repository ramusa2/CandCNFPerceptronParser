package multitagger.layers.inputlayers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

import multitagger.layers.MultitaggerInputLayer;

import org.jblas.DoubleMatrix;

import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;

public class ProbabilityBitVectorLookupLayer  extends MultitaggerInputLayer {
	
	private static final int K = 10;

	protected final static String MISSING = "MISSING";

	protected HashMap<String, Double> lookupTable;
	
	protected HashMap<String, Integer> index;

	protected String inputFeature;
	
	protected int inputFeatureIndex;
	
	protected MultitaggerTrainingItem inputItem;

	public ProbabilityBitVectorLookupLayer() {
		this.index= new HashMap<String, Integer>();
		this.lookupTable = new HashMap<String, Double>();
		this.addFeatureValue(MISSING);
	}

	public void addFeatureValue(String feature) {
		if(!this.index.containsKey(feature)) {
			this.index.put(feature, this.index.size());
		}
		this.lookupTable.put(feature, Math.random()-0.5);		
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(this.lookupTable);
		out.writeObject(this.index);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		this.lookupTable = (HashMap<String, Double>) in.readObject();
		this.index = (HashMap<String, Integer>) in.readObject();
	}

	@Override
	public int getOutputDimension() {
		return this.index.size();
	}

	@Override
	protected void fillOutputVector(DoubleMatrix output,
			MultitaggerTrainingSentence sentence, int position) {
		for(int i=0; i<output.length; i++) {
			output.put(i,  0.0);
		}
		this.inputItem = sentence.getItems().get(position);
		for(int c=0; c<K; c++) {
			String cat = this.inputItem.getCat(c);
			Integer ind = this.index.get(cat);
			if(ind == null) {
				ind = this.index.get(MISSING);
			}
			output.put(ind, this.inputItem.getProb(c));
		}
	}

	@Override
	public void updateParameters(DoubleMatrix outputGradient,
			double learningRate) {
		for(int c=0; c<K; c++) {
			String cat = this.inputItem.getCat(c);
			Integer ind = this.index.get(cat);
			if(ind == null) {
				ind = this.index.get(MISSING);
				cat = MISSING;
			}
			double oldWeight = this.lookupTable.get(cat);
			this.lookupTable.put(cat, oldWeight+this.inputItem.getProb(c)*learningRate*outputGradient.get(ind));
		}
	}

	@Override
	public void saveWeightsToFile(File file) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
			for(String key : this.lookupTable.keySet()) {
				pw.print(key+" "+this.lookupTable.get(key));
				pw.println();
			}
			pw.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to save lookup layer weights to: "+file.getPath());
		}
	}

	@Override
	public void loadWeightsFromFile(File file) {
		Scanner sc = null;
		try {
			sc = new Scanner(file);
			String line;
			while(sc.hasNextLine()) {
				line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					String[] toks = line.trim().split("\\s+");
					this.lookupTable.put(toks[0], Double.parseDouble(toks[1]));
					if(toks.length == 2) {
						this.lookupTable.put(toks[0], Double.parseDouble(toks[1]));
					}
					else {
						System.out.println("Malformed weights file; dimensionality does not match " +
								"(expected 1 output, " +
								"read "+(toks.length-1)+" weights).\nAborting.");
						sc.close();
						return;
					}
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			System.out.println("Failed to load lookup layer weights from: "+file.getPath());
		}
	}
}

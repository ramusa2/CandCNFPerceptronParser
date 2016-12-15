package supertagger.lsbeta;

import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

import org.jblas.DoubleMatrix;

import supertagger.lewissteedman.LSSupertagger;

public class BetaLSSupertagger {
	
	private LSSupertagger tagger;
	
	private DoubleMatrix weights, gradientVector;
	
	private double bias;
	
	private double gamma;
	
	private double eta;
	

	public BetaLSSupertagger(LSSupertagger supertagger, double initialBeta, double learningRate,
			double objectiveFunctionGamma) {
		this.tagger = supertagger;
		this.weights = new DoubleMatrix(1, this.tagger.getHiddenLayer().getNumberOfNodes());
		this.gradientVector = new DoubleMatrix(1, this.weights.length);
		//this.bias = initialBeta;
		this.bias = - Math.log(1.0/initialBeta - 1.0);
		this.gamma = objectiveFunctionGamma;
		this.eta = learningRate; 
	}
	

	private BetaLSSupertagger(LSSupertagger supertagger) {
		this.tagger = supertagger;
		this.weights = new DoubleMatrix(1, this.tagger.getHiddenLayer().getNumberOfNodes());
	}
	
	public void trainBeta(Collection<Sentence> sentences, int numIterations, String weightsFile) {
		ArrayList<BetaTrainingExample> data = getTrainingData(sentences);		
		for(int T=0; T<numIterations; T++) {
			System.out.println("Starting training iteration "+(T+1)+" out of "+numIterations);
			int d = 0;
			int D = data.size()/10;
			for(BetaTrainingExample ex : data) {
				if((d % D) == D-1) {
					System.out.println("  "+((d/D)+1)+"0% complete");
				}
				double beta = this.predictBeta(ex.hiddenLayer);
				double gradient = getAndUpdateGradient(beta, ex); // also sets gradient vector
				if(gradient != 0) {
					System.out.println("Updating");
				}
				this.bias += gradient;
				this.weights.addi(this.gradientVector);
				d++;
			}
		}		
		try {
			this.save(new File(weightsFile));
		}
		catch(Exception e) {
			System.err.println("Unable to save weights file");
		}
	}
	

	private double getAndUpdateGradient(double beta, BetaTrainingExample ex) {
		// Get
		/*
		int k = ex.getNumberInBeam(beta);
		double gradient = k - ex.goldRank;
		gradient *= beta*(1.0-beta);
		*/
		double gradient = ex.targetBeta - beta;
		
		// Update
		if(gradient != 0) {
			System.out.println("Updating");
		}
		gradient *= this.eta;
		//this.gradientVector = gradientVector.muli(gradient).muli(ex.hiddenLayer);
		ex.hiddenLayer.muli(gradient, this.gradientVector);
		this.weights.addi(this.gradientVector);
		return gradient;
	}


	private ArrayList<BetaTrainingExample> getTrainingData(
			Collection<Sentence> sentences) {
		ArrayList<BetaTrainingExample> data = new ArrayList<BetaTrainingExample>();
		int d = 0;
		System.out.println("Starting to tag "+sentences.size()+" sentences of training data.");
		for(Sentence sentence : sentences) {
			if((d % sentences.size()/10) == (sentences.size()/10) -1) {
				System.out.println("  "+((10*d)/sentences.size())+"0% complete");
			}
			for(int i=0; i<sentence.length(); i++) {
				BetaTrainingExample ex = this.getContext(sentence, i);
				if(ex.goldProb != -1.0) {
					data.add(ex);
				}
			}
			d++;
		}
		return data;
	}

	public double[] getBetas(Sentence sentence) {
		double[] betas = new double[sentence.length()];
		for(int i=0; i<betas.length; i++) {
			betas[i] = predictBeta(sentence, i);
		}
		return betas;
	}
	
	public double predictBeta(Sentence sentence, int i) {	
		return this.predictBeta(this.getHiddenLayer(sentence, i));
	}
	
	private DoubleMatrix getHiddenLayer(Sentence sentence, int i) {
		return this.tagger.getHiddenLayerOutput(sentence, i);	
	}

	private double predictBeta(DoubleMatrix hiddenLayer) {	
		return this.sigmoid(hiddenLayer);
	}
	
	/**
	 * Calculates sigmoid function over input, including bias term
	 */
	private double sigmoid(DoubleMatrix input) {
		double t = this.bias + this.weights.mul(input).sum();
		return 1.0/(1.0+Math.exp(-t));
	}
	
	private BetaTrainingExample getContext(Sentence sentence, int i) {
		return new BetaTrainingExample(this.getHiddenLayer(sentence, i), 
				this.tagger.getCategoryIndex(sentence.get(i).getCategory()),
				this.tagger.predict(sentence, i));
	}
	
	public void save(File weightsFile) throws FileNotFoundException {
		PrintWriter pw = new PrintWriter(weightsFile);
		pw.println(bias);
		pw.println(this.weights.length);
		for(int w=0; w<this.weights.length; w++) {
			pw.println(this.weights.get(w));
		}
		pw.close();
	}
	
	public static BetaLSSupertagger loadForTagging(LSSupertagger tagger, File weightsFile) throws FileNotFoundException {
		Scanner sc = new Scanner(weightsFile);
		double b = Double.parseDouble(sc.nextLine());
		int numWeights = Integer.parseInt(sc.nextLine());
		BetaLSSupertagger betaTagger = new BetaLSSupertagger(tagger);
		betaTagger.bias = b;
		for(int w=0; w<numWeights; w++) {
			betaTagger.weights.put(w, Double.parseDouble(sc.nextLine()));
		}
		sc.close();
		return betaTagger;
	}
}

class BetaTrainingExample {
	
	DoubleMatrix hiddenLayer;
	int goldIndex, goldRank;
	double goldProb;
	double targetBeta;
	TagProb[] taggerProbs;
	
	public BetaTrainingExample(DoubleMatrix features, int goldTagIndex, DoubleMatrix prediction) {
		this.hiddenLayer = features;
		this.goldIndex = goldTagIndex;
		this.taggerProbs = new TagProb[prediction.length];
		this.goldProb = -1.0;
		for(int i=0; i<this.taggerProbs.length; i++) {
			taggerProbs[i] = new TagProb(i, prediction.get(i));
			if(i == goldIndex) {
				this.goldProb = taggerProbs[i].prob;
			}
		}
		Arrays.sort(taggerProbs);
		this.goldRank = -1;
		for(int i=0; i<taggerProbs.length; i++) {
			if(taggerProbs[i].index == this.goldIndex) {
				this.goldRank = i+1;
				this.targetBeta = this.goldProb/this.taggerProbs[0].prob;
				if(i <this.taggerProbs.length-1) {
					// TODO: use different mean
					double goalProb = (this.goldProb + this.taggerProbs[i+1].prob)/2.0;
					this.targetBeta = goalProb/this.taggerProbs[0].prob;
				}
				break;
			}
		}
	}

	public int getNumberInBeam(double beta) {
		int k = 0;
		double cutoff = this.taggerProbs[0].prob * beta;
		for(TagProb tp : this.taggerProbs) {
			if(tp.prob < cutoff) {
				break;
			}
			k++;
		}
		return k;
	}
	
}

class TagProb implements Comparable<TagProb> {

	double prob;
	int index;
	
	public TagProb(int i, double p) {
		this.index = i;
		this.prob = p;
	}
	
	@Override
	public int compareTo(TagProb o) {
		return (int) Math.signum(o.prob - this.prob);
	}
	
	public String toString() {
		return "("+this.index+", "+this.prob+")";
	}
}

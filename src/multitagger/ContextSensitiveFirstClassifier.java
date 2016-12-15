package multitagger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import multitagger.layers.ContextSensitiveFirstClassifierLookupLayer;
import multitagger.layers.LogisticLayer;
import neuralnet.deprecated.LinearLayer;

import org.jblas.DoubleMatrix;

import supertagger.lewissteedman.LSSupertagger;
import supertagger.lsbeta.FirstClassifierContextLookupLayer;
import supertagger.lsbeta.FirstMultitaggerClassifier;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.LogisticRegressionNN;
import supertagger.nn.StringEmbeddings;

public class ContextSensitiveFirstClassifier extends LogisticRegressionNN {
		
		
	
	//private LogisticRegressionNN net;
	
	private ContextSensitiveFirstClassifierLookupLayer myLookup;
	
	public ContextSensitiveFirstClassifier(int numHiddenNodes, int numCatsToLookAt, int contextWindowSize,  
			int wordEmbeddingDimension, int categoryEmbeddingDimension,  int surroundingContextDepth,
			StringEmbeddings wordEmbeddings, StringEmbeddings categoryEmbeddings, int mappingDimension) {
		this.myLookup = new ContextSensitiveFirstClassifierLookupLayer(
				numCatsToLookAt, contextWindowSize, wordEmbeddingDimension, categoryEmbeddingDimension,
				surroundingContextDepth, wordEmbeddings, categoryEmbeddings, mappingDimension);
		this.firstLinearLayer = new LinearLayer(this.myLookup.getOutputDimension(), numHiddenNodes);
		this.outputLayer = new LogisticLayer();
		this.costGrad = new DoubleMatrix(this.firstLinearLayer.getNumberOfNodes());
		//super(null, numHiddenNodes);
	}

	public double hardPredict(MultitaggerTrainingSentence sentence, int i) {
		return this.predict(myLookup.getOutput(sentence, i))  > 0.5 ? 1.0 : 0.0;
	}
	
	public double hardPredict(DoubleMatrix input) {
		return this.predict(input) < 0.5 ? 0.0 : 1.0;
	}
	
	public double predict(MultitaggerTrainingSentence sentence, int i) {
		return this.predict(myLookup.getOutput(sentence, i));
	}

	public double predict(DoubleMatrix input) {
		return this.outputLayer.output(firstLinearLayer.output(input));
	}


	/**
	 * Adjusts the network parameters based on the gradient of the cross-entropy loss function
	 * for probabilities assigned by the network (based on the specified active variables) and
	 * the target correct label.
	 *  
	 * The gradient is back-propagated through the network, and the parameters at each layer
	 * are adjusted using a step scaled by the learning rate.
	 * @return 
	 */
	public double trainOn(MultitaggerTrainingSentence sentence, int i, 
			double correctLabel, double learningRate) {
		DoubleMatrix lookupOutput = this.myLookup.getOutput(sentence, i);
		DoubleMatrix linearOutput = this.firstLinearLayer.output(lookupOutput);
		double prediction = this.outputLayer.output(linearOutput);
		double loss = this.outputLayer.calculateGradientOfCostFunction(prediction, correctLabel);
		for(int j=0; j<this.costGrad.length; j++) {
			this.costGrad.put(j, loss*learningRate);
		}
		// Update gradient w.r.t. linear layer
		//linearOutput.muli(loss, this.costGrad);		
		DoubleMatrix linGrad = 
				this.firstLinearLayer.calculateGradientWithRespectToInput(lookupOutput, this.costGrad);
		this.firstLinearLayer.updateParameters(this.costGrad, 1.0);
		this.myLookup.updateParameters(linGrad, 1.0);
		//this.firstLinearLayer.updateParameters(this.costGrad, learningRate);
		//this.myLookup.updateParameters(linGrad, learningRate); // (learning rate already added to cost grad above)
		double pred = (prediction > 0.5) ? 1.0 : 0.0;
		return pred == correctLabel ? 1.0 : 0.0;
	}
	

	public void save(File dir) {
		dir.mkdirs();
		File lookupWeights = new File(dir.getPath()+File.separator+"lookup");
		lookupWeights.mkdir();
		this.myLookup.saveWeightsToFile(lookupWeights);
		File linearWeights = new File(dir.getPath()+File.separator+"linear");
		this.firstLinearLayer.saveWeightsToFile(linearWeights);
		try {
			File config = new File(dir.getPath()+File.separator+"config");
			this.writeConfig(config);
		}
		catch(Exception e) {
			System.err.println("Failed to save multitagger.");
			e.printStackTrace();
		}
	}

	private void writeConfig(File config) throws FileNotFoundException {
		/*
		PrintWriter pw = new PrintWriter(config);
		pw.println("contextWindowSize="+this.CONTEXT_WINDOW_SIZE);
		pw.println("featuresPerVariable="+this.lookupLayer.getNumberOfFeaturesPerInput());
		pw.println("embeddingDimension="+this.lookupLayer.getEmbeddingDimension());
		pw.close();
		*/
	}

	public void loadWeights(File dir) {
		File lookupWeights = new File(dir.getPath()+File.separator+"lookup");
		this.myLookup.loadWeightsFromFile(lookupWeights);
		File linearWeights = new File(dir.getPath()+File.separator+"linear");
		this.firstLinearLayer.loadWeightsFromFile(linearWeights);
	}

	public static FirstMultitaggerClassifier load(File dir) {
		/*
		try {
			File categoriesFile = new File(dir.getPath()+File.separator+"categories");
			ArrayList<String> categories = readCategories(categoriesFile);
			File config = new File(dir.getPath()+File.separator+"config");
			FirstMultitaggerClassifier tagger = readFromConfig(config, categories);
			tagger.loadWeights(dir);
			return tagger;
		}
		catch(Exception e) {
			System.err.println("Failed to save supertagger.");
			e.printStackTrace();
			return null;
		}
		*/
		return null;
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

}

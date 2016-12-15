package supertagger.lewissteedman;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;


/**
 * Development script for neural-net supertagger.
 * 
 * @author ramusa2
 *
 */
public class NNSupertaggerDevel {

	public static void main(String[] args) throws Exception {
		//testSoftMax();
		//testSupertaggerNet();
		//testLoadEmbeddings();
		trainActualTagger();
	}

	private static void trainActualTagger() throws Exception {
		ArrayList<String> catList = getCatList();
		Collection<Sentence> data = CCGbankReader.getCCGbankData(2,  21, "data/CCGbank/AUTO");
		//Collection<Sentence> data = getSampleData();
		WordEmbeddings embeddings = new WordEmbeddings(50, false);
		embeddings.loadEmbeddingsFromFile(new File("embeddings/turian/embeddings-scaled.EMBEDDING_SIZE=50.txt"));
		System.out.println("Loaded embeddings.");
		System.out.println("Embedding for *UNKNOWN*:\n\t"+embeddings.lookup("*UNKNOWN*"));
		//System.out.println(grammar.getNumberOfCategories());
		LSSupertagger net = new LSSupertagger(7, 60, 50, catList);
		net.initializeLookupFeatures(embeddings);
		double learningRate = 0.01;
		net.train(data, 15, learningRate);
	}

	private static void testLoadEmbeddings() throws Exception {
		//Grammar grammar = Grammar.load(new File("grammar"));
		ArrayList<String> catList = getCatList();
		ArrayList<Sentence> data = getSampleData();
		
		
		WordEmbeddings embeddings = new WordEmbeddings(50, false);
		embeddings.loadEmbeddingsFromFile(new File("embeddings/turian/embeddings-scaled.EMBEDDING_SIZE=50.txt"));
		System.out.println("Loaded embeddings.");
		System.out.println("Embedding for *UNKNOWN*:\n\t"+embeddings.lookup("*UNKNOWN*"));
		//System.out.println(grammar.getNumberOfCategories());
		LSSupertagger net = new LSSupertagger(7, 60, 50, catList);
		net.initializeLookupFeatures(embeddings);
		System.out.println("Created net.");
		double learningRate = 0.01;
		net.train(data, 3, learningRate);
		net.save(new File("saved_st"));

		System.out.println("Before loading:");
		for(Sentence sen : data) {
			System.out.println(net.tagSentence(sen).toStringWithBeam(0.01));
		}
		
		//SupertaggerNetNoAdditionalHiddenLayer net = new SupertaggerNetNoAdditionalHiddenLayer(7, 60, 50, catList);
		net = new LSSupertagger(7, 60, 50, catList);
		net.loadWeights(new File("saved_st"));
		
		System.out.println("After loading:");
		for(Sentence sen : data) {
			System.out.println(net.tagSentence(sen).toStringWithBeam(0.01));
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

	private static ArrayList<Sentence> getSampleData() {
		ArrayList<Sentence> data = new ArrayList<Sentence>();
		data.add(new Sentence("I_NULL_NP row_NULL_S[dcl]\\NP ._NULL_."));
		data.add(new Sentence("I_NULL_NP row_NULL_(S[dcl]\\NP)/NP the_NULL_NP[nb]/N boat_NULL_N ._NULL_."));
		return data;
	}

	/*
	private static void testSupertaggerNet() {
		int inputs = 3;
		int features = 2;
		int hidden = 4;
		int labels = 3;
		//Grammar grammar = Grammar.load(new File("grammar"));
		//SupertaggerNetNoAdditionalHiddenLayer net = new SupertaggerNetNoAdditionalHiddenLayer(features, labels, grammar);
		int[] active = new int[]{0, 1, 1};
		int correctLabel = 2;
		double learningRate = 0.01;
		for(int i=0; i<10; i++) {
			DoubleMatrix preds = net.predict(active);
			for(int l=0; l<labels; l++) {
				System.out.println("P(label = "+l+"): "+preds.get(l));
			}
			System.out.println();
			net.trainOn(active, correctLabel, learningRate);
		}
	}
	*/
	
	/*
	private static void testSupertaggerNet() {
		int numInputs = 3;
		int numFeaturesPerInput = 4;
		int numHiddenNodes = 5;
		int numOutputs = 3;
		double learningRate = 0.01;
		LookupTableLayer lookup = new LookupTableLayer(numInputs, numFeaturesPerInput);
		LinearLayer linear = new LinearLayer(lookup.getNumberOfOutputs(), numHiddenNodes);
		SoftMaxOutputLayer softmax = new SoftMaxOutputLayer(numHiddenNodes, numOutputs);
		DoubleMatrix target = new DoubleMatrix(numOutputs);
		target.put(0, 1.0);
		
		//DoubleMatrix input = DoubleMatrix.ones(numInputs);
		//input.put(0, 0.0);
		int[] input = new int[]{0, 1, 1};
		
		for(int R=0; R<8; R++) {
			DoubleMatrix lookupOutput = lookup.getOutput(input);
			DoubleMatrix hidput = linear.getOutput(lookupOutput);
			DoubleMatrix output = softmax.getOutput(hidput);
			for(int i=0; i<numOutputs; i++) {
				System.out.println("Prob of label "+i+": "+output.get(i));
			}
			DoubleMatrix grad = softmax.getFinalGradient(target);
			for(int i=0; i<numOutputs; i++) {
				System.out.println("Cost gradient for label "+i+": "+grad.get(i));
			}
			//DoubleMatrix hidCost = softmax.getCost(grad);
			softmax.updateParameters(grad, learningRate);
			DoubleMatrix hidGrad = linear.getCost(grad);
			linear.updateParameters(hidGrad, learningRate);
			//hidden.updateParameters(hidCost);
			//hidden.updateParameters(grad);
			lookup.updateParameters(hidGrad, learningRate);
		}
		System.out.println();
	}

	private static void testSoftMax() {
		int in = 5;
		int out = 3;
		double learningRate = 0.01;
		SoftMaxOutputLayer softmax = new SoftMaxOutputLayer(in, out);
		DoubleMatrix input = DoubleMatrix.ones(in);
		for(int R=0; R<5; R++) {
			DoubleMatrix output = softmax.getOutput(input);
			for(int i=0; i<out; i++) {
				System.out.println("Prob of label "+i+": "+output.get(i));
			}
			DoubleMatrix target = new DoubleMatrix(out);
			target.put(0, 1.0);
			DoubleMatrix grad = softmax.getFinalGradient(target);
			for(int i=0; i<out; i++) {
				System.out.println("Grad for label "+i+": "+grad.get(i));
			}
			softmax.updateParameters(grad, learningRate);
		}
		System.out.println("Finished.");
	}
	*/

}

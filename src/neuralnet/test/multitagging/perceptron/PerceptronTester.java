package neuralnet.test.multitagging.perceptron;

import java.io.File;
import java.util.ArrayList;

import neuralnet.MulticlassNeuralNet;
import neuralnet.Perceptron;
import neuralnet.SingleOutputNeuralNet;
import neuralnet.hidden.HiddenLayer;
import neuralnet.input.ConcatenatedInputLayer;
import neuralnet.input.InputLayer;
import neuralnet.input.SimpleInputLayer;
import neuralnet.lossfunctions.NegativeLogLikelihood;
import neuralnet.lossfunctions.ProbabilisticHingeLoss;
import neuralnet.output.MulticlassOutputLayer;
import neuralnet.output.ProbabilisticBinaryOutputLayer;
import neuralnet.output.SingleValueOutputLayer;
import neuralnet.simplelayers.LinearLayer;
import neuralnet.test.multitagging.MultitaggerExample;
import neuralnet.test.multitagging.input.CategoryEmbeddingContextExtractor;
import neuralnet.test.multitagging.input.LogProbabilityContextExtractor;
import neuralnet.test.multitagging.input.ProbabilityContextExtractor;
import neuralnet.test.multitagging.input.SentenceLengthContextExtractor;
import neuralnet.test.multitagging.input.StringEmbeddingContextExtractor;
import neuralnet.test.multitagging.input.WordEmbeddingContextExtractor;
import neuralnet.transferfunctions.HardTanh;
import neuralnet.transferfunctions.Logistic;
import neuralnet.transferfunctions.SoftMax;
import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.nn.StringEmbeddings;
import util.Util;

public class PerceptronTester {

	protected static int trainIters = 10;

	protected static double learningRate = 0.05;

	protected static int CATEGORY_DIM = 15;
	protected static int WORD_DIM = 50; 
	protected static int SAVED_CATEGORY_DIM = 421; 
	protected static int PROB_DIM = 5;
	protected static boolean STDZE_PROBS = true;

	protected static Perceptron<MultitaggerExample> net;
	
	protected static ArrayList<MultitaggerExample> trainData;

	protected static ArrayList<MultitaggerExample> testData;

	public static void main(String[] args) {
		InputLayer<MultitaggerExample> input;
		//input = getWordEmbeddingInputLayer();
		//input = getCategoryEmbeddingInputLayer();
		//input = getLogProbabilityInputLayer(STDZE_PROBS);
		
		input = getLearnedViterbiCategoryEmbeddingLayer();

		PROB_DIM = 1;
		STDZE_PROBS = false;

		//input = getProbabilityAndLengthInputLayer();

		//input = getProbabilityInputLayer(STDZE_PROBS);
		//input = getProbabilityAndLengthInputLayer();
		//input = getConcatenatedInputVector();
		
		net = new Perceptron<MultitaggerExample>(input);


		//trainData = getMultitaggedData("multitagger_training_data/cv_wsj2-2_k=20.supertagged.ser");
		trainData = getMultitaggedData("multitagger_training_data/cv_wsj2-21_k=20.supertagged.ser");
		testData = getMultitaggedData("multitagger_training_data/wsj0_k=20.supertagged.ser");
		
		if(STDZE_PROBS) {
			net.whitenFeatures(trainData);
		}

		train();
		test();
	}
	private static InputLayer<MultitaggerExample> getLearnedViterbiCategoryEmbeddingLayer() {
		StringEmbeddings embeddings = Util.loadLearnedCategoryEmbeddings100();
		//StringEmbeddings embeddings = Util.loadWord2VecCategoryEmbeddings();
		StringEmbeddingContextExtractor extractor = new CategoryEmbeddingContextExtractor(SAVED_CATEGORY_DIM, embeddings, 0);
		return new SimpleInputLayer<MultitaggerExample>(extractor, false);
	}
	private static void train() {		
		for(int T=1; T<=trainIters; T++) {
			System.out.println("Starting iteration "+T);
			for(MultitaggerExample ex : trainData) {
				double target = ex.getItem().isCorrect() ? 1.0 : -1.0;
				net.trainOn(ex, target, learningRate);
			}
		}
	}

	private static void test() {
		double total = 0.0;
		double correct = 0.0;
		//for(MultitaggerExample ex : trainData) {
		for(MultitaggerExample ex : testData) {
			boolean pred;
			pred = net.hardPredict(ex) == 1.0 ? true : false;
			total++;
			if(pred == ex.getItem().isCorrect()) {
				correct++;
			}
		}
		System.out.println("Accuracy on test set: "+(correct/total));
	}

	public static ArrayList<MultitaggerExample> getMultitaggedData(String filePath) {
		try {
			MultitaggerTrainingData data = MultitaggerTrainingData.loadMultitaggerTrainingData(new File(filePath));
			return MultitaggerExample.getExamples(data);
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println("Could not load multitagger training data.");
			return null;
		}
	}

	private static InputLayer<MultitaggerExample> getWordEmbeddingInputLayer() {
		StringEmbeddings embeddings = util.Util.loadLearnedWordEmbeddings();
		StringEmbeddingContextExtractor extractor = new WordEmbeddingContextExtractor(WORD_DIM, embeddings);
		return new SimpleInputLayer<MultitaggerExample>(extractor, false);
	}

	private static InputLayer<MultitaggerExample> getCategoryEmbeddingInputLayer(int catToGet) {
		//StringEmbeddings embeddings = Util.loadLearnedCategoryEmbeddings();
		StringEmbeddings embeddings = Util.loadWord2VecCategoryEmbeddings();
		StringEmbeddingContextExtractor extractor = new CategoryEmbeddingContextExtractor(SAVED_CATEGORY_DIM, embeddings, catToGet);
		return new SimpleInputLayer<MultitaggerExample>(extractor, false);
	}

	private static InputLayer<MultitaggerExample> getRandomInitialCategoryEmbeddingLayer(int catToGet) {
		StringEmbeddings embeddings = new StringEmbeddings();
		for(String cat : Util.getCatList()) {
			embeddings.addEmbedding(cat, Util.initializeRandomRowVector(CATEGORY_DIM));
		}
		StringEmbeddingContextExtractor extractor = new CategoryEmbeddingContextExtractor(CATEGORY_DIM, embeddings, catToGet);
		return new SimpleInputLayer<MultitaggerExample>(extractor, false);		
	}
	
	private static InputLayer<MultitaggerExample> getProbabilityInputLayer(boolean stdze) {
		return new SimpleInputLayer<MultitaggerExample>(new ProbabilityContextExtractor(PROB_DIM), stdze);
	}

	private static InputLayer<MultitaggerExample> getLogProbabilityInputLayer(
			boolean stdze) {
		return new SimpleInputLayer<MultitaggerExample>(new LogProbabilityContextExtractor(PROB_DIM), stdze);
	}

	private static InputLayer<MultitaggerExample> getConcatenatedInputVector() {
		InputLayer<MultitaggerExample> prob = new SimpleInputLayer(new ProbabilityContextExtractor(PROB_DIM), false);
		InputLayer<MultitaggerExample> stdProb = new SimpleInputLayer(new ProbabilityContextExtractor(PROB_DIM), true);
		//InputLayer<MultitaggerExample> cats = getCategoryEmbeddingInputLayer();
		InputLayer<MultitaggerExample> randCats1 = getRandomInitialCategoryEmbeddingLayer(0);
		InputLayer<MultitaggerExample> randCats2 = getRandomInitialCategoryEmbeddingLayer(1);
		InputLayer<MultitaggerExample> randCats3 = getRandomInitialCategoryEmbeddingLayer(2);
		//return new ConcatenatedInputLayer<MultitaggerExample>(prob, stdProb, cats);
		//InputLayer<MultitaggerExample> logProb = new SimpleInputLayer(new LogProbabilityContextExtractor(PROB_DIM), false);
		InputLayer<MultitaggerExample> logStdProb = new SimpleInputLayer(new LogProbabilityContextExtractor(PROB_DIM), true);

		//return new ConcatenatedInputLayer<MultitaggerExample>(prob, logStdProb /*, randCats1, randCats2, randCats3*/);
		return new ConcatenatedInputLayer<MultitaggerExample>(prob, randCats1);
	}

	private static InputLayer<MultitaggerExample> getProbabilityAndLengthInputLayer() {
		InputLayer<MultitaggerExample> prob = new SimpleInputLayer(new ProbabilityContextExtractor(PROB_DIM), false);
		InputLayer<MultitaggerExample> length = new SimpleInputLayer(new SentenceLengthContextExtractor(), true);
		return new ConcatenatedInputLayer<MultitaggerExample>(prob, length);
	}

}

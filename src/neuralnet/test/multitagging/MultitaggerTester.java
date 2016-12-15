package neuralnet.test.multitagging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.nn.StringEmbeddings;
import util.Util;
import neuralnet.MulticlassNeuralNet;
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
import neuralnet.test.multitagging.input.CategoryEmbeddingContextExtractor;
import neuralnet.test.multitagging.input.LogProbabilityContextExtractor;
import neuralnet.test.multitagging.input.ProbabilityContextExtractor;
import neuralnet.test.multitagging.input.SentenceLengthContextExtractor;
import neuralnet.test.multitagging.input.StringEmbeddingContextExtractor;
import neuralnet.test.multitagging.input.WordEmbeddingContextExtractor;
import neuralnet.transferfunctions.HardTanh;
import neuralnet.transferfunctions.Logistic;
import neuralnet.transferfunctions.SoftMax;

public class MultitaggerTester {


	protected static boolean USE_MULTICLASS = true;

	protected static int trainIters = 10;

	protected static int[] hiddenLayerSize = new int[]{50, 25};

	protected static double margin = 0.3;

	protected static double learningRate = 0.05;

	protected static int CATEGORY_DIM = 15;
	protected static int WORD_DIM = 50; 
	protected static int SAVED_CATEGORY_DIM = 50; 
	protected static int PROB_DIM = 5;
	protected static boolean STDZE_PROBS = false;

	protected static SingleOutputNeuralNet<MultitaggerExample> net;
	protected static MulticlassNeuralNet<MultitaggerExample> net2;

	protected static ArrayList<MultitaggerExample> trainData;

	protected static ArrayList<MultitaggerExample> testData;

	public static void main(String[] args) {
		InputLayer<MultitaggerExample> input;
		//input = getWordEmbeddingInputLayer();
		//input = getCategoryEmbeddingInputLayer();
		//input = getLogProbabilityInputLayer(STDZE_PROBS);

		//input = getProbabilityInputLayer(STDZE_PROBS);
		input = getProbabilityAndLengthInputLayer();
		//input = getConcatenatedInputVector();

		if(USE_MULTICLASS) {
			net2 = initializeMulticlassNet(input, hiddenLayerSize);
		}
		else {
			net = initializeNet(input, hiddenLayerSize);
		}


		//trainData = getMultitaggedData("multitagger_training_data/cv_wsj2-2_k=20.supertagged.ser");
		trainData = getMultitaggedData("multitagger_training_data/cv_wsj2-21_k=20.supertagged.ser");
		testData = getMultitaggedData("multitagger_training_data/wsj0_k=20.supertagged.ser");

		train();
		test();
	}

	private static MulticlassNeuralNet<MultitaggerExample> initializeMulticlassNet(
			InputLayer<MultitaggerExample> input, int[] hiddenLayerSizes) {		
		HiddenLayer[] layers = new HiddenLayer[hiddenLayerSizes.length];
		int inputSize = input.getOutputVectorDimension();
		for(int l=0; l<layers.length; l++) {
			int nextSize = hiddenLayerSizes[l];
			layers[l] = new HiddenLayer(new LinearLayer(inputSize, nextSize), new HardTanh(nextSize, nextSize));
			//layers[l].addRegularizer(new L2Regularizer(), 0.1);
			inputSize = nextSize;
		}
		MulticlassOutputLayer output = new MulticlassOutputLayer(inputSize, new SoftMax(2, 2), new NegativeLogLikelihood());
		return new MulticlassNeuralNet<MultitaggerExample>(input, output, layers);
	}

	private static void train() {
		if(USE_MULTICLASS) {
			//net2.standardizeFeatures(trainData);
			net2.whitenFeatures(trainData);
		}
		else {
			net.standardizeFeatures(trainData);
		}
		
		for(int T=1; T<=trainIters; T++) {
			System.out.println("Starting iteration "+T);
			for(MultitaggerExample ex : trainData) {
				if(USE_MULTICLASS) {
					int target = ex.getItem().isCorrect() ? 1 : 0;
					net2.trainOn(ex, target, learningRate);
				}
				else {
					double target = ex.getItem().isCorrect() ? 1.0 : 0.0;
					net.trainOn(ex, target, learningRate);
				}
			}
		}
	}

	private static double[] test() {
		double total = 0.0;
		double truePos = 0.0;
		double trueNeg = 0.0;
		double falsePos = 0.0;
		double falseNeg = 0.0;
		//for(MultitaggerExample ex : trainData) {
		for(MultitaggerExample ex : testData) {
			boolean pred;
			if(USE_MULTICLASS) {
				pred = net2.hardPredict(ex) == 1 ? true : false;
			}
			else {
				pred = net.predict(ex) >= 0.5 ? true : false;
			}
			total++;
			if(ex.getItem().isCorrect()) {
				if(pred) {
					truePos++;
				}
				else {
					falseNeg++;
				}
			}
			else {
				if(pred) {
					falsePos++;
				}
				else {
					trueNeg++;
				}
			}
		}
		double correct = truePos+trueNeg;
		System.out.println("Accuracy on test set: "+(correct/total));
		System.out.println(" True pos. rate: "+(truePos/total));
		System.out.println(" True neg. rate: "+(trueNeg/total));
		System.out.println("False pos. rate: "+(falsePos/total));
		System.out.println("False neg. rate: "+(falseNeg/total));
		return new double[]{total, correct, truePos, trueNeg, falsePos, falseNeg};
	}
	
	private static void test(String outFile) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(outFile));
		}
		catch(Exception e) {
			System.err.println("Failed to open output file for testing.");
		}
		double[] res = test();
		double total = res[0];
		StringBuilder str = new StringBuilder();
		String header = "Accuracy,TP%,TN%,FP%,FN%,TPCount,TNCount,FPCount,FNCount";
		str.append(res[1]/total+",");
		str.append(res[2]/total+",");
		str.append(res[3]/total+",");
		str.append(res[4]/total+",");
		str.append(res[5]/total+",");
		str.append(res[2]+",");
		str.append(res[3]+",");
		str.append(res[4]+",");
		str.append(res[5]);
		pw.println(str.toString());
		pw.println();
		pw.println("Accuracy on test set: "+(res[1]/total));
		pw.println(" True pos. rate: "+(res[2]/total));
		pw.println(" True neg. rate: "+(res[3]/total));
		pw.println("False pos. rate: "+(res[4]/total));
		pw.println("False neg. rate: "+(res[5]/total));
		if(pw != null) {
			pw.close();
		}
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

	public static SingleOutputNeuralNet<MultitaggerExample> initializeNet(InputLayer input, int... hiddenLayerSizes) {	
		HiddenLayer[] layers = new HiddenLayer[hiddenLayerSizes.length];
		int inputSize = input.getOutputVectorDimension();
		for(int l=0; l<layers.length; l++) {
			int nextSize = hiddenLayerSizes[l];
			layers[l] = new HiddenLayer(new LinearLayer(inputSize, nextSize), new HardTanh(nextSize, nextSize));
			//layers[l].addRegularizer(new L2Regularizer(), 0.1);
			inputSize = nextSize;
		}

		SingleValueOutputLayer output = new ProbabilisticBinaryOutputLayer(inputSize, new Logistic(1), new ProbabilisticHingeLoss(margin));
		return new SingleOutputNeuralNet<MultitaggerExample>(input, output, layers);
	}

}

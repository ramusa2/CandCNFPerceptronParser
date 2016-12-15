package neuralnet.test.gridsearch;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

import org.jblas.DoubleMatrix;

import neuralnet.MulticlassNeuralNet;
import neuralnet.Perceptron;
import neuralnet.hidden.HiddenLayer;
import neuralnet.input.ConcatenatedInputLayer;
import neuralnet.input.InputLayer;
import neuralnet.input.SimpleInputLayer;
import neuralnet.lossfunctions.NegativeLogLikelihood;
import neuralnet.output.MulticlassOutputLayer;
import neuralnet.simplelayers.LinearLayer;
import neuralnet.test.multitagging.MultitaggerExample;
import neuralnet.test.multitagging.input.CategoryEmbeddingContextExtractor;
import neuralnet.test.multitagging.input.ProbabilityContextExtractor;
import neuralnet.test.multitagging.input.StringEmbeddingContextExtractor;
import neuralnet.test.multitagging.input.WordEmbeddingContextExtractor;
import neuralnet.transferfunctions.HardTanh;
import neuralnet.transferfunctions.SoftMax;
import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.nn.StringEmbeddings;
import util.Util;

public class SearchNetworkArchitecture {


	protected static ArrayList<MultitaggerExample> trainData;

	protected static ArrayList<MultitaggerExample> testData;

	protected static int expID = 1;

	protected static PrintWriter pw;

	protected static int MAX_ITERS = 25;

	protected static double LEARNING_RATE = 0.05; 

	protected static int CATEGORY_EMBEDDING_DIMENSION = 25;

	protected static int WORD_EMBEDDING_DIMENSION = 50;

	protected static boolean USE_WORD_EMBEDDINGS = false;

	protected static int NUM_CATEGORY_EMBEDDINGS_TO_USE = 1;

	protected static boolean WHITEN_PROBABILITIES = true;

	protected static int NUM_PROBABILITIES_TO_USE = 5;

	protected static int TEST_INTERVAL = 5;

	protected static int NUM_NODES_IN_LAYER = 5;

	protected static boolean USE_DROPOUT = false;

	protected static boolean BALANCE_DATA = false;

	protected static double DROPOUT_RATE = 0.5;

	protected static boolean USE_CAT_ID = false;

	protected static String DIRECTORY = "./";

	protected static boolean USE_PERCEPTRON = false;


	public static void main(String[] args) throws Exception {

		//DIRECTORY = "grid_search_experiments/experiments/devexp";
		//saveArgsToFile();

		long start = System.currentTimeMillis();
		// Prep
		DIRECTORY = args[0];
		if(!DIRECTORY.endsWith(File.separator)) {
			DIRECTORY += File.separator;
		}
		loadFromDirectory();
		if(!DIRECTORY.endsWith(File.separator)) {
			DIRECTORY += File.separator;
		}
		pw = new PrintWriter(new File(DIRECTORY+"log"));
		printArgs();
		loadData();

		// Actual experiment
		if(!USE_PERCEPTRON) {
			InputLayer<MultitaggerExample> input = getInputLayer();
			MulticlassNeuralNet<MultitaggerExample> net = initializeMulticlassNet(input, NUM_NODES_IN_LAYER);
			println("Running experiments with "+input.getOutputVectorDimension()+" input features and "+NUM_NODES_IN_LAYER+" hidden nodes.");
			runExperiment(net);
		}
		/*

		String dir = args[0];
		readArgsFromFile(dir+"/config");
		loadData();


		setArgs(args);


		int probDim = 5;


		pw = new PrintWriter(new File("overnight_simple_prob_input_grid_seach.csv"));
		println("Model,Accuracy on first decision");
		double always = (countCorrect(testData))/testData.size();
		println();
		println("Baseline:");
		println("Always stop,"+always);
		println();
		println("Perceptron:");
		InputLayer<MultitaggerExample> probInput = getProbabilityInputLayer(probDim, false);
		InputLayer<MultitaggerExample> probInputStd = getProbabilityInputLayer(probDim, true);
		InputLayer<MultitaggerExample> catEmbed = getCategoryEmbeddingInputLayer(0);
		InputLayer<MultitaggerExample> catID = getCategoryIDInputLayer();
		InputLayer<MultitaggerExample> probAndCatID = new ConcatenatedInputLayer<MultitaggerExample>(probInput, catID);
		InputLayer<MultitaggerExample> probAndCatEmbed = new ConcatenatedInputLayer<MultitaggerExample>(probInput, catEmbed);
		InputLayer<MultitaggerExample> probStdAndCatID = new ConcatenatedInputLayer<MultitaggerExample>(probInputStd, catID);
		InputLayer<MultitaggerExample> probStdAndCatEmbed = new ConcatenatedInputLayer<MultitaggerExample>(probInputStd, catEmbed);


		System.out.println("Got input layers");
		// Perceptron baselines

		Perceptron probPrcp = new Perceptron(probInput);
		Perceptron probPrcpStd = new Perceptron(probInputStd);
		Perceptron catEmbedPrcp = new Perceptron(catEmbed);
		Perceptron catIDPrcp = new Perceptron(catID);


		int[] smallSizes = new int[]{3};
		println("Probability alone:");
		for(int s=0; s<smallSizes.length; s++) {
			int size = smallSizes[s];
			MulticlassNeuralNet<MultitaggerExample> probNet = initializeMulticlassNet(probInput, size);
			MulticlassNeuralNet<MultitaggerExample> probStdNet = initializeMulticlassNet(probInputStd, size);
			println("Prob5,"+score(probNet));
			println("Prob5Std,"+score(probStdNet));
		}

		int[] netSizes = new int[]{5, 15, 50, 100};
		int[] hiddenNetSizes = new int[]{5, 15, 50};

		smallSizes = new int[]{3, 5, 10};
		println("Cat ID (plus prob), single hidden layer");
		for(int s=0; s<smallSizes.length; s++) {
			int size = smallSizes[s];
			println(size+" nodes in layer:");
			MulticlassNeuralNet<MultitaggerExample> probNet = initializeMulticlassNet(probAndCatID , size);
			MulticlassNeuralNet<MultitaggerExample> probStdNet = initializeMulticlassNet(probStdAndCatID , size);
			println("Prob5+CatID,"+score(probNet));
			println("Prob5Std+CatID,"+score(probStdNet));
		}

		int[] largeSizes = new int[]{5, 15, 50, 100};
		println("Cat embedding (plus prob), single hidden layer");
		for(int s=0; s<largeSizes.length; s++) {
			int size = largeSizes[s];
			println(size+" nodes in layer:");
			MulticlassNeuralNet<MultitaggerExample> probNet = initializeMulticlassNet(probAndCatEmbed , size);
			MulticlassNeuralNet<MultitaggerExample> probStdNet = initializeMulticlassNet(probStdAndCatEmbed , size);
			println("Prob5+CatEmbed,"+score(probNet));
			println("Prob5Std+CatEmbed,"+score(probStdNet));
		}


		println("Prob5,"+score(probPrcp));
		println("Prob5Std,"+score(probPrcpStd));
		println("CatID,"+score(catIDPrcp));
		println("CatEmbed,"+score(catEmbedPrcp));		

		//println(","+score());


		// Search net size, one layer
		println();
		println();
		println("Single-hidden-layer nets:");
		for(int s=0; s<netSizes.length; s++) {
			int size = netSizes[s];
			println(size+" nodes:");

			MulticlassNeuralNet<MultitaggerExample> probNet = initializeMulticlassNet(probInput, size);
			MulticlassNeuralNet<MultitaggerExample> probStdNet = initializeMulticlassNet(probInputStd, size);\
			println("Prob5,"+score(probNet));
			println("Prob5Std,"+score(probStdNet));

			MulticlassNeuralNet<MultitaggerExample> catEmbedNet = initializeMulticlassNet(catEmbed, size);
			MulticlassNeuralNet<MultitaggerExample> catIDNet = initializeMulticlassNet(catID, size);
			println("CatID,"+score(catIDNet));
			println("CatEmbed,"+score(catEmbedNet));	
		}


		// Search net size, two layers

		println();
		println("Double-hidden-layer nets:");
		for(int s=0; s<netSizes.length; s++) {
			int size1 = netSizes[s];
			for(int s2=0; s2<hiddenNetSizes.length; s2++) {
				int size2 = netSizes[s2];
				println(size1+"/"+size2+" nodes:");
				MulticlassNeuralNet<MultitaggerExample> probNet = initializeMulticlassNet(probInput, size1, size2);
				//MulticlassNeuralNet<MultitaggerExample> probStdNet = initializeMulticlassNet(probInputStd, size1, size2);
				MulticlassNeuralNet<MultitaggerExample> catEmbedNet = initializeMulticlassNet(catEmbed, size1, size2);
				MulticlassNeuralNet<MultitaggerExample> catIDNet = initializeMulticlassNet(catID, size1, size2);
				println("Prob5,"+score(probNet));
				//println("Prob5Std,"+score(probStdNet));
				println("CatID,"+score(catIDNet));
				println("CatEmbed,"+score(catEmbedNet));	
			}
		}
		 */

		long end = System.currentTimeMillis();
		long millis = end - start;
		long second = (millis / 1000) % 60;
		long minute = (millis / (1000 * 60)) % 60;
		long hour = (millis / (1000 * 60 * 60)) % 24;

		String time = String.format("%02d:%02d:%02d", hour, minute, second);
		println("Running time: "+time);

		summarize(time);
		if(pw != null) {
			pw.close();
		}
	}

	private static void summarize(String time) throws Exception {
		PrintWriter spw = new PrintWriter(new File(DIRECTORY+"results.txt"));
		File[] files = (new File(DIRECTORY).listFiles(new FilenameFilter() {
			public boolean accept(File file, String name) {
				return name.contains("iter");
			}
		}));

		Arrays.sort(files, new Comparator<File>() {
			public int compare(File f1, File f2) {
				Integer i1 = Integer.parseInt(f1.getName().split("\\.")[0].substring(4));
				Integer i2 = Integer.parseInt(f1.getName().split("\\.")[0].substring(4));
				return i1.compareTo(i2);
			}
		});
		spw.println("Iteration,Accuracy,TP%,TN%,FP%,FN%,TPCount,TNCount,FPCount,FNCount");
		for(File file : files) {
			Scanner sc = new Scanner(file);
			sc.nextLine();
			spw.println(file.getName().split("\\.")[0].substring(4)+","+sc.nextLine());
			sc.close();
		}
		spw.println();
		spw.println("Total running time:,"+time);
		spw.println();
		spw.println("Arguments:");
		Scanner sc = new Scanner(new File(DIRECTORY+"config"));
		while(sc.hasNextLine()) {
			spw.println(sc.nextLine());
		}
		sc.close();
		spw.close();
	}

	private static void runExperiment(
			MulticlassNeuralNet<MultitaggerExample> net) {
		// Train
		for(int T=1; T<=MAX_ITERS; T++) {
			System.out.println("Starting iteration "+T);
			doTrainIteration(net);			
			if(T%TEST_INTERVAL == 0) {
				System.out.println("Evaluating after iteration "+T);
				doEvaluation(net, DIRECTORY+"iter"+T+".results.txt");
			}
		}
	}

	private static void doEvaluation(
			MulticlassNeuralNet<MultitaggerExample> net, String outFile) {
		double[] res = test(net);
		try {
			PrintWriter pw = new PrintWriter(new File(outFile));
			double total = res[0];
			StringBuilder str = new StringBuilder();
			String header = "Accuracy,TP%,TN%,FP%,FN%,TPCount,TNCount,FPCount,FNCount";
			str.append(header+"\n");
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
			pw.close();
		}
		catch(Exception e) {
			System.err.println("Failed to write evaluation results to: "+outFile);
		}
	}

	private static void doTrainIteration(
			MulticlassNeuralNet<MultitaggerExample> net) {
		for(MultitaggerExample ex : trainData) {
			int target = ex.getItem().isCorrect() ? 1 : 0;
			net.trainOn(ex, target, LEARNING_RATE);
		}
	}

	private static void loadData() {
		trainData = getMultitaggedData("multitagger_training_data/cv_wsj2-21_k=20.supertagged.ser");
		testData = getMultitaggedData("multitagger_training_data/wsj0_k=20.supertagged.ser");
		if(BALANCE_DATA) {
			trainData = balanceData(trainData);
		}
		System.out.println("Loaded data.");
	}

	private static ArrayList<MultitaggerExample> balanceData(
			ArrayList<MultitaggerExample> data) {
		ArrayList<MultitaggerExample> balanced = new ArrayList<MultitaggerExample>();
		int correct = 0;
		int incorrect = 0;
		for(MultitaggerExample ex : data) {
			if(ex.getItem().isCorrect()) {
				correct++;
			}
			else {
				incorrect++;
			}
			balanced.add(ex);
		}
		boolean shouldContinue = correct == incorrect;
		while(shouldContinue) {
			for(MultitaggerExample ex : data) {
				if(incorrect < correct) {
					if(!ex.getItem().isCorrect()) {
						balanced.add(ex);
						incorrect++;
					}
				}
				else if(correct > incorrect) {
					if(ex.getItem().isCorrect()) {
						balanced.add(ex);
						correct++;
					}
				}
				else {
					shouldContinue = false;
					break;
				}
			}
		}
		Collections.shuffle(balanced);
		return balanced;
	}

	public static void readArgsFromFile(String filename) {
		try {
			Scanner sc = new Scanner(new File(filename));
			ArrayList<String> list = new ArrayList<String>();
			while(sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if(!line.isEmpty()) {
					list.add(line);
				}
			}
			sc.close();
			setArgs(list.toArray(new String[list.size()]));
		}
		catch(Exception e) {
			System.err.println("Failed to parse arguments from file: "+filename);
			e.printStackTrace();
		}
	}
	public static void loadFromDirectory() {
		readArgsFromFile(DIRECTORY+"config");
	}

	public static void saveArgsToFile() {
		String out = DIRECTORY;
		if(!out.endsWith("/")) {
			out += "/";
		}
		try {
			(new File(out)).mkdirs();
			PrintWriter pw = new PrintWriter(new File(out+"config"));
			pw.println("directory="+DIRECTORY);
			pw.println("maxIterations="+MAX_ITERS);
			pw.println("testInterval="+TEST_INTERVAL);
			pw.println("balanceData="+BALANCE_DATA);
			pw.println("useDropout="+USE_DROPOUT);
			pw.println("dropoutRate="+DROPOUT_RATE);
			pw.println("learningRate="+LEARNING_RATE);
			pw.println("usePerceptron="+USE_PERCEPTRON);
			pw.println("numNodesInHiddenLayer="+NUM_NODES_IN_LAYER);
			pw.println("useCategoryID="+USE_CAT_ID);
			pw.println("numProbabilities="+NUM_PROBABILITIES_TO_USE);
			pw.println("whitenProbabilities="+WHITEN_PROBABILITIES);
			pw.println("numCategoryEmbeddings="+NUM_CATEGORY_EMBEDDINGS_TO_USE);
			pw.println("categoryEmbeddingDimension="+CATEGORY_EMBEDDING_DIMENSION);
			pw.println("wordEmbeddingDimension="+WORD_EMBEDDING_DIMENSION);
			pw.println("useWordEmbeddings="+USE_WORD_EMBEDDINGS);
			pw.close();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	private static void setArgs(String[] args) {
		for(String arg : args) {
			String[] keyValue = arg.trim().split("=");
			if(keyValue.length != 2) {
				System.out.println("Failed to parse arg: "+arg);
				continue;
			}
			String key = keyValue[0];
			String value = keyValue[1];
			if(key.equalsIgnoreCase("maxIterations")) {
				MAX_ITERS = Integer.parseInt(value);
			}
			else if(key.equalsIgnoreCase("learningRate")) {
				LEARNING_RATE = Double.parseDouble(value);
			}
			else if(key.equalsIgnoreCase("categoryEmbeddingDimension")) {
				CATEGORY_EMBEDDING_DIMENSION = Integer.parseInt(value);
			}
			else if(key.equalsIgnoreCase("wordEmbeddingDimension")) {
				WORD_EMBEDDING_DIMENSION = Integer.parseInt(value);
			}
			else if(key.equalsIgnoreCase("useWordEmbeddings")) {
				USE_WORD_EMBEDDINGS = Boolean.parseBoolean(value);
			}
			else if(key.equalsIgnoreCase("numCategoryEmbeddings")) {
				NUM_CATEGORY_EMBEDDINGS_TO_USE = Integer.parseInt(value);
			}
			else if(key.equalsIgnoreCase("whitenProbabilities")) {
				WHITEN_PROBABILITIES = Boolean.parseBoolean(value);
			}
			else if(key.equalsIgnoreCase("numProbabilities")) {
				NUM_PROBABILITIES_TO_USE = Integer.parseInt(value);
			}
			else if(key.equalsIgnoreCase("testInterval")) {
				TEST_INTERVAL = Integer.parseInt(value);
			}
			else if(key.equalsIgnoreCase("numNodesInHiddenLayer")) {
				NUM_NODES_IN_LAYER = Integer.parseInt(value);
			}
			else if(key.equalsIgnoreCase("balanceData")) {
				BALANCE_DATA = Boolean.parseBoolean(value);
			}
			else if(key.equalsIgnoreCase("useDropout")) {
				USE_DROPOUT = Boolean.parseBoolean(value);
			}
			else if(key.equalsIgnoreCase("dropoutRate")) {
				DROPOUT_RATE = Double.parseDouble(value);
			}
			else if(key.equalsIgnoreCase("useCategoryID")) {
				USE_CAT_ID = Boolean.parseBoolean(value);
			}
			else if(key.equalsIgnoreCase("usePerceptron")) {
				USE_PERCEPTRON = Boolean.parseBoolean(value);
			}
			else if(key.equalsIgnoreCase("directory")) {
				DIRECTORY = value;
			}
			else {
				System.out.println("Failed to parse unrecognized arg: "+keyValue);
			}
		}
	}

	public static void printArgs() {
		println("directory="+DIRECTORY);
		println("maxIterations="+MAX_ITERS);
		println("testInterval="+TEST_INTERVAL);
		println("balanceData="+BALANCE_DATA);
		println("useDropout="+USE_DROPOUT);
		println("dropoutRate="+DROPOUT_RATE);
		println("learningRate="+LEARNING_RATE);
		println("usePerceptron="+USE_PERCEPTRON);
		println("numNodesInHiddenLayer="+NUM_NODES_IN_LAYER);
		println("useCategoryID="+USE_CAT_ID);
		println("numProbabilities="+NUM_PROBABILITIES_TO_USE);
		println("whitenProbabilities="+WHITEN_PROBABILITIES);
		println("numCategoryEmbeddings="+NUM_CATEGORY_EMBEDDINGS_TO_USE);
		println("categoryEmbeddingDimension="+CATEGORY_EMBEDDING_DIMENSION);
		println("wordEmbeddingDimension="+WORD_EMBEDDING_DIMENSION);
		println("useWordEmbeddings="+USE_WORD_EMBEDDINGS);
	}

	public static InputLayer<MultitaggerExample> getInputLayer() {
		ArrayList<InputLayer> list = new ArrayList<InputLayer>();
		if(NUM_PROBABILITIES_TO_USE > 0) {
			list.add(getProbabilityInputLayer(NUM_PROBABILITIES_TO_USE, WHITEN_PROBABILITIES));
		}
		if(USE_CAT_ID) {
			list.add(getCategoryIDInputLayer());
		}
		if(NUM_CATEGORY_EMBEDDINGS_TO_USE > 0) {
			for(int k=0; k<NUM_CATEGORY_EMBEDDINGS_TO_USE; k++) {
				list.add(getCategoryEmbeddingInputLayer(k));
			}
		}
		if(USE_WORD_EMBEDDINGS) {
			list.add(getWordEmbeddingInputLayer());
		}
		return new ConcatenatedInputLayer<MultitaggerExample>(list);
	}

	private static void println() {
		println("");
	}

	private static void println(String string) {
		System.out.println(string);
		if(pw != null) {
			pw.println(string);
		}
	}

	private static double[] test(MulticlassNeuralNet<MultitaggerExample> net) {
		double total = 0.0;
		double truePos = 0.0;
		double trueNeg = 0.0;
		double falsePos = 0.0;
		double falseNeg = 0.0;
		for(MultitaggerExample ex : testData) {
			boolean pred = net.hardPredict(ex) == 1 ? true : false;
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

	private static InputLayer<MultitaggerExample> getCategoryEmbeddingInputLayer(int categoryIndex) {
		StringEmbeddings embeddings = Util.loadLearnedCategoryEmbeddings(CATEGORY_EMBEDDING_DIMENSION);
		//StringEmbeddings embeddings = Util.loadWord2VecCategoryEmbeddings();
		StringEmbeddingContextExtractor extractor = new CategoryEmbeddingContextExtractor(CATEGORY_EMBEDDING_DIMENSION, embeddings, categoryIndex);
		return new SimpleInputLayer<MultitaggerExample>(extractor, false);
	}


	private static InputLayer<MultitaggerExample> getWordEmbeddingInputLayer() {
		StringEmbeddings embeddings = Util.loadLearnedWordEmbeddings();
		StringEmbeddingContextExtractor extractor = new WordEmbeddingContextExtractor(50, embeddings);
		return new SimpleInputLayer<MultitaggerExample>(extractor, false);
	}

	private static InputLayer<MultitaggerExample> getCategoryIDInputLayer() {
		ArrayList<String> cats = Util.getCatList();
		StringEmbeddings embeddings = new StringEmbeddings();
		for(int c=0; c<cats.size(); c++) {
			DoubleMatrix vec = new DoubleMatrix(425, 1);
			vec.put(c, 1.0);
			embeddings.addEmbedding(cats.get(c), vec);
		}		
		StringEmbeddingContextExtractor extractor = new CategoryEmbeddingContextExtractor(425, embeddings, 0);
		return new SimpleInputLayer<MultitaggerExample>(extractor, false);
	}

	private static InputLayer<MultitaggerExample> getProbabilityInputLayer(int dim, boolean stdze) {
		return new SimpleInputLayer<MultitaggerExample>(new ProbabilityContextExtractor(dim), stdze);
	}

	private static double countCorrect(ArrayList<MultitaggerExample> data) {
		double c = 0.0;
		for(MultitaggerExample ex : data) {
			if(ex.getItem().isCorrect()) {
				c++;
			}
		}
		return c;
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

	private static MulticlassNeuralNet<MultitaggerExample> initializeMulticlassNet(
			InputLayer<MultitaggerExample> input, int... hiddenLayerSizes) {		
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


}

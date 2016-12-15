package multitagger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;

import neuralnet.deprecated.LinearLayer;

import multitagger.layers.BinaryOutputLayer;
import multitagger.layers.LogisticLayer;
import multitagger.layers.LogisticLayerWithHingeLoss;
import multitagger.layers.MappingFunctionInputLayer;
import multitagger.layers.EmbeddingMultitaggerInputLayer;
import multitagger.layers.SimpleInputLayer;

import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.StringEmbeddings;
import util.Util;

public class FirstClassifierExperiment {

	private HashMap<String, String> setArgs;

	private String directory;

	private static String CONFIG = "config";

	private static String TAGGER_DIR = "tagger";
	
	private static String CLASSIFIER_DIR = "first_classifier";

	private Regularization regularizer;
	private static final String REGULARIZER = "regularization";

	private double learningRate;
	private static final String LEARNING_RATE = "learningRate";

	private int numIterations;
	private static final String NUM_ITERATIONS = "numIterations";

	private boolean useDropout;
	private static final String USE_DROPOUT = "useDropout";

	private double regularizationWeight;
	private static final String REGULARIZATION_WEIGHT = "regularizationWeight";

	private LossFunction lossFunction;
	private static final String LOSS_FUNCTION = "lossFunction";
	private double hingeLossMargin;
	private static final String HINGE_LOSS_MARGIN = "hingeLossMargin";

	private int contextWindowSize;
	private static final String CXT_WINDOW_SIZE = "contextWindowSize";
	private int contextWindowDepth;
	private static final String CXT_WINDOW_DEPTH = "contextWindowDepth";

	private int numCatsInList;
	private static final String NUM_CATS_IN_LIST = "numCatsInList";

	private boolean useCategoryEmbeddings;
	private static final String USE_CATEGORY_EMBEDDINGS = "useCategoryEmbeddings";
	private int categoryEmbeddingDimension;
	private static final String CATEGORY_EMBEDDING_DIMENSION = "categoryEmbeddingDimension";
	private String categoryEmbeddingFile;
	private static final String CATEGORY_EMBEDDING_FILE = "categoryEmbeddingFile";

	private boolean useWordEmbeddings;
	private static final String USE_WORD_EMBEDDINGS = "useWordEmbeddings";
	private int wordEmbeddingDimension;
	private static final String WORD_EMBEDDING_DIMENSION = "wordEmbeddingDimension";
	private String wordEmbeddingFile;
	private static final String WORD_EMBEDDING_FILE = "wordEmbeddingFile";

	private boolean useMappingLayers;
	private static final String USE_MAPPING_LAYERS = "useMappingLayers";

	private int mappingLayerDimension;
	private static final String MAPPING_LAYER_DIMENSION = "mappingLayerDimension";

	private int numHiddenNodes;
	private static final String NUM_HIDDEN_NODES = "numHiddenNodes";

	private String trainingDataFile;
	private static final String TRAINING_DATA_FILE = "trainingDataFile";
	private MultitaggerTrainingData trainingData;

	private String testingDataFile;
	private static final String TESTING_DATA_FILE = "testingDataFile";
	private MultitaggerTrainingData testingData;
	
	private boolean rescaleFalsePositives;
	private static final String RESCALE_FALSE_POSITIVES = "rescaleFalsePositives";
	
	private FirstClassifier classifier;

	public FirstClassifierExperiment(String experimentDirectory) throws Exception {
		this.directory = experimentDirectory;
		(new File(this.directory)).mkdirs();
		this.setArgs = new HashMap<String, String>();
		this.setDefaultArgs();
	}

	private void setDefaultArgs() {
		this.setArgument(REGULARIZER, "NONE");
		this.setArgument(LEARNING_RATE, "0.005");
		this.setArgument(NUM_ITERATIONS, "15");
		this.setArgument(USE_DROPOUT, "False");
		this.setArgument(REGULARIZATION_WEIGHT, "0.5");
		this.setArgument(LOSS_FUNCTION, "Logistic");
		this.setArgument(CXT_WINDOW_SIZE, "2");
		this.setArgument(NUM_CATS_IN_LIST, "3");
		this.setArgument(USE_CATEGORY_EMBEDDINGS, "True");
		this.setArgument(CATEGORY_EMBEDDING_FILE, "embeddings/category_embeddings_learned_from_supertagger_training.50.txt");
		this.setArgument(USE_WORD_EMBEDDINGS, "True");
		this.setArgument(WORD_EMBEDDING_FILE, "embeddings/word_embeddings_learned_from_supertagger_training.50.txt");
		this.setArgument(USE_MAPPING_LAYERS, "True");
		this.setArgument(MAPPING_LAYER_DIMENSION, "20");
		this.setArgument(NUM_HIDDEN_NODES, "100");
		this.setArgument(TRAINING_DATA_FILE, "multitagger_training_data/cv_wsj2-21_k=20.supertagged.ser");
		this.setArgument(TESTING_DATA_FILE, "multitagger_training_data/wsj0_k=20.supertagged.ser");
		this.setArgument(HINGE_LOSS_MARGIN, "0.2");
		this.setArgument(WORD_EMBEDDING_DIMENSION, "50");
		this.setArgument(CATEGORY_EMBEDDING_DIMENSION, "50");
		this.setArgument(CXT_WINDOW_DEPTH, "5");
		this.setArgument(RESCALE_FALSE_POSITIVES, "False");
	}

	public void setArgument(String arg, String value) {
		if(arg.equals(REGULARIZER)) {
			this.regularizer = Regularization.valueOf(value);
		}		
		else if(arg.equals(LEARNING_RATE)) {
			this.learningRate = Double.parseDouble(value);
		}
		else if(arg.equals(NUM_ITERATIONS)) {
			this.numIterations = Integer.parseInt(value);
		}
		else if(arg.equals(USE_DROPOUT)) {
			this.useDropout = Boolean.parseBoolean(value);
		}
		else if(arg.equals(REGULARIZATION_WEIGHT)) {
			this.regularizationWeight = Double.parseDouble(value);
		}
		else if(arg.equals(LOSS_FUNCTION)) {
			this.lossFunction = LossFunction.valueOf(value);
		}
		else if(arg.equals(CXT_WINDOW_SIZE)) {
			this.contextWindowSize = Integer.parseInt(value);
		}
		else if(arg.equals(NUM_CATS_IN_LIST)) {
			this.numCatsInList = Integer.parseInt(value);
		}
		else if(arg.equals(USE_CATEGORY_EMBEDDINGS)) {
			this.useCategoryEmbeddings = Boolean.parseBoolean(value);
		}
		else if(arg.equals(USE_WORD_EMBEDDINGS)) {
			this.useWordEmbeddings = Boolean.parseBoolean(value);
		}
		else if(arg.equals(USE_MAPPING_LAYERS)) {
			this.useMappingLayers = Boolean.parseBoolean(value);
		}
		else if(arg.equals(MAPPING_LAYER_DIMENSION)) {
			this.mappingLayerDimension = Integer.parseInt(value);
		}
		else if(arg.equals(NUM_HIDDEN_NODES)) {
			this.numHiddenNodes = Integer.parseInt(value);
		}
		else if(arg.equals(TRAINING_DATA_FILE)) {
			this.trainingDataFile = value;
		}
		else if(arg.equals(TESTING_DATA_FILE)) {
			this.testingDataFile = value;
		}
		else if(arg.equals(HINGE_LOSS_MARGIN)) {
			this.hingeLossMargin = Double.parseDouble(value);
		}
		else if(arg.equals(WORD_EMBEDDING_DIMENSION)) {
			this.wordEmbeddingDimension = Integer.parseInt(value);
		}
		else if(arg.equals(CATEGORY_EMBEDDING_DIMENSION)) {
			this.categoryEmbeddingDimension = Integer.parseInt(value);
		}
		else if(arg.equals(CXT_WINDOW_DEPTH)) {
			this.contextWindowDepth = Integer.parseInt(value);
		}
		else if(arg.equals(CATEGORY_EMBEDDING_FILE)) {
			this.categoryEmbeddingFile = value;
		}
		else if(arg.equals(WORD_EMBEDDING_FILE)) {
			this.wordEmbeddingFile = value;
		}
		else if(arg.equals(RESCALE_FALSE_POSITIVES)) {
			this.rescaleFalsePositives = Boolean.parseBoolean(value);
		}
		else {
			System.out.println("Failed to set argument: "+arg+"="+value);
			return;
		}
		this.setArgs.put(arg, value);
	}

	public void runExperiment() throws Exception {
		this.writeConfig();
		doTraining();
		this.classifier.save(this.getFile(CLASSIFIER_DIR));
	}

	private void doTraining() throws Exception {
		if(this.classifier == null) {
			this.initializeClassifier();
		}
		this.trainingData = MultitaggerTrainingData.loadMultitaggerTrainingData(new File(this.trainingDataFile));
		
		double rescale = 1.0;
		if(this.rescaleFalsePositives) {
			double total = 0.0;
			double negative = 0.0;
			for(MultitaggerTrainingSentence sen : this.trainingData.getData()) {
				for(MultitaggerTrainingItem item : sen.getItems()) {
					total++;
					if(!item.isCorrect()) {
						negative++;
					}
				}
			}
			rescale = (total-negative)/negative;
			System.out.println("Rescale factor: "+rescale);
			this.learningRate /= rescale;
		}
		
		int S = this.trainingData.getData().size();
		PrintWriter fpw = new PrintWriter(this.getFile("results.csv"));
		for(int T=0; T<this.numIterations; T++) {
			double totalT = 0.0;
			double correctT = 0.0;
			double tP = 0.0;
			double tN = 0.0;
			double fP = 0.0;
			double fN = 0.0;
			int s = 0;
			System.out.println("Starting training iteration "+(T+1)+" out of "+this.numIterations);
			System.out.println("Threshold: "+classifier.gamma);
			for(MultitaggerTrainingSentence sen : trainingData.getData()) {
				if( s != 0 & (s++)%(S/4) == 0) {
					System.out.println("\t"+((100*s)/S)+"% complete.");
				}
				for(int i=0; i<sen.sentence().length(); i++) {
					totalT++;
					double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
					double prediction = this.classifier.hardPredict(sen, i);
					if(prediction == correctLabel) {
						correctT++;
						if(prediction == 0.0) {
							tN++;
						}
						else {
							tP++;
						}
					}
					else {
						if(prediction == 0.0) {
							fN++;
						}
						else {
							fP++;
						}
					}
				}
				for(Integer i : sen.getTestingIndices()) {
					double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
					this.classifier.trainOn(sen, i, correctLabel, learningRate, this.regularizer, this.regularizationWeight, this.useDropout, rescale);
					//this.classifier.trainOn(sen, i, correctLabel, learningRate);
				}
			}
			fpw.print((correctT/totalT));
			System.out.println("Training accuracy during past iteration: "+(correctT/totalT));
			System.out.println("True positive: "+(tP/totalT));
			System.out.println("True negative: "+(tN/totalT));
			System.out.println("False positive: "+(fP/totalT));
			System.out.println("False negative: "+(fN/totalT));
			System.out.println("Finished training iteration "+(T+1)+" out of "+this.numIterations);

			System.out.println("Checking static model: ");
			if(this.useDropout) {
				this.classifier.scaleWeightsDownAfterDropoutTraining();
			}
			fpw.println(","+doTesting());
			if(this.useDropout) {
				this.classifier.scaleWeightsUpForDropoutTraining();
			}
			System.out.println("\n\n");
		}
		fpw.close();
		if(this.useDropout) {
			this.classifier.scaleWeightsDownAfterDropoutTraining();
			System.out.println("Re-scaled weights after dropout training.");
		}
	}

	private void initializeClassifier() throws FileNotFoundException {
		EmbeddingMultitaggerInputLayer input;
		StringEmbeddings wordEmbeddings = Util.loadLearnedWordEmbeddings();
		StringEmbeddings categoryEmbeddings = Util.loadLearnedCategoryEmbeddings(this.categoryEmbeddingDimension);
		//StringEmbeddings wordEmbeddings = StringEmbeddings.loadFromFile(new File(wordEmbeddingFile));
		//StringEmbeddings categoryEmbeddings = StringEmbeddings.loadFromFile(new File(categoryEmbeddingFile));
		if(this.useMappingLayers) {
			input = new MappingFunctionInputLayer(this.numCatsInList, this.contextWindowSize, this.wordEmbeddingDimension,
					this.categoryEmbeddingDimension, this.contextWindowDepth, wordEmbeddings, categoryEmbeddings,
					this.mappingLayerDimension);
		}
		else {
			input = new SimpleInputLayer(this.numCatsInList, this.contextWindowSize, this.wordEmbeddingDimension,
					this.categoryEmbeddingDimension, this.contextWindowDepth, wordEmbeddings, categoryEmbeddings);
		}
		LinearLayer linear = new LinearLayer(input.getOutputDimension(), this.numHiddenNodes);
		BinaryOutputLayer output;
		switch(this.lossFunction) {
		case QuadraticHinge:
			output = new LogisticLayerWithHingeLoss(this.hingeLossMargin);
			break;
		case Logistic:
		default:
			output = new LogisticLayer();
		}
		this.classifier = new FirstClassifier(input, linear, output);
	}

	private double doTesting() {
		if(this.classifier == null) {
			System.out.println("Aborting testing; first classifier is null.");
			return -1.0;
		}
		System.out.println("Starting testing.");
		if(this.testingData == null) {
			this.testingData = MultitaggerTrainingData.loadMultitaggerTrainingData(new File(this.testingDataFile));
		}
		double total = 0.0;
		double correct = 0.0;
		double tP = 0.0;
		double tN = 0.0;
		double fP = 0.0;
		double fN = 0.0;
		for(MultitaggerTrainingSentence sen : this.testingData.getData()) {
			for(int i=0; i<sen.sentence().length(); i++) {
				total++;
				double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
				double prediction = this.classifier.hardPredict(sen, i);
				if(prediction == correctLabel) {
					correct++;
					if(prediction == 0.0) {
						tN++;
					}
					else {
						tP++;
					}
				}
				else {
					if(prediction == 0.0) {
						fN++;
					}
					else {
						fP++;
					}
				}
			}
		}
		System.out.println("Accuracy: "+(correct/total));
		System.out.println("True positive: "+(tP/total));
		System.out.println("True negative: "+(tN/total));
		System.out.println("False positive: "+(fP/total));
		System.out.println("False negative: "+(fN/total));
		System.out.println("Finished testing.");
		return correct/total;
	}
	
	private FirstClassifier getFirstClassifier() {
		FirstClassifier c = null;
		
		return c;
	}

	public void writeConfig() {
		try {
			PrintWriter pw = new PrintWriter(this.getFile(CONFIG));
			for(String arg : this.setArgs.keySet()) {
				if(		!(this.regularizer == Regularization.NONE && arg.equals(REGULARIZATION_WEIGHT))
					&& 	!(!this.useMappingLayers && arg.equals(MAPPING_LAYER_DIMENSION))
					&& 	!(this.lossFunction!=LossFunction.QuadraticHinge && arg.equals(HINGE_LOSS_MARGIN))) {
					pw.println(arg+"="+this.setArgs.get(arg));					
				}
			}
			pw.close();
		} catch(Exception e) {
			System.err.println("Failed to write experiment config file.");
		}
	}

	private File getFile(String... filenames) throws FileNotFoundException {
		String fullname = this.directory;
		for(String filename : filenames) {
			fullname += File.separator+filename;
		}
		return new File(fullname);
	}

}

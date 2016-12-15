package supertagger.lsbeta;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.jblas.DoubleMatrix;

import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;
import supertagger.nn.StringEmbeddings;
import supertagger.nn.LogisticRegressionNN;
import supertagger.nn.modules.LookupTableLayer;
import util.Util;

public class TestLogisticRegressionClassifier {

	public static void main(String[] args) throws Exception {
		testFirstDecisionClassifier();
	}

	private static void testFirstDecisionClassifier() throws Exception {
		
		/*
		StringEmbeddings embeddings = StringEmbeddings.loadFromFile(new File("multitagger_dev/wsj2-21.cats.50.txt"));
		HashMap<String, DoubleMatrix> map = embeddings.getAllVectors();
		for(String cat : map.keySet()) {
			System.out.println(cat);
		}
		
		if(1==1) {
			return;
		}
		*/
		
		LSSupertagger supertagger = Util.loadTagger();
		int topK = 20;
		
		File dir = new File("multitagger_dev");
		dir.mkdirs();
		File trainingFile = new File("multitagger_dev/dev_training_data.ser");
		Collection<Sentence> trainingSentences = getTrainingData();
		MultitaggerTrainingData trainingData2 = new MultitaggerTrainingData(supertagger, trainingSentences, topK);
		trainingData2.saveToFile(trainingFile);
		
		File testingFile = new File("multitagger_dev/dev_testing_data.ser");
		Collection<Sentence> testingSentences2 = getTestingData();
		MultitaggerTrainingData testingData2 = new MultitaggerTrainingData(supertagger, testingSentences2, topK);
		testingData2.saveToFile(testingFile);
		
		if(1==1) {
			return;
		}
		
		
/*
		File trainingFile = new File("multitagger_dev/dev_training_data.ser");

		MultitaggerTrainingData trainingData = MultitaggerTrainingData.loadMultitaggerTrainingData(trainingFile);
*/	
		
		int numCatsToLookAt = 3;
		int numHiddenNodes = 100;
		double learningRate = 0.01;
		int numIterations = 10;
		//Collection<Sentence> trainingSentences = getTrainingData();
		/*
		Collection<SupertagAssignment> trainingData = new ArrayList<SupertagAssignment>();
		for(Sentence sen : trainingSentences) {
			trainingData.add(supertagger.tagSentence(sen));
		}
		*/
		
		NNMultitagger tagger = new NNMultitagger(supertagger, numHiddenNodes, numCatsToLookAt);

		// Train
		for(int t=0; t<numIterations; t++) {
			double total = 0.0;
			double correct = 0.0;
			System.out.println("Starting training iteration "+(t+1));
			/*
			for(SupertagAssignment tags : trainingData) {
				for(int i=0; i<tags.sentence().length(); i++) {
					total++;
					correct += tagger.trainOn(tags, i, tags.getBest(i).category().equals(tags.getGold(i)), learningRate);
				}
			}
			*/
			System.out.println("Training accuracy: "+correct/total);
		}

		// Test
		Collection<Sentence> testingData = getTestingData();
		double total = 0.0;
		double correct = 0.0;
		double betaCOrrect = 0.0;
		for(Sentence sen : testingData) {
			SupertagAssignment tags = supertagger.tagSentence(sen);
			for(int i=0; i<sen.length(); i++) {
				total++;
				boolean useFirst = tagger.returnOnlyViterbiCategory(tags, i);
				boolean shouldHaveUsedFirst = tags.getBest(i).category().equals(sen.get(i).getCategory());
				if(useFirst == shouldHaveUsedFirst) {
					correct++;
				}
			}
		}
		System.out.println("Accuracy: "+(correct/total));
	}

	private static Collection<Sentence> getTrainingData() {
		//return CCGbankReader.getCCGbankData(10, 10, "data/CCGbank/AUTO");
		return CCGbankReader.getCCGbankData(2, 21, "data/CCGbank/AUTO");
	}

	private static Collection<Sentence> getTestingData() {
		return CCGbankReader.getCCGbankData(0, 0, "data/CCGbank/AUTO");
	}

	public static void testOnSampleData() {
		double learningRate = 0.01;
		int T = 10;
		int embeddingDim = 10;
		ArrayList<String> dictionary = getDictionary();
		ArrayList<DataItem> data = getRandomData(dictionary);
		LookupTableLayer lookup = getLookupTableLayer(dictionary, embeddingDim);
		LogisticRegressionNN net = new LogisticRegressionNN(lookup, 10);

		// Train
		for(int t=0; t<T; t++) {
			for(DataItem item : data) {
				System.out.println("Training on "+item.word+" ("+item.label+")");
				net.trainOn(lookup.getWordEmbedding(item.word), item.label, learningRate);
			}
			System.out.println("---");
		}

		// Test
		double total = 0.0;
		double correct = 0.0;
		for(DataItem item : data) {
			total++;
			double prob = net.predict(lookup.getWordEmbedding(item.word));
			double pred = net.hardPredict(lookup.getWordEmbedding(item.word));
			if(pred == item.label) {
				correct++;
				System.out.println("Correct: "+item.word+" (prob: "+prob+")");
			}
			else {
				System.out.println("Incorrect: "+item.word+" (prob: "+prob+")");
			}
		}
		System.out.println("Accuracy: "+(correct/total));
	}

	private static ArrayList<String> getDictionary() {
		ArrayList<String> dictionary = new ArrayList<String>();
		dictionary.add("abe");
		dictionary.add("george");
		dictionary.add("barack");
		dictionary.add("bill");
		dictionary.add("marco");
		dictionary.add("hillary");
		dictionary.add("bernie");
		dictionary.add("franklin");
		return dictionary;
	}

	private static ArrayList<DataItem> getRandomData(ArrayList<String> dictionary) {
		ArrayList<DataItem> data = new ArrayList<DataItem>();
		for(String w : dictionary) {
			data.add(new DataItem(w));
		}
		return data;
	}

	private static LookupTableLayer getLookupTableLayer(ArrayList<String> dictionary, int embeddingDim) {
		return new SampleLookupLayer(dictionary, embeddingDim);
	}

}

class DataItem {


	String word;
	double label;

	public DataItem(String w) {
		this.word = w;
		assignLabel();
	}

	private void assignLabel() {
		boolean cond = this.word.charAt(0) < 'g';
		if(cond) {
			this.label = 1.0;
		}
		else {
			this.label = 0.0;
		}
	}

}

class SampleLookupLayer extends LookupTableLayer {

	public SampleLookupLayer(ArrayList<String> dict, int dimensions) {
		super(1, dimensions);
		for(String w : dict) {
			double[] vec = new double[dimensions];
			for(int i=0; i<vec.length; i++) {
				vec[i] = (Math.random()-0.5)*0.05;
			}
			super.setEmbeddingForVariable(w, vec);
		}
	}



	@Override
	public DoubleMatrix getWordEmbedding(String w) {
		this.word = w;
		return toMatrix(super.wordEmbeddingsLookupTable.get(super.stringToIndex.get(word)));
	}

	private String word;

	@Override
	public void updateParameters(DoubleMatrix outputGradient,
			double learningRate) {
		double[] embedding = super.wordEmbeddingsLookupTable.get(super.stringToIndex.get(word));
		for(int w=0; w<embedding.length; w++) {
			embedding[w] += outputGradient.get(w);
		}		
	}

	public DoubleMatrix getOutput(DoubleMatrix rawInput) {
		return rawInput;
	}

	private DoubleMatrix toMatrix(double[] vec) {
		return new DoubleMatrix(vec.length, 1, vec);
	}

	@Override
	protected Integer getEmbeddingID(String word) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void saveWeightsToFile(File file) {
		// TODO Auto-generated method stub

	}

	@Override
	public void loadWeightsFromFile(File file) {
		// TODO Auto-generated method stub

	}
}

package multitagger.runnables;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import supertagger.lewissteedman.LSSupertagger;
import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import util.Util;

public class CreateMergeCVMultitaggedTrainingData {

	private static int topK = 20;

	private static int SMALL = 2;

	private static String smallFile = "multitagger_training_data/cv_wsj2-"+SMALL+"_k="+topK+".supertagged.ser";

	private static String outFile = "multitagger_training_data/cv_wsj2-21_k="+topK+".supertagged.ser";
	
	private static String balancedFile = "multitagger_training_data/cv_wsj2-21_k="+topK+".supertagged.balanced.ser";

	private static String devOutFile = "multitagger_training_data/wsj0_k="+topK+".supertagged.ser";

	private static String prefix = "cross_validation/omit";

	private static String suffix = "/tagger__iter=15";

	private static int LOW = 2;

	private static int HIGH = 21;

	public static void main(String[] args) throws Exception {
		createDevData();
		MultitaggerTrainingData orig = mergeTrainingData();
		mergeTrainingDataBalanced(orig);
		mergeSmallTrainingData();
	}

	private static MultitaggerTrainingData createDevData() {	
		MultitaggerTrainingData data = new MultitaggerTrainingData(topK);	
		Collection<Sentence> secData = CCGbankReader.getCCGbankData(0, 0, Util.AUTO_DIR);	
		LSSupertagger tagger = LSSupertagger.load(new File("tagger"));
		data.generateAndAddToData(tagger, secData);
		data.saveToFile(new File(devOutFile));	
		return data;
	}

	private static MultitaggerTrainingData mergeTrainingData() {		
		MultitaggerTrainingData data = new MultitaggerTrainingData(topK);		
		for(int s=LOW; s<=HIGH; s++) {
			String taggerDir = prefix+s+suffix;
			System.out.println(taggerDir);
			LSSupertagger tagger = LSSupertagger.load(new File(taggerDir));
			Collection<Sentence> secData = CCGbankReader.getCCGbankData(s, s, Util.AUTO_DIR);
			data.generateAndAddToData(tagger, secData);
		}
		data.saveToFile(new File(outFile));		
		return data;
	}

	private static void mergeTrainingDataBalanced(MultitaggerTrainingData orig) {		
		//MultitaggerTrainingData data = new MultitaggerTrainingData(topK);		
		ArrayList<MultitaggerTrainingItem> items = new ArrayList<MultitaggerTrainingItem>();
		double total = 0.0;
		double negative = 0.0;
		for(MultitaggerTrainingSentence sen : orig.getData()) { 
			for(MultitaggerTrainingItem item : sen.getItems()) {
				total++;
				if(!item.isCorrect()) {
					negative++;
				}
			}
		}
		int scale = (int) ((total-negative)/negative);

		MultitaggerTrainingData data = new MultitaggerTrainingData(topK);
		
		for(MultitaggerTrainingSentence sen : orig.getData()) { 
			for(int i=0; i<sen.sentence().length(); i++) {
				if(!sen.getItems().get(i).isCorrect()) {
					for(int s=0; s<scale; s++) {
						sen.addItemIndex(i);
					}
				}
			}
			sen.shuffleItemIndices();
			data.addSentence(sen);
		}
		data.saveToFile(new File(balancedFile));		
	}

	private static void mergeSmallTrainingData() {		
		MultitaggerTrainingData data = new MultitaggerTrainingData(topK);		
		for(int s=LOW; s<=SMALL; s++) {
			String taggerDir = prefix+s+suffix;
			System.out.println(taggerDir);
			LSSupertagger tagger = LSSupertagger.load(new File(taggerDir));
			Collection<Sentence> secData = CCGbankReader.getCCGbankData(s, s, Util.AUTO_DIR);
			data.generateAndAddToData(tagger, secData);
		}
		data.saveToFile(new File(smallFile));		
	}

}

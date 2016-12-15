package multitagger.runnables;

import java.io.File;

import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;

public class CheckSupertaggerAccuracy {
	
	public static void main(String[] args) {		
		System.out.println("Training data:");
		checkAccuracy("multitagger_training_data/cv_wsj2-21_k=20.supertagged.ser");
		
		System.out.println("Development data:");
		checkAccuracy("multitagger_training_data/wsj0_k=20.supertagged.ser");
	}
	
	public static void checkAccuracy(String file) {
		MultitaggerTrainingData data = MultitaggerTrainingData.loadMultitaggerTrainingData(new File(file));
		double total = 0.0;
		double vitCorrect = 0.0;
		double avgRank = 0.0;
		double rankTotal = 0.0;
		for(MultitaggerTrainingSentence sen : data.getData()) {
			for(MultitaggerTrainingItem item : sen.getItems()) {
				total++;
				if(item.isCorrect()) {
					vitCorrect++;
				}
				if(item.getGoldIndex() != -1) {
					avgRank += item.getGoldIndex() + 1;
					rankTotal++;
				}
			}
		}
		System.out.println("    Accuracy: "+(100*vitCorrect/total));
		System.out.println("  Error rate: "+(100-(100*vitCorrect/total)));
		System.out.println("Average rank: "+(avgRank/rankTotal));
	}

}

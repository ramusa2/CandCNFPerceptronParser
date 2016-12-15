package multitagger.runnables;

import java.io.File;
import java.io.PrintWriter;

import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import multitagger.FirstClassifier;
import multitagger.Regularization;
import multitagger.SimplerFirstClassifier;

public class SimplerClassifierTester {
	
	public static void main(String[] args) throws Exception {
		String testingDataFile = "multitagger_training_data/wsj0_k=20.supertagged.ser";
		//String trainingDataFile = "multitagger_training_data/cv_wsj2-2_k=20.supertagged.ser";
		//String trainingDataFile = "multitagger_training_data/cv_wsj2-21_k=20.supertagged.ser";
		String trainingDataFile = "multitagger_training_data/cv_wsj2-21_k=20.supertagged.balanced.ser";
		MultitaggerTrainingData trainingData = MultitaggerTrainingData.loadMultitaggerTrainingData(new File(trainingDataFile));

		MultitaggerTrainingData testingData = MultitaggerTrainingData.loadMultitaggerTrainingData(new File(testingDataFile));
		
		FirstClassifier classifier = SimplerFirstClassifier.getDevClassifier(trainingData);
		doTraining(classifier, trainingData, testingData);
	}
	

	private static void doTraining(FirstClassifier classifier, MultitaggerTrainingData trainingData,
			MultitaggerTrainingData testingData) throws Exception {
		double learningRate = 0.005;
		int numIterations = 10;
		Regularization regularizer = Regularization.L2;
		double regWeight = 0.5;
		boolean useDropout = true;
		
		int S = trainingData.getData().size();
		for(int T=0; T<numIterations; T++) {
			double totalT = 0.0;
			double correctT = 0.0;
			double tP = 0.0;
			double tN = 0.0;
			double fP = 0.0;
			double fN = 0.0;
			int s = 0;
			System.out.println("Starting training iteration "+(T+1)+" out of "+numIterations);
			System.out.println("Threshold: "+classifier.gamma);
			for(MultitaggerTrainingSentence sen : trainingData.getData()) {
				if( s != 0 & (s++)%(S/4) == 0) {
					System.out.println("\t"+((100*s)/S)+"% complete.");
				}
				for(int i=0; i<sen.sentence().length(); i++) {
					totalT++;
					double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
					double prediction = classifier.hardPredict(sen, i);
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
					classifier.trainOn(sen, i, correctLabel, learningRate, regularizer, regWeight, useDropout);
					//this.classifier.trainOn(sen, i, correctLabel, learningRate);
				}
			}
			System.out.println("Training accuracy during past iteration: "+(correctT/totalT));
			System.out.println("True positive: "+(tP/totalT));
			System.out.println("True negative: "+(tN/totalT));
			System.out.println("False positive: "+(fP/totalT));
			System.out.println("False negative: "+(fN/totalT));
			System.out.println("Finished training iteration "+(T+1)+" out of "+numIterations);

			System.out.println("Checking static model: ");
			if(useDropout) {
				classifier.scaleWeightsDownAfterDropoutTraining();
			}
			if(useDropout) {
				classifier.scaleWeightsUpForDropoutTraining();
			}
			doTesting(classifier, testingData);
		}
		if(useDropout) {
			classifier.scaleWeightsDownAfterDropoutTraining();
			System.out.println("Re-scaled weights after dropout training.");
		}
	}
	
	private static double doTesting(FirstClassifier classifier, MultitaggerTrainingData testingData) {
		System.out.println("Starting testing.");
		double total = 0.0;
		double correct = 0.0;
		double tP = 0.0;
		double tN = 0.0;
		double fP = 0.0;
		double fN = 0.0;
		for(MultitaggerTrainingSentence sen : testingData.getData()) {
			for(int i=0; i<sen.sentence().length(); i++) {
				total++;
				double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
				double prediction = classifier.hardPredict(sen, i);
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

}

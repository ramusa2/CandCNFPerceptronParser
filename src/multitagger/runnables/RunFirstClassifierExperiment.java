package multitagger.runnables;

import java.io.File;

import multitagger.ContextSensitiveFirstClassifier;
import multitagger.FirstClassifier;
import multitagger.SimpleFirstClassifier;
import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import supertagger.nn.StringEmbeddings;
import util.Util;

public class RunFirstClassifierExperiment {

	public static void main(String[] args) {
		devSimpleClassifier();
	}
	
	public static void devSimpleClassifier() {
		// Get classifier
		FirstClassifier multitagger = SimpleFirstClassifier.getSampleClassifier();
		
		File trainingFile = new File("multitagger_training_data/cv_wsj2-21_k=20.supertagged.ser");
		//File trainingFile = new File("multitagger_training_data/cv_wsj2-2_k=20.supertagged.ser");
		MultitaggerTrainingData trainingData = MultitaggerTrainingData.loadMultitaggerTrainingData(trainingFile);
		System.out.println("Loaded training data.");

		File testingFile = new File("multitagger_training_data/wsj0_k=20.supertagged.ser");
		MultitaggerTrainingData testingData = MultitaggerTrainingData.loadMultitaggerTrainingData(testingFile);
		System.out.println("Loaded testing data.");


		// Set parameters
		int T = 10;
		double learningRate = 0.005;
		System.out.println("Training parameters:\n"
				+"\tIterations: "+T+"\n"
				+"\tLearning rate: "+learningRate+"\n");

		// Train
		int S = trainingData.getData().size();
		for(int t=0; t<T; t++) {
			double totalT = 0.0;
			double correctT = 0.0;
			double tP = 0.0;
			double tN = 0.0;
			double fP = 0.0;
			double fN = 0.0;

			System.out.println("Starting training iteration "+(t+1)+".");
			int s = 0;
			for(MultitaggerTrainingSentence sen : trainingData.getData()) {
				if((s++)%(S/4) == 0) {
					System.out.println("\t"+((100*s)/S)+"% complete.");
				}

				// TODO: this block checks accuracy for a whole sentence before updating
				for(int i=0; i<sen.sentence().length(); i++) {
					totalT++;
					double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
					double prediction = multitagger.hardPredict(sen, i);
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
				for(int i=0; i<sen.sentence().length(); i++) {
					double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
					multitagger.trainOn(sen, i, correctLabel, learningRate);
				}

				// TODO: this block checks for accuracy only at each token before updating
				/*
			for(int i=0; i<sen.sentence().length(); i++) {
				totalT++;
				double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
				double prediction = multitagger.hardPredict(sen, i);
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
				multitagger.trainOn(sen, i, correctLabel, learningRate);
			}
				 */
			}
			System.out.println("Training accuracy during past iteration: "+(correctT/totalT));
			System.out.println("True positive: "+(tP/totalT));
			System.out.println("True negative: "+(tN/totalT));
			System.out.println("False positive: "+(fP/totalT));
			System.out.println("False negative: "+(fN/totalT));

			System.out.println("Checking static model: ");
			doTest(multitagger, testingData);
		}

		// Test

		//File testingFile = new File("multitagger_dev/dev_testing_data.ser");
		//MultitaggerTrainingData testingData = MultitaggerTrainingData.loadMultitaggerTrainingData(testingFile);
		System.out.println("Final testing: ");
		//doTest(multitagger, testingData);
		doTest(multitagger, testingData);
	}

	public static void doTest(FirstClassifier multitagger, MultitaggerTrainingData data) {
		System.out.println("Starting testing.");
		double total = 0.0;
		double correct = 0.0;
		double tP = 0.0;
		double tN = 0.0;
		double fP = 0.0;
		double fN = 0.0;
		for(MultitaggerTrainingSentence sen : data.getData()) {
			for(int i=0; i<sen.sentence().length(); i++) {
				total++;
				double correctLabel = sen.getItems().get(i).isCorrect() ? 1.0 : 0.0;
				double prediction = multitagger.hardPredict(sen, i);
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
	}


}

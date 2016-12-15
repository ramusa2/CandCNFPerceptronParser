package util;

import java.io.File;
import perceptron.parser.SupertaggedSentence;
import perceptron.parser.SupertaggedTrainingData;

public class DivideMultitaggedData {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 3) {
			System.out.println("Please provide three arguments: \n  " +
					"(1) the multitagged data to divide \n  " +
					"(2) the number of folds to divide into \n  " +
					"(3) the output directory to write the folds to");
			return;
		}
		String trainFile = args[0];
		int numFolds = Integer.parseInt(args[1]);
		String outputDir = args[2];
		if(!outputDir.endsWith("/")) {
			outputDir += "/";
		}
		File dir = new File(outputDir);
		dir.mkdirs();
		// Load data
		SupertaggedTrainingData data = SupertaggedTrainingData.load(trainFile);
		
		int foldIndex = (data.size()+numFolds)/numFolds;
		SupertaggedTrainingData cur = new SupertaggedTrainingData();
		int i=0;
		for(SupertaggedSentence sen : data.getData()) {
			i++;
			if(i%foldIndex==0) {
				cur.save(outputDir+"fold_"+(i/foldIndex)+"_.multitagged.txt");
				cur = new SupertaggedTrainingData();
			}
			else {
				cur.addSentence(sen.sentence(), sen.tags());
			}
		}
		
		
		/*
		// Train grammar
		// Get training data
		int[] secs = new int[20];
		for(int s=2; s<=21; s++) {
			secs[s-2] = s;
		}
		String autoDir = "data/CCGbank/AUTO";
		String pargDir = "data/CCGbank/PARG";
		Collection<Sentence> sentences = CCGbankReader.getCCGbankData(secs, autoDir, pargDir);
		Grammar grammar = new Grammar();
		SupervisedTrainingConfig c = SupervisedTrainingConfig.getDefaultConfig();
		try {
			CCGbankTrainer.readGrammarAndTrainModel(sentences, grammar, null, c);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		*/
		
		
	}

}

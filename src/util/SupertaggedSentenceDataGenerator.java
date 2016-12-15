package util;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.util.Collection;

import perceptron.parser.SupertaggedTrainingData;

public class SupertaggedSentenceDataGenerator {

	public static void main(String[] args) {
		if(args.length < 1) {
			System.out.println("Please provide at least one argument, the max sentence length; " +
					"you may also specify two more argument, i.e. an .auto file to supertag and the target file to write the multitages to.");
			return;
		}

		int maxLength = Integer.parseInt(args[0]);
		double beta = 0.004;
		System.out.println(maxLength);
		Collection<Sentence> sens;
		if(args.length==1) {
			int[] secs = new int[20];
			for(int s=2; s<=21; s++) {
				secs[s-2] = s;
			}
			String autoDir = "data/CCGbank/AUTO";
			sens = CCGbankReader.getCCGbankData(secs, autoDir);
			SupertaggedTrainingData predata = new SupertaggedTrainingData(sens, maxLength, beta);
			predata.save("wsj2-21_max"+maxLength+".multitagged.txt");
		}
		else {
			sens = CCGbankReader.getSentencesFromAutoFile(args[1]);
			SupertaggedTrainingData predata = new SupertaggedTrainingData(sens, maxLength, beta);
			predata.save(args[2]);
		}
	}
}

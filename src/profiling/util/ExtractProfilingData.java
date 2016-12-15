package profiling.util;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

public class ExtractProfilingData {

	private static int minLength = 20;
	private static int maxLength = 30;
	private static int numSens = 10;


	private static String autoDir = "data/CCGbank/AUTO";

	public static void getEvalSentences(String autoFile)  {
		ArrayList<Sentence> evalSens = new ArrayList<Sentence>();
		int[] secs = new int[]{2};
		Collection<Sentence> sens = CCGbankReader.getCCGbankData(secs, autoDir);

		for(Sentence sen : sens) {
			if(sen.length() <= maxLength && sen.length() >= minLength) {
				evalSens.add(sen);
			}
			if(evalSens.size() >= numSens) {
				break;
			}
		}
		try {
			PrintWriter pw = new PrintWriter(new File(autoFile));
			for(Sentence sen : evalSens) {
				pw.println(sen.getCCGbankParse());
			}
			pw.close();
		}
		catch(Exception e) {
			e.printStackTrace();
			System.out.println("Failed to write eval set for profiling.");
		}
	}

}

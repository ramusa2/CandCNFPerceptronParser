package util;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;

public class ReadAndWriteAUTOFromCCGbank {

	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		int sec = 0;
		String outfile = "wsj0.auto";
		PrintWriter pw = new PrintWriter(new File(outfile));
		Collection<Sentence> sens = CCGbankReader.getCCGbankData(new int[]{sec}, "data/CCGbank/AUTO");
		for(Sentence sen : sens) {
			pw.println(sen.getCCGbankParse());
		}
		pw.close();
	}

}

package util;

import illinoisParser.CCGbankReader;
import illinoisParser.LexicalToken;
import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;

/**
 * Given two arguments (a .auto file and an output file), translates the sentences
 * in the .auto file into C&C .stagged form (one sentence on each line of the output 
 * file, with each token in the word|POS|category format separated by a single space). 
 * 
 * @author ramusa2
 *
 */
public class ConvertAutoToSupertagged {

	public static void main(String[] args) {
		if(args.length < 2) {
			System.out.println("Please provide at least two arguments, a .auto file to read sentences from and a target file to write WORD|POS|CAT output to.");
			System.out.println("You may also specify the max sentence length to include as an optional third argument.");
			return;
		}
		String inputFile = args[0];
		String outputFile = args[1];
		System.out.println("Reading from input file: "+inputFile);
		System.out.println("Writing to output file: "+outputFile);
		int maxLength = Integer.MAX_VALUE;
		if(args.length >= 3) {
			maxLength = Integer.parseInt(args[3]);
			System.out.println("Maximum allowed sentence length: "+maxLength);
		}
		else {
			System.out.println("No maximum sentence length cap.");
		}
		System.out.println("Reading sentences...");
		Collection<Sentence> sens = CCGbankReader.getSentencesFromAutoFile(inputFile);
		System.out.println("Writing sentences...");
		try {
			PrintWriter pw = new PrintWriter(new File(outputFile));
			for(Sentence sen : sens) {
				pw.println(getCandCSentenceString(sen));
			}
			pw.close();
		}catch(FileNotFoundException e) {
			System.out.println("Failed to open output file: "+outputFile+"\nAborting.");
			return;
		}
		System.out.println("Done.");
	}

	private static String getCandCSentenceString(Sentence sen) {
		StringBuffer buf = new StringBuffer();
		for(LexicalToken tok : sen.getTokens()) {
			String tokString = tok.getWord()+"|"+tok.getPOS()+"|"+tok.getCategory()+" ";
			buf.append(tokString);
		}
		return buf.toString().trim();
	}

}

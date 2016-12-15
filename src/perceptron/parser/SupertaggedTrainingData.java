package perceptron.parser;

import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import supertagger.CandCSupertaggerWrapper;
import supertagger.SupertagAssignment;

public class SupertaggedTrainingData {
	
	private ArrayList<SupertaggedSentence> data;
	
	public SupertaggedTrainingData() {
		data = new ArrayList<SupertaggedSentence>();
	}
	
	/**
	 * Note: multitags each sentence with default beta parameter (0.1)
	 */
	public SupertaggedTrainingData(Collection<Sentence> sentences) {
		this(sentences, Integer.MAX_VALUE, 0.1);
	}
	
	public SupertaggedTrainingData(Collection<Sentence> sentences, int maxLength, double beta) {
		data = new ArrayList<SupertaggedSentence>();
		int tagged = 1;
		for(Sentence sen : sentences) {
			if(sen.length() <= maxLength) {
				SupertagAssignment tags = CandCSupertaggerWrapper.multi(sen, beta);
				data.add(new SupertaggedSentence(sen, tags));
			}
			tagged++;
			if(tagged % 100 == 0) {
				System.out.println("Tagged "+tagged+" out of "+sentences.size()+" sentences.");
			}
		}
	}
	
	public void addSentence(Sentence sen) {
		data.add(new SupertaggedSentence(sen));
	}
	
	public void addSentence(Sentence sen, SupertagAssignment tags) {
		data.add(new SupertaggedSentence(sen, tags));
	}
	
	public Collection<SupertaggedSentence> getData() {
		return data;
	}
	
	private static String DELIMITER = "####";
	
	public void save(String filename) {
		PrintWriter pw;
		try {
			pw = new PrintWriter(new File(filename));
			for(SupertaggedSentence sen : data) {
				pw.println(sen.toString());
				pw.println(DELIMITER);
			}
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static SupertaggedTrainingData load(String filename) {
		SupertaggedTrainingData loaded = new SupertaggedTrainingData();
		Scanner sc;
		try {
			sc = new Scanner(new File(filename));
			String cache = "";
			while(sc.hasNextLine()) {
				String line = sc.nextLine().trim();
				if(line.equals(DELIMITER)) {
					loaded.data.add(SupertaggedSentence.fromString(cache));
					cache = "";
				}
				else {
					if(cache.isEmpty()) {
						cache = line;
					}
					else {
						cache += "\n"+line;
					}
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return loaded;
	}

	public int size() {
		return data.size();
	}

}

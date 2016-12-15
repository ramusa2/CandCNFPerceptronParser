package training;

import illinoisParser.CCGbankReader;
import illinoisParser.LexicalToken;
import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;
import util.statistics.FrequencyList;

public class SupertaggerTestHarness {
	
	LSSupertagger net;
	
	ArrayList<TaggerResult> results;
	
	public SupertaggerTestHarness(LSSupertagger tagger, Collection<Sentence> data, 
			HashMap<String, Integer> wordFreqs) {
		results = new ArrayList<TaggerResult>();
		net = tagger;
		int index = 0;
		for(Sentence sen : data) {
			index++;
			if(index % 100 == 0) {
				System.out.println("Tagged "+(index)+" out of "+data.size()+ " sentences.");
			}
			SupertagAssignment tagged = net.tagSentence(sen);
			for(int i=0; i<sen.length(); i++) {
				LexicalToken token = sen.get(i);
				String word = token.getWord();
				Integer freq = wordFreqs.get(word);
				if(freq == null) {
					freq = 0;
				}
				LexicalCategoryEntry[] cats = tagged.getAll(i);
				Arrays.sort(cats);
				results.add(new TaggerResult(word, freq, token.getCategory(), cats));
			}
		}
	}
	
	HashMap<Integer, ArrayList<TaggerResult>> resByFreq;
	
	/**
	 * Extracts statistics from the tagger output
	 * @throws FileNotFoundException 
	 */
	private void evaluate() throws FileNotFoundException {
		resByFreq = new HashMap<Integer, ArrayList<TaggerResult>>();
		for(TaggerResult r : this.results) {
			addFreqResult(r);
		}
		PrintWriter pw = new PrintWriter(new File("test_harness_for_supertagger/results_by_word_freq.txt"));
		int lastF = -1;
		for(int f : cutoffs) {
			if(lastF == -1) {
				pw.println("Results for words not appearing in training data:");
			}
			else if(lastF==0) {
				pw.println("Results for words appearing 1 time in training data:");
			}
			else {
				int lower = lastF+1;
				if(lower==f) {
					pw.println("Results for words appearing "+f+" times in training data:");
				}
				else {
					pw.println("Results for words appearing between "+lower+" and "+f+" times in training data:");
				}
			}
			if(this.resByFreq.get(f) != null) {
				writeSummary(pw, this.resByFreq.get(f));
				pw.println();
			}
			lastF = f;
		}
		pw.println("Results over ALL tokens:");
		writeSummary(pw, this.results);
		
		pw.close();
	}

	private void writeSummary(PrintWriter pw, ArrayList<TaggerResult> list) {
		int pad = 40;
		int pad2 = 4;
		double vit = 0.0;
		double total = 0.0;
		double totalBeforeGold = 0.0;
		double[] betas = new double[]{0.1, 0.05, 0.025, 0.01, 0.005, 0.001};
		double[] betaCorrect = new double[betas.length];
		double[] betaCats = new double[betas.length];
		FrequencyList<Integer> goldRankHist = new FrequencyList<Integer>();
		for(TaggerResult r : list) {
			total += 1.0;
			if(r.correct()) {
				vit += 1.0;
			}
			totalBeforeGold += r.goldRank+1;
			for(int b=0; b<betas.length; b++) {
				if(r.goldProb >= betas[b]*r.vitProb) {
					betaCorrect[b] += 1.0;
				}
				betaCats[b] += r.numInBeam(betas[b]);
			}
			goldRankHist.addCount(r.goldRank());
		}
		pw.println(rpad(lpad("Tokens: ", pad), pad2)+(total));
		pw.println(rpad(lpad("Viterbi: ", pad), pad2)+(vit/total));
		pw.println(rpad(lpad("Avg. rank of gold cat: ", pad), pad2)+(totalBeforeGold/total));
		for(int b=0; b<betas.length; b++) {
			pw.println(rpad(lpad("Oracle for beta = "+betas[b]+": ", pad), pad2)+(betaCorrect[b]/total));
			pw.println(rpad(lpad("Avg. cats/word for beta = "+betas[b]+": ", pad), pad2)+(betaCats[b]/total));
		}
		pw.println();
		goldRankHist.exportToCSV(new File("test_harness_for_supertagger/gold_rank_histogram.csv"));
	}
	
	public static String rpad(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}

	public static String lpad(String s, int n) {
	    return String.format("%1$" + n + "s", s);  
	}

	private int[] cutoffs = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 40, 50, 75, 100, 150, 200, 250, 500, 1000, Integer.MAX_VALUE};
	
	private void addFreqResult(TaggerResult r) {
		int cutoff = -1;
		for(int f : cutoffs) {
			if(r.freq <= f) {
				cutoff = f;
				break;
			}
		}
		ArrayList<TaggerResult> bin = this.resByFreq.get(cutoff);
		if(bin == null) {
			bin = new ArrayList<TaggerResult>();
			this.resByFreq.put(cutoff, bin);
		}
		bin.add(r);
	}

	public static void main(String[] args) throws Exception {
		ArrayList<String> catList = getCatList();
		LSSupertagger net = new LSSupertagger(7, 60, 50, catList);
		net.loadWeights(new File("tagger"));
		Collection<Sentence> data = CCGbankReader.getCCGbankData(0, 0, "data/CCGbank/AUTO");
		Collection<Sentence> trainingData = CCGbankReader.getCCGbankData(2, 21, "data/CCGbank/AUTO");
		HashMap<String, Integer> wordFreqs = new HashMap<String, Integer>();
		HashMap<String, Integer> catFreqs = new HashMap<String, Integer>();
		for(Sentence sen : trainingData) {
			for(LexicalToken token : sen.getTokens()) {
				increment(wordFreqs, token.getWord());
				increment(catFreqs, token.getCategory());
			}
		}
		SupertaggerTestHarness test = new SupertaggerTestHarness(net, data, wordFreqs);
		test.evaluate();
	}
	
	private static void increment(HashMap<String, Integer> map, String key) {
		Integer old = map.get(key);
		if(old == null) {
			old = 0;
		}
		map.put(key, old+1);
	}

	private static ArrayList<String> getCatList() throws Exception {
		Scanner sc = new Scanner(new File("categories"));
		ArrayList<String> cats = new ArrayList<String>();
		while(sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			if(!line.isEmpty()) {
				cats.add(line);
			}
		}
		sc.close();
		return cats;
	}

}

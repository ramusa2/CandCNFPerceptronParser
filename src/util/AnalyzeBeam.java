package util;

import illinoisParser.CCGbankReader;
import illinoisParser.LexicalToken;
import illinoisParser.Sentence;

import java.io.File;
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
import training.SupertaggerTestHarness;
import training.TaggerResult;
import util.statistics.BinnableResult;
import util.statistics.BinnedResults;
import util.statistics.STBeamAnalysisResult;
import util.statistics.STBeamAnalysisResultFactory;
import util.statistics.Spreadsheet;
import util.statistics.SpreadsheetColumn;

public class AnalyzeBeam {
	
	private static double tokens = 0.0;
	
	private static char BETA = '\u03B2';
	
	private static String DIR = "/home/ramusa2/thesis/latex/figures/beamanalysis/";
	
	private static Spreadsheet freqSS = new Spreadsheet(DIR+"word_frequency", "Word frequency in sections 2-21", "Percent of tokens in section 00");

	private static Spreadsheet oracleSS = new Spreadsheet(DIR+"beam_oracle", "Word frequency in sections 2-21", "Percent of gold categories in beam");

	private static Spreadsheet ambSS = new Spreadsheet(DIR+"beam_ambiguity", "Word frequency in sections 2-21", "Average number of categories/word");

	private static Spreadsheet goldInLexiconSS = new Spreadsheet(DIR+"gold_in_lexicon", "Word frequency in sections 2-21", "Percent of gold categories in training lexicon");

	private static Spreadsheet goldRankSS = new Spreadsheet(DIR+"gold_avg_rank", "Word frequency in sections 2-21", "Average rank of gold category in beam");

	private static Spreadsheet beamCooccurSS = new Spreadsheet(DIR+"beam_cooccur_size", "Word frequency in sections 2-21", "Avg. # of non-gold cats from lexicon in beam");

	//private static Spreadsheet goldInLexiconSS = new Spreadsheet(DIR+"gold_in_lexicon", "Word frequency in sections 2-21", "Percent of gold categories in training lexicon");


	public static void main(String[] args) throws Exception {
		ArrayList<String> catList = Util.getCatList();
		LSSupertagger net = new LSSupertagger(7, 60, 50, catList);
		net.loadWeights(new File("tagger"));
		Collection<Sentence> data = CCGbankReader.getCCGbankData(0, 0, "data/CCGbank/AUTO");
		Collection<Sentence> trainingData = CCGbankReader.getCCGbankData(2, 21, "data/CCGbank/AUTO");
		HashMap<String, Integer> wordFreqs = new HashMap<String, Integer>();
		HashMap<String, Integer> catFreqs = new HashMap<String, Integer>();
		HashMap<String, HashSet<String>> seenPairs = new HashMap<String, HashSet<String>>();
		for(Sentence sen : trainingData) {
			for(LexicalToken token : sen.getTokens()) {
				increment(wordFreqs, token.getWord());
				increment(catFreqs, token.getCategory());
				HashSet<String> temp = seenPairs.get(token.getCategory());
				if(temp == null) {
					temp = new HashSet<String>();
					seenPairs.put(token.getCategory(), temp);
				}
				temp.add(token.getWord());
			}
		}


		//int[] cutoffs = new int[]{0, 10, 30, 80, 250, 500, 2000, 6000, 20000, 40000};
		int[] cutoffs = new int[]{0, 1, 5, 10, 30, 80, 250, 500, 2000, 6000, 20000, 40000};
		SpreadsheetColumn<String> xLabels = new SpreadsheetColumn<String>("Word frequency in sections 2-21");
		for(int c=0; c<cutoffs.length-1; c++) {
			String cell;
			if(cutoffs[c] == cutoffs[c+1]-1) {
				cell = cutoffs[c]+"";
			}
			else {
				cell = pplabel(cutoffs[c])+"-"+pplabel(cutoffs[c+1]);
			}
			xLabels.append(cell);
		}
		xLabels.append(pplabel(cutoffs[cutoffs.length-1])+"+");
		freqSS.addColumn(xLabels);
		oracleSS.addColumn(xLabels);
		ambSS.addColumn(xLabels);
		goldRankSS.addColumn(xLabels);
		SpreadsheetColumn<String> xLabelsWithoutZero = new SpreadsheetColumn<String>("Word frequency in sections 2-21");
		for(int c=1; c<cutoffs.length-1; c++) {
			String cell;
			if(cutoffs[c] == cutoffs[c+1]-1) {
				cell = cutoffs[c]+"";
			}
			else {
				cell = pplabel(cutoffs[c])+"-"+pplabel(cutoffs[c+1]);
			}
			xLabelsWithoutZero.append(cell);
		}
		xLabelsWithoutZero.append(pplabel(cutoffs[cutoffs.length-1])+"+");
		goldInLexiconSS.addColumn(xLabelsWithoutZero);
		beamCooccurSS.addColumn(xLabelsWithoutZero);
				
		
		BinnedResults<TaggerResult> results = new BinnedResults<TaggerResult>(
				new STBeamAnalysisResultFactory(), cutoffs);
		tokens = 0.0;
		int index = 0;
		PrintWriter pw = new PrintWriter("beam_summary.csv");
		for(Sentence sen : data) {
			index++;
			if(index % 100 == 0) {
				System.out.println("Tagged "+(index)+" out of "+data.size()+ " sentences.");
			}
			SupertagAssignment tagged = net.tagSentence(sen);
			tokens += sen.length();
			for(int i=0; i<sen.length(); i++) {
				LexicalToken token = sen.get(i);
				String word = token.getWord();
				Integer freq = wordFreqs.get(word);
				if(freq == null) {
					freq = 0;
				}
				LexicalCategoryEntry[] cats = tagged.getAll(i);
				Arrays.sort(cats);
				results.add(freq, new TaggerResult(word, freq, token.getCategory(), cats));
			}
		}
		
		double[] betas = new double[]{0.075, 0.03, 0.01, 0.005, 0.001};


		for(double beta : betas) {
			int c=0;
			pw.println();
			pw.println("Beta = "+beta);
			pw.println("Word frequency, Number of tokens, Pct. of tokens, Avg. cats/word, Pct. of tokens where gold is in beam, Pct. of tokens where gold is in beam & was seen in training data, Average categories/word in beam, Average categories/word in beam that were seen in training data, Avg. rank of gold category");
			for(BinnableResult<TaggerResult> bin : results.getBins()) {			
				printAggregateStats(cutoffs[c], (STBeamAnalysisResult) bin, seenPairs, beta, pw);
				c++;
			}			
		}
		
		// Write dev word freqs
		String freqColHeader = "Percent of tokens in section 00";
		freqSS.addColumn(freqColHeader);
		for(BinnableResult<TaggerResult> bin : results.getBins()) {		
			double total = ((STBeamAnalysisResult) bin).results().size();
			freqSS.appendData(freqColHeader, 100.0*total/tokens);
		}	
		freqSS.writeToCSV();

		pw.println("\n\n");
		HashSet<String> commonWords = new HashSet<String>();

		for(double beta : betas) {
		//for(double beta : new double[]{0.1, 0.05, 0.025, 0.01, 0.005, 0.001}) {
			SpreadsheetColumn<Double> oracleCol = new SpreadsheetColumn<Double>(BETA+"="+beta);
			SpreadsheetColumn<Double> ambCol = new SpreadsheetColumn<Double>(BETA+"="+beta);
			SpreadsheetColumn<Double> rankCol = new SpreadsheetColumn<Double>(BETA+"="+beta);
			SpreadsheetColumn<Double> lexiconCol = new SpreadsheetColumn<Double>(BETA+"="+beta);
			SpreadsheetColumn<Double> cooccurCol = new SpreadsheetColumn<Double>(BETA+"="+beta);
			int binNum = 0;
			BinnableResult<TaggerResult> lastBin = results.getBins().get(results.getBins().size()-1);
			for(BinnableResult<TaggerResult> bin : results.getBins()) {	
				double total = 0.0;
				double oracle = 0.0;
				double numCats = 0.0;
				double inLexicon = 0.0;
				double rank = 0.0;
				double nonGoldInLexiconInBeam = 0.0;
				for(TaggerResult res : ((STBeamAnalysisResult) bin).results()) {
					total++;
					if(res.goldProb >= beta*res.vitProb) {
						oracle++;
						rank += res.goldRank;
						if(!(results.getCutoffs()[binNum] == 0 && results.getCutoffs()[1] != 1) && seenPairs.containsKey(res.goldCat) && seenPairs.get(res.goldCat).contains(res.word)) {
							inLexicon++;
						}
					}
					for(LexicalCategoryEntry e : res.cats) {
						if(e.score() < beta*res.vitProb) {
							break;
						}
						numCats++;
						if( !e.category().equals(res.goldCat) &&
								seenPairs.containsKey(e.category()) && seenPairs.get(e.category()).contains(res.word)) {
							nonGoldInLexiconInBeam++;
						}
					}
				}
				oracleCol.append(100.0*oracle/total);
				ambCol.append(numCats/total);
				rankCol.append(1.0 + (rank/oracle));
				if(binNum > 0) {
					cooccurCol.append(nonGoldInLexiconInBeam/total);
					lexiconCol.append(100.0*inLexicon/total);
				}
				binNum++;
			}
			oracleSS.addColumn(oracleCol);
			ambSS.addColumn(ambCol);
			goldRankSS.addColumn(rankCol);
			goldInLexiconSS.addColumn(lexiconCol);
			beamCooccurSS.addColumn(cooccurCol);
		}
		pw.close();
		oracleSS.writeToCSV();
		ambSS.writeToCSV();
		goldRankSS.writeToCSV();
		beamCooccurSS.writeToCSV();
		goldInLexiconSS.writeToCSV();
		
		System.out.println("40k+ words:");
		for(String w : commonWords) {
			System.out.println(w);
		}
	}
	
	private static String pplabel(int i) {
		if (i <1000) {
			return i+"";
		}
		int pre = i/1000;
		int post = (i%1000)/100;
		if(post == 0) {
			return pre+"k";
		}
		return pre+"."+post+"k";
	}

	private static String ppct(double pct) {
		if(pct == 1.0) {
			return "100.0";
		}
		if (pct == 0.0) {
			return "0.0";
		}
		String ret = (new Double(pct*100)).toString();
		return ret.substring(0, Math.min(5, ret.length()));
	}
	
	private static String pnum(double num) {
		String ret = (new Double(num)).toString();
		return ret.substring(0, Math.min(ret.indexOf('.')+3, ret.length()));
	}

	private static void printAggregateStats(int cutoff, STBeamAnalysisResult bin, HashMap<String, HashSet<String>> seenPairs, double beta, PrintWriter pw) {
		double total = 0.0;
		double numInBeam = 0.0;
		double goldInBeamAndSeen = 0.0;
		double goldInBeam = 0.0;
		double numCatsInBeam = 0.0;
		double numCatsInBeamAndSeen = 0.0;
		double goldRank = 0.0;
		double goldDiv = 0.0;
		for(TaggerResult res : bin.results()) {
			total++;
			if(res.goldProb >= beta*res.vitProb) {
				goldInBeam++;
				if(seenPairs.containsKey(res.goldCat) && seenPairs.get(res.goldCat).contains(res.word)) {
					goldInBeamAndSeen++;
				}
				if(goldRank >= 0) {
					goldDiv++;
					goldRank += res.goldRank+1;
				}
			}
			for(LexicalCategoryEntry e : res.cats) {
				if(e.score() < beta*res.vitProb) {
					break;
				}
				numInBeam++;
				if(e.category().equals(res.goldCat)) {
					continue;
				}
				numCatsInBeam++;
				if(seenPairs.containsKey(e.category()) && seenPairs.get(e.category()).contains(res.word)) {
					numCatsInBeamAndSeen++;
				}
			}
		}
		pw.println(cutoff+","+total+","+(total/tokens)+","+(numInBeam/total)+","+(goldInBeam/total)+","+(goldInBeamAndSeen/total)+","+(numCatsInBeam/total)+","+(numCatsInBeamAndSeen/total)+","+(goldRank/goldDiv));
	}

	private static void increment(HashMap<String, Integer> map, String key) {
		Integer old = map.get(key);
		if(old == null) {
			old = 0;
		}
		map.put(key, old+1);
	}

}

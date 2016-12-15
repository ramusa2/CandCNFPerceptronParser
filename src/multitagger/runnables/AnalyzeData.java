package multitagger.runnables;

import illinoisParser.LexicalToken;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;
import util.Util;

public class AnalyzeData {

	private static String cvTrainingDataFile = "multitagger_training_data/cv_wsj2-21_k=20.supertagged.ser";
	private static String devDataFile = "multitagger_training_data/wsj0_k=20.supertagged.ser";



	public static void main(String[] args) throws Exception {
		//writerTest();
		MultitaggerTrainingData trainingData = MultitaggerTrainingData.loadMultitaggerTrainingData(new File(cvTrainingDataFile));
		MultitaggerTrainingData devData = MultitaggerTrainingData.loadMultitaggerTrainingData(new File(devDataFile));
		System.out.println("Read data.");
		HashMap<String, Integer> wordFreqs = getFrequencies(trainingData);
		HashMap<String, Integer> catFreqs = getCatFrequencies(trainingData);
		HashMap<String, Integer> vitCatFreqs = getViterbiCatFrequencies(trainingData);

		// Write word frequency
		writeFrequencies(devData, wordFreqs);
		System.out.println("Wrote frequency results.");

		// Write rank by position in sentence
		writePositions(devData);
		System.out.println("Wrote position results.");

		// Write rank by position in sentence
		writeSentenceLength(devData);
		System.out.println("Wrote sentence length results.");

		// Write accuracy by gold category
		writeViterbiCatFrequencies(devData, vitCatFreqs);
		System.out.println("Wrote accuracy by gold category.");
		
		// Write accuracy by Viterbi category
		writeCatFrequencies(devData, catFreqs);
		System.out.println("Wrote accuracy by Viterbi category.");
	}

	private static void writeFrequencies(MultitaggerTrainingData data, HashMap<String, Integer> wordFreqs) throws Exception {

		ArrayList<String> freqWords = getKMostFrequent(wordFreqs, 100);
		writeFrequentWordChart(data, freqWords, "words");


		// Set fields
		String file = "multitagger_data_analysis/rank_by_word_frequency.csv";
		String xLabel = "Word frequency";
		int[] frequencies = new int[]{0, 5, 10, 20, 30, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000};
		String[] binLabels = new String[frequencies.length+1];
		for(int b=0; b<frequencies.length; b++) {
			binLabels[b] = (frequencies[b])+"";
		}
		binLabels[binLabels.length-1] = (frequencies[frequencies.length-1]+1)+"+";
		int[] ranks = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
		String[] rankLabels = new String[ranks.length+1];
		for(int r=0; r<ranks.length; r++) {
			rankLabels[r] = (ranks[r])+"";
		}
		rankLabels[rankLabels.length-1] = (ranks[ranks.length-1]+1)+"+";
		double[][] values = new double[binLabels.length][rankLabels.length];
		// Get values
		for(MultitaggerTrainingSentence sen : data.getData()) {
			int pos = 0;
			for(MultitaggerTrainingItem item : sen.getItems()) {
				int rank = Math.min(item.getGoldIndex(), ranks.length);
				if(rank > -1) {
					Integer freq = wordFreqs.get(sen.sentence().getTokens()[pos].getWord());
					if(freq == null) {
						freq = 0;
					}
					values[getBin(freq, frequencies)][rank]++;
				}	
				pos++;
			}
		}
		// Print chart
		printBarChart(file, xLabel, binLabels, rankLabels, values, true, false);
		String normFile = "multitagger_data_analysis/rank_by_word_frequency.normalized.csv";
		printBarChart(normFile, xLabel, binLabels, rankLabels, values, true, true);
		String noFirstFile = "multitagger_data_analysis/rank_by_word_frequency.onlyerrors.csv";
		printBarChart(noFirstFile, xLabel, binLabels, rankLabels, values, false, false);
		String normNoFirstFile = "multitagger_data_analysis/rank_by_word_frequency.onlyerrors.normalized.csv";
		printBarChart(normNoFirstFile, xLabel, binLabels, rankLabels, values, false, true);
	}
	
	private static void writeCatFrequencies(MultitaggerTrainingData data, HashMap<String, Integer> catFreqs) throws Exception {

		ArrayList<String> freqCats = getKMostFrequent(catFreqs, 100);
		writeFrequentWordChart(data, freqCats, "goldcategory");


		// Set fields
		String file = "multitagger_data_analysis/rank_by_goldcategory_frequency.csv";
		String xLabel = "Cat frequency";
		int[] frequencies = new int[]{0, 5, 10, 20, 30, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000};
		String[] binLabels = new String[frequencies.length+1];
		for(int b=0; b<frequencies.length; b++) {
			binLabels[b] = (frequencies[b])+"";
		}
		binLabels[binLabels.length-1] = (frequencies[frequencies.length-1]+1)+"+";
		int[] ranks = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
		String[] rankLabels = new String[ranks.length+1];
		for(int r=0; r<ranks.length; r++) {
			rankLabels[r] = (ranks[r])+"";
		}
		rankLabels[rankLabels.length-1] = (ranks[ranks.length-1]+1)+"+";
		double[][] values = new double[binLabels.length][rankLabels.length];
		// Get values
		for(MultitaggerTrainingSentence sen : data.getData()) {
			int pos = 0;
			for(MultitaggerTrainingItem item : sen.getItems()) {
				int rank = Math.min(item.getGoldIndex(), ranks.length);
				if(rank > -1) {
					Integer freq = catFreqs.get(sen.sentence().getTokens()[pos].getCategory());
					if(freq == null) {
						freq = 0;
					}
					values[getBin(freq, frequencies)][rank]++;
				}	
				pos++;
			}
		}
		// Print chart
		printBarChart(file, xLabel, binLabels, rankLabels, values, true, false);
		String normFile = "multitagger_data_analysis/rank_by_goldcategory_frequency.normalized.csv";
		printBarChart(normFile, xLabel, binLabels, rankLabels, values, true, true);
		String noFirstFile = "multitagger_data_analysis/rank_by_goldcategory_frequency.onlyerrors.csv";
		printBarChart(noFirstFile, xLabel, binLabels, rankLabels, values, false, false);
		String normNoFirstFile = "multitagger_data_analysis/rank_by_goldcategory_frequency.onlyerrors.normalized.csv";
		printBarChart(normNoFirstFile, xLabel, binLabels, rankLabels, values, false, true);
	}
	
	private static void writeViterbiCatFrequencies(MultitaggerTrainingData data, HashMap<String, Integer> catFreqs) throws Exception {

		ArrayList<String> freqCats = getKMostFrequent(catFreqs, 100);
		writeFrequentWordChart(data, freqCats, "viterbicategory");


		// Set fields
		String file = "multitagger_data_analysis/rank_by_viterbicategory_frequency.csv";
		String xLabel = "Cat frequency";
		int[] frequencies = new int[]{0, 5, 10, 20, 30, 50, 100, 250, 500, 1000, 2500, 5000, 10000, 25000};
		String[] binLabels = new String[frequencies.length+1];
		for(int b=0; b<frequencies.length; b++) {
			binLabels[b] = (frequencies[b])+"";
		}
		binLabels[binLabels.length-1] = (frequencies[frequencies.length-1]+1)+"+";
		int[] ranks = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
		String[] rankLabels = new String[ranks.length+1];
		for(int r=0; r<ranks.length; r++) {
			rankLabels[r] = (ranks[r])+"";
		}
		rankLabels[rankLabels.length-1] = (ranks[ranks.length-1]+1)+"+";
		double[][] values = new double[binLabels.length][rankLabels.length];
		// Get values
		for(MultitaggerTrainingSentence sen : data.getData()) {
			int pos = 0;
			for(MultitaggerTrainingItem item : sen.getItems()) {
				int rank = Math.min(item.getGoldIndex(), ranks.length);
				if(rank > -1) {
					Integer freq = catFreqs.get(item.getCat(0));
					if(freq == null) {
						freq = 0;
					}
					values[getBin(freq, frequencies)][rank]++;
				}	
				pos++;
			}
		}
		// Print chart
		printBarChart(file, xLabel, binLabels, rankLabels, values, true, false);
		String normFile = "multitagger_data_analysis/rank_by_viterbicategory_frequency.normalized.csv";
		printBarChart(normFile, xLabel, binLabels, rankLabels, values, true, true);
		String noFirstFile = "multitagger_data_analysis/rank_by_viterbicategory_frequency.onlyerrors.csv";
		printBarChart(noFirstFile, xLabel, binLabels, rankLabels, values, false, false);
		String normNoFirstFile = "multitagger_data_analysis/rank_by_viterbicategory_frequency.onlyerrors.normalized.csv";
		printBarChart(normNoFirstFile, xLabel, binLabels, rankLabels, values, false, true);
	}

	private static void writeFrequentWordChart(MultitaggerTrainingData data,
			ArrayList<String> freqWords, String key) throws Exception {
		HashSet<String> set = new HashSet<String>();
		set.addAll(freqWords);
		HashMap<String, Integer> index = new HashMap<String, Integer>();
		String[] binLabels = new String[freqWords.size()];
		for(int i=0; i<freqWords.size(); i++) {
			index.put(freqWords.get(i), i);
			binLabels[i] = freqWords.get(i);
		}

		// Set fields
		String file = "multitagger_data_analysis/frequent_"+key+"_rank.csv";
		String xLabel = "Words";
		int[] ranks = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
		String[] rankLabels = new String[ranks.length+1];
		for(int r=0; r<ranks.length; r++) {
			rankLabels[r] = (ranks[r])+"";
		}
		rankLabels[rankLabels.length-1] = (ranks[ranks.length-1]+1)+"+";
		double[][] values = new double[binLabels.length][rankLabels.length];
		// Get values
		for(MultitaggerTrainingSentence sen : data.getData()) {
			int pos = 0;
			for(MultitaggerTrainingItem item : sen.getItems()) {
				int rank = Math.min(item.getGoldIndex(), ranks.length);
				if(rank > -1) {
					Integer i = null;
					if(key.contains("ord")) {
						 i = index.get(sen.sentence().getTokens()[pos].getWord());
					}
					else if(key.contains("viterbi")) {
						i = index.get(item.getCat(0));
					}
					else if(key.contains("ategor")) {
						 i = index.get(sen.sentence().getTokens()[pos].getCategory());
					}
					if(i != null) {
						values[i][rank]++;
					}
				}	
				pos++;
			}
		}
		// Print chart
		printBarChart(file, xLabel, binLabels, rankLabels, values, true, false);
		String normFile = "multitagger_data_analysis/frequent_"+key+"_rank.normalized.csv";
		printBarChart(normFile, xLabel, binLabels, rankLabels, values, true, true);
		String noFirstFile = "multitagger_data_analysis/frequent_"+key+"_rank.onlyerrors.csv";
		printBarChart(noFirstFile, xLabel, binLabels, rankLabels, values, false, false);
		String normNoFirstFile = "multitagger_data_analysis/frequent_"+key+"_rank.onlyerrors.normalized.csv";
		printBarChart(normNoFirstFile, xLabel, binLabels, rankLabels, values, false, true);
	}

	private static void writeSentenceLength(MultitaggerTrainingData data) throws Exception {
		// Set fields
		String file = "multitagger_data_analysis/rank_by_sentence_length.csv";
		String xLabel = "Length of sentence";
		int[] positions = new int[]{5, 10, 15, 20, 25, 30, 35, 40};
		String[] binLabels = new String[positions.length+1];
		for(int b=0; b<positions.length; b++) {
			binLabels[b] = (positions[b])+"";
		}
		binLabels[binLabels.length-1] = (positions[positions.length-1]+1)+"+";
		int[] ranks = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
		String[] rankLabels = new String[ranks.length+1];
		for(int r=0; r<ranks.length; r++) {
			rankLabels[r] = (ranks[r])+"";
		}
		rankLabels[rankLabels.length-1] = (ranks[ranks.length-1]+1)+"+";
		double[][] values = new double[binLabels.length][rankLabels.length];
		// Get values
		for(MultitaggerTrainingSentence sen : data.getData()) {
			int length = sen.sentence().length();
			for(MultitaggerTrainingItem item : sen.getItems()) {
				int rank = Math.min(item.getGoldIndex(), ranks.length);
				if(rank > -1) {
					values[getBin(length, positions)][rank]++;
				}		
			}
		}
		// Print chart
		printBarChart(file, xLabel, binLabels, rankLabels, values, true, false);
		String normFile = "multitagger_data_analysis/rank_by_sentence_length.normalized.csv";
		printBarChart(normFile, xLabel, binLabels, rankLabels, values, true, true);
		String noFirstFile = "multitagger_data_analysis/rank_by_sentence_length.onlyerrors.csv";
		printBarChart(noFirstFile, xLabel, binLabels, rankLabels, values, false, false);
		String normNoFirstFile = "multitagger_data_analysis/rank_by_sentence_length.onlyerrors.normalized.csv";
		printBarChart(normNoFirstFile, xLabel, binLabels, rankLabels, values, false, true);
	}

	private static double[][] removeFirst(double[][] values) {
		double[][] removed = new double[values.length][values[0].length];
		for(int r=0; r<values.length; r++) {
			for(int c=1; c<values[r].length; c++) {
				removed[r][c-1] = values[r][c];
			}
		}
		return removed;
	}

	private static void writePositions(MultitaggerTrainingData data) throws Exception {
		// Set fields
		String file = "multitagger_data_analysis/rank_by_position.csv";
		String xLabel = "Position of word in sentence";
		int[] positions = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 15, 20, 25, 30, 35};
		String[] binLabels = new String[positions.length+1];
		for(int b=0; b<positions.length; b++) {
			binLabels[b] = (positions[b])+"";
		}
		binLabels[binLabels.length-1] = (positions[positions.length-1]+1)+"+";
		int[] ranks = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
		String[] rankLabels = new String[ranks.length+1];
		for(int r=0; r<ranks.length; r++) {
			rankLabels[r] = (ranks[r])+"";
		}
		rankLabels[rankLabels.length-1] = (ranks[ranks.length-1]+1)+"+";
		double[][] values = new double[binLabels.length][rankLabels.length];
		// Get values
		for(MultitaggerTrainingSentence sen : data.getData()) {
			int pos = 0;
			for(MultitaggerTrainingItem item : sen.getItems()) {
				int rank = Math.min(item.getGoldIndex(), ranks.length);
				if(rank > -1) {
					values[getBin(pos+1, positions)][rank]++;
				}				
				pos++;
			}
		}
		// Print chart
		printBarChart(file, xLabel, binLabels, rankLabels, values, true, false);
		String normFile = "multitagger_data_analysis/rank_by_position.normalized.csv";
		printBarChart(normFile, xLabel, binLabels, rankLabels, values, true, true);
		String noFirstFile = "multitagger_data_analysis/rank_by_position.onlyerrors.csv";
		printBarChart(noFirstFile, xLabel, binLabels, rankLabels, values, false, false);
		String normNoFirstFile = "multitagger_data_analysis/rank_by_position.onlyerrors.normalized.csv";
		printBarChart(normNoFirstFile, xLabel, binLabels, rankLabels, values, false, true);
	}

	private static double[][] normalize(double[][] values) {
		double[][] normed = new double[values.length][values[0].length];
		for(int r=0; r<values.length; r++) {
			double Z = 0.0;
			for(double v : values[r]) {
				Z += v;
			}
			for(int c=0; c<values[r].length; c++) {
				normed[r][c] = values[r][c]/Z;
			}
		}
		return normed;
	}

	private static int getBin(int val, int[] bins) {
		int b=0;
		for(; b<bins.length; b++) {
			if(val<=bins[b]) {
				return b;
			}
		}
		if(val<bins[bins.length-1]) {
			return b-1;
		}
		return b;
	}

	private static void writerTest() throws Exception {
		String file = "multitagger_data_analysis/test.csv";
		String xLabel = "Toy Bins";
		String[] binLabels = new String[]{"Bin A", "Bin B", "Bin C", "Bin D"};
		String[] dataCategories = new String[]{"C1", "C2", "C3"};
		double[][] values = new double[][]{
				randData(dataCategories.length),
				randData(dataCategories.length),
				randData(dataCategories.length),
				randData(dataCategories.length)
		};
		printBarChart(file, xLabel, binLabels, dataCategories, values, true, false);
	}

	private static double[] randData(int dim) {
		double[] vec = new double[dim];
		for(int d=0; d<dim; d++) {
			vec[d] = Math.random();
		}
		return vec;
	}

	private static void printBarChart(String file, String xLabel, String[] binLabels, 
			String[] dataCategories, double[][] values, boolean includeFirst, boolean normalize) throws Exception {
		if(!includeFirst) {
			String[] dataCats = new String[dataCategories.length-1];
			for(int c=1; c<dataCategories.length; c++) {
				dataCats[c-1] = dataCategories[c];
			}
			dataCategories = dataCats;
			values = removeFirst(values);
		}
		if(normalize) {
			values = normalize(values);
		}
		PrintWriter pw = new PrintWriter(new File(file));
		// Header
		pw.print(wrap(xLabel));
		for(String dl : dataCategories) {
			pw.print(","+wrap(dl));
		}
		pw.println();
		// Data
		for(int b=0; b<binLabels.length; b++) {
			pw.print(wrap(binLabels[b]));
			for(double v : values[b]) {
				pw.print(","+v);
			}
			pw.println();
		}
		pw.close();
	}


	private static String wrap(String str) {
		return "\""+str+"\"";
	}

	private static HashMap<String, Integer> getFrequencies(
			MultitaggerTrainingData data) {
		HashMap<String, Integer> wordFreqs = new HashMap<String, Integer>();
		for(MultitaggerTrainingSentence sen : data.getData()) {
			for(LexicalToken tok : sen.sentence().getTokens()){
				Util.increment(wordFreqs, tok.getWord());
			}
		}
		return wordFreqs;
	}

	private static HashMap<String, Integer> getCatFrequencies(
			MultitaggerTrainingData data) {
		HashMap<String, Integer> catFreqs = new HashMap<String, Integer>();
		for(MultitaggerTrainingSentence sen : data.getData()) {
			int i=0;
			for(LexicalToken tok : sen.sentence().getTokens()) {
				Util.increment(catFreqs, tok.getCategory());
				i++;
			}
		}
		return catFreqs;
	}

	private static HashMap<String, Integer> getViterbiCatFrequencies(
			MultitaggerTrainingData data) {
		HashMap<String, Integer> catFreqs = new HashMap<String, Integer>();
		for(MultitaggerTrainingSentence sen : data.getData()) {
			for(MultitaggerTrainingItem item : sen.getItems()) {
				Util.increment(catFreqs, item.getCat(0));
			}
		}
		return catFreqs;
	}

	private static ArrayList<String> getKMostFrequent(HashMap<String, Integer> map, int k) {
		PriorityQueue<StringInt> queue = new PriorityQueue<StringInt>();
		for(String s : map.keySet()) {
			Integer f = map.get(s);
			if(queue.size() < k) {
				queue.add(new StringInt(s, f));
			}
			else {
				StringInt old = queue.peek();
				if(f > old.i) {
					queue.poll();
					queue.add(new StringInt(s, f));
				}
			}
		}

		ArrayList<String> list = new ArrayList<String>();
		while(!queue.isEmpty()) {
			list.add(queue.poll().str);
		}
		Collections.reverse(list);
		return list;
	}

}

class StringInt implements Comparable<StringInt> {
	String str;

	Integer i;

	StringInt(String s, int in) {
		this.str = s;
		this.i = in;
	}

	@Override
	public int compareTo(StringInt o) {
		return this.i - o.i;
	}

	public String toString() {
		return this.str+" "+this.i;
	}
}

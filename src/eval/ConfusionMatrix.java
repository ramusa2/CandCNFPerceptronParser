package eval;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class ConfusionMatrix {

	private static final int SIG_FIGS = 2; 
	private static final int CONF_FREQ_MIN = 10; 

	private double total;
	private HashSet<String> seenCats;
	private HashMap<String, Double> goldCounts;
	private HashMap<String, Double> guessCounts;

	private HashMap<String, HashMap<String, Double>> matrix;

	public ConfusionMatrix() {
		total = 0.0;
		seenCats = new HashSet<String>();
		goldCounts = new HashMap<String, Double>();
		guessCounts = new HashMap<String, Double>();
		matrix = new HashMap<String, HashMap<String, Double>>();
	}

	public void add(String gold, String guess) {
		add(gold, guess, 1.0);
	}

	public void add(String gold, String guess, double count) {
		total += count;
		seenCats.add(gold);
		seenCats.add(guess);
		addToCounts(goldCounts, gold, count);
		addToCounts(guessCounts, guess, count);
		addToMatrix(matrix, gold, guess, count);
	}

	private void addToCounts(HashMap<String, Double> counts, String key, double count) {
		Double d = counts.get(key);
		if(d == null) {
			d = 0.0;
		}
		counts.put(key,  d+count);
	}

	private void addToMatrix(HashMap<String, HashMap<String, Double>> mat,
			String rKey, String cKey, double count) {
		HashMap<String, Double> row = mat.get(rKey);
		if(row == null) {
			row = new HashMap<String, Double>();
		}
		addToCounts(row, cKey, count);
		mat.put(rKey, row);
	}

	private ArrayList<String> keysSortedDescending(HashMap<String, Double> counts) {
		ArrayList<StringDouble> temp = new ArrayList<StringDouble>();
		for(String key : counts.keySet()) {
			temp.add(new StringDouble(key, counts.get(key)));
		}
		Collections.sort(temp);
		ArrayList<String> ret = new ArrayList<String>();
		for(StringDouble sd : temp) {
			ret.add(sd.s());
		}
		return ret;
	}

	public String getFullMatrix() {
		String str = "";
		ArrayList<String> rowLabels = keysSortedDescending(this.goldCounts);
		ArrayList<String> colLabels = new ArrayList<String>();
		colLabels.addAll(rowLabels);
		for(String lab : this.seenCats) {
			if(this.goldCounts.get(lab) == null) {
				colLabels.add(lab);
			}
		}
		int firstTabLength = 0;
		for(String row : rowLabels) {
			firstTabLength = Math.max(firstTabLength, row.length()+2);
		}
		String firstTab = pad("", firstTabLength);
		ArrayList<Integer> cumLengths = new ArrayList<Integer>();
		//cumLengths.add(firstTabLength);
		for(String col : colLabels) {
			double len = Math.max(4, col.length()+2);
			Double freqMax = this.goldCounts.get(col);
			if(freqMax == null) {
				freqMax = 0.0;
			}
			len = Math.max(len, (freqMax+"").length()+2);
			cumLengths.add(((int)len));
		}
		// header
		str += firstTab;
		for(int i=0; i<colLabels.size(); i++) {
			str += lpad(colLabels.get(i), cumLengths.get(i));
		}
		str += "\n";
		for(String row : rowLabels) {
			HashMap<String, Double> freqs = this.matrix.get(row);
			String rowString = pad(row, firstTabLength);
			for(int i=0; i<colLabels.size(); i++) {
				String cell = freqs.get(colLabels.get(i))+"";
				if(cell.equals("null")) {
					cell = "---";
				}
				if(row.equals(colLabels.get(i))) {
					cell = "*"+cell;
				}
				rowString += lpad(cell, cumLengths.get(i));
			}
			str += rowString+"\n";
		}
		return str;		
	}


	public String getTokenRecall() {
		ArrayList<String> labels = keysSortedDescending(this.goldCounts);
		String ret = pad("Tokens", 8)+pad("Correct", 8)+pad("Recall", 8)+"Category"+"\n";
		for(String gold : labels) {
			double tot = this.goldCounts.get(gold);
			double cor = this.matCount(gold, gold);
			ret += pad(((int)tot)+"", 8)+pad(((int)cor)+"", 8)+pad(pct(cor, tot), 8)+gold+"\n";
		}
		return ret;
	}

	private Double matCount(String gold, String guess) {
		HashMap<String, Double> row = this.matrix.get(gold);
		if(row == null) {
			return 0.0;
		}
		Double count = row.get(guess);
		if(count == null) {
			return 0.0;
		}
		return count;
	}

	public String getSparseMatrixSortedByGoldLabels() {
		ArrayList<String> labels = keysSortedDescending(this.goldCounts);
		String ret = "";
		for(String gold : labels) {
			ret += rowToString(gold) +"\n";
		}
		return ret;
	}

	private String rowToString(String gold) {
		HashMap<String, Double> row = this.matrix.get(gold);
		double count = this.goldCounts.get(gold);
		double correct = this.matCount(gold, gold);
		String rowString = gold+"\n"
				+"\tTokens:        "+this.goldCounts.get(gold)+"  ("+pct(count, this.total)+"% of all tokens)\n"
				+"\tRecall:        "+pct(correct, count)+"\n"
				+"\tConfusions:\n";
		ArrayList<String> guesses = keysSortedDescending(row); 
		for(String guess : guesses) {
			double guessCount = this.matCount(gold, guess);
			rowString += "\t\t"+pad(pct(guessCount, count)+"", 10)+guess+"\n";
		}
		return rowString;
	}

	private String pad(String s, int l) {
		while(s.length() < l) {
			s += " ";
		}
		return s+" ";
	}

	private String lpad(String s, int l) {
		while(s.length() < l) {
			s = " "+s;
		}
		return " "+s;
	}

	private String pct(double num, double denom) {
		if(num==0.0 || denom == 0.0) {
			return "0.0";
		}
		if(num== denom) {
			return "1.0";
		}
		String d = (100*num/denom)+ "";
		int l = d.indexOf(".")+SIG_FIGS+1;
		l = Math.min(l,  d.length());
		return (d+"").substring(0, l);
	}

	public void writeConfusionsToDir(File dir, String fileprefix) {
		String dirName;
		try {
			dirName = dir.getCanonicalPath();
			if(!dirName.endsWith("/")) {
				dirName +="/";
			}
			writeFile(dirName+fileprefix+"conf.recall.txt", this.getTokenRecall());
			writeFile(dirName+fileprefix+"conf.fullmatrix.txt", this.getFullMatrix());
			writeFile(dirName+fileprefix+"conf.sparsematrix.txt", this.getSparseMatrixSortedByGoldLabels());
			writeFile(dirName+fileprefix+"conf.pairwise_confusions.txt", this.getFrequentConfusions(CONF_FREQ_MIN));
		} catch (IOException e) {
			System.out.println("Failed to write confusion files to directory: "+dir);
		}
		
	}
	
	private String getFrequentConfusions(int minFreq) {
		ArrayList<StringDouble> cellFreqs = new ArrayList<StringDouble>();
		for(String gold : this.goldCounts.keySet()) {
			HashMap<String, Double> row = this.matrix.get(gold);
			for(String guess :  row.keySet()) {
				Double count = row.get(guess);
				if(count != null && count >= minFreq) {
					cellFreqs.add(new StringDouble(gold+" "+guess, count));
				}
			}
		}
		Collections.sort(cellFreqs);
		String ret = lpad("Conf Freq", 12)+lpad("Gold Freq", 12)+lpad("% of Gold", 12)
				+pad("    Gold", 40)+pad("    Guess", 20)+"\n";
		for(StringDouble sd : cellFreqs) {
			String gold = sd.s().split("\\s+")[0];
			String guess = sd.s().split("\\s+")[1];
			if(!gold.equals(guess)) {
				double freq = sd.d();
				double goldFreq = this.goldCounts.get(gold);
				ret += lpad(((int)freq)+"", 12)+lpad(((int)goldFreq)+"", 12)+lpad(pct(freq, goldFreq), 12)
						+pad("    "+gold, 40)+"    "+guess+"\n";
			}
		}
		return ret;
	}

	private void writeFile(String filename, String contents) {
		try {
			PrintWriter pw = new PrintWriter(new File(filename));
			pw.print(contents);
			pw.close();		
		}
		catch(Exception e) {
			System.out.println("Failed to write confusion file: "+filename);
		}
	}

	public String getSummaryString() {
		String summary = "Tokens seen: "+((int)total)+"\n";
		summary += "Lexical category accuracy: ";
		double cor = 0.0;
		for(String gold : this.goldCounts.keySet()) {
			cor += this.matCount(gold, gold);
		}
		return summary+pct(cor, total)+"%";
	}
}

class StringDouble implements Comparable<StringDouble> {
	private final String key;
	private final double val;

	StringDouble(String s, double d) {
		key = s;
		val = d;
	}

	String s() {
		return key;
	}

	double d() {
		return val;
	}

	@Override
	public int compareTo(StringDouble o) {
		if(o.d() == this.d()) {
			return 0;
		}
		else if(o.d() > this.d()) {
			return 1;
		}
		return -1;
	}
}

package eval;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class PredArgDepResults {

	private static final int SIG_FIGS = 2; 

	double sens;
	double parsedSens;
	double goldDepsTotal;
	double parserDepsTotal;
	double matchedUnlabeled;
	double matchedLabeled;
	
	private HashMap<String, Double> depTotals;
	private HashMap<String, Double> depLabeledCorrect;
	
	public PredArgDepResults() {
		sens = 0.0;
		parsedSens = 0.0;
		goldDepsTotal = 0.0;
		parserDepsTotal = 0.0;
		matchedUnlabeled = 0.0;
		matchedLabeled = 0.0;
		depTotals = new HashMap<String, Double>();
		depLabeledCorrect = new HashMap<String, Double>();
	}
	
	public void addResult(DepSet gold, DepSet guess) {
		sens++;
		goldDepsTotal += gold.deps.size();
		for(Dep d : gold.deps) {
			add(d.desc(), depTotals);
		}
		if(guess.deps.size() > 0) {
			parsedSens++;
			parserDepsTotal+=guess.deps.size();
			matchedUnlabeled += guess.numMatchedUnlabeled(gold);
			matchedLabeled += guess.numMatchedLabeled(gold);
			for(Dep d : gold.deps) {
				if(guess.matchesLabeledDirected(d)) {
					add(d.desc(), depLabeledCorrect);
				}
			}
		}
	}

	private void add(String d, HashMap<String, Double> map) {
		Double dub = map.get(d);
		if(dub == null) {
			dub = 1.0;
		}
		else {
			dub += 1.0;
		}
		map.put(d, dub);
	}

	public void writeDepResultsToDir(File dir, String fileprefix) {
		String dirName;
		try {
			dirName = dir.getCanonicalPath();
			if(!dirName.endsWith("/")) {
				dirName +="/";
			}
			writeFile(dirName+fileprefix+"deps.summary.txt", this.getSummaryString());
			writeFile(dirName+fileprefix+"deps.labeled_accuracy_by_dep.txt", this.getDepAccuracyString());
		} catch (IOException e) {
			System.out.println("Failed to write lexcat results to directory: "+dir);
		}
	}

	private String getDepAccuracyString() {
		String[] header = new String[]{"Dependency/Arg. Slot", "Recall", "% Freq.", "Raw Freq.", "# Errors"};
		int[] colLengths = new int[]{60, 15, 15, 15, 15};
		String str = "";
		for(int i=0; i<colLengths.length; i++) {
			str += pad(header[i], colLengths[i]);
		}
		str+= "\n";
		ArrayList<String> sorted = depsSortedDescending(depTotals);
		double total = this.goldDepsTotal;
		for(String d : sorted) {
			double depTotal = this.depTotals.get(d);
			Double depCorrect = this.depLabeledCorrect.get(d);
			if(depCorrect == null) {
				depCorrect = 0.0;
			}
			String rowString = "";
			String[] entries = new String[]{d, pct(depCorrect, depTotal)+"", pct(depTotal, total)+"", depTotal+"", (depTotal-depCorrect)+""};
			for(int i=0; i<colLengths.length; i++) {
				rowString += pad(entries[i], colLengths[i]);
			}
			str += rowString+"\n";
		}
		return str;
	}

	private ArrayList<String> depsSortedDescending(HashMap<String, Double> map) {
		ArrayList<StringDouble> temp = new ArrayList<StringDouble>();
		for(String dep : map.keySet()) {
			temp.add(new StringDouble(dep, map.get(dep)));
		}
		Collections.sort(temp);
		ArrayList<String> ret = new ArrayList<String>();
		for(StringDouble sd : temp) {
			ret.add(sd.s());
		}
		return ret;
	}

	private String pad(String s, int l) {
		while(s.length() < l) {
			s += " ";
		}
		return s+" ";
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

	public String getSummaryString() {
		String ret = "";
		ret += "Number of sentences seen: "+sens+"\n";
		ret += "Number of sentences parsed: "+parsedSens+"\n";
		ret += "Number of gold deps: "+goldDepsTotal+"\n";
		ret += "Number of parser deps: "+parserDepsTotal+"\n";
		ret += "Matched unlabeled deps: "+matchedUnlabeled+"\n";
		ret += "Matched labeled deps: "+matchedLabeled+"\n";
		double unlabeledRecall = 100*matchedUnlabeled/goldDepsTotal;
		double unlabeledPrecision = 100*matchedUnlabeled/parserDepsTotal;
		double unlabeledF1 = (2 * unlabeledPrecision * unlabeledRecall) / (unlabeledPrecision + unlabeledRecall);
		double labeledRecall = 100*matchedLabeled/goldDepsTotal;
		double labeledPrecision = 100*matchedLabeled/parserDepsTotal;
		double labeledF1 = (2 * labeledPrecision * labeledRecall) / (labeledPrecision + labeledRecall);
		ret += format("UR: ", unlabeledRecall)+"\n";
		ret += format("UP: ", unlabeledPrecision)+"\n";
		ret += format("UF1: ", unlabeledF1)+"\n";
		ret += format("LR: ", labeledRecall)+"\n";
		ret += format("LP: ", labeledPrecision)+"\n";
		ret += format("LF1: ", labeledF1)+"\n";
		return ret;
	}


	private static String format(String label, double score) {
		return String.format("%-6s %2.3f", label, score);
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
}

class DepDouble implements Comparable<DepDouble> {
	private final Dep key;
	private final double val;

	DepDouble(Dep dep, double d) {
		key = dep;
		val = d;
	}

	Dep dep() {
		return key;
	}

	double d() {
		return val;
	}

	@Override
	public int compareTo(DepDouble sd) {
		if(sd.d() == this.d()) {
			return 0;
		}
		else if(sd.d() > this.d()) {
			return 1;
		}
		return -1;
	}
}

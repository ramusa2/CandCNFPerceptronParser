package util.evaluation;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;

import eval.ConfusionMatrix;

import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;
import util.statistics.FrequencyList;
import util.statistics.FrequencyListEntry;

public class AnalyzeSupertaggerErrors {
	
	/**
	 * Takes three arguments:
	 * 
	 *   (0) Directory from which to load the tagger
	 *   (1) .auto or .auto.gz file from which to read the eval data
	 *   (2) Multitagging beam width (beta)
	 *   (3) Path to log/output directory
	 */
	public static void main(String[] args) throws Exception {
		if(args.length < 3) {
			System.out.println("Takes three arguments:" +
					"\n  (0) Directory from which to load the tagger" +
					"\n  (1) .auto/.auto.gz file from which to read the eval data" +
					"\n  (2) Multitagging beam width (beta)" +
					"\n  (3) Path to log/output file");
			return;
		}
		// Load tagger
		ArrayList<String> cats = getCatList("categories");
		LSSupertagger net = 
				new LSSupertagger(7, 60, 50, cats);
		net.loadWeights(new File(args[0]));
		
		// Load data
		Collection<Sentence> sentences = CCGbankReader.getSentencesFromAutoFile(args[1]);
		
		// Evaluate and Analyze
		double beta = Double.parseDouble(args[2]);
		SupertaggerResults results = new SupertaggerResults();
		for(Sentence sen : sentences) {
			SupertagAssignment tags = net.tagSentence(sen, beta);
			results.addResults(sen, tags);
		}
		
		// Write output
		File dir = new File(args[3]);
		dir.mkdirs();
		HashSet<String> knownCats = new HashSet<String>();
		knownCats.addAll(cats);
		results.writeResults(dir, beta, knownCats);
	}
	


	private static ArrayList<String> getCatList(String catFile) throws Exception {
		Scanner sc = new Scanner(new File(catFile));
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

class SupertaggerResults {
	
	HashMap<String, CategoryResult> catRes;
	
	SupertaggerResults() {
		this.catRes = new HashMap<String, CategoryResult>();
	}
	
	public void writeResults(File dir, double beta, HashSet<String> knownCats) throws Exception {
		String d = dir.getPath()+File.separator;
		FrequencyList<CategoryResult> catsByFreq = new FrequencyList<CategoryResult>();
		FrequencyList<CategoryResult> catsByMisses = new FrequencyList<CategoryResult>();
		for(CategoryResult res : this.catRes.values()) {
			catsByFreq.addCount(res, res.total);
			catsByMisses.addCount(res, res.total - res.inBeam());
		}
		List<FrequencyListEntry<CategoryResult>> byFreq = catsByFreq.sortedList();
		List<FrequencyListEntry<CategoryResult>> byMisses = catsByMisses.sortedList();
		
		// Write misses
		PrintWriter pwMisses = new PrintWriter(new File(d+"beam_misses.beta="+beta+".txt"));
		pwMisses.println(rpad("Category:", 40)+rpad("# Misses:", 15)+rpad("# Tokens:", 15)+"Known Category?");
		for(FrequencyListEntry<CategoryResult> e : byMisses) {
			CategoryResult res = e.value();
			pwMisses.println(rpad(res.cat, 40)+rpad(""+(e.frequency()), 15)+rpad(""+(res.total), 15) +knownCats.contains(res.cat));
		}
		pwMisses.close();
		
		// Write cats per word histogram
		HashMap<Integer, Integer> freqs = new HashMap<Integer, Integer>();
		for(FrequencyListEntry<CategoryResult> e : byMisses) {
			for(Entry<Integer, Integer> e2 : e.value().catsPerWord.entrySet()) {
				add(freqs, e2.getKey(), e2.getValue());
			}
		}
		PrintWriter pwHist = new PrintWriter(new File(d+"cats_per_word_hist.csv"));
		pwHist.println("\"Cats. Per Word\",\"Frequency\"");
		for(int i=1; i<=knownCats.size(); i++) {
			Integer f = freqs.get(i);
			if(f==null) {
				f = 0;
			}
			pwHist.println(i+","+f);
		}
		pwHist.close();
		
		// Write Viterbi confusion matrix
		ConfusionMatrix conf = new ConfusionMatrix();
		for(FrequencyListEntry<CategoryResult> e : byFreq) {
			for(Entry<String, Integer> e2 : e.value().vitCats.entrySet()) {
				conf.add(e.value().cat, e2.getKey(), e2.getValue());
			}
		}
		PrintWriter pwVitConf = new PrintWriter(new File(d+"viterbi_confusion.txt"));
		pwVitConf.print(conf.getSparseMatrixSortedByGoldLabels());
		pwVitConf.close();
		
		// Write confusion matrix for beam misses
		ConfusionMatrix missConf = new ConfusionMatrix();
		for(FrequencyListEntry<CategoryResult> e : byMisses) {
			for(Entry<String, Integer> e2 : e.value().missCats.entrySet()) {
				missConf.add(e.value().cat, e2.getKey(), e2.getValue());
			}
		}
		PrintWriter pwMissConf = new PrintWriter(new File(d+"viterbi_confusion.beam_misses.txt"));
		pwMissConf.print(missConf.getSparseMatrixSortedByGoldLabels());
		pwMissConf.close();
		
		// Write summary of beam misses
		PrintWriter pwMissSumm = new PrintWriter(new File(d+"beam_miss_details.txt"));
		for(FrequencyListEntry<CategoryResult> e : byMisses) {
			if(e.frequency() > 0) {
				CategoryResult r = e.value();
				String cat = r.cat;
				String line = rpad(cat, 40)+rpad((r.total-r.inBeam())+"/"+r.total+" missed", 30)+(knownCats.contains(cat)?"Known Category":"Unknown Category");
				pwMissSumm.println(line);
				pwMissSumm.println("Average # of cats/word in beam: "+(r.totalCatsInBeam/r.total));
				FrequencyList<String> list = new FrequencyList<String>();				
				for(Entry<String, Double> e2 : e.value().missCatProb.entrySet()) {
					list.addCount(e2.getKey(), e2.getValue());
				}
				pwMissSumm.println("\tCategories in beam (with total marginal probability):");
				for(FrequencyListEntry<String> e3 : list.sortedList()) {
					pwMissSumm.println("\t\t"+rpad(e3.value(), 40)+e3.frequency());
				}
				pwMissSumm.println();
			}
		}
		pwMissSumm.close();
		
		
		// Write summary of beam
		double correct = 0.0;
		double total = 0.0;
		double totalCats = 0.0;
		for(FrequencyListEntry<CategoryResult> e : byFreq) {
			total += e.frequency();
			Integer cor = e.value().catsInBeam.get(e.value().cat);
			if(cor != null)
				correct += cor;
			totalCats += e.value().totalCatsInBeam;
		}
		PrintWriter pwSumm = new PrintWriter(new File(d+"summary.txt"));
		pwSumm.println(lpad("Beam width: ", 30)+beta);
		pwSumm.println(lpad("Overall oracle accuracy: ", 30)+(correct/total));
		pwSumm.println(lpad("Average cats/word: ", 30)+(totalCats/total));
		pwSumm.close();
		
	}
	
	public static String rpad(String s, int n) {
	     return String.format("%1$-" + n + "s", s);  
	}

	public static String lpad(String s, int n) {
	    return String.format("%1$" + n + "s", s);  
	}

	void addResults(Sentence sen, SupertagAssignment tags) {
		for(int i=0; i<sen.length(); i++) {
			LexicalCategoryEntry vit = tags.getBest(i);
			String gold = tags.getGold(i);
			LexicalCategoryEntry[] entries = tags.getAll(i);
			if(!this.catRes.containsKey(gold)) {
				this.catRes.put(gold, new CategoryResult(gold));
			}
			this.catRes.get(gold).addTokenResult(vit, entries);
		}
	}
	

	
	private static void add(HashMap<Integer, Integer> map, Integer key, Integer value) {
		Integer val = map.get(key);
		if(val == null) {
			val = 0;
		}
		map.put(key, val + value);
	}
	
}

class CategoryResult implements Comparable<CategoryResult> {
	
	String cat;
	
	int total, totalCatsInBeam;
	
	HashMap<String, Integer> catsInBeam;
	HashMap<String, Integer> vitCats;
	HashMap<Integer, Integer> catsPerWord;
	HashMap<String, Integer> missCats;
	HashMap<String, Double> missCatProb;
	
	CategoryResult(String category) {
		this.cat = category;
		this.catsInBeam = new HashMap<String, Integer>();
		this.vitCats = new HashMap<String, Integer>();
		this.missCats = new HashMap<String, Integer>();
		this.missCatProb = new HashMap<String, Double>();
		this.catsPerWord = new HashMap<Integer, Integer>();
	}

	public void addTokenResult(LexicalCategoryEntry vit,
			LexicalCategoryEntry[] entries) {
		this.total += 1;
		this.totalCatsInBeam += entries.length;
		add(this.vitCats, vit.category());
		boolean inBeam = false;
		for(LexicalCategoryEntry entry : entries) {
			add(this.catsInBeam, entry.category());
			inBeam = inBeam || entry.category().equals(this.cat);
		}
		add(this.catsPerWord, entries.length);
		if(!inBeam) {
			add(this.missCats, vit.category());
			for(LexicalCategoryEntry entry : entries) {
				add(this.missCatProb, entry.category(), entry.score());
			}
		}
	}
	
	int inBeam() {
		Integer f = this.catsInBeam.get(this.cat);
		if(f==null)
			return 0;
		return f;
	}
	
	private static void add(HashMap<String, Integer> map, String key) {
		Integer val = map.get(key);
		if(val == null) {
			val = 0;
		}
		map.put(key, val + 1);
	}
	
	private static void add(HashMap<String, Double> map, String key, double v) {
		Double val = map.get(key);
		if(val == null) {
			val = 0.0;
		}
		map.put(key, val + v);
	}
	
	private static void add(HashMap<Integer, Integer> map, Integer key) {
		Integer val = map.get(key);
		if(val == null) {
			val = 0;
		}
		map.put(key, val + 1);
	}

	@Override
	public int compareTo(CategoryResult o) {
		return this.cat.compareTo(o.cat);
	}
}

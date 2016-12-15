package supertagger;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;

import supertagger.lewissteedman.LSSupertagger;

public class TestLSSupertagger {
	
	public static void main(String[] args) throws Exception {
		int iter = 10;
		if(args.length > 0)
			iter = Integer.parseInt(args[0]);
		tagSec0(iter);
		//benchmarkTagger();
	}

	private static void benchmarkTagger(int iter) throws Exception {
		int numSens = 2000;
		double beta = 0.1;
		Collection<Sentence> data = CCGbankReader.getCCGbankData(0, 0, "data/CCGbank/AUTO");
		System.out.println("Tagging "+numSens+" sentences.");
		System.out.println("Beta: "+beta);
		/*
		CandCSupertaggerWrapper candc = CandCSupertaggerWrapper.getTagger();
		long candcStart = System.currentTimeMillis();
		int c=0;
		for(Sentence sen : data) {
			if(c >= numSens) {
				break;
			}
			CandCSupertaggerWrapper.multi(sen, beta);
			c++;
		}
		long candcStop = System.currentTimeMillis();
		double candcTime = (candcStop-candcStart)/1000.0;
		System.out.println("C&C time: "+candcTime+" seconds");

*/
		ArrayList<String> catList = getCatList();
		LSSupertagger net = new LSSupertagger(7, 60, 50, catList);
		net.loadWeights(new File("tagger_sec2-21_window=7_iter="+iter));
		long lsStart = System.currentTimeMillis();
		int c=0;
		for(Sentence sen : data) {
			if(c >= numSens) {
				break;
			}
			net.tagSentence(sen);
			c++;
		}		
		long lsStop = System.currentTimeMillis();
		double lsTime = (lsStop-lsStart)/1000.0;
		System.out.println("LS time: "+lsTime+" seconds");
		
	}

	private static void tagSec0(int iter) throws Exception {
		ArrayList<String> catList = getCatList();
		HashSet<String> knownCats = new HashSet<String>();
		knownCats.addAll(catList);
		//SupertaggerNetNoAdditionalHiddenLayer net = new SupertaggerNetNoAdditionalHiddenLayer(5, 60, catList);
		//net.load(new File("tagger_sec2-21_iter=8"));
		LSSupertagger net = new LSSupertagger(7, 60, 50, catList);
		net.loadWeights(new File("tagger_sec2-21_window=7_iter="+iter));
		System.out.println("Loaded tagger.");
		Collection<Sentence> data = CCGbankReader.getCCGbankData(0, 0, "data/CCGbank/AUTO");
		int index = 0;
		double total = 0.0;
		double correct = 0.0;
		double oracle = 0.0;
		double beta = 0.01;
		double totalCats = 0.0;
		for(Sentence sen : data) {
			SupertagAssignment predictions = net.tagSentence(sen);
			for(int i=0; i<sen.length(); i++) {
				String cat = sen.get(i).getCategory();
				if(knownCats.contains(cat)) {
					LexicalCategoryEntry entry = predictions.getBest(i);
					total++;
					if(entry.category().equals(cat)) {
						correct++;
					}
					double cutoff = entry.score()*beta;
					for(LexicalCategoryEntry other : predictions.getAll(i)) {
						double score = other.score();
						if(score >= cutoff) {
							totalCats++;
							if(other.category().equals(cat)) {
								oracle++;
							}
						}
					}
				}
			}
			index++;
			if(index%100 == 0) {
				System.out.println("Tagged "+index+" out of "+data.size()+" sentences.");
			}
		}
		System.out.println("Token accuracy: "+(correct/total));
		System.out.println("Oracle coverage (beta = 0.1): "+(oracle/total));
		System.out.println("Average cats/word (beta = 0.1): "+(totalCats/total));
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

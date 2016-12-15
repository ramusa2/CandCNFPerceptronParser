package training;

import supertagger.LexicalCategoryEntry;

public class TaggerResult {
	
	public String word, goldCat, vitCat;
	
	public int freq, goldRank;
	
	public double vitProb;

	public double goldProb;
	
	public LexicalCategoryEntry[] cats;
	
	public TaggerResult(String w, int f, String gold, LexicalCategoryEntry[] tagged) {
		word = w;
		freq = f;
		goldCat = gold;
		cats = tagged;
		goldRank = -1; // -1 means that gold is not in set
		for(int r=0; r<cats.length; r++) {
			if (cats[r].category().equals(goldCat)) {
				goldRank = r;
				goldProb = cats[r].score();
				break;
			}
		}
		vitProb = cats[0].score();
		vitCat = cats[0].category();
	}
	
	public boolean correct() {
		return this.goldCat.equals(this.vitCat);
	}

	public int numInBeam(double beta) {
		double cutoff = beta*this.vitProb;
		int numInBeam = 0;
		for(LexicalCategoryEntry cat : this.cats) {
			if(cat.score() >= cutoff) {
				numInBeam++;
			}
			else {
				break;
			}
		}
		return numInBeam;
	}

	public int numAboveProbThreshold(double thresh) {
		int numInBeam = 0;
		for(LexicalCategoryEntry cat : this.cats) {
			if(cat.score() > thresh) {
				numInBeam++;
			}
			else {
				break;
			}
		}
		return numInBeam;
	}
	
	public int goldRank() {
		return this.goldRank;
	}
}

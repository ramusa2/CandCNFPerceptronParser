package supertagger.lsbeta;

import java.io.Serializable;
import java.util.Arrays;

import supertagger.LexicalCategoryEntry;
import supertagger.SupertagAssignment;

public class MultitaggerTrainingItem implements Serializable {
	
	int goldCategoryIndex;
	
	double[] probs;
	
	String[] topKCats;
	
	public MultitaggerTrainingItem() {}
	
	public MultitaggerTrainingItem(SupertagAssignment tagAssignment, int i, int topK) {
		this.goldCategoryIndex = -1;
		this.probs = new double[topK];
		this.topKCats = new String[topK];
		int k = 0;
		LexicalCategoryEntry[] tags = tagAssignment.getAll(i);
		Arrays.sort(tags);
		String gold = tagAssignment.getGold(i);
		while(k<tags.length && (k < topK || goldCategoryIndex == -1)) {
			LexicalCategoryEntry tag = tags[k];
			if(tag.category().equals(gold)) {
				this.goldCategoryIndex = k;
			}
			if(k < topK) {
				this.probs[k] = tag.score();
				this.topKCats[k] = tag.category();
			}
			k++;
		}
	}
	
	public double getProb(int i) {
		return this.probs[i];
	}
	
	public String getCat(int i) {
		return this.topKCats[i];
	}

	public int getGoldIndex() {
		return this.goldCategoryIndex;
	}
	
	public boolean isGold(int i) {
		return i == this.goldCategoryIndex;
	}
	
	public double getViterbiProb() {
		return this.probs[0];
	}

	public boolean isCorrect() {
		return this.isGold(0) && this.probs.length > this.goldCategoryIndex;
	}
}

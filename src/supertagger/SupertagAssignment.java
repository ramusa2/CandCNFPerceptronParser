package supertagger;

import java.util.ArrayList;
import java.util.PriorityQueue;

import illinoisParser.LexicalToken;
import illinoisParser.Sentence;

public class SupertagAssignment {
	
	private Sentence sentence;
	
	private ArrayList<PriorityQueue<LexicalCategoryEntry>> lexcats;
	
	public SupertagAssignment(Sentence sen) {
		sentence = sen;
		lexcats = new ArrayList<PriorityQueue<LexicalCategoryEntry>>();
		for(int i=0; i<sen.length(); i++) {
			lexcats.add(new PriorityQueue<LexicalCategoryEntry>());
		}
	}

	public void addLexcat(int i, LexicalCategoryEntry lexcat) {
		lexcats.get(i).add(lexcat);
	}
	
	public void addLexcat(int i, String cat, double score) {
		this.addLexcat(i, new LexicalCategoryEntry(cat, score));
	}
	
	public LexicalCategoryEntry getBest(int i) {
		return lexcats.get(i).peek();
	}
	
	public LexicalCategoryEntry[] getAll(int i) {
		PriorityQueue<LexicalCategoryEntry> queue = lexcats.get(i); 
		return queue.toArray(new LexicalCategoryEntry[queue.size()]);
	}
	
	public Sentence sentence() {
		return sentence;
	}
	


	@Override
	public String toString() {
		String ret = "";
		for(int i=0; i<sentence.length(); i++) {
			LexicalToken lt = sentence.getTokens()[i];
			ret += lbuf(lt.getWord(), 3)+"  "+lbuf(lt.getPOS().toString(), 1)+lbuf("", 1);
			for(LexicalCategoryEntry st : 
				lexcats.get(i).toArray(new LexicalCategoryEntry[lexcats.get(i).size()])) {
				ret += lbuf(st.toString(), 3);
			}
			ret += "\n";
		}
		return ret;
	}
	
	private String lbuf(String s, int tabs) {
		int l = s.length();
		if(l >= tabs*4) {
			return s+" \t";
		}
		int numTabs = ((tabs*4 - l) / 4) + Math.min(l%4, 1);
		for(int t=0; t<numTabs; t++) {
			s += "\t";
		}
		return s;
	}
	
	public static SupertagAssignment fromString(String input) {
		String[] lines = input.split("\\n");
		String auto = lines[0];
		Sentence sen = new Sentence();
		sen.addCCGbankParse(auto);
		for(int i=1; i<lines.length; i++) {
			String line = lines[i];
			if(!line.trim().isEmpty()) {
				String[] toks = line.trim().split("\\s+");
				String word = toks[0];
				String pos = toks[1];
				sen.addLexicalItem(new LexicalToken(word, pos));
			}
		}
		SupertagAssignment tags = new SupertagAssignment(sen);
		for(int i=1; i<lines.length; i++) {
			String line = lines[i];
			if(!line.trim().isEmpty()) {
				String[] toks = line.trim().split("\\s+");
				for(int j=2; j<toks.length-1; j+=2) {
					tags.addLexcat(i-1, LexicalCategoryEntry.fromString(toks[j], toks[j+1]));
				}
			}
		}
		return tags;
	}

	public String getGold(int i) {
		return this.sentence.getTokens()[i].getCategory();
	}

	public String toStringWithBeam(double beta) {
		String ret = "";
		for(int i=0; i<sentence.length(); i++) {
			LexicalToken lt = sentence.getTokens()[i];
			ret += lbuf(lt.getWord(), 3)+"  "+lbuf(lt.getPOS().toString(), 1)+lbuf("", 1);
			double maxProb = this.getBest(i).score();
			for(LexicalCategoryEntry st : 
				lexcats.get(i).toArray(new LexicalCategoryEntry[lexcats.get(i).size()])) {
				if(st.score() >= maxProb*beta) {
					ret += lbuf(st.toString(), 3);
				}
			}
			ret += "\n";
		}
		return ret;
	}
}

package eval;

import java.util.ArrayList;

public class DepSet {

	PartialSentence sen;
	ArrayList<Dep> deps;
	int senLength;

	public DepSet(int length) {
		deps = new ArrayList<Dep>();
		senLength = length;
		sen = new PartialSentence();
	}

	public void addWord(Integer index, String word) {
		sen.addWord(index, word);
	}

	public boolean matches(DepSet oth) {
		return this.sen.matches(oth.sen);
	}

	public double numMatchedLabeled(DepSet gold) {
		double matched = 0.0;
		for(Dep goldDep : gold.deps) {
			if(this.matchesLabeledDirected(goldDep)) {
				matched++;
			}
		}
		return matched;
	}

	public double numMatchedUnlabeled(DepSet gold) {
		double matched = 0.0;
		for(Dep goldDep : gold.deps) {
			if(this.matchesUnlabeledUndirected(goldDep)) {
				matched++;
			}
		}
		return matched;
	}

	public void addDep(Dep dep) {
		deps.add(dep);
	}

	boolean matchesLabeledDirected(Dep oth) {
		for(Dep dep : deps) {
			if(dep.matchesLabeledDirected(oth)) {
				return true;
			}
		}
		return false;
	}

	boolean matchesUnlabeledUndirected(Dep oth) {
		for(Dep dep : deps) {
			if(dep.matchesUnlabeledUndirected(oth)) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		String ret = "<s> "+senLength;
		for(Dep d : deps) {
			ret += "\n"+d.toString()+"\t"+sen.getWord(d.target)+" "+sen.getWord(d.source);
		}
		return ret+"\n<\\s>";
	}
	
	public static DepSet getDepSetFromPargEntry(String parg) {
		String[] lines = parg.trim().split("\n");
		DepSet newDeps = new DepSet(Integer.parseInt(lines[0].substring(4)));
		for(int i=1; i<lines.length; i++) {
			String line  = lines[i].trim();
			if(!line.isEmpty() && !line.equals("<\\s>")) {
				newDeps.addDep(new Dep(line));
				String[] toks = line.split("\\s+");
				newDeps.addWord(Integer.parseInt(toks[0]), toks[4]);
				newDeps.addWord(Integer.parseInt(toks[1]), toks[5]);
			}
		}
		return newDeps;
	}
	
	public int size() {
		return deps.size();
	}
}

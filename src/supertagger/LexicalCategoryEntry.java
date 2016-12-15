package supertagger;

public class LexicalCategoryEntry implements Comparable<LexicalCategoryEntry> {
	
	private final String category;
	
	private final double score;
	
	public LexicalCategoryEntry(String cat, double prob) {
		category = cat;
		score = prob;
	}
	
	public String category() {
		return category;
	}
	
	public double score() {
		return score;
	}

	@Override
	public int compareTo(LexicalCategoryEntry o) {
		return (int)  Math.signum(o.score - this.score);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof LexicalCategoryEntry)
				|| o == null) {
			return false;
		}
		if(this == o) {
			return true;
		}
		LexicalCategoryEntry o2 = (LexicalCategoryEntry) o;
		return this.category.equals(o2.category) && this.score == o2.score;
	}
	
	@Override
	public int hashCode() {
		int hash = 17;
		hash = 31*hash + category.hashCode();
		long scoreLong = Double.doubleToLongBits(score);
		hash = 31*hash + (int) (scoreLong ^ (scoreLong >>> 32));
		return hash;
	}

	@Override
	public String toString() {
		return category+" ("+score+")";
	}
	
	public static LexicalCategoryEntry fromString(String cat, String scParens) {
		double sc = Double.parseDouble(scParens.substring(1, scParens.length()-1));
		return new LexicalCategoryEntry(cat, sc);
	}
}

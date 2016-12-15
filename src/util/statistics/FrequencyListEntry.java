package util.statistics;

public class FrequencyListEntry<T> implements Comparable<FrequencyListEntry<T>> {
	
	private T value;
	
	private double count;

	public FrequencyListEntry(T val) {
		this(val, 0);
	}

	
	public FrequencyListEntry(T val, double frequency) {
		value = val;
		count = frequency;
	}
	
	public void addCount(double c) {
		this.count += c;
	}
	
	public double frequency() {
		return count;
	}
	
	public T value() {
		return value;
	}

	@Override
	public int compareTo(FrequencyListEntry<T> o) {
		return (int) Math.signum(o.count - this.count);
	}
	
	public String toString() {
		return this.toString(3);
	}
	
	public String toString(int tabs) {
		return tab(count, tabs)+value;
	}
	
	private String tab(double c, int t) {
		int tabbedLength = t*4;
		String s = c+"";
		int sl = s.length();
		if(sl%4 != 0) {
			s+="\t";
			sl += (4-(sl%4));
		}
		while(sl < tabbedLength) {
			s += "\t";
			sl += 4;
		}
		return s;
	}

}

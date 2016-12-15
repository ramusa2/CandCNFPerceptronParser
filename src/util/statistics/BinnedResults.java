package util.statistics;

import java.util.ArrayList;

public class BinnedResults<T> {
	
	private ArrayList<BinnableResult<T>> bins;
	private int[] cutoffs;

	public BinnedResults(BinnableResultFactory<T> factory, int... binLowerBoundsInclusive) {
		this.cutoffs = binLowerBoundsInclusive;
		this.bins = new ArrayList<BinnableResult<T>>();
		for(int c=0; c<cutoffs.length; c++) {
			this.bins.add(factory.createResult());
		}
	}
	
	public String toString() {
		return this.toCSV();
	}
	
	public void add(int key, T value) {
		if(this.cutoffs.length == 0) {
			return;
		}
		int k=0; 
		while(k < cutoffs.length-1 && key >= cutoffs[k] && key >= cutoffs[k+1]) {
			k++;
		}
		if(k==0) {
			int j = 2;
		}
		this.bins.get(k).add(value);
	}
	
	public ArrayList<BinnableResult<T>> getBins() {
		return this.bins;
	}
	
	public int[] getCutoffs() {
		return this.cutoffs;
	}
	
	public String toCSV() {
		StringBuffer buf = new StringBuffer();
		for(int c=0; c<cutoffs.length; c++) {
			buf.append(cutoffs[c]);
			buf.append(",");
			buf.append(bins.get(c).toString());
			buf.append("\n");
		}
		return buf.toString();
	}
	
}

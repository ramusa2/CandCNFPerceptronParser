package util.statistics;

import java.util.ArrayList;

import training.TaggerResult;

public class STBeamAnalysisResult extends BinnableResult<TaggerResult> {

	ArrayList<TaggerResult> results;
	
	public STBeamAnalysisResult() {
		this.results = new ArrayList<TaggerResult>();
	}
	
	@Override
	public void add(TaggerResult value) {
		this.results.add(value);
	}
	
	public ArrayList<TaggerResult> results() {
		return this.results;
	}

}

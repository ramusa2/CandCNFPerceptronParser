package util.statistics;

public class STBeamAnalysisResultFactory<TaggerResult> extends BinnableResultFactory<TaggerResult> {

	@Override
	public BinnableResult<TaggerResult> createResult() {
		return (BinnableResult<TaggerResult>) create();
	}
	
	private static STBeamAnalysisResult create() {
		return new STBeamAnalysisResult();
	}
	
	

}

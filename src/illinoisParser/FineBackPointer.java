package illinoisParser;

public class FineBackPointer {
	
	private Rule rule;
	private Rule_Direction dir;
	private FineChartItem B, C;
	
	public FineBackPointer(Unary r, FineChartItem child) {
		rule = r;
		dir = Rule_Direction.None;
		B = child;
		C = null;
	}
	
	public FineBackPointer(Binary r, FineChartItem left, FineChartItem right) {
		rule = r;
		dir = r.head;
		B = left;
		C = right;
	}
	
	public FineChartItem B() {
		return B;
	}
	
	public FineChartItem C() {
		return C;
	}
	
	public Rule_Direction direction() {
		return dir;
	}

	public Rule rule() {
		return rule;
	}
	@Override
	public int hashCode() {
		int result = 17;
		result = result * 31 + rule.hashCode();
		//TODO: try this out including direction in hashCode
		result = result * 31 + (B == null ? 0 : B.hashCode());
		result = result * 31 + (C == null ? 0 : C.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object oth) {
		if(this == oth) {
			return true;
		}
		if (oth == null || !(oth instanceof FineBackPointer)) {
			return false;
		}
		FineBackPointer o = (FineBackPointer) oth;
		return  o.B == this.B &&
				o.C == this.C &&
				o.dir == this.dir &&
				o.rule.equals(this.rule);	
	}
	
	public boolean isUnary() {
		return C == null;
	}
}

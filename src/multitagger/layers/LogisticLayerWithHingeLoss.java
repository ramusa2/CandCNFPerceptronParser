package multitagger.layers;

public class LogisticLayerWithHingeLoss extends LogisticLayer {
	
	private final static double THRESHOLD = 0.5;
	
	private double margin;
	
	public LogisticLayerWithHingeLoss(double myMargin) {
		this.margin = myMargin;
	}

	@Override
	public double calculateGradientOfCostFunction(double output, double target) {
		if(target == 1.0) {
			return Math.max(THRESHOLD+margin - output, 0.0);
		}
		else if(target == 0.0) {
			return -Math.max(output-THRESHOLD+margin, 0.0);
		}
		else {
			System.err.println("Using inappropriate code; hinge loss expects binary label (neg==0.0, pos==1.0).");
			return target - output;
		}
	}
}

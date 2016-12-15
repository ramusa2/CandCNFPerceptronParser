package multitagger;

import neuralnet.deprecated.LinearLayer;
import multitagger.layers.BinaryOutputLayer;
import multitagger.layers.MultitaggerInputLayer;

public class SimpleFirstClassifier extends FirstClassifier {

	protected SimpleFirstClassifier(MultitaggerInputLayer myInputLayer,
			LinearLayer myLinearLayer, BinaryOutputLayer myOutputLayer) {
		super(myInputLayer, myLinearLayer, myOutputLayer);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected boolean shouldUpdate(double predictedResponse,
			double correctResponse) {
		return this.outputLayer.calculateGradientOfCostFunction(predictedResponse, correctResponse) != 0.0;
		//return this.makeHardPrediction(predictedResponse) != correctResponse;
		//return true;
	}

}

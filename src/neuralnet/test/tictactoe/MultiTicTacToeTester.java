package neuralnet.test.tictactoe;

import java.util.ArrayList;

import neuralnet.MulticlassNeuralNet;
import neuralnet.SingleOutputNeuralNet;
import neuralnet.hidden.HiddenLayer;
import neuralnet.input.InputLayer;
import neuralnet.input.SimpleInputLayer;
import neuralnet.lossfunctions.NegativeLogLikelihood;
import neuralnet.lossfunctions.ProbabilisticHingeLoss;
import neuralnet.output.MulticlassOutputLayer;
import neuralnet.output.ProbabilisticBinaryOutputLayer;
import neuralnet.output.SingleValueOutputLayer;
import neuralnet.regularizers.L1Regularizer;
import neuralnet.regularizers.L2Regularizer;
import neuralnet.regularizers.Regularizer;
import neuralnet.simplelayers.LinearLayer;
import neuralnet.transferfunctions.HardTanh;
import neuralnet.transferfunctions.Logistic;
import neuralnet.transferfunctions.SoftMax;
import neuralnet.transferfunctions.TransferFunction;

public class MultiTicTacToeTester extends TicTacToeTester {

	protected static MulticlassNeuralNet<TicTacToe> net2;

	static {
	trainSize = 50000;
	testSize = 10000;
	dim = 4;
	ArrayList<TicTacToe> train;
	ArrayList<TicTacToe> test;
	
	trainIters = 20;
	hiddenLayerSize = 100;
	margin = 0.25;
	
	learningRate = 0.05;

	}
	
	public static void main(String[] args) {
		createData();
		initializeNetwork(40, 20);
		train();
		evaluate();
	}

	private static void train() {
		for(int T=1; T<=trainIters; T++) {
			System.out.println("Starting iteration "+T);
			for(TicTacToe t : train) {
				//if(t.winnerID != 3) {
					int target = t.getWinnerID();
					net2.trainOn(t, target, learningRate);
				//}
				//double pred = net.predict(t);
				//System.out.println(t.hasWinner+": "+target+" was "+pred);
			}
		}
	}

	private static void evaluate() {
		double total = 0.0;
		double correct = 0.0;
		//for(TicTacToe t : train) {
		for(TicTacToe t : test) {
			//if(t.winnerID != 3) {
				int predictedClass = net2.hardPredict(t);
				total++;
				if(predictedClass == t.getWinnerID()) {
					correct++;
				}
			//}
		}
		System.out.println("Accuracy on test set: "+(correct/total));
	}

	public static void initializeNetwork(int... hiddenLayerSizes) {
		TicTacToeContextExtractor extractor = new TicTacToeContextExtractor(dim);
		InputLayer<TicTacToe> input = new SimpleInputLayer<TicTacToe>(extractor, false);
		//HiddenLayer hidden = new HiddenLayer(new LinearLayer(input.getOutputVectorDimension(), hiddenLayerSize), new ReLU(hiddenLayerSize, hiddenLayerSize));

		//TransferFunction tFunct = new HardTanh(hiddenLayerSize, hiddenLayerSize);
		//TransferFunction tFunct = new HardTanh(hiddenLayerSize, hiddenLayerSize);
		//TransferFunction tFunct2 = new HardTanh(4, 4);
		
		//HiddenLayer hidden = new HiddenLayer(new LinearLayer(input.getOutputVectorDimension(), hiddenLayerSize), tFunct);
		//hidden.addRegularizer(new L1Regularizer(), 0.0);
		//HiddenLayer hidden2 = new HiddenLayer(new LinearLayer(hiddenLayerSize, 4), tFunct2);
		//hidden2.addRegularizer(new L2Regularizer(), 0.1);
		
		HiddenLayer[] layers = new HiddenLayer[hiddenLayerSizes.length];
		int inputSize = input.getOutputVectorDimension();
		for(int l=0; l<layers.length; l++) {
			int nextSize = hiddenLayerSizes[l];
			layers[l] = new HiddenLayer(new LinearLayer(inputSize, nextSize), new HardTanh(nextSize, nextSize));
			//layers[l].addRegularizer(new L2Regularizer(), 0.1);
			inputSize = nextSize;
		}
		
		MulticlassOutputLayer output = new MulticlassOutputLayer(inputSize, new SoftMax(4, 4), new NegativeLogLikelihood());
		net2 = new MulticlassNeuralNet<TicTacToe>(input, output, layers);
	}

}

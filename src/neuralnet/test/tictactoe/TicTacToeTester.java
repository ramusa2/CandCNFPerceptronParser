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
import neuralnet.regularizers.L2Regularizer;
import neuralnet.simplelayers.LinearLayer;
import neuralnet.transferfunctions.HardTanh;
import neuralnet.transferfunctions.Logistic;
import neuralnet.transferfunctions.ReLU;
import neuralnet.transferfunctions.SoftMax;
import neuralnet.transferfunctions.TransferFunction;

public class TicTacToeTester {


	protected static int trainSize = 50000;
	protected static int testSize = 10000;
	protected static int dim = 3;
	protected static ArrayList<TicTacToe> train;
	protected static ArrayList<TicTacToe> test;
	
	protected static int trainIters = 20;
	protected static int hiddenLayerSize = 10;
	protected static double margin = 0.3;
	
	protected static double learningRate = 0.1;
	
	protected static SingleOutputNeuralNet<TicTacToe> net;
	
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
				double target = t.hasWinner ? 1.0 : 0.0;
				net.trainOn(t, target, learningRate);
				//double pred = net.predict(t);
				//System.out.println(t.hasWinner+": "+target+" was "+pred);
			}
		}
	}

	private static void evaluate() {
		double total = 0.0;
		double correct = 0.0;
		for(TicTacToe t : train) {
		//for(TicTacToe t : test) {
			boolean pred = net.predict(t) >= 0.5 ? true : false;
			total++;
			if(pred == t.hasWinner) {
				correct++;
			}
			//double target = t.hasWinner ? 1.0 : 0.0;
			//System.out.println(target+" was "+net.predict(t));
		}
		System.out.println("Accuracy on test set: "+(correct/total));
	}

	public static void initializeNetwork(int... hiddenLayerSizes) {
		TicTacToeContextExtractor extractor = new TicTacToeContextExtractor(dim);
		InputLayer<TicTacToe> input = new SimpleInputLayer<TicTacToe>(extractor, false);
		HiddenLayer[] layers = new HiddenLayer[hiddenLayerSizes.length];
		int inputSize = input.getOutputVectorDimension();
		for(int l=0; l<layers.length; l++) {
			int nextSize = hiddenLayerSizes[l];
			layers[l] = new HiddenLayer(new LinearLayer(inputSize, nextSize), new HardTanh(nextSize, nextSize));
			//layers[l].addRegularizer(new L2Regularizer(), 0.1);
			inputSize = nextSize;
		}

		SingleValueOutputLayer output = new ProbabilisticBinaryOutputLayer(inputSize, new Logistic(1), new ProbabilisticHingeLoss(margin));
		net = new SingleOutputNeuralNet<TicTacToe>(input, output, layers);
	}

	public static void createData() {
		train = TicTacToe.generateBoards(trainSize, dim);
		test = TicTacToe.generateBoards(testSize, dim);
		System.out.println("Training:");
		summarize(train);
		System.out.println("Testing:");
		summarize(test);
		System.out.println();
	}

	public static void summarize(ArrayList<TicTacToe> list) {
		int winners = 0;
		for(TicTacToe t : list) {
			if(t.hasWinner) {
				winners++;
			}
		}
		System.out.println("  "+winners+" out of "+list.size()+" have a winner.");
	}

}

package perceptron.parser.io;

import illinoisParser.Chart;
import illinoisParser.Grammar;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import util.serialization.ChartDeserializationCallable;

public class CoarseChartDeserializer {

	ChartDeserializationCallable myThread; // use separate thread for deserializing
	private static final int NUM_THREADS = 1;

	Future<Chart> nextChartAsFuture;
	ExecutorService executor;

	public CoarseChartDeserializer(String coarseParseDir, Grammar grammar) {
		executor = Executors.newFixedThreadPool(NUM_THREADS);
		myThread = new ChartDeserializationCallable(coarseParseDir, grammar);
		nextChartAsFuture = executor.submit((Callable<Chart>) myThread);
	}

	public Chart next() {
		try {
			Chart chart = nextChartAsFuture.get();
			nextChartAsFuture = executor.submit((Callable<Chart>) myThread);
			return chart;
		}
		catch (Exception ex) { 
			ex.printStackTrace();
			return null;
		}
	}
	
	public void reset() {
		myThread.reset();
	}

	public void close() {
		executor.shutdown();
		myThread.close();
	}

	public int size() {
		return myThread.size();
	}

}

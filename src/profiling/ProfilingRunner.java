package profiling;

import java.io.File;

import illinoisParser.Grammar;
import illinoisParser.SupervisedParsingConfig;
import perceptron.core.representations.parsing.PackedFeatureForest;
import perceptron.parser.PerceptronChart;
import perceptron.parser.PerceptronParser;
import perceptron.parser.SupertaggedTrainingData;
import perceptron.parser.ccnormalform.NormalFormPerceptronParser;
import util.CoarseParseGenerator;
import util.FineParseGenerator;
import util.TrainPerceptronParserFromCharts;
import util.serialization.CompressionType;
import util.serialization.SerializedFSTDiskData;

public class ProfilingRunner {

	public static final boolean USE_FST = true;
	public static final boolean USE_GZIP = false;
	public static final boolean USE_LZ4 = false;

	public static double beta = 0.004;

	public static int MAX_LENGTH = 100;
	public static int NUM_ITERS = 10;

	public static String DIR = "profiling_results";
	static {
		if(USE_FST) {
			DIR += File.separator+"fst"+File.separator;
		}
		else {
			DIR += File.separator+"default_serialization"+File.separator;
		}
	}
	public static String AUTO_EVAL = DIR+"eval.auto";
	public static String MULTITAGGED = DIR+"eval.multitagged.txt";
	public static String COARSE_PARSES = DIR+"eval.coarseparses.externalized";
	public static String FINE_PARSES = DIR+"eval.fineparses.externalized";
	static {
		if(USE_GZIP) {
			COARSE_PARSES += ".gz";
			FINE_PARSES += ".gz";
		}
		else if(USE_LZ4) {
			COARSE_PARSES += ".lz4";
			FINE_PARSES += ".lz4";
		}
	}
	public static String PERC_SAVE = "perceptron_weights.txt";
	public static String GRAMMAR_DIR = DIR+"grammar";
	/**
	 * @param args
	 * @throws Exception 
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		SupervisedParsingConfig c2 = SupervisedParsingConfig.getDefaultConfig();
		/*
		// Get eval set
		ExtractProfilingData.getEvalSentences(AUTO_EVAL);
		Collection<Sentence> sens = CCGbankReader.getSentencesFromAutoFile(AUTO_EVAL);
		// Get and save multitags
		SupertaggedTrainingData data = new SupertaggedTrainingData(sens, 999, beta);
		data.save(MULTITAGGED);
		 */


		SupertaggedTrainingData data = SupertaggedTrainingData.load(MULTITAGGED);

		// Train grammar
		//Grammar grammar = trainDefaultGrammar();
		//grammar.save(new File(GRAMMAR_DIR));
		// Load grammar
		Grammar grammar = Grammar.load(new File(GRAMMAR_DIR));
		System.out.println("Loaded grammar.");

		/*
		// Basic tests using CoarseChartDeserializer
		// Generate coarse parses
		CoarseParseGenerator.generateCoarseParses(data, grammar, COARSE_PARSES);
		// Generate fine forests
		FineParseGenerator.generateFineParses(grammar, COARSE_PARSES, FINE_PARSES);
		// Train perceptron
		TrainPerceptronParserFromCharts.trainPerceptronParser(grammar, DIR, MAX_LENGTH, NUM_ITERS,  DIR, PERC_SAVE);
		 */

		// Better (?) tests using SerializedFSTData classes (memory and/or disk)
		// Defined compression type and memory/disk data structure
		CompressionType compType = CompressionType.LZ4;
		//SerializedFSTMemoryData<PerceptronChart> mem = new SerializedFSTMemoryData<PerceptronChart>(compType);
		File dataDir = new File(DIR+"coarse_parses");
		dataDir.mkdirs();
		//SerializedFSTDiskData<PerceptronChart> mem = 
		//		SerializedFSTDiskData.createNew(dataDir, compType);
		SerializedFSTDiskData<PerceptronChart> mem = 
				(SerializedFSTDiskData<PerceptronChart>) SerializedFSTDiskData.useExisting(dataDir, compType);

		// Generate coarse parses
		CoarseParseGenerator.generateCoarseParses(data, grammar, mem);
		// Generate fine forests
		File fineDataDir = new File(DIR+"coarse_parses");		
		fineDataDir.mkdirs();
		SerializedFSTDiskData<PackedFeatureForest> memFine = 
				(SerializedFSTDiskData<PackedFeatureForest>) SerializedFSTDiskData.createNew(fineDataDir, compType);
		PerceptronParser parser = new NormalFormPerceptronParser(grammar, c2);
		FineParseGenerator.generateFineParses(grammar, mem, parser, memFine);
		// Train perceptron (building fine parses at each iteration)
		TrainPerceptronParserFromCharts.trainPerceptronParserOnPackedForests(
				grammar, memFine, MAX_LENGTH, NUM_ITERS,  DIR, PERC_SAVE);
	}

}

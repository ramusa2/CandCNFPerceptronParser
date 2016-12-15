package util;

import illinoisParser.CCGbankReader;
import illinoisParser.CCGbankTrainer;
import illinoisParser.Grammar;
import illinoisParser.Model;
import illinoisParser.Sentence;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.SupervisedTrainingConfig;
import illinoisParser.models.HWDepDistModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.zip.GZIPOutputStream;

import perceptron.parser.PerceptronChart;
import perceptron.parser.SupertaggedSentence;
import perceptron.parser.SupertaggedTrainingData;
import profiling.ProfilingRunner;
import supertagger.SupertagAssignment;
import util.serialization.FastSerializationUtil;
import util.serialization.SerializedData;

public class CoarseParseGenerator {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 3) {
			System.out.println("Please provide three arguments: \n  " +
					"(1) the multitagged data to parse \n  " +
					"(2) the path to the CCGbank directory to train the model \n" +
					"(3) the path to output directory");
			return;
		}
		String trainFile = args[0];
		String ccgPath = args[1];
		if(!ccgPath.endsWith("/")) {
			ccgPath += "/";
		}
		String outDir = args[2];
		File dir = new File(outDir);
		dir.mkdirs();
		if(!outDir.endsWith("/")) {
			outDir += "/";
		}
		SupertaggedTrainingData data = SupertaggedTrainingData.load(trainFile);

		// Train grammar
		// Get training data
		int[] secs = new int[20];
		for(int s=2; s<=21; s++) {
			secs[s-2] = s;
		}
		String autoDir = ccgPath+"AUTO";
		Grammar grammar = new Grammar();
		Model model = new HWDepDistModel(grammar);
		SupervisedTrainingConfig c = SupervisedTrainingConfig.getDefaultConfig();
		Collection<Sentence> train = CCGbankReader.getCCGbankData(c.getTrainingSectionRange(), 
				autoDir);
		try {
			CCGbankTrainer.readGrammarAndTrainModel(train, grammar, model, c);
		} catch (Exception e) {
			e.printStackTrace();
		}
		String outfile = outDir+trainFile.substring(trainFile.indexOf("/"), trainFile.indexOf("."))+"coarseparses.externalized";
		generateCoarseParses(data, grammar, outfile);
	}

	public static void generateCoarseParses(SupertaggedTrainingData data, Grammar grammar, String outfile) {
		try {
			ObjectOutputStream o;
			if(ProfilingRunner.USE_GZIP) {
				o= new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(outfile)));
			}
			else {
				o = new ObjectOutputStream(new FileOutputStream(outfile));
			}
			FastSerializationUtil.setOutputStream(o);
			if(ProfilingRunner.USE_FST) {
				FastSerializationUtil.writeInt(o, data.getData().size());	 	
			}
			else {
				o.writeInt(data.getData().size());
			}    
			for(SupertaggedSentence taggedSen : data.getData()) {
				if(ProfilingRunner.USE_FST) {
					FastSerializationUtil.writePerceptronChart(o, new PerceptronChart(taggedSen.sentence(), grammar));	 	
				}
				else {
					o.writeObject(new PerceptronChart(taggedSen.sentence(), grammar));
				}    
			}	    
			o.flush();
			o.close();	
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println("Failed to open/write to stream: "+outfile);
		}
	}

	public static void generateCoarseParses(SupertaggedTrainingData data,
			Grammar grammar,
			SerializedData<PerceptronChart> mem) {
		SupervisedParsingConfig c2 = SupervisedParsingConfig.getDefaultConfig();
		try {   
			for(SupertaggedSentence taggedSen : data.getData()) {
				PerceptronChart chart = new PerceptronChart(taggedSen.sentence(), grammar);
				SupertagAssignment tags = taggedSen.tags();
				if(grammar.licenses(taggedSen.sentence())) {
					chart.coarseParseWithSupertags(grammar, c2, tags, false);
					if(chart.successfulCoarseParse()) {
						System.out.println("Parses: "+chart.coarseRoot().parses);
						chart.cleanChart();    		
					}
					else {
						System.out.println("Coarse parse failure for sentence: "+taggedSen.sentence()+"\n"+taggedSen.sentence().getCCGbankParse()+"\n");
						chart = new PerceptronChart(taggedSen.sentence(), grammar);
					}
					mem.addObject(chart);
				}
				else {
					System.out.println("Unlicensed sentence: "+taggedSen.sentence()+"\n"+taggedSen.sentence().getCCGbankParse()+"\n");
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

}

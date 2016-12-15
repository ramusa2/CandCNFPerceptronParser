package util;

import illinoisParser.CCGbankReader;
import illinoisParser.CCGbankTrainer;
import illinoisParser.Grammar;
import illinoisParser.Model;
import illinoisParser.ParseResult;
import illinoisParser.Parser;
import illinoisParser.Sentence;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.SupervisedTrainingConfig;
import illinoisParser.Util;
import illinoisParser.models.HWDepModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
/**
 * An executable class used to train and test an ACL '02 model (e.g. {@link illinoisParser.models.LexCatModel} 
 * or {@link illinoisParser.models.HWDepModel}) on CCGbank.
 *  
 * @author ramusa2
 * @see illinoisParser.models.LexCatModel
 * @see	illinoisParser.models.HWDepModel
 */
public abstract class AutoGenerator {

	public static final String OUTPUT_NAME = "data/auto/captions_test_output_from_web40.auto";
	//public static final String OUTPUT_NAME = "data/auto/captions_test_output.auto";
	
	public static String[] trainingFiles = new String[]{
		//"data/auto/wsj02-21.auto",
		"data/auto/WebCCGbank.auto"
		//"data/auto/captions.auto"
		//"data/auto/captions_test_output.auto"
		};

	public static String[] testFiles = new String[]{
		"data/auto/captions.auto"};

	/**
	 * Runs the training and testing routine
	 */
	public static void main(String[] args) throws Exception {
		Util.openLog("captions_dev_log.txt");
		// Initialize config, grammar, and HWDep model
		SupervisedTrainingConfig trainConfig = SupervisedTrainingConfig.getDefaultConfig();
		Grammar grammar = new Grammar();
		Model model = new HWDepModel(grammar);
		// Train model on training auto file(s)
		Collection<Sentence> train = CCGbankReader.getSentencesFromMultipleAutoFiles(trainingFiles);
		try {
			CCGbankTrainer.readGrammarAndTrainModel(train, grammar, model, trainConfig);
		}
		catch(Exception e) {
			e.printStackTrace();
			Util.Error("Failed to train model.");
		}
		// Evaluate model on testing auto file(s)
		try {
			SupervisedParsingConfig testConfig = SupervisedParsingConfig.getDefaultConfig();
			//String out = "captions_dev_output.txt";
			//PrintWriter pw = new PrintWriter(new File(out));
			String autoOut = OUTPUT_NAME;
			PrintWriter ao = new PrintWriter(new File(autoOut));
			Collection<Sentence> test = CCGbankReader.getSentencesFromMultipleAutoFiles(testFiles);
			Parser parser = new Parser(testConfig, grammar, model);
			int numAttempted = 0;
			int numCoarseFailures = 0;
			int numFineFailures = 0;
			for(Sentence sen : test) {
				Util.logln("Parsing sentence: "+sen.getID()+" out of "+test.size());
				if (sen.length() <= trainConfig.getMaxSentenceLength()) {
					numAttempted++;
					ParseResult res = parser.parse(sen, testConfig);
					if(res.isCoarseParseFailure()) {
						numCoarseFailures++;
					}
					else if(res.isFineParseFailure()) {
						numFineFailures++;
					}
					else {
						//pw.println(res.viterbiCCGDependencies());
						StringBuilder sb = new StringBuilder();
						Util.buildAUTORecurse(sb, res.getSentence(), model, res.getViterbiParse());
						ao.println(sb.toString());
					}
				}
			}
			//pw.close();
			ao.close();
			Util.logln("Attempted to parse "+numAttempted+" sentences");
			Util.logln("Coarse parse failures: "+numCoarseFailures);
			Util.logln("Fine parse failures: "+numFineFailures);
		}
		catch(Exception e) {
			e.printStackTrace();
			Util.Error("Failed to evaluate model.");
		}
		Util.closeLog();
	}


	/**
	 * Builds a training configuration by writing a static set of options 
	 * to a file, which is then deleted.
	 * 
	 * @param type the name of the parsing model type to use
	 * @return a training configuration object
	 */
	@SuppressWarnings("unused")
	private static SupervisedTrainingConfig getConfig(String type) {
		// TODO: copy these options to file, below
		// See SupervisedTrainingConfig.setDefaultParameters() for available options
		/*
		config.printToScreen = true;
		config.useBeam = true;
		config.logBeamWidth = Math.log(1.0/10000.0);
		config.lowTrainSec = 2;
		config.highTrainSec = 21;
		config.freqCutoff = 5;
		config.CCGcat_CCG = true;
		config.Folder = "dev";
		config.ignorePunctuation = false;
		config.testSec = 0;
		config.autoDir = "data/CCGbank/AUTO";
		config.pargDir = "data/CCGbank/PARG";
		config.longestTestSentence = 100;
		config.longestSentence = 200;
		config.DEBUG = false;
		config.NF = Normal_Form.None;
		config.useGoldSupertags = false;
		 */
		try {
			String tempFileName = "temporary_config_file.to_be_deleted.txt";
			File tempFile = new File(tempFileName);
			PrintWriter pw = new PrintWriter(tempFile);
			// TODO: add options here
			pw.println("modelType  "+type);
			pw.println("  ");
			pw.println("  ");
			pw.println("  ");
			//
			pw.close();
			tempFile.delete();
			SupervisedTrainingConfig config = SupervisedTrainingConfig.readFromFile(tempFileName);
			return config;	
		}
		catch(FileNotFoundException e) {
			System.out.println("Failed to write temporary config file.\nExiting...");
			System.exit(1);
			return null;
		}
	}
	
	
}

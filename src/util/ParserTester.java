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
import illinoisParser.models.BaselineModel;
import illinoisParser.models.HWDepDistModel;
import illinoisParser.models.HWDepModel;
import illinoisParser.models.LexCatModel;

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
public abstract class ParserTester {

	/**
	 * Runs the training and testing routine
	 */
	public static void main(String[] args) throws Exception {
		SupervisedTrainingConfig config = SupervisedTrainingConfig.getDefaultConfig();
		Grammar grammar = new Grammar();
		String type = config.getModelType();
		Util.openLog("dev_output_train_"+type+"_"+config.getMaxSentenceLength());
		Model model = null;
		if(type.equals("lexcat")) {
			model = new LexCatModel(grammar);
		}
		if(type.equals("hwdep")) {
			model = new HWDepModel(grammar);
		}
		if(type.equals("hwdepdist")) {
			model = new HWDepDistModel(grammar);
		}
		if(type.equals("baseline")) {
			model = new BaselineModel(grammar);
		}
		Collection<Sentence> train = CCGbankReader.getCCGbankData(config.getTrainingSectionRange(), 
				config.getAutoDirPath());

		try {
			CCGbankTrainer.readGrammarAndTrainModel(train, grammar, model, config);
		}
		catch(Exception e) {
			e.printStackTrace();
			Util.Error("Failed to train model.");
		}
		try {
			SupervisedParsingConfig config2 = SupervisedParsingConfig.getDefaultConfig();
			String out = "dev_output_test_"+type+"_"+config.getMaxSentenceLength()+".parg";
			PrintWriter pw = new PrintWriter(new File(out));
			Collection<Sentence> test = CCGbankReader.getCCGbankData(config2.getTestingSectionRange(), 
					config2.getAutoDirPath());
			Parser parser = new Parser(config2, grammar, model);
			int numAttempted = 0;
			int numCoarseFailures = 0;
			int numFineFailures = 0;
			for(Sentence sen : test) {
				Util.logln("Parsing sentence: "+sen.getID()+" out of "+test.size());
				if (sen.length() <= config.getMaxSentenceLength()) {
					numAttempted++;
					ParseResult res = parser.parse(sen, config2);
					if(res.isCoarseParseFailure()) {
						numCoarseFailures++;
					}
					else if(res.isFineParseFailure()) {
						numFineFailures++;
					}
					pw.println(res.viterbiCCGDependencies());
				}
			}
			pw.close();
			Util.logln("Attempted to parse "+numAttempted+" sentences");
			Util.logln("Coarse parse failures: "+numCoarseFailures);
			Util.logln("Fine parse failures: "+numFineFailures);
		}
		catch(Exception e) {
			e.printStackTrace();
			Util.Error("Failed to test model.");
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

package illinoisParser;

import illinoisParser.models.BaselineModel;
import illinoisParser.models.HWDepModel;
import illinoisParser.models.LexCatModel;

import java.io.File;
import java.util.Collection;

/**
 * An abstract class that provides static high-level utility methods for training, 
 * saving, loading, and evaluating parsing models.
 *  
 * @author ramusa2
 */
public abstract class SupervisedParser {
	
	/**
	 * Given a training configuration, train a new parsing model by building a
	 * grammar and accumulating observed counts from CCGbank.
	 * 
	 * @param config the {@link SupervisedTrainingConfig training configuration} object
	 * @return a trained parsing {@link Model}
	 * @see illinoisParser.CCGbankTrainer CCGbankTrainer
	 */
	public static Model trainModel(SupervisedTrainingConfig config) {
		// When training a new model, we'll start with an empty grammar
		Grammar grammar = new Grammar();
		Model model = null;
		String type = config.getModelType();
		if(type.equals("lexcat")) {
			model = new LexCatModel(grammar);
		}
		if(type.equals("hwdep")) {
			model = new HWDepModel(grammar);
		}
		if(type.equals("baseline")) {
			model = new BaselineModel(grammar);
		}
		// Fill sections from low to high, inclusive
		int[] sections = config.getTrainingSectionRange();
		String autoDir = config.getAutoDirPath();
		String pargDir = config.getPargDirPath();
		Collection<Sentence> train = CCGbankReader.getCCGbankData(sections, autoDir);
		try {
			CCGbankTrainer.readGrammarAndTrainModel(train, grammar, model, config);
		}
		catch(Exception e) {
			e.printStackTrace();
			Util.Error("Failed to train model.");
		}
		return model;
	}

	/**
	 * Saves a parsing model's distributions and grammar to disk.
	 * 
	 * @param model the parsing model to save
	 * @param saveDir the location of the saved model files
	 */
	public static void saveModel(Model model, String saveDir) {
		// Make directory
		if(!saveDir.endsWith("/")) {
			saveDir += "/";
		}
		File dir = new File(saveDir);
		dir.mkdirs();
		// Save grammar
		File grammarDir = new File(dir.getAbsolutePath()+"/grammar");
		grammarDir.mkdirs();
		model.getGrammar().save(grammarDir);
		// Save model
		File modelDir = new File(dir.getAbsolutePath()+"/model");
		modelDir.mkdirs();
		model.save(modelDir);
	}

	/**
	 * Loads a parsing model (distributions and grammar) from disk.
	 * 
	 * @param loadDir the location of the saved directory
	 * @returns the parsing model loaded from disk
	 */
	public static Model loadModel(String loadDir) {
		File dir = new File(loadDir);
		if(!loadDir.endsWith("/")) {
			loadDir += "/";
		}
		// Get grammar subdirectory and read in grammar
		File grammarDir = new File(dir.getAbsolutePath()+"/grammar");
		Grammar grammar = Grammar.load(grammarDir);
		// Get model subdirectory and read in model
		File modelDir = new File(dir.getAbsolutePath()+"/model");
		return Model.load(grammar, modelDir);
	}

	/**
	 * Given a trained parsing model and a parsing configuration file, evaluate
	 * that model on the data specified by that configuration file, using the
	 * parameters specified by that configuration file.
	 * 
	 * @param model the trained parsing model
	 * @param config the {@link SupervisedParsingConfig parsing configuration} object
	 */
	public static void parse(Model model, SupervisedParsingConfig config) {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Tester function for SUpervisedParser methods
	 */
	public static void main(String[] args) {
		SupervisedTrainingConfig config = SupervisedTrainingConfig.getDefaultConfig();
		Model model = SupervisedParser.trainModel(config);
		String dir = "save_load_test/";
		SupervisedParser.saveModel(model, dir);
		SupervisedParser.loadModel(dir);
	}
	

}

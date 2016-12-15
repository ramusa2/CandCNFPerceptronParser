package illinoisParser;

import java.io.FileNotFoundException;

/**
 * Stores the parameters for training a supervised parser on CCGbank; this includes
 * everything from the location of the .AUTO and .PARG CCGbank files to thresholds
 * on the length of acceptable sentences and frequency of words and categories.
 * 
 * @author ramusa2
 */
public class SupervisedTrainingConfig extends SupervisedConfig {
	
	/*** Parameter Keys ***/
	
	/** Key for the parsing model type, e.g. HWDep **/
	private static final String MODEL_TYPE = "modelType";

	/** Key for the range of CCGbank sections to include as training data, e.g. 2-21 **/
	private static final String TRAINING_SECTION_RANGE = "trainSecRange";

	/** Key for the path to the CCGbank AUTO directory, e.g. data/CCGbank/AUTO **/
	private static final String AUTO_DIR = "autoDir";

	/** Key for the path to the CCGbank PARG directory, e.g. data/CCGbank/PARG **/
	private static final String PARG_DIR = "pargDir";

	/** Key for the maximum training sentence length, e.g. 40 **/
	private static final String MAX_SENTENCE_LENGTH = "maxSentLength";
	
	/** Key for the minimum known word frequency, e.g. 10 **/
	private static final String KNOWN_WORD_FREQ_MIN = "knownWord";

	/*** End of Parameter Keys ***/

	/**
	 * Private default constructor used by factory method(s)
	 */
	private SupervisedTrainingConfig() {
		super();
	}

	@Override
	protected void setDefaultParameterValues() {
		super.addNewParameter(MODEL_TYPE, new ParameterSetting("hwdep"));
		
		super.addNewParameter(TRAINING_SECTION_RANGE, new ParameterSetting("2-21"));
		super.addNewParameter(MAX_SENTENCE_LENGTH, new ParameterSetting(100));
		
		super.addNewParameter(AUTO_DIR, new ParameterSetting("data/CCGbank/AUTO"));
		super.addNewParameter(PARG_DIR, new ParameterSetting("data/CCGbank/PARG"));
		super.addNewParameter(KNOWN_WORD_FREQ_MIN, new ParameterSetting(10));
	}
	
	/**
	 * Default factory method; constructs a SupervisedTrainingConfig object 
	 * using default parameters.
	 * 
	 * @return	a SupervisedTrainingConfig object with default parameters
	 */
	public static SupervisedTrainingConfig getDefaultConfig() {
		return new SupervisedTrainingConfig();
	}
	
	/**
	 * ; constructs a SupervisedTrainingConfig object by
	 * reading in parameters from a file
	 * 
	 * @param filename	path to the configuration text file
	 * @return	a SupervisedTrainingConfig object with the same parameters as the text file
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 */
	public static SupervisedTrainingConfig readFromFile(String filename) 
			throws FileNotFoundException, IllegalArgumentException {
		SupervisedTrainingConfig config = new SupervisedTrainingConfig();
		SupervisedConfig.readParametersFromFile(config, filename);
		return config;
	}

	/**
	 * Returns the name of the parsing model type being trained
	 * @return the name of the parsing model type
	 */
	public String getModelType() {
		return ((String) this.lookup(MODEL_TYPE).getValue()).toLowerCase();
	}

	/**
	 * Returns an array spanning the range of CCGbank trianing sections
	 * @return 	a sorted array of integers, where each int is the index 
	 * 			of a training section
	 */
	public int[] getTrainingSectionRange() {
		String range = ((String) this.lookup(TRAINING_SECTION_RANGE).getValue());
		int lowSec = -1;
		int highSec = -1;
		if(range.contains("-")) {
			lowSec = Integer.parseInt(range.split("-")[0]);
			highSec = Integer.parseInt(range.split("-")[1]);
		}
		else {
			lowSec = Integer.parseInt(range);
			highSec = lowSec;
		}
		int[] sections = new int[highSec - lowSec + 1];
		for(int i=0; i<sections.length; i++) {
			sections[i] = lowSec + i;
		}
		return sections;
	}

	/**
	 * Returns the path to the CCGbank AUTO directory
	 * @return the path to the CCGbank AUTO directory
	 */
	public String getAutoDirPath() {
		return (String) this.lookup(AUTO_DIR).getValue();
	}

	/**
	 * Returns the path to the CCGbank PARG directory
	 * @return the path to the CCGbank PARG directory
	 */
	public String getPargDirPath() {
		return (String) this.lookup(PARG_DIR).getValue();
	}

	/**
	 * Returns the maximum accepted length of sentences used in the training data 
	 * @return the length threshold for training sentences
	 */
	public int getMaxSentenceLength() {
		return (int) this.lookup(MAX_SENTENCE_LENGTH).getValue();
	}

	/**
	 * Returns the minimum number of observed occurrences for a word to be "known" 
	 * @return minimum known word frequency
	 */
	public int getKnownWordFreqCutoff() {
		return (int) this.lookup(KNOWN_WORD_FREQ_MIN).getValue();
	}
}

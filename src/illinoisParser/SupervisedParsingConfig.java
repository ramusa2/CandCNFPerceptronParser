package illinoisParser;

import java.io.FileNotFoundException;

/**
 * Stores the parameters for parsing using a trained CCGbank parser.
 * 
 * @author ramusa2
 */
public class SupervisedParsingConfig extends SupervisedConfig {

	/** Key for the path to the CCGbank AUTO directory, e.g. data/CCGbank/AUTO **/
	private static final String AUTO_DIR = "autoDir";

	/** Key for the path to the CCGbank PARG directory, e.g. data/CCGbank/PARG **/
	private static final String PARG_DIR = "pargDir";


	/** Key for the range of CCGbank sections to include as training data, e.g. 2-21 **/
	private static final String TESTING_SECTION_RANGE = "testSecRange";

	/** Key for the maximum testing sentence length, e.g. 40 **/
	private static final String MAX_SENTENCE_LENGTH = "maxSentLength";
	
	/** If true, use beam during parsing **/
	private static final String USE_BEAM_SEARCH = "useBeamSearch";
	
	/** Beam width for beam search (in logspace) **/
	private static final String BEAM_WIDTH_LOGSPACE = "beamWidthInLogspace";

	/**
	 * Private default constructor used by factory method(s)
	 */
	private SupervisedParsingConfig() {
		super();
	}

	@Override
	protected void setDefaultParameterValues() {
		super.addNewParameter(AUTO_DIR, new ParameterSetting("data/CCGbank/AUTO"));
		super.addNewParameter(PARG_DIR, new ParameterSetting("data/CCGbank/PARG"));

		//super.addNewParameter(MAX_SENTENCE_LENGTH, new ParameterSetting(10));
		//super.addNewParameter(TESTING_SECTION_RANGE, new ParameterSetting("0"));
		super.addNewParameter(MAX_SENTENCE_LENGTH, new ParameterSetting(100));
		super.addNewParameter(TESTING_SECTION_RANGE, new ParameterSetting("2"));
		
		super.addNewParameter(USE_BEAM_SEARCH, new ParameterSetting(true));
		super.addNewParameter(BEAM_WIDTH_LOGSPACE, new ParameterSetting(Math.log(0.0001)));
	}
	
	/**
	 * Default factory method; constructs a SupervisedTrainingConfig object 
	 * using default parameters.
	 * 
	 * @return	a SupervisedTrainingConfig object with default parameters
	 */
	public static SupervisedParsingConfig getDefaultConfig() {
		return new SupervisedParsingConfig();
	}
	
	/**
	 * 
	 * Default factory method; constructs a SupervisedParsingConfig object by
	 * reading in parameters from a file
	 * 
	 * @param filename	path to the configuration text file
	 * @return	a SupervisedParsingConfig object with the same parameters as the text file
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 */
	public static SupervisedParsingConfig readFromFile(String filename) 
			throws FileNotFoundException, IllegalArgumentException {
		SupervisedParsingConfig config = new SupervisedParsingConfig();
		SupervisedConfig.readParametersFromFile(config, filename);
		return config;
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
	 * Returns an array spanning the range of CCGbank testing sections
	 * @return 	a sorted array of integers, where each int is the index 
	 * 			of a training section
	 */
	public int[] getTestingSectionRange() {
		String range = ((String) this.lookup(TESTING_SECTION_RANGE).getValue());
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
	 * Returns true if using a beam search during parsing, else false
	 */
	public boolean useBeamSearch() {
		return (boolean) this.lookup(USE_BEAM_SEARCH).getValue();
	}

	/**
	 * Returns the width of the beam, in logspace
	 */
	public double getLogBeamWidth() {
		return (double) this.lookup(BEAM_WIDTH_LOGSPACE).getValue();
	}
}

package perceptron.parser.training;

/**
 * The status of a training sentence marks if the sentence is usable,  
 * or else indicates why the sentence has been effectively removed 
 * from the training data.
 * 
 * @author ramusa2
 *
 */
public enum TrainingSentenceStatus {
	
	/** Default status; until demonstrated otherwise, train on this sentence **/
	USABLE,
	
	/** Sentence is above the maximum allowed length **/
	TOO_LONG,
	
	/** Sentence's gold parse is not licensed by the grammar **/
	UNLICENSED_GOLD_PARSE,
	
	/** Sentence could not be parsed **/
	COARSE_FAILURE,
	
	/** Sentence's parse forest takes up too much memory/has too many items **/
	TOO_LARGE

}

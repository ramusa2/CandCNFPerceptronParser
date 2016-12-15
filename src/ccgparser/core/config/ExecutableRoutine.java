package ccgparser.core.config;

/**
 * Enumeration of the supported routines for running the Illinois CCG parser:
 * 
 * - create (set up the file structure for the supertagging and parsing models)
 * - train (learn supertagger and parsing model parameters)
 * - supertag (assign lexical category distributions to input text)
 * - parse (assign parse trees to supertagged text)
 * - evaluate (supertag and parse input text and evaluate against gold derivations)
 * 
 * @author ramusa2
 *
 */
public enum ExecutableRoutine {

	create,
	
	train,
	
	supertag,
	
	parse,
	
	evaluate
	
}

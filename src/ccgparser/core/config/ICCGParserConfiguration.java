package ccgparser.core.config;

/**
 * The IllinoisCCGParserConfiguration maintains the status and relevant
 * settings of an instance of the Illinois CCG parser, and is stored in
 * a file within the parser's base directory.
 * 
 * @author ramusa2
 *
 */
public class ICCGParserConfiguration {
	
	private ICCGParserConfiguration() {
		
	}

	public static ICCGParserConfiguration getInitialConfiguration() {
		ICCGParserConfiguration config = new ICCGParserConfiguration();
		// TODO: set initial settings
		return config;
	}

}

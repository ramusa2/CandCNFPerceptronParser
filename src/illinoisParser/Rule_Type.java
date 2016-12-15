package illinoisParser;

/**
 * Rule types for use in the grammar
 * 
 * @author bisk1
 */
public enum Rule_Type {
	/**
	 * X PUNC --> X ( Head Left)
	 */
	BW_PUNCT,
	/**
	 * Y\Z X\Y --> X \ Z ( Head depends if modifier )
	 */
	BW_COMPOSE,
	/**
	 * (Y|Z)|Z' X\Y --> (X|Z)|Z'
	 */
	BW_2_COMPOSE,
	/**
	 * ((Y|Z)|Z')|Z'' X\Y --> ((X|Z)|Z')Z''
	 */
	BW_3_COMPOSE,
	/**
	 * Y/Z X\Y --> X / Z
	 */
	BW_XCOMPOSE,
	/**
	 * X X[conj] ---> X ( Head Left )
	 */
	BW_CONJOIN, BW_CONJOIN_TR,
	/**
	 * Y X\Y --> X
	 */
	BW_APPLY,
	/**
	 * S\(S/N) ---> N
	 */
	BW_TYPERAISE,
	/**
	 * Produce lexical item
	 */
	PRODUCTION, TO_PRODUCTION,
	/**
	 * S/(S\N) ---> N
	 */
	FW_TYPERAISE,
	/**
	 * X/Y Y ---> X
	 */
	FW_APPLY,
	/**
	 * conj X ---> X ( Head Right )
	 */
	FW_CONJOIN, FW_CONJOIN_TR,
	/**
	 * X/Y Y/Z ---> X/Z
	 */
	FW_COMPOSE,
	/**
	 * X/Y (Y|Z)|Z' --> (X|Z)|Z'
	 */
	FW_2_COMPOSE,
	/**
	 * X/Y ((Y|Z)|Z')|Z'' --> ((X|Z)|Z')|Z''
	 */
	FW_3_COMPOSE,
	/**
	 * X/Y Y\Z ---> X\Z
	 */
	FW_XCOMPOSE,
	/**
	 * PUNC X ---> X
	 */
	FW_PUNCT,
	/**
	 * Swap N, NP, etc
	 */
	TYPE_CHANGE,
	/**
	 * TOP -> S
	 */
	TYPE_TOP, TO_TYPE_TOP,
	/**
	 * The actual lexical item
	 */
	LEX,
	/**
	 * NULL
	 */
	NULL,
	// Added by Ryan to handle existing WSJ CCGbank rules
	BW_CONJOIN_TC, FW_CONJOIN_TC,
	FW_PUNCT_TC, BW_PUNCT_TC, FW_SUBSTITUTION, BW_SUBSTITUTION, COORDINATION, POS;

	public static boolean isCoordination(Rule_Type type) {
		switch (type) {
		case BW_CONJOIN:
		case COORDINATION:
			return true;
		default:
			return false;
		}
	}
	

	public static boolean isComposition(Rule_Type type) {
		switch (type) {
		case FW_COMPOSE:
		case BW_COMPOSE:
			return true;
		default:
			return false;
		}
	}

	  /**
	   * Checks if typeraising
	   */
	  public static boolean TR(Rule_Type type) {
	    return type == Rule_Type.FW_TYPERAISE || type == Rule_Type.BW_TYPERAISE;
	  }
}
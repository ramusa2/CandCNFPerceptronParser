package illinoisParser;

import java.io.*;
import java.util.*;

/**
 * An implementation of CCG categories as recursive data structures, and of the
 * combinatory rules as operations on these. At the moment, no logical form is
 * constructed. Instead, a list of word-word ccgDependencies is generated.
 * <p>
 * Each category has the following fields:
 * <ul>
 * <li>A <b>catString</b>, the string representation of the category.<br>
 * (Contrary to the implementation of categories in {@link BinaryTree
 * BinaryTree}, these are the "real" string representations, not an encoded form
 * of these).
 * <li>An <b>id</b> number, which identifies this category token.<br>
 * Required as bookkeeping device for unification.
 * <li>A list of lexical heads <b>heads</b>, which is possibly null (in the case
 * of type-raising).
 * <li>A <b>HeadId</b> number, identifying the lexical heads; also bookkeeping
 * device for unification.
 * <li>A pointer <b>function</b> which points to the parent if this category is
 * part of a complex category, or to null otherwise.
 * <li>A list of <b>unfilled ccgDependencies</b>, which is only non-null if the
 * category is an argument.
 * <li>A list of <b>filled_ccgDependencies</b>, which records the
 * ccgDependencies filled by the last application of a combinatory rule. This is
 * used to compute the dependency probabilities during parsing.
 * </ul>
 * A complex category has additionally the following fields:
 * <ul>
 * <li>A <b>result</b> category
 * <li>An <b>argument</b> category
 * <li>An integer <b>argDir</b>, indicating the directionality of the argument.
 * </ul>
 * <p>
 * This class implements methods which
 * <ul>
 * <li>read in a categorial lexicon from a file
 * <li>when creating a lexical category for a word, treat special cases of
 * lexical categories so that ccgDependencies between argument slots can be
 * passed around.
 * <li>create new categories by performing the operations of the combinatory
 * rules on existing categories
 * <li>perform unification operations necessary to fill word-word
 * ccgDependencies.
 * </ul>
 */

public class CCGcat implements Cloneable {

	/**
	 * Coordination styles: - CCG: Parent->X1, Parent->X2 - CoNLL: Parent->conj,
	 * conj->X1, conj->X2 - Spanish: Parent->X1, X1->conj, X1->X2 - Other:
	 * Parent->X1, X1->X2, X2->conj
	 */
	public enum ConjStyle {
		CC_X1___CC_X2, X1_CC___X1_X2, X1_CC___CC_X2, X1_X2___X2_CC, X2_X1___X2_CC
	};

	// private static ConjStyle Configuration.CONJSTYLE =
	// ConjStyle.X1_X2___X2_CC;
	private Rule_Type type;
	//private int arity = -1;
	private String POS;

	private static boolean FOR = false;
	private static boolean SUBSTITUTION = false;
	private static boolean DEBUG_COORDINATE = false;
	private static final String NP = "NP";
	private static final String NOUN = "N";
	private static final String S = "S";
	private static final String VP = "S\\NP";
	private static final String CONJFEATURE = "[conj]";
	/** open brackets */
	private static final char OB = '(';
	/** closing brackets */
	private static final char CB = ')';
	/** forward slash */
	private static final char FSLASH = '/';
	/** backward slash */
	private static final char BSLASH = '\\';

	/** directionality of argument: forward */
	public static final int FW = 0;
	/** directionality of argument: backward */
	public static final int BW = 1;
	/** directionality of argument: unspecified */
	private static final int NODIR = -1;

	/** a counter for category instances */
	private static int idCounter = 0;
	/** a counter for head instances */

	private static int headIdCounter = 1;
	/**
	 * the ID of this category -- unifiable categories which are part of the same
	 * category have the same ID
	 */
	private int id;
	/**
	 * the ID of the head of this category. -- parts of ONE category with the same
	 * head have the same headID. Coordination and substitution create a new
	 * headID.
	 */
	private int headId;
	/** the string which denotes this category. */
	private String catString;
	/** the argument category. Null in an atomic category */
	private CCGcat argument;
	/** the result category. Null in an atomic category */
	private CCGcat result;
	/** a pointer to the 'parent' of this category */
	private CCGcat function;
	/** forward or backward */
	private int argDir;
	/**
	 * a list of ccgDependencies which should hold for each of the elements of
	 * heads
	 */
	private DepList ccgDependencies;
	/** a list of the filled ccgDependencies */
	private DepList filled_ccgDependencies;
	/**
	 * a list of conllDependencies which should hold for each of the elements of
	 * heads
	 */
	private DepList conllDependencies;
	/** a list of the filled conllDependencies */
	private DepList filled_conllDependencies;

	/** a list of heads */
	private HeadWordList heads;
	private HeadWordList conjHeads;
	private boolean extracted;// long-range, really
	private boolean bounded;

	private static boolean DEBUG = false;

	// ##########################################################
	//
	// CONSTRUCTORS
	// ============
	//
	// ##########################################################

	/**
	 * Creates a category from a string denoting the category. Called by
	 * parseCat(String)
	 */
	private CCGcat(String string) {
		id = newId();
		headId = newHeadId();
		catString = string;
		argument = null;
		result = null;
		argDir = NODIR;
		function = null;
		ccgDependencies = null;
		conllDependencies = null;
		heads = null;
		filled_ccgDependencies = null;
		filled_conllDependencies = null;
		extracted = false;
		bounded = true;
	}

	/** a dummy constructor */
	CCGcat() {
		id = newId();
		headId = -1;// dummy constructor
		catString = null;
		argument = null;
		result = null;
		argDir = NODIR;
		function = null;
		ccgDependencies = null;
		conllDependencies = null;
		heads = null;
		filled_ccgDependencies = null;
		filled_conllDependencies = null;
		extracted = false;
		bounded = true;
	}

	/**
	 * Build up a category consisting of a result and argument category. This does
	 * not clone the result and argument categories!!
	 */
	private CCGcat(CCGcat resCat, CCGcat argCat) {
		id = newId();
		headId = -1; // dummy constructor
		catString = null;
		argument = argCat;
		argument.function = this;
		result = resCat;
		result.function = this;
		argDir = NODIR;
		ccgDependencies = null;
		conllDependencies = null;
		function = null;
		heads = resCat.heads();
		filled_ccgDependencies = null;
		filled_conllDependencies = null;
		extracted = false;
		bounded = true;
	}

	public String toString() {
		return catString;
	}

	// ##########################################################
	//
	// ACCESS THE FIELDS
	//
	// ##########################################################

	/** returns the catString -- the string representation of the category */
	public String catString() {
		return catString;
	}

	public String catStringIndexed() {
		if (argument == null) {
			return catString();
		}
		return catStringRecIndexedArgs();
	}

	// top-level method to return category where only indices that are not the
	// same as the head are printed.
	public String catStringRecIndexedArgs() {

		if (argument != null) {
			int head = this.headId;

			String argString = argument.catStringRecIndexedArgs(head);
			String resultString = result.catStringRecIndexedArgs(head);
			String slash = "/";
			if (argDir == BW)
				slash = "\\";
			return new String(resultString + slash + argString);
		}
		return catString();

	}

	/**
	 * print out the category with all head indices that are not the same as
	 * "head". If an argument is extracted, this is indicated by :U (unbounded) or
	 * :B (bounded). Examples: RelPron: (NP_11\NP_11)/(S[dcl]_12/NP_11:U)_12 what
	 * RelPron: (NP_11\NP_11)/(S[dcl]_12\NP_11:B)_12 what
	 */
	public String catStringRecIndexedArgs(int head) {
		String thisCatString = null;
		String isBounded = null;
		if (this.extracted) {
			if (this.bounded)
				isBounded = ":B";
			else
				isBounded = ":U";
		}
		// A complex category...
		if (argument != null) {
			String argString = argument.catStringRecIndexedArgs(head);
			String resultString = result.catStringRecIndexedArgs(head);
			String slash = "/";
			if (argDir == BW)
				slash = "\\";
			// ... that does not require the head index:
			if (this.headId == head)
				thisCatString = new String("(" + resultString + slash + argString + ")");
			// ... that does require the head index:
			else {

				thisCatString = isBounded == null ? new String("(" + resultString + slash + argString
						+ ")_" + this.headId) : new String("(" + resultString + slash + argString + ")_"
								+ this.headId + isBounded);
			}

		}
		// An atomic category...
		// ...that does not require the head index:
		else if (this.headId == head)
			thisCatString = catString();
		// ...that does require the head index:
		else
			thisCatString = isBounded == null ? new String(catString() + "_" + this.headId) : new String(
					catString() + "_" + this.headId + isBounded);
			return thisCatString;
	}

	public String catStringRecIndexed() {
		if (argument != null) {
			String argString = argument.catStringRecIndexed();
			String resultString = result.catStringRecIndexed();
			String slash = "/";
			if (argDir == BW)
				slash = "\\";
			return new String("(" + resultString + slash + argString + "):" + headId);
		}
		return new String(catString() + ":" + headId);
	}

	/** returns the catString without any features */
	public String catStringNoFeatures() {
		return noFeatures(catString);
	}

	/** strips off atomic features from category string representations */
	private static String noFeatures(String catString) {
		int oIndex = catString.indexOf('[');
		if (oIndex > -1) {
			int cIndex;
			StringBuffer nofeatures = new StringBuffer(catString.substring(0, oIndex));
			cIndex = catString.indexOf(']', oIndex);
			oIndex = catString.indexOf('[', cIndex);
			while (cIndex > -1 && oIndex > -1) {
				nofeatures.append(catString.substring(cIndex + 1, oIndex));
				cIndex = catString.indexOf(']', oIndex);
				oIndex = catString.indexOf('[', cIndex);
			}
			if (oIndex == -1) {
				nofeatures.append(catString.substring(cIndex + 1));
			}
			return nofeatures.toString();
		}
		return catString;
	}

	public String indexedCatString() {
		return catStringIndexed();
	}

	/** returns the Rule_Type that generated this cat **/
	public Rule_Type type() {
		return type;
	}

	/** returns the arity of the rule that generated this cat **/
	/*
	public int arity() {
		return arity;
	}
	*/

	/** returns the list of filled ccgDependencies **/
	public DepList filled_ccgDependencies() {
		return filled_ccgDependencies;
	}

	/** returns the list of filled conllDependencies **/
	public DepList filled_conllDependencies() {
		return filled_conllDependencies;
	}

	/** returns the direction of the argument. FW== 0, BW == 1 */
	public int argDir() {
		return argDir;
	}

	/** returns the argument category of this category */
	public CCGcat argument() {
		return argument;
	}

	/** returns the result category of this category */
	public CCGcat result() {
		return result;
	}

	/**
	 * returns the function category of this category. The inverse of argument and
	 * result
	 */
	public CCGcat function() {
		return function;
	}

	/** returns the global (=outermost) function of this category. */
	public CCGcat globalFunction() {
		if (function != null) {
			return function.globalFunction();
		}
		return this;
	}

	/** returns the list of ccgDependencies defined by this category */
	public DepList CCGdeps() {
		return ccgDependencies;
	}

	/** returns the list of conllDependencies defined by this category */
	public DepList CoNLLdeps() {
		return conllDependencies;
	}

	/** returns the list of head words */
	public HeadWordList heads() {
		return heads;
	}

	/** returns the first head word of this category */
	public String headWord() {
		return heads.word();
	}

	/**
	 * returns the target category. If a category is atomic, it is its own target.
	 * The target of a complex category is the target of its result category. This
	 * is not encoded as a field, but is equally important!
	 */
	public CCGcat target() {
		CCGcat target;
		if (result != null) {
			target = result.target();
		} else
			target = this;
		return target;
	}

	public static synchronized void resetCounters() {
		// TODO: this method is unsafe with newHeadId()
		/*
		 * idCounter = 0; headIdCounter = 1;//reset the counters
		 */
	}

	// ##########################################################
	//
	// CHANGE THE FIELDS
	//
	// ##########################################################
	/**
	 * sets the category String to <tt>string</tt>. Does not change the internal
	 * structure of the category. For initialization only
	 */
	public void setCatString(String string) {
		catString = string;
	}

	/** sets the argDir to <tt>dir</tt> */
	public void setArgDir(int dir) {
		argDir = dir;
	}

	/**
	 * sets the argument category to <tt>argCat</tt>. Does not copy
	 * <tt>argCat</tt>.</tt>
	 */
	public void setArg(CCGcat argCat) {
		argument = argCat;
	}

	/**
	 * sets the result category to <tt>resCat</tt>. Does not copy <tt>resCat</tt>.
	 */
	public void setRes(CCGcat resCat) {
		result = resCat;
	}

	/** sets the function to <tt>fctCat</tt>. Does not copy <tt>fctCat</tt>. */
	public void setFunction(CCGcat fctCat) {
		function = fctCat;
	}

	/** sets the ccgDependencies to <tt>dep</tt> */
	public void setCCGDeps(DepList dep) {
		if (dep != null) {
			ccgDependencies = dep;
		}
	}

	/** sets the conllDependencies to <tt>dep</tt> */
	public void setCoNLLDeps(DepList dep) {
		if (dep != null) {
			conllDependencies = dep;
		}
	}

	private void setToExtractedBounded() {
		extracted = true;
		bounded = true;
	}

	private void setToExtractedUnbounded() {
		extracted = true;
		bounded = false;
	}

	/** Prints the filled ccgDependencies. */
	public void printFilledCCGDeps() {
		System.out.print("###  Filled CCG Dependencies: ");
		if (filled_ccgDependencies != null)
			filled_ccgDependencies.print();
		else
			System.out.println("<null>");
	}

	/** Prints the filled ccgDependencies. */
	public void printFilledCoNLLDeps() {
		// System.out.print("###  Filled CoNLL Dependencies: ");
		if (filled_conllDependencies != null)
			filled_conllDependencies.printDepGrammar();
		else
			// System.out.println("<null>");
			System.out.println();
	}

	/** returns true if the argument contains a dependency at <tt>index</tt> */
	public boolean delayedDependency(int index) {
		boolean retval = false;
		// 06/08/2010 commented out dependency on rest of StatCCG
		/*
		 * if (retval == false && argument != null){ if
		 * (argument.containsDependency(index)){ // and it's a dependency we count:
		 * // ie. if UNBOUNDED: everything counts // if BOUNDED: only backward
		 * arguments count // if not BOUNDED: nothing counts if
		 * (StatCCGModel.PREDARGMODEL){ if (StatCCGModel.UNBOUNDED){ retval =
		 * true;// all ccgDependencies taken into account } else { if
		 * (StatCCGModel.BOUNDED){ if (argDir == BW) retval = true;// only backward
		 * arguments taken intoa account else retval = false; } else {retval =
		 * false;// not BOUNDED: local. } } } else { retval = true; } } else{ retval
		 * = argument.delayedDependency(index); if (retval == false){ retval =
		 * result.delayedDependency(index); } } }
		 */
		return retval;
	}

	/**
	 * if the ccgDependencies are not null, calls
	 * {@link DepList#containsDependency deps().containsDependency(index)}
	 */
	public boolean containsDependency(int index) {
		boolean retval = false;
		// boolean BOUNDED = true;
		// boolean UNBOUNDED = true;
		// 06/08/2010 commented out dependency on StatCCG
		/**
		 * if (StatCCGModel.PREDARGMODEL && StatCCGModel.probModel instanceof
		 * PredArgModel){ BOUNDED = ((PredArgModel)StatCCGModel.probModel).BOUNDED;
		 * UNBOUNDED = ((PredArgModel)StatCCGModel.probModel).UNBOUNDED; } if
		 * (deps() != null){ retval = deps().containsDependency(index); } if (heads
		 * != null && retval== false){ retval = heads.containsWord(index); }
		 */
		return retval;
	}

	/**
	 * Apply all ccgDependencies defined by <tt>deps</tt> to all heads in
	 * <tt>depHeads</tt>. Filled ccgDependencies are appended to
	 * <tt>filledDeps</tt>
	 */
	private DepList applyCCGDependencies(DepList deps, HeadWordList depHeads, DepList filledDeps) {
		DepList allDeps = null;
		HeadWordList h = depHeads;
		// System.out.println("### Apply ccgDependencies: ");
		// System.out.println("### Deps: "); if (deps != null){deps.print();}
		// System.out.println("### to Heads: "); if (depHeads !=
		// null){depHeads.print();}

		if (deps != null) {
			while (h != null) {
				DepList d = deps.copy();

				DepList tmp = d;
				while (tmp != null) {
					tmp.argWord = h.headWord();
					tmp.argCat = h.lexCat();
					tmp.argIndex = h.index();
					tmp = tmp.next();
				}
				if (allDeps == null) {
					allDeps = d;
				} else
					allDeps.append(d);
				h = h.next();
			}
			if (allDeps != null) {
				// System.out.print("### New ccgDependencies: ");
				// allDeps.print();
				filledDeps = appendDeps(filledDeps, allDeps);
				// filledDeps.print();
				allDeps = null;
			} else {
				allDeps = deps.copy();
			}
		}

		ccgDependencies = allDeps;
		return filledDeps;
	}

	/**
	 * Apply all conllDependencies defined by <tt>deps</tt> to all heads in
	 * <tt>depHeads</tt>. Filled conllDependencies are appended to
	 * <tt>filledDeps</tt>
	 */
	private DepList applyConLLDependencies(DepList deps, HeadWordList depHeads, DepList filledDeps) {
		DepList allDeps = null;
		HeadWordList h = depHeads;
		// System.out.println("### Apply ccgDependencies: ");
		// System.out.println("### Deps: "); if (deps != null){deps.print();}
		// System.out.println("### to Heads: "); if (depHeads !=
		// null){depHeads.print();}

		if (deps != null) {
			while (h != null) {
				DepList d = deps.copy();

				DepList tmp = d;
				while (tmp != null) {
					tmp.argWord = h.headWord();
					tmp.argCat = h.lexCat();
					tmp.argIndex = h.index();
					tmp = tmp.next();
				}
				if (allDeps == null) {
					allDeps = d;
				} else
					allDeps.append(d);
				h = h.next();
			}
			if (allDeps != null) {
				// System.out.print("### New ccgDependencies: ");
				// allDeps.print();
				filledDeps = appendDeps(filledDeps, allDeps);
				// filledDeps.print();
				allDeps = null;
			} else {
				allDeps = deps.copy();
			}
		}

		conllDependencies = allDeps;
		return filledDeps;
	}

	/**
	 * standard append operation -- appends a copy of <tt>dep</tt> to
	 * ccgDependencies; if ccgDependencies are null, copies <tt>dep</tt>.
	 */
	private void appendCCGDeps(DepList dep) {
		if (dep != null) {
			if (ccgDependencies != null)
				ccgDependencies.append(dep.copy());
			else
				ccgDependencies = dep.copy();
		}
	}

	/**
	 * standard append operation -- appends a copy of <tt>dep</tt> to
	 * conllDependencies; if conllDependencies are null, copies <tt>dep</tt>.
	 */
	private void appendCoNLLDeps(DepList dep) {
		if (dep != null) {
			if (conllDependencies != null)
				conllDependencies.append(dep.copy());
			else
				conllDependencies = dep.copy();
		}
	}

	/**
	 * standard append operation -- appends a copy of <tt>dep2</tt> to
	 * <tt>dep1</tt>; if <tt>dep1</tt> is null, copies <tt>dep2</tt>.
	 */
	private static DepList appendDeps(DepList dep1, DepList dep2) {
		if (dep2 != null) {
			if (dep1 != null) {
				dep1.append(dep2.copy());
			} else {
				dep1 = dep2.copy();
			}
		}
		return dep1;
	}

	/**
	 * set the heads of this category to a copy of the HeadWordList given as
	 * argument
	 */
	public void setHeads(HeadWordList hw) {
		heads = hw.copy();
	}

	/** Append the HeadWordList given as argument to the current head word list */
	private void appendHeads(HeadWordList hw) {
		if (hw != null) {
			if (heads != null) {
				heads.append(hw);
			} else {
				heads = hw.copy();
			}
		}
	}

	/** like setHeads, but also applies recursively */
	public void setHeadsRec(HeadWordList hw) {
		setHeadsRec(hw, this.headId);
	}

	private void setHeadsRec(HeadWordList hw, int id) {
		if (hw == null)
			return;
		if (this.headId == id) {
			this.heads = hw.copy();
		}
		if (this.result != null) {
			this.result.setHeadsRec(hw, id);
		}
		if (this.argument != null) {
			this.argument.setHeadsRec(hw, id);
		}
	}

	// ##########################################################
	//
	// CREATE LEXICAL CATEGORIES
	//
	// ##########################################################
	public static CCGcat lexCat(String word, String cat, String pos, int index) {
		CCGcat lexCat = parseCat(cat);

		lexCat.adjustAdjs();
		// lexCat.printCat();
		if (word == null) {
			System.err.println("WORD == null;  Cat: " + cat);
			word = "xxx";
		}
		lexCat.heads = new HeadWordList(word, cat, index);
		lexCat.POS = pos;
		lexCat.headId = newHeadId();// new lexical category
		lexCat.target().assignHeadsDeps(1, lexCat.heads, lexCat.headId);// lexCat
		lexCat.treatSpecialCases();
		return lexCat;
	}

	public String POS() {
		return POS;
	}

	/** creates a lexical category for a word */
	private static CCGcat lexCat(String word, String cat) {
		CCGcat lexCat = parseCat(cat);
		lexCat.adjustAdjs();
		if (word == null) {
			System.out.println("WORD == null;  Cat: " + cat);
			word = "xxx";
		}

		lexCat.heads = new HeadWordList(word, cat);
		lexCat.headId = newHeadId();// lexCat
		lexCat.target().assignHeadsDeps(1, lexCat.heads, lexCat.headId);// lexCat
		lexCat.treatSpecialCases();

		return lexCat;
	}

	/** If the argument is the same as the result category, make them equal */
	private void adjustAdjs() {
		if (argument != null && result != null) {
			argument.adjustAdjs();
			if (isAdjunctCat()) {// argument.catString.equals(result.catString)){
				argument.setHeadId(newHeadId());// adjustAdjs -- recursively set all
				// headIds.
				result = argument.copy();// result is equal to argurment
				result.ccgDependencies = null;
				result.conllDependencies = null;
			}
		}
	}

	/**
	 * go from the target category outwards, and assign the head and appropriate
	 * ccgDependencies
	 */
	private void assignHeadsDeps(int i, HeadWordList head, int headIdNumber) {
		headId = headIdNumber;// set the headId to this number
		if (heads == null) {
			heads = head;
		}

		// this constructor does not deal with lists of headwords,
		// since it only assigns the first element of heads.
		// but that is okay, since this is used for lexical categories only.
		if (argument != null) {
			argument.setHeadId(newHeadId());// also set the argument headID to a new
			// number
			if (heads() != null) {
				if (Configuration.CCGcat_CCG)
					argument.setCCGDeps(new DepList("arg", heads(), null, i, argDir,
							isAdjunctOrDeterminerCat()));// record the direction of the
				// argument as well

				if (Configuration.CCGcat_CoNLL)

					argument.setCoNLLDeps(new DepList("arg", heads(), null, i, argDir,
							isAdjunctOrDeterminerCat()));// record the direction of the
				// argument as well

				i++;
			}
			// Adjuncts: an adjunct to a function passes the function on;
			// whereas an adjunct to an atomic category doesn't need to do that
			if (isAdjunctCat()) {
				// System.out.println("ADJUNCT CAT: " + catString());
				argument.setHeadId(newHeadId());// adjunct: also set argument headID to
				// new.
				result = argument.copy();
			}
		}
		if (function != null)
			function.assignHeadsDeps(i, head, headIdNumber);
	}

	/** after adjuncts have been adjusted.. */
	public boolean isAdjunct() {
		boolean retval = false;
		if (argument != null && argument.id == this.id) {// isAdjunct()
			retval = true;
		}
		return retval;
	}

	/**
	 * A test for adjunct categories, which are defined here as categories whose
	 * argument category string is identical to its result category string, and
	 * which do not have any features other than [adj]. This doesn't work in all
	 * generality, but does the job for the categories in CCGbank
	 */
	public boolean isAdjunctCat() {
		boolean retval = false;
		if (argument != null && argument.catString.equals(result.catString)) {
			int index = argument.catString.indexOf('[');
			if (index > -1) {
				if (argument.catString.startsWith("[adj]", index)) {
					retval = true;
				}
			} else {
				retval = true;
			}
		}
		return retval;
	}

	public boolean isAdjunctOrDeterminerCat() {
		boolean retval = false;
		if (catString.equals("NP[nb]/N") || isAdjunctCat())
			retval = true;
		return retval;
	}

	public boolean isAtomic() {
		if (result == null || argument == null)
			return true;
		return false;
	}

	/**
	 * This does the coindexation for all special cases of categories, such as
	 * auxiliaries/modals, control verbs, subject extraction, vp modifiers,
	 * relative pronouns, piep piping, etc.
	 */
	private void treatSpecialCases() {

		// atomic categories aren't special
		if (isAtomic()) {
			// System.out.println("Don't bother with me " + catString());
			return;
		}

		// simplifying assumption: verbs with expletive it or there arguments don't
		// project any ccgDependencies. Or at least we don't capture them.
		/*
		 * if (catString().indexOf("[expl]") != -1 || catString().indexOf("[there]")
		 * != -1) return;
		 */
		// if (DEBUG)
		// System.out.println("treatSpecialCases "+ indexedCatString() + "\t " +
		// heads.word());

		if (isAuxModal()) {
			treatAuxModal();
			if (DEBUG)
				System.out.println("AuxModal: " + indexedCatString() + "\t " + heads.word());
		}
		// control verbs => co-index subject of complement vp with sbj or obj np
		else {
			if (isControlVerb()) {

				if (isSubjectControl()) {
					treatSubjectControl();
					if (DEBUG)
						System.out.println("SubjectControl: " + indexedCatString() + "\t " + heads.word());
				} else {
					treatObjectControl();
					if (DEBUG)
						System.out.println("ObjectControl: " + indexedCatString() + "\t " + heads.word());
				}
			}

			else {
				if (isVerb()) {

					if (isSbjExtractionVerb()) {
						treatSbjExtractionVerb();
						if (DEBUG)
							System.out.println("SbjExtraction: " + indexedCatString() + "\t " + heads.word());
					} else {
						if (isOtherObjectControlVerb()) {
							if (DEBUG && catString().indexOf(":B") != -1)
								System.out.println("OtherObjectControlNEW: " + indexedCatString() + "\t "
										+ heads.word());
						} else {
							treatComplementVPs();
							if (DEBUG && catString().indexOf(":B") != -1)
								System.out.println("ComplementVP(NEW): " + indexedCatString() + "\t "
										+ heads.word());
						}
					}

					if (isToughVerb()) {
						treatToughVerb();
						if (DEBUG)
							System.out.println("ToughVerb: " + indexedCatString() + "\t " + heads.word());
					}
				} else {
					if (isVPModifier()) {
						treatVPModifier();

						if (isSmallClausePP()) {
							treatSmallClausePP();
							if (DEBUG)
								System.out.println("SmallClausePP: " + indexedCatString() + "\t " + heads.word());
						}
					}

					else {
						if (isRelPronoun()) {
							treatRelPronoun();
							if (DEBUG)
								System.out.println("RelPron: " + indexedCatString() + "\t " + heads.word());
						} else {
							if (isFreeRelPronoun()) {// new 30/12/2004
								treatFreeRelPronoun();
								if (DEBUG)
									System.out.println("FreeRelPron: " + indexedCatString() + "\t " + heads.word());
							} else {
								if (isGenitiveRelPronoun()) {
									treatGenitiveRelPronoun();
									if (DEBUG)
										System.out.println("GenitiveRelPron: " + indexedCatString() + "\t "
												+ heads.word());
								} else {
									if (isYNQCategory()) {
										treatYNQCategory();
										if (DEBUG)
											System.out.println("YNQ: " + indexedCatString() + "\t " + heads.word());
									} else {
										if (isNonStandardPiedPipingCategory()) {
											treatNonStandardPiedPipingCategory();
											if (DEBUG)
												System.out.println("NonStandardPiedPiping: " + indexedCatString() + "\t "
														+ heads.word());
										} else {
											if (isPiedPipingAdjunctExtractionCategory()) {
												treatPiedPipingAdjunctExtractionCategory();
												if (DEBUG)
													System.out.println("PiedPipingAdjunctExtraction: " + indexedCatString()
															+ "\t " + heads.word());
											} else {
												if (isStandardPiedPipingCategory()) {
													treatStandardPiedPipingCategory();
													if (DEBUG)
														System.out.println("StandardPiedPiping: " + indexedCatString() + "\t "
																+ heads.word());
												} else {
													if (isPiedPipingRelPronCategory()) {
														treatPiedPipingRelPronCategory();
														if (DEBUG)
															System.out.println("PiedPipingRelPron: " + indexedCatString() + "\t "
																	+ heads.word());
													} else {
														if (isPiedPipingSBJRelPronCategory()) {
															treatPiedPipingSBJRelPronCategory();
															if (DEBUG)
																System.out.println("PiedPipingSBJRelPron: " + indexedCatString()
																		+ "\t " + heads.word());
														} else {
															if (isPiedPipingEmbeddedQuestionCategory()) {
																treatPiedPipingEmbeddedQuestionCategory();
																if (DEBUG)
																	System.out.println("PiedPipingEmbeddedQuestion: "
																			+ indexedCatString() + "\t " + heads.word());
															}
															// Determiners
															else {
																if (isDet()) {
																	treatDet();
																} else {
																	if (isPOS()) {
																		treatPOS();
																	} else {
																		if (isSmallClausePP()) {
																			treatSmallClausePP();
																			if (DEBUG)
																				System.out.println("SmallClausePP: " + indexedCatString()
																						+ "\t " + heads.word());
																		} else {
																			if (isToughAdj()) {
																				treatToughAdj();
																				if (DEBUG)
																					System.out.println("ToughAdj: " + indexedCatString()
																							+ "\t " + heads.word());
																			}

																			else {
																				if (DEBUG
																						&& !catString.equals(NOUN)
																						&& !catString.equals("N/N")
																						&& !catString.equals(".")
																						&& !catString.equals(",")
																						&& !catString.equals(":")
																						&& !catString.equals(NP)
																						&& !catString.equals("NP/NP")
																						&& !catString.equals("NP\\NP")
																						&& !catString.equals("N\\N")
																						&& !catString.equals("(N/N)/(N/N)")
																						&& !catString.equals("(N/N)\\(N/N)")
																						&& !catString.equals("((S\\NP)\\(S\\NP))")
																						&& !catString.equals("((S\\NP)//(S\\NP))")
																						&& !catString
																						.equals("((S\\NP)\\(S\\NP))//((S\\NP)\\(S\\NP))")
																						&& !catString.equals("S/S")
																						&& !catString.equals("N/S[em]")
																						&& !catString.equals("S[adj]\\NP")
																						&& !catString.equals("(S[adj]\\NP)/PP")
																						&& !catString.equals("S[pss]\\NP")
																						&& !catString.equals("S[dcl]\\NP")
																						&& !catString.equals("S[ng]\\NP")
																						&& !catString.equals("conj")
																						&& !catString.equals("(NP\\NP)/NP")
																						&& !catString.equals("(NP\\NP)/(NP\\NP)")
																						&& !catString.equals("(S[dcl]\\NP)/NP")
																						&& !catString.equals("(S[b]\\NP)/NP")
																						&& !catString.equals("(S[ng]\\NP)/NP")
																						&& !catString.equals("((S[ng]\\NP)/PP)/NP")
																						&& !catString.equals("(S[pt]\\NP)/NP")
																						&& !catString.equals("(S[dcl]\\NP)/PP")
																						&& !catString.equals("(S[pss]\\NP)/PP")
																						&& !catString.equals("(S[dcl]\\S[dcl])\\NP")) {
																					System.out.println("_OrdinaryCategory: " + catString()
																							// indexedCatString()
																							+ "\t " + heads.word());
																				}
																			}
																		}
																	}
																}
															}
														}
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	/** Possessive 's and ' */
	public boolean isPOS() {
		if (catString.equals("(NP[nb]/N)\\NP"))
			return true;
		return false;
	}

	/** VP pre- and postmodifiers */
	public boolean isVPModifier() {
		boolean retval = false;
		if (argument != null && result != null && argument.catString.equals(VP)
				&& result.catString.equals(argument.catString)) {
			retval = true;
		} else {
			if (result != null) {
				retval = result.isVPModifier();
			}
		}
		return retval;
	}

	private void treatVPModifier() {
		// VP\VP/VP ==> set the subject NP of the VP complement to the main VP
		if (argument.matches(VP) && !argument.catString.equals("S[asup]\\NP")
				&& !argument.catString.equals(VP)) {
			if (result.catString.equals("(S\\NP)\\(S\\NP)") || result.catString.equals("(S\\NP)/(S\\NP)")) {
				argument.argument = result.argument.argument.copy();
				argument.argument.setToExtractedBounded();
				if (DEBUG)
					System.out.println("VPmodifier(1): " + indexedCatString() + "\t " + heads.word());
			} else {
				if (result.argument != null
						&& result.argument.catString.equals(result.result.catString)
						&& (result.argument.catString.equals("(S\\NP)\\(S\\NP)") || result.argument.catString
								.equals("(S\\NP)/(S\\NP)"))) {

					argument.argument = result.argument.argument.argument.copy();
					argument.argument.setToExtractedBounded();
					if (DEBUG)
						System.out.println("VPmodifier(2):  " + indexedCatString() + "\t " + heads.word());
				}
			}
		}
	}

	public boolean isGenitiveRelPronoun() {
		boolean retval = false;
		if (argument != null && result.argument != null && result.argument.argument != null
				&& argument.catString.equals(NOUN)
				&& (result.argument.matches("S/NP") || result.argument.matches(VP))
				&& !result.argument.catString.equals("S[adj]\\NP")
				&& !result.argument.catString.equals("S[asup]\\NP")
				&& !(result.argument.id == result.result.id)// isGenitiveRelPron
				) {
			retval = true;
		}
		return retval;
	}

	private void treatGenitiveRelPronoun() {
		result.argument.argument = argument.copy();
		if (result.argument.argDir == BW)
			result.argument.argument.setToExtractedBounded();
		if (result.argument.argDir == FW)
			result.argument.argument.setToExtractedUnbounded();
	}

	public boolean isRelPronoun() {
		boolean retval = false;
		String plainCat = catStringNoFeatures();
		if ((plainCat.equals("(NP\\NP)/(S\\NP)") || plainCat.equals("(NP\\NP)/(S/NP)"))
				&& !argument.catString.equals("S[adj]\\NP") && !argument.catString.equals("S[asup]\\NP")
				&& !argument.catString.equals("S[ng]\\NP")// new: 28/12/2003
				) {
			retval = true;
		}
		return retval;
	}

	private void treatRelPronoun() {
		argument.argument = result.argument.copy();
		if (argument.argDir == BW)
			argument.argument.setToExtractedBounded();
		if (argument.argDir == FW)
			argument.argument.setToExtractedUnbounded();
	}

	public boolean isFreeRelPronoun() {// NEW!!! 30 Dec 2004
		boolean retval = false;
		String plainCat = catStringNoFeatures();
		if ((plainCat.equals("NP/(S\\NP)") || plainCat.equals("NP/(S/NP)"))
				&& !argument.catString.equals("S[adj]\\NP") && !argument.catString.equals("S[asup]\\NP")
				&& !argument.catString.equals("S[ng]\\NP")) {
			retval = true;
		}
		return retval;
	}

	private void treatFreeRelPronoun() {
		argument.argument = result.copy();
		if (argument.argDir == BW)
			argument.argument.setToExtractedBounded();
		if (argument.argDir == FW)
			argument.argument.setToExtractedUnbounded();
		// System.err.println("IS FREE REL PRONOUN");
	}

	public boolean isSmallClausePP() {
		boolean retval = false;

		if (argument != null
				&& argDir == FW
				&& argument.matches(NP)
				&& !argument.catString().equals("N")
				&& result.argument != null
				&& result.argDir == FW
				&& result.argument.matches(VP)
				&& result.result != null
				&& ((result.result.argument != null && result.result.argument.id == result.result.result.id)// isSmallClausePP
						|| ((result.result.matches("PP") || result.result.catString.equals("S[for]")
								|| result.result.matches("(S\\NP)\\(S\\NP)") || result.result.matches("NP\\NP")
								|| result.result.matches("S\\S") || result.result.matches("S/S"))))
								&& catString().indexOf("[thr]") == -1 && catString().indexOf("[expl") == -1) {

			retval = true;
		}
		return retval;
	}

	private void treatSmallClausePP() {
		argument.headId = result.argument.argument.headId;// smallClausePP
		result.argument.argument.setToExtractedBounded();
	}

	public boolean isToughAdj() {
		if (catString.equals("(S[adj]\\NP)/((S[to]\\NP)/NP)"))
			return true;
		return false;
	}

	private void treatToughAdj() {
		argument.argument = result.argument.copy();
		argument.argument.setToExtractedUnbounded();
	}

	public boolean isToughVerb() {
		// (X takes Y weeks to complete)
		if (argument != null && argument.catString.equals("NP") && result.argument != null
				&& result.argument.catString.equals("(S[to]\\NP)/NP") && result.result.argument != null
				&& result.result.argument.catString.equals("NP")
				// not expletive subject!
				) {
			return true;
		}
		return false;
	}

	private void treatToughVerb() {
		result.argument.argument = result.result.argument.copy();
		result.argument.argument.setToExtractedUnbounded();
	}

	public boolean isDet() {
		boolean retval = false;
		if (catString.equals("NP[nb]/N")) {
			retval = true;
		}
		return retval;
	}

	private void treatDet() {
		result.headId = argument.headId;// determiner
		result.heads = null;
	}

	private void treatPOS() {
		result.result.headId = result.argument.headId;// POS
		result.result.heads = null;
	}

	/** true for non-topicalized verbs -- with non-expletive subject (new!) */
	public boolean isVerb() {
		boolean retval = false;
		CCGcat target = target();
		if (target.matches(S) && !target.catString.equals(S) && !target.catString.equals("S[adj]")
				&& target.function != null && target.function.argDir == BW
				&& target.function.argument.matches(NP)
				&& !target.function.argument.catString.equals("NP[expl]")
				&& !target.function.argument.catString.equals("NP[thr]")) {
			retval = true;
		}
		return retval;
	}

	public boolean isSbjExtractionVerb() {
		boolean retval = false;
		if (matches("((S\\NP)/NP)/(S\\NP)") && argument.catString.equals("S[dcl]\\NP")) {
			retval = true;
		}
		return retval;
	}

	private void treatSbjExtractionVerb() {
		argument.argument = result.argument.copy();
		argument.argument.setToExtractedUnbounded();
	}

	public boolean isOtherObjectControlVerb() {
		boolean retval = false;
		CCGcat tmp = target().function.function;
		// boolean hasComplVP = false;
		// boolean hasComplNP = false;
		CCGcat complVP = null;
		CCGcat complNP = null;
		while (tmp != null && tmp.argument != null) {

			if (tmp.argument.matches(VP)
					// "adjectives" can also be particles
					// &&tmp.argument.catString.equals("S[adj]\\NP")
					) {
				// hasComplVP = true;
				complVP = tmp.argument;
			}
			if (tmp.argument.matches(NP) && !tmp.argument.catString().equals("NP[expl]")
					&& !tmp.argument.catString().equals("NP[thr]")) {

				// hasComplNP = true;
				complNP = tmp.argument;
			}
			tmp = tmp.function;
		}
		if (complVP != null && complNP != null) {
			complVP.argument = complNP.copy();
			complVP.argument.setToExtractedBounded();// new FEB 11/2003
			retval = true;
		}
		return retval;
	}

	private void treatComplementVPs() {
		// verbs that subcategorize for expletives are different..
		if (this.catString().indexOf("[expl]") != -1) {
			return;
		}
		// System.out.print("A VERB: ");heads.print();
		CCGcat sbjNP = target().function.argument;

		CCGcat tmp = target().function.function;
		// boolean hasComplVP = false;
		while (tmp != null) {
			if (tmp.argument.matches(VP)) {
				// hasComplVP = true;

				tmp.argument.argument = sbjNP.copy();
				tmp.argument.argument.setToExtractedBounded();
				// System.out.print("A VERB CATEGORY WITH COMPLEMENT VP -> VP-sbj := verb.sbj: ");
				// heads.print();// Hmmm, this doesn't always yield the right
				// categories...
				// break;
				if (DEBUG)
					System.out.println("ComplementVerb: " + indexedCatString() + "\t " + heads.word());
			}
			tmp = tmp.function;
		}

	}

	public boolean isAuxModal() {
		boolean retval = false;
		if (!catString.equals("(S\\NP)/(S\\NP)") && !catString.equals("(S[adj]\\NP)/(S[adj]\\NP)")
				&& matches("(S\\NP)/(S\\NP)") && !result.catString.equals("S[adj]\\NP")
				&& !result.argument.catString.equals("NP[expl]")// new
				&& !result.argument.catString.equals("NP[thr]")// new
				) {
			retval = true;
		}
		return retval;
	}

	private void treatAuxModal() {
		// "to" doesn't introduce a dependency on the subject

		if (!result.catString.equals("S[dcl]\\NP")) {
			if (result.catString.equals("S[to]\\NP")) {
				result.argument.ccgDependencies = null;
				result.argument.conllDependencies = null;
			}
			if (headWord().equals("be") || headWord().equals("been") || headWord().equals("have")
					|| headWord().equals("going")) {
				result.argument.ccgDependencies = null;
				result.argument.conllDependencies = null;
			}

		}
		argument.argument = result.argument.copy();
		argument.argument.setToExtractedBounded();
	}

	public boolean isControlVerb() {
		boolean retval = false;
		if (!catString.equals("((S\\NP)/(S\\NP))/NP")
				&& !catString.equals("((S[adj]\\NP)/(S[adj]\\NP))/NP")
				&& !catString.startsWith("((S[pss]\\NP)") && matches("((S\\NP)/(S\\NP))/NP")
				&& catString.indexOf("[expl]") == -1 && catString.indexOf("[thr]") == -1) {
			retval = true;
		}
		return retval;
	}

	public boolean isSubjectControl() {
		boolean retval = false;
		if (isControlVerb() && isSubjectControlVerb()) {
			retval = true;
		}
		return retval;
	}

	private boolean isSubjectControlVerb() {
		if (headWord().startsWith("promis"))
			return true;
		return false;
	}

	private void treatSubjectControl() {
		// the only case is promised|((S[pss]\NP)/(S[to]\NP))/NP, which is an odd
		// category!
		result.argument.argument.headId = result.result.argument.headId;// sbj
		// control
		result.argument.argument.setToExtractedBounded();

	}

	public boolean isObjectControl() {
		boolean retval = false;
		if (isControlVerb() && !isSubjectControlVerb()) {
			retval = true;
		}
		return retval;
	}

	private void treatObjectControl() {
		result.argument.argument = argument.copy();
		result.argument.argument.setToExtractedBounded();
	}

	public boolean isYNQCategory() {
		boolean retval = false;
		// true of (S[q]/(S[xxx]\NP))/NP
		if (argument != null && result.argument != null && result.argument.argument != null
				&& argument.matches(NP) && !argument.catString().equals("NP[expl]")
				&& result.argument.matches(VP) && result.result.catString.equals("S[q]")) {
			retval = true;
		}
		return retval;
	}

	private void treatYNQCategory() {
		result.argument.argument = argument.copy();
		result.argument.argument.setToExtractedBounded();
	}

	public boolean isNonStandardPiedPipingCategory() {
		boolean retval = false;
		if (argument != null && argument.matches("(NP\\NP)/NP") && result != null
				&& (result.argument.matches(VP) || result.argument.matches("S//NP"))
				&& (target().matches(NP) || target().matches(NOUN))) {
			retval = true;
			// System.out.println("Non-standard PIED PIPING!!!! " + catString());
		}
		return retval;
	}

	private void treatNonStandardPiedPipingCategory() {
		// ((NP\NP)/(S[to/dcl]\NP))\((NP\NP)/NP)

		// "some of whom are suing"
		// the NP of the preposition is the same as the subject NP;
		if (target().matches(NP) || target().matches(NOUN)) {
			// get the NP as argument of the preposition
			// argument.argument = target().function().argument.copy();
			argument.argument.headId = target().function().argument.headId;
			argument.argument.setToExtractedBounded();
			// get the NP as argument of the verb

			if (!result.argument.catString().equals("S[to]\\NP")) {
				result.argument.argument.headId = target().function().argument.headId;
				if (result.argument.argDir == BW)
					result.argument.argument.setToExtractedBounded();
				else
					result.argument.argument.setToExtractedUnbounded();
			}
			// System.out.println("NON-STANDARD PIED PIPING: " + catString());
			// System.out.println("NON-STANDARD PIED PIPING WITH CO-INDEXATION: " +
			// indexedCatString());

		}
	}

	public boolean isPiedPipingAdjunctExtractionCategory() {
		boolean retval = false;
		if (argument != null && argument.matches("(NP\\NP)/NP") && result != null
				&& result.argument.matches(S) && (target().matches(NP) || target().matches(NOUN))) {
			retval = true;
		}
		return retval;
	}

	// ((NP\NP)/S[dcl])\((NP\NP)/NP)
	private void treatPiedPipingAdjunctExtractionCategory() {
		// System.out.println("PIED PIPING ADJUNCT EXTRACTION: " + catString());
		// printCat();
		// the NP of the preposition is the same as the subject NP;
		if (target().matches(NP) || target().matches(NOUN)) {
			// argument.argument = target().function().argument.copy();
			argument.argument.headId = target().function().argument.headId;
			argument.argument.setToExtractedBounded();
			// System.out.println("PIED PIPING ADJUNCT EXTRACTION WITH CO-INDEXATION: "
			// + indexedCatString());
			// DEBUG = true;
		}
	}

	// under whose auspices:(((NP\NP)/S[dcl])\((NP\NP)/NP))/N
	public boolean isPiedPipingRelPronCategory() {
		boolean retval = false;
		if (argument != null && argument.matches(NOUN) && result != null && result.argument != null
				&& result.argument.matches("(NP\\NP)/NP") && result.result != null
				&& result.result.argument != null && result.result.argument.matches("S")
				&& result.result.result != null && result.result.result.matches("NP\\NP")) {
			retval = true;
		}
		return retval;
	}

	private void treatPiedPipingRelPronCategory() {

		result.argument.argument.headId = argument.headId;
		result.argument.argument.setToExtractedBounded();

		// System.out.println("PIED PIPING REL PRON (?) -- " + catString());

	}

	// ((NP\NP)/(S[dcl]\NP))\(NP/NP))/N whose
	// Ruth Messinger, some of whose programs have made..."

	public boolean isPiedPipingSBJRelPronCategory() {
		boolean retval = false;
		if (argument != null && argument.matches(NOUN) && result != null && result.argument != null
				&& result.argument.matches("NP/NP") && result.result != null
				&& result.result.argument != null && result.result.argument.matches("S\\NP")

				&& result.result.result != null && result.result.result.matches("NP\\NP")) {
			retval = true;
		}
		return retval;
	}

	// ((NP\NP)/(S[dcl]\NP_j))\(NP_j/NP_i))/N_i whose
	// Ruth Messinger, some of whose programs have made..."
	private void treatPiedPipingSBJRelPronCategory() {
		argument.headId = result.argument.argument.headId;
		result.argument.argument.setToExtractedBounded();
		if (result.result.argument.matches("S\\NP") && !result.result.argument.equals("S[to]\\NP")) {
			result.result.argument.argument.headId = result.argument.result.headId;
			result.result.argument.argument.setToExtractedBounded();
		}
		// System.out.println("PIED PIPING SBJ REL PRON (?) -- " + catString());
	}

	public boolean isStandardPiedPipingCategory() {
		boolean retval = false;
		if (argument != null && argument.matches("NP/NP") && result != null
				&& (result.argument.matches(VP) || result.argument.matches("S//NP"))
				&& (target().matches(NP) || target().matches(NOUN))) {
			retval = true;

		}
		return retval;
	}

	private void treatStandardPiedPipingCategory() {
		// "some of whom..." (((N\N_j)/(S[dcl]\NP_i))\(NP_i/NP_j))
		// System.out.println("STANDARD PIED PIPING : " + catString());
		// DEBUG = true;
		// printCat();
		// the NP of the preposition is the same as the subject NP;
		if (target().matches(NP) || target().matches(NOUN)) {
			argument.headId = result.argument.argument.headId;
			result.argument.argument.setToExtractedBounded();

			// get the dependency for the PP right
			// argument.argument = target().function().argument.copy();
			argument.argument.headId = target().function().argument.headId;
			argument.argument.setToExtractedBounded();

		}
	}

	public boolean isPiedPipingEmbeddedQuestionCategory() {
		String local_catString = catString();
		if (local_catString != null && local_catString.equals("((S[qem]/S[dcl])\\((NP\\NP)/NP))/N"))
			return true;
		return false;
	}

	private void treatPiedPipingEmbeddedQuestionCategory() {
		result.argument.argument.headId = argument.headId;
		result.argument.argument.setToExtractedBounded();
	}

	// ##################################################
	// READ IN A CATEGORY FROM A STRING
	// ##################################################

	/**
	 * parseCat(String cat) This works only if cat really spans an entire category
	 */
	private static CCGcat parseCat(String cat) {
		// Create a new category
		if (cat.endsWith(CONJFEATURE)) {// otherwise it might crash
			int index = cat.lastIndexOf(CONJFEATURE);
			cat = cat.substring(0, index);
		}

		CCGcat newCat = new CCGcat(cat);
		// CASE 1: No brackets
		if (cat.indexOf(OB) == -1 && cat.indexOf(CB) == -1) {

			// CASE 1(a): And no slashes
			if (cat.indexOf(FSLASH) == -1 && cat.indexOf(BSLASH) == -1) {
				// ==> newCat is atomic category
			}
			// CASE 1(b): a slash
			else {
				int slashIndex = 0;
				if (cat.indexOf(FSLASH) == -1 && cat.indexOf(BSLASH) != -1) {
					slashIndex = cat.indexOf(BSLASH);
					newCat.argDir = BW;
				}
				if (cat.indexOf(BSLASH) == -1 && cat.indexOf(FSLASH) != -1) {
					slashIndex = cat.indexOf(FSLASH);
					newCat.argDir = FW;
				}
				// Recurse on rescat
				CCGcat resCat = parseCat(cat.substring(0, slashIndex));
				resCat.function = newCat;
				newCat.result = resCat;
				// Recurse on argcat
				CCGcat argCat = parseCat(cat.substring(slashIndex + 1));
				argCat.function = newCat;
				newCat.argument = argCat;
			}
		}
		// CASE 2: Brackets
		else {
			int obNumber = 0; // the number of unclosed open brackets
			int start = 0; // the start of a new category
			int end = 0; // the end of a new category

			// Iterate through the characters in the string
			for (int i = 0; i < cat.length(); i++) {

				// If: this character is an open bracket
				// Then: if there are no other unclosed open brackets,
				// then: the next character starts a new category
				// - also: increment the number of unclosed open brackets
				if (cat.charAt(i) == OB) {
					if (obNumber == 0)
						start = i + 1;
					obNumber++;
				}

				// If: this character is a forward slash
				// and there are no unclosed open brackets
				// Then: this is the end of the result category
				if (cat.charAt(i) == FSLASH && obNumber == 0) {
					newCat.argDir = FW;
					if (newCat.result == null) {
						end = i;
						CCGcat resCat = parseCat(cat.substring(start, end));
						resCat.function = newCat;
						newCat.result = resCat;
					}
					start = i + 1;
					end = i + 1;
				}
				// If: this character is a backward slash
				// and there are no unclosed open brackets
				// Then: this is the end of the result category
				if (cat.charAt(i) == BSLASH && obNumber == 0) {
					newCat.argDir = BW;
					if (newCat.result == null) {
						end = i;
						CCGcat resCat = parseCat(cat.substring(start, end));
						resCat.function = newCat;
						newCat.result = resCat;
					}
					start = i + 1;
					end = i + 1;
				}
				// If this is a closing bracket:
				// Then: decrement the number of open unclosed brackets
				if (cat.charAt(i) == CB) {
					obNumber--;
					if (obNumber == 0) {
						end = i;
						if (newCat.result == null) {
							CCGcat resCat = parseCat(cat.substring(start, end));
							resCat.function = newCat;
							newCat.result = resCat;
						} else {
							CCGcat argCat = parseCat(cat.substring(start, end));
							argCat.function = newCat;
							newCat.argument = argCat;
						}
					}
				}
				// If this is the end of the string
				if (i == cat.length() - 1 && cat.charAt(i) != CB) {
					end = i + 1;

					if (newCat.result == null) {
						CCGcat resCat = parseCat(cat.substring(start, end));
						resCat.function = newCat;
						newCat.result = resCat;
					} else {
						CCGcat argCat = parseCat(cat.substring(start, end));
						argCat.function = newCat;
						newCat.argument = argCat;
					}
				}
			}
		}
		return newCat;
	}

	// ##################################################
	// PRINT CATEGORIES
	// ##################################################

	private void printCat(PrintStream out) {
		out.println("   |------------------------");
		printCatRec(1, out);
		out.println("   |------------------------");
		out.println();
	}

	private void printCat() {
		printCat(System.out);

	}

	public String print() {
		String out = "";
		out += "   |------------------------\n";
		out = printCatRec(1, out);
		out += "   |------------------------\n";
		return out;
	}

	private String printCatRec(int offset, String out) {
		out = printOffset(offset, out);
		out += "ID:" + id + " HeadId:" + headId + "\n";

		out = printOffset(offset, out);
		out += "Cat: " + catString + "\n";
		// out = printOffset(offset, out);
		// out += "Extracted: " + extracted + " bounded:" + bounded + "\n";
		// if (yield != null){
		// printOffset(offset, out);
		// out.println("Yield: <" + yield + ">");
		// }
		if (heads() != null) {
			out = printOffset(offset, out);
			out += "Head: ";
			out = heads().print(out);
		}
		if (Configuration.CCGcat_CCG) {
			if (CCGdeps() != null) {
				out = printOffset(offset, out);
				out += "CCG Deps: ";
				out = CCGdeps().print(out);
			}
			if (filled_ccgDependencies != null) {
				out = printOffset(offset, out);
				out += "FilledCCGDeps: ";
				out = filled_ccgDependencies.print(out);
			}
		}
		if (Configuration.CCGcat_CoNLL) {
			if (CoNLLdeps() != null) {
				out = printOffset(offset, out);
				out += "CoNLL Deps: ";
				out = CoNLLdeps().print(out);
			}

			if (filled_conllDependencies != null) {
				out = printOffset(offset, out);
				out += "FilledConLLDeps: ";
				out = filled_conllDependencies.print(out);
			}
		}
		// out += "\n";
		if (argument != null) {
			out = printOffset(offset, out);
			if (argDir == FW) {
				out += "FW ";
			}
			if (argDir == BW) {
				out += "BW ";
			}
			out += "Arg: \n";
			out = argument.printCatRec(offset + 1, out);
		}
		if (result != null) {
			out = printOffset(offset, out);
			out += "Result: \n";
			out = result.printCatRec(offset + 1, out);
		}
		return out;
	}

	private void printCatRec(int offset, PrintStream out) {
		printOffset(offset, out);
		out.println("ID:" + id + " HeadId:" + headId);

		printOffset(offset, out);
		out.println("Cat: " + catString);
		// printOffset(offset, out);
		// out.println("Extracted: " + extracted + " bounded:" + bounded);

		if (heads() != null) {
			printOffset(offset, out);
			out.print("Head: ");
			heads().print(out);
		}
		if (Configuration.CCGcat_CCG) {
			if (CCGdeps() != null) {
				printOffset(offset, out);
				out.print("CCG Deps: ");
				CCGdeps().print(out);
			}
			// else {printOffset(offset, out); out.println("CCGDeps = null");}
			if (filled_ccgDependencies != null) {
				printOffset(offset, out);
				out.print("FilledCCGDeps: ");
				filled_ccgDependencies.print(out);
			}
			// else {printOffset(offset, out); out.println("FilledCCGDeps = null");}
		}
		if (Configuration.CCGcat_CoNLL) {
			if (CoNLLdeps() != null) {
				printOffset(offset, out);
				out.print("CoNLL Deps: ");
				CoNLLdeps().print(out);
			}
			// else {printOffset(offset, out); out.println("ConLL Deps = null");}
			if (filled_conllDependencies != null) {
				printOffset(offset, out);
				out.print("FilledConLLDeps: ");
				filled_conllDependencies.print(out);
			}
			// else {printOffset(offset, out);
			// out.println("filled ConLLDeps = null");}
		}

		if (argument != null) {
			printOffset(offset, out);
			if (argDir == FW) {
				out.print("FW ");
			}
			if (argDir == BW) {
				out.print("BW ");
			}
			out.println("Arg: ");
			argument.printCatRec(offset + 1, out);
		}
		if (result != null) {
			printOffset(offset, out);
			out.println("Result: ");
			result.printCatRec(offset + 1, out);
		}

	}

	private static void printOffset(int offset, PrintStream out) {
		for (int i = offset; i > 0; i--) {
			out.print("   |");
		}
	}

	private static String printOffset(int offset, String out) {
		for (int i = offset; i > 0; i--) {
			out += "   |";
		}
		return out;
	}

	// ##########################################################
	// COPY
	// ##########################################################
	public CCGcat copy() {
		CCGcat copy = null;
		try {
			copy = (CCGcat) this.clone();
		} catch (Exception E) {
			E.printStackTrace();
		}
		CCGcat tmp, tmpcopy;
		tmp = this;
		tmpcopy = copy;
		tmpcopy.copyHeadsDeps(tmp);
		if (tmp.argument != null) {
			tmpcopy.argument = tmp.argument.copy();
			tmpcopy.result = tmp.result.copy();
			tmpcopy.result.function = tmpcopy;
		}
		return copy;
	}

	private void copyHeadsDeps(CCGcat original) {
		this.headId = original.headId;// copyHeadsDeps
		if (original.heads() != null)
			this.heads = original.heads().copy();
		else
			this.heads = null;

		if (Configuration.CCGcat_CCG) {
			if (original.CCGdeps() != null)
				this.ccgDependencies = original.CCGdeps().copy();
			if (original.filled_ccgDependencies != null)
				this.filled_ccgDependencies = original.filled_ccgDependencies.copy();
			else
				this.filled_ccgDependencies = null;
		}

		if (Configuration.CCGcat_CoNLL) {
			if (original.CoNLLdeps() != null)
				this.conllDependencies = original.CoNLLdeps().copy();
			if (original.filled_conllDependencies != null)
				this.filled_conllDependencies = original.filled_conllDependencies.copy();
			else
				this.filled_conllDependencies = null;
		}
	}

	// ##########################################################
	// MATCHING, UNIFICATION
	// ##########################################################
	private boolean matches(CCGcat cat) {
		boolean retval = false;
		// strict equality matches
		if (this.catString().equals(cat.catString())
				|| (this.catString().equals(NOUN) && cat.catString().equals(NP))
				|| (this.catString().equals(NP) && cat.catString().equals(NOUN))) {
			retval = true;
		} else {
			// no features match any features
			if (this.catStringNoFeatures().equals(cat.catString())) {
				retval = true;
			} else {
				// and any features match no features
				if (cat.catStringNoFeatures().equals(this.catString())) {
					retval = true;
				} else {
					// but if both have features, then we need a more thorough check!
					if (cat.catStringNoFeatures().equals(this.catStringNoFeatures())) {
						retval = matchRecursively(cat);
					}
				}
			}
		}
		return retval;
	}

	private boolean matchRecursively(CCGcat cat) {
		boolean retval = false;
		// strict equality matches

		// no features match any features
		if (cat != null) {
			if (this.catString().equals(cat.catString())
					|| this.catStringNoFeatures().equals(cat.catString())
					|| cat.catStringNoFeatures().equals(this.catString())) {
				retval = true;
			} else {
				if (result != null && argument != null) {
					retval = argument.matchRecursively(cat.argument);
					if (retval) {
						retval = result.matchRecursively(cat.result);
					}
				}
			}
		}
		return retval;
	}

	private boolean matches(String catString1) {
		boolean retval = false;
		if (this.catString != null) {
			if (catString.equals(catString1)) {
				retval = true;
			} else {
				if ((catString.equals(NOUN) && catString1.equals(NP))
						|| (catString.equals(NP) && catString1.equals(NOUN))) {
					retval = true;
				} else if (catStringNoFeatures().equals(noFeatures(catString1))) {
					retval = true;
				}
			}
		}
		// System.out.println("Matches: " + catString + " " + catString1 + "? " +
		// retval);
		return retval;
	}

	// ##################################################
	// METHODS FOR CATSTRING
	// - FORWARD, BACKWARD
	// - BRACKETING
	// - REPARSE CATSTRING
	// ##################################################
	private static String forward(String cat1, String cat2) {
		String tmp1, tmp2;
		if (needsBrackets(cat1)) {
			tmp1 = bracket(cat1);
		} else
			tmp1 = cat1;
		if (needsBrackets(cat2)) {
			tmp2 = bracket(cat2);
		} else
			tmp2 = cat2;
		return (new StringBuffer(tmp1).append(FSLASH).append(tmp2)).toString();
	}

	private static String backward(String cat1, String cat2) {
		String tmp1, tmp2;
		if (needsBrackets(cat1)) {
			tmp1 = bracket(cat1);
		} else
			tmp1 = cat1;
		if (needsBrackets(cat2)) {
			tmp2 = bracket(cat2);
		} else
			tmp2 = cat2;
		return (new StringBuffer(tmp1).append(BSLASH).append(tmp2)).toString();
	}

	/** bracket */
	private static String bracket(String cat) {
		return (new StringBuffer("(").append(cat).append(CB)).toString();
	}

	/** boolean: needs brackets? **/
	private static boolean needsBrackets(String cat) {
		if (cat.indexOf(BSLASH) != -1 || cat.indexOf(FSLASH) != -1) {
			return true;
		}
		return false;
	}

	/** reparse the cat string in complex category */
	private void catStringReparse() {

		if (result != null) {
			result.catStringReparse();
			if (argDir == FW) {
				this.catString = forward(result.catString, argument.catString);
			} else {
				if (argDir == BW) {
					this.catString = backward(result.catString, argument.catString);
				} else {
					System.err.println("catStringReparse ERROR: no direction!");
					this.printCat(System.err);
				}
			}
			if (result.function != this) {
				System.err.println("ERROR: result.function != this");
				System.err.println("Result function");
				result.function.printCat(System.err);
				System.err.println("THIS");
				this.printCat(System.err);
			}
		}
	}

	private static synchronized int newId() {
		if (idCounter < Integer.MAX_VALUE)
			++idCounter;
		else
			idCounter = 0;
		return idCounter;
	}

	private static synchronized int newHeadId() {
		if (headIdCounter < Integer.MAX_VALUE)
			++headIdCounter;
		else
			headIdCounter = 0;
		return headIdCounter;
	}

	// ##########################################################
	// ##
	// ## THE COMBINATORY RULES
	// ##
	// ##########################################################
	public static CCGcat typeRaiseTo(CCGcat X, String typeRaisedX) {
		CCGcat typeRaised = null;
		if (typeRaisedX.equals("S/(S/NP)")) {
			typeRaised = X.topicalize(S);
		} else {
			if (typeRaisedX.indexOf('/') > -1 && typeRaisedX.indexOf('\\') > -1) {
				CCGcat tmp = parseCat(typeRaisedX);

				if (tmp.argument.result != null
						&& tmp.argument.result.catString.equals(tmp.result.catString)) {
					typeRaised = X.typeRaise(tmp.result.catString, tmp.argDir);
				}
			}
		}

		return typeRaised;

	}

	// in LEFT node raising (Y X\Y conj X\Y),
	// all backward args in the second conjunct are long-range deps
	private void adjustLongRangeDepsLNR() {
		CCGcat tmp = this;
		if (argument != null && argDir == FW) {
			tmp = tmp.result;
			while (tmp.argument != null && tmp.argDir == FW) {
				tmp = tmp.result;
			}
		}
		if (tmp.argument != null && tmp.argDir == BW && (tmp.argument.ccgDependencies != null)) {
			tmp.adjustLongRangeDepsLNRRec();
		}
		// if (tmp.argument != null
		// && tmp.argDir == BW
		// && tmp.argument.conllDependencies != null){ // TODO: CoNLL dependencies?
		// tmp.adjustLongRangeDepsLNRRec();
		// }
	}

	private void adjustLongRangeDepsLNRRec() {
		if (argument != null && argDir == BW && argument.ccgDependencies != null) {
			argument.ccgDependencies.setToExtractedUnbounded();
			result.adjustLongRangeDepsLNRRec();
		}
		// if (argument != null
		// && argDir == BW
		// && argument.conllDependencies != null){ // TODO: CoNLL dependencies?
		// argument.conllDependencies.setToExtractedUnbounded();
		// result.adjustLongRangeDepsLNRRec();
		// }

	}

	// in RIGHT node raising (X/Y conj X/Y Y),
	// all forward args are long-range deps
	private void adjustLongRangeDepsRNR() {
		if (argument != null && argDir == FW && argument.ccgDependencies != null) {
			argument.ccgDependencies.setToExtractedUnbounded();
			result.adjustLongRangeDepsRNR();
		}
		// if (argument != null
		// && argDir == FW
		// && argument.conllDependencies != null){ // TODO: CoNLL dependencies?
		// argument.conllDependencies.setToExtractedUnbounded();
		// result.adjustLongRangeDepsRNR();
		// }
	}

	/**
	 * COMBINING TWO CONSTITUENTS TO OBTAIN A CONSTITUENT WITH CATEGORY
	 * resultCatString
	 */
	public static CCGcat combine(CCGcat leftCat, CCGcat rightCat, String resultCatString) {
		CCGcat resultCat = null;
		FOR = false;
		if (leftCat != null && rightCat != null) {
			// INTERMEDIATE STAGE IN CONJUNCTION?
			if (resultCatString.endsWith(CONJFEATURE)
					|| (leftCat.catString().equals("conj") && rightCat.matches(resultCatString))
					|| (rightCat.catString().equals("conj") && leftCat.matches(resultCatString))) {
				// X[conj] --> conj X
				// intermediate stage in coordination
				// if (!resultCatString.endsWith(CONJFEATURE)){
				// System.out.println("### Intermediate stage in conjunction: " +
				// resultCatString + " --> " + leftCat.catString() + " " +
				// rightCat.catString());
				// }
				resultCat = conjunction(rightCat, leftCat);// rightCat = X, leftcat =
				// conj;//X[conj] --> conj X

				if (resultCat != null) {
					resultCat.adjustLongRangeDepsLNR();
					if (!resultCat.matches(resultCatString)) {
						resultCat = typeChangingRule(rightCat, resultCatString);

						resultCat.type = Rule_Type.FW_CONJOIN_TC; // added by Ryan
					} else {

						resultCat.type = Rule_Type.FW_CONJOIN; // added by Ryan
					}
					resultCat.catString = resultCatString;

					// System.out.println("### Intermediate stage in conjunction: " +
					// resultCatString);
					// leftCat.printCat();
					// rightCat.printCat();
					// resultCat.printCat();
				}
				if (resultCat == null) {// X[conj] --> X conj
					// intermediate stage in coordination: X -> X conj.
					// This is there due to noise in CCGbank.
					// ATM, 53 such instances in wsj02-21.
					resultCat = conjunction(leftCat, rightCat);

					if (resultCat != null) {
						if (!resultCat.matches(resultCatString)) {
							resultCat = typeChangingRule(leftCat, resultCatString);
							resultCat.type = Rule_Type.BW_CONJOIN_TC; // added by Ryan
						} else {
							resultCat.type = Rule_Type.BW_CONJOIN; // added by Ryan
						}
						resultCat.catString = resultCatString;

					}
				}

			} else {
				if (rightCat.catString().endsWith(CONJFEATURE)) {// X ==> X X[conj]
					// the actual coordination step
					resultCat = coordinate(leftCat, rightCat);// leftCat: X rightCat:
					// X[conj]
					if (resultCat == null) {// X --> , X[conj]
						resultCat = punctuation(rightCat, leftCat);
						if (resultCat != null) {
							if (!resultCat.matches(resultCatString)) {
								resultCat = typeChangingRule(rightCat, resultCatString);
								resultCat.type = Rule_Type.FW_PUNCT_TC; // added by Ryan
							} else {

								resultCat.type = Rule_Type.FW_PUNCT; // added by Ryan
							}
							resultCat.catString = resultCatString;
						}
					} else {
						resultCat.type = Rule_Type.COORDINATION; // added by Ryan
					}

				} else {
					if (leftCat.argDir == FW) {
						resultCat = apply(leftCat, rightCat);
						if (resultCat != null && resultCat.matches(resultCatString)) { // Removed
							// by
							// Ryan:
							// &&
							// !resultCat.equals(resultCatString)){
							resultCat.catString = resultCatString;// NP[nb] etc.

							resultCat.type = Rule_Type.FW_APPLY; // added by Ryan
						}

						if (leftCat.matches("(S[dcl]\\NP)/PP") && rightCat.heads != null
								&& rightCat.heads.cat().equals("PP/(S[ng]\\NP)")) {
							// System.out.print("###PP/VP[ng]: " + leftCat.heads.lexCat +
							// "\t <" +leftCat.headWord() + "," + rightCat.headWord()
							// +"> \t");
							// FOR = true;
						}

					}
					if (resultCat == null && rightCat.argDir == BW) {
						resultCat = apply(rightCat, leftCat);
						// if (resultCat != null) System.out.println("< APPLY");
						if (resultCat != null && resultCat.matches(resultCatString)) { // Removed
							// by
							// Ryan:
							// &&
							// !resultCat.equals(resultCatString)){
							resultCat.catString = resultCatString;// NP[nb] etc.

							resultCat.type = Rule_Type.BW_APPLY; // added by Ryan
						}
						if (rightCat != null && rightCat.heads != null && rightCat.heads.lexCat() != null
								&& rightCat.heads.cat().equals("((S\\NP)\\(S\\NP))/(S[ng]\\NP)") && leftCat != null
								&& leftCat.heads != null && leftCat.heads.lexCat() != null

								) {
							// System.out.print("###modifierPP/VP[ng]: " +
							// leftCat.heads.lexCat + "\t <" +leftCat.headWord() + "," +
							// rightCat.headWord() +"> \t");
							// FOR = true;
						}
					}
					if (resultCat == null) {// X ,
						resultCat = punctuation(leftCat, rightCat);
						if (resultCat != null) {
							// resultCat.yield = new String(leftCat.yield + " " +
							// rightCat.yield);
							if (!resultCat.catString().equals(resultCatString)) {
								resultCat = typeChangingRule(leftCat, resultCatString);
								resultCat.type = Rule_Type.BW_PUNCT_TC; // added by Ryan
							} else {

								resultCat.type = Rule_Type.BW_PUNCT; // added by Ryan
							}
						}
					}
					if (resultCat == null) {// , X
						resultCat = punctuation(rightCat, leftCat);
						if (resultCat != null) {
							// resultCat.yield
							// = new String(leftCat.yield + " " + rightCat.yield);
							if (!resultCat.catString().equals(resultCatString)) {
								resultCat = typeChangingRule(rightCat, resultCatString);
								resultCat.type = Rule_Type.FW_PUNCT_TC; // added by Ryan
							} else {

								resultCat.type = Rule_Type.FW_PUNCT; // added by Ryan
							}
						}
					}
					if (resultCat == null && leftCat.argDir == FW) {
						resultCat = compose(leftCat, rightCat);
						if (resultCat != null && !resultCat.matches(resultCatString)) {
							// System.out.println(resultCat.catString + " doesn't match " +
							// resultCatString);
							resultCat = null;
						} else if (resultCat != null) { // Ryan removed: &&
							// resultCat.equals(resultCatString)){
							resultCat.catString = resultCatString;

							resultCat.type = Rule_Type.FW_COMPOSE; // added by Ryan
						}
					}
					if (resultCat == null && rightCat.argDir == BW) {
						resultCat = compose(rightCat, leftCat);
						if (resultCat != null && !resultCat.matches(resultCatString)) {
							// System.out.println(resultCat.catString + " doesn't match " +
							// resultCatString);
							resultCat = null;
						}
						if (resultCat != null) {
							resultCat.catString = resultCatString; // added by Ryan
							resultCat.type = Rule_Type.BW_COMPOSE; // added by Ryan
						}
					}
					if (resultCat == null) {
						resultCat = substitute(rightCat, leftCat);
						if (resultCat != null) {
							SUBSTITUTION = true;
							/*
							 * System.out.println("Substitution successful: " +
							 * leftCat.catString() + " " + rightCat.catString() + " ==> " +
							 * resultCat.catString());
							 */
							resultCat.type = Rule_Type.FW_SUBSTITUTION;
						}
					}
					if (resultCat == null) {

						resultCat = substitute(leftCat, rightCat);
						if (resultCat != null) {
							SUBSTITUTION = true;
							/*
							 * System.out.println("Substitution successful: " +
							 * leftCat.catString() + " " + rightCat.catString() + " ==> " +
							 * resultCat.catString());
							 */
							resultCat.type = Rule_Type.BW_SUBSTITUTION;
						}
					}
					if (resultCat == null && leftCat.catString().equals(resultCatString)
							&& rightCat.catString().equals(resultCatString)) {// X ==> X X
						// an error in the treebank, but this is also like coordination
						// note: if the cats are adjuncts, they will have composed already
						// System.out.println("COORDINATION " + leftCat.catString + "and " +
						// rightCat.catString);
						DEBUG_COORDINATE = true;
						resultCat = coordinate(leftCat, rightCat);

						resultCat.type = Rule_Type.COORDINATION;
					}
				}
			}
			// if (resultCat != null && !resultCat.catString.equals(resultCatString)){
			// System.out.println("ERROR: " + leftCat.catString() + " and " +
			// rightCat.catString() + " ==> " + resultCat.catString() + " != " +
			// resultCatString);
			// }

			if (resultCat == null && leftCat != null && rightCat != null) {
				System.out.println("ERROR: can't combine " + leftCat.catString() + " and "
						+ rightCat.catString() + " to " + resultCatString);
			} else {
				if (resultCat != null && !resultCat.matches(resultCatString)) {
					// System.out.println("ERROR: combined " + leftCat.catString() +
					// " and " + rightCat.catString() + " to " + resultCat.catString +
					// " != " + resultCatString);
					resultCat = null;
				}
			}
		}
		// if (resultCat == null){System.out.println("ERROR: " + leftCat.catString()
		// + " " + rightCat.catString() + " cannot combine\n");}
		// if (resultCat != null) { System.out.println("COMBINATION RESULT: " +
		// leftCat.catString() + " " + rightCat.catString() + " ==> +" +
		// resultCat.catString()); resultCat.printCat();}

		boolean MYDEBUG = false;
		if (MYDEBUG && resultCat != null) {
			resultCat.printCat();
		}
		return resultCat;
	}

	/**
	 * sets the <tt>headId</tt> of this category and recursively of all result
	 * categories of this category to the same number (<tt>headIdNumber</tt>)
	 */
	public void setHeadId(int headIdNumber) {
		headId = headIdNumber;// setHeadId
		if (result != null) {
			result.setHeadId(headIdNumber);
		}
	}

	// ##################################################
	// APPLICATION
	// ##################################################

	/**
	 * Function application. Unify the argument of the functor with arg, and
	 * return the result of the functor.
	 */

	public static CCGcat apply(CCGcat functor, CCGcat arg) {
		if (functor.argument != null && functor.argument.matches(arg)) {// was arg
			if (DEBUG) {
				System.out.println("### Application: functor");
				functor.printCat();
				System.out.println("### Application: argument");
				arg.printCat();
			}
			CCGcat result = functor.result.copy();
			result.function = null;
			// System.out.println("Application: intermediate category result");
			// result.printCat();
			CCGcat Y = unify(functor.argument, arg);
			if (DEBUG) {
				System.out.println("### Application: intermediate category Y");
				Y.printCat(System.out);
			}
			result.replace(functor.argument, Y);

			// CCG
			if (Configuration.CCGcat_CCG) {
				result.filled_ccgDependencies = Y.filled_ccgDependencies;
				Y.filled_ccgDependencies = null;
			}
			if (Configuration.CCGcat_CoNLL) {
				result.filled_conllDependencies = Y.filled_conllDependencies;
				Y.filled_conllDependencies = null;
			}
			if (functor.argDir == FW) {
				if (functor.catString.equals("NP[nb]/N"))
					result.catString = NP;
			}
			if (DEBUG) {
				System.out.println("### Application: result");
				result.printCat(System.out);
			}
			return result;
		}
		// Commented out by Ryan -- using combine(), we don't necessarily care if we
		// can't successfully apply()
		// System.err.println("Application returns null");
		return null;
	}

	// ##################################################
	// TYPERAISE
	// ##################################################

	/**
	 * Typeraising -- typeraise the current category. The current category
	 * together with <tt>T</tt> and the direction specify the type-raised
	 * category.
	 */
	private CCGcat typeRaise(String T, int direction) {
		CCGcat t = parseCat(T);
		t.setHeadId(newHeadId());// typeraise
		CCGcat newArg = new CCGcat(t.copy(), this);
		newArg.headId = t.headId;// typeraise: argument
		// newArg = T|X
		CCGcat resCat = new CCGcat(t.copy(), newArg);
		// resCat = T|(T|X);
		resCat.argument.result = resCat.result;
		if (direction == FW) {
			newArg.argDir = BW;
			resCat.argDir = FW;
			newArg.catString = backward(t.catString, this.catString);
			resCat.catString = forward(t.catString, newArg.catString);
		} else {
			newArg.argDir = FW;
			resCat.argDir = BW;
			newArg.catString = forward(t.catString, this.catString);
			resCat.catString = backward(t.catString, newArg.catString);
		}
		resCat.function = null;
		resCat.heads = this.heads();
		resCat.headId = this.headId;// typeRaise
		resCat.filled_conllDependencies = null;
		resCat.filled_ccgDependencies = null; // FIXME: Yonatan added (think this is
		// right... ?)
		return resCat;
	}

	// ##################################################
	// TOPICALIZATION
	// ##################################################
	/** topicalization -- similar to type-raising, but yields S/(S/X) */
	private CCGcat topicalize(String local_S) {
		CCGcat t = parseCat(local_S);
		t.setHeadId(newHeadId());// topicalize
		CCGcat newArg = new CCGcat(t.copy(), this);
		newArg.headId = t.headId;
		// newArg = S|X
		CCGcat resCat = new CCGcat(t.copy(), newArg);
		// resCat = S|(S|X);
		resCat.argument.result = resCat.result;
		newArg.argDir = FW;
		resCat.argDir = FW;

		newArg.catString = forward(t.catString, this.catString);
		resCat.catString = forward(t.catString, newArg.catString);

		resCat.function = null;
		resCat.filled_ccgDependencies = null;// new
		resCat.filled_conllDependencies = null;// new
		resCat.heads = this.heads();
		resCat.headId = this.headId;// topicalize
		// resCat.yield = new String(this.yield);
		// resCat.printCat();

		return resCat;
	}

	// ##################################################
	// COMPOSE
	// ##################################################

	/**
	 * (Generalized) function composition. Forward crossing composition is
	 * excluded at the moment.
	 */
	public static CCGcat compose(CCGcat functor, CCGcat arg) {
		// functor == X|Y
		// arg == Y|Z$
		// ==> resCat = X|Z$....

		CCGcat resultCat = null;
		if (functor.argument != null) {
			resultCat = arg.copy();
			resultCat.function = null;
			if (functor.heads != null) {
				if (functor.isAdjunctCat() && !arg.isAdjunctCat()) {
					// NEW MARCH 2005
					// modified Nov 2012: added !arg.isAdjunctCat(): "very + big_red"
					// should be "very BIG", not "very red".
					// System.out.println("XXX COMPOSITION with adjunct " +
					// functor.catString() + " " + arg.catString() + " " +
					// arg.isAdjunctCat());
					resultCat.heads = arg.heads.copy();
				} else
					resultCat.heads = functor.heads.copy(); // hack!
			}
			// make tmp point to a category Y/Z$,
			// then replace its result with X.
			CCGcat tmp = resultCat;
			CCGcat functorArg = functor.argument();
			while (tmp != null && tmp.result() != null && !tmp.result().matches(functorArg)) {
				tmp = tmp.result();
			}

			if (tmp.result() != null && tmp.result().matches(functorArg)
					// && !(functor.argDir == FW && tmp.argDir == BW)// no forward crossing
					// composition!
					) {

				CCGcat functorCopy = functor.copy();
				CCGcat Y = unify(functorCopy.argument(), tmp.result);
				functorCopy.replace(functorCopy.argument(), Y);
				resultCat.replace(tmp.result(), Y);
				tmp.result = functorCopy.result().copy();
				tmp.result.function = tmp;
				if (Configuration.CCGcat_CCG) {
					resultCat.filled_ccgDependencies = Y.filled_ccgDependencies;
					Y.filled_ccgDependencies = null;
				}
				if (Configuration.CCGcat_CoNLL) {
					resultCat.filled_conllDependencies = Y.filled_conllDependencies;
					Y.filled_conllDependencies = null;
				}
				resultCat.catStringReparse();
				// adjust the head
				while (tmp != null) {
					if (functorCopy.result.heads != null)
						tmp.heads = functorCopy.result.heads.copy();// again, this is just
					// another copy
					tmp.headId = functorCopy.result.headId;// topicalize
					tmp = tmp.function;
				}
			} else
				resultCat = null;
		}
		if (DEBUG && resultCat != null) {
			System.out.println("compose result:");
			resultCat.printCat();
		}
		return resultCat;
	}

	/**
	 * coordinate two categories. Calls coordinateRec
	 */
	public static CCGcat coordinate(CCGcat cat1, CCGcat cat2) {// X X[conj]
		CCGcat coordination = null;
		if (cat1.matches(cat2.catString)) {
			coordination = cat1.copy();

			if (Configuration.CCGcat_CCG) {
				coordination.filled_ccgDependencies = null;
			}

			if (Configuration.CCGcat_CoNLL) {
				coordination.filled_conllDependencies = null;
				if (Configuration.CONJSTYLE == ConjStyle.X2_X1___X2_CC)
					coordination.removeConLLDeps();
			}

			// before recursing: adjust the long range ccgDependencies:
			coordination.adjustLongRangeDepsRNR();

			coordination.coordinateRec(cat2);

			if (Configuration.CCGcat_CoNLL) {
				/**
				 * Coordination styles: - CCG: Parent->X1, Parent->X2 - CoNLL:
				 * Parent->conj, conj->X1, conj->X2: conjHeads = conj - Spanish:
				 * Parent->X1, X1->conj, X1->X2 - Other: Parent->X1, X1->X2, X2->conj:
				 * conjHeads = X2;
				 */
				DepList dConj = null;
				switch (Configuration.CONJSTYLE) {
				case CC_X1___CC_X2:// X1<-conj->X2
					// cat2.conjheads = conj
					// cat1.heads = X1 head
					dConj = new DepList("arg", cat2.conjHeads, cat1.heads, 1, BW, false);
					dConj.next = null;
					if (coordination.filled_conllDependencies == null) {
						coordination.filled_conllDependencies = dConj.copy();
					} else {
						coordination.filled_conllDependencies.append(dConj);
					}
					if (cat2.conjHeads != null) {
						coordination.setHeadsRec(cat2.conjHeads);// conj is head of
						// everything
						if (coordination.isAdjunctCat()) {
							// System.out.println("DEBUG CONLL ADJUNCT COORDINATION");
							DepList depArg = null;
							if (coordination.argument != null && coordination.result != null) {
								depArg = coordination.argument.CoNLLdeps();

								if (depArg != null && depArg.argWord == null) {
									DepList newDepArg = new DepList(depArg.rel(), coordination.heads(), null,
											depArg.argIndex, depArg.argDir(), depArg.modifier);
									// System.out.println("New dep: "); newDepArg.print();
									coordination.argument.conllDependencies = newDepArg.copy();
									coordination.result.conllDependencies = newDepArg.copy();
								}
							}
						}
					}
					break;
				case X1_CC___X1_X2:
					// cat1: X1
					// cat2: X2;
					// cat2.conjhead: conj
					// System.out.println("SPANISH cat1: " + cat1.heads.asString()
					// + " cat2: " + cat2.heads.asString()
					// + " cat2.conj: " + cat2.conjHeads.asString());
					dConj = new DepList("arg", cat1.heads, cat2.heads, 1, FW, false);
					dConj.next = new DepList("arg", cat1.heads, cat2.conjHeads, 1, FW, false);
					dConj.next.next = null;
					if (coordination.filled_conllDependencies == null) {
						coordination.filled_conllDependencies = dConj.copy();
					} else {
						coordination.filled_conllDependencies.append(dConj);
					}
					coordination.setHeadsRec(cat1.heads);
					if (coordination.isAdjunctCat()) {
						// System.out.println("ADJUNCT COORDINATION!!!");
						DepList depArg = null;
						if (coordination.argument != null && coordination.result != null) {
							depArg = coordination.argument.CoNLLdeps();
							// depArg.print();
							if (depArg != null && depArg.argWord == null) {
								DepList newDepArg = new DepList(depArg.rel(), coordination.heads(), null,
										depArg.argIndex, depArg.argDir(), depArg.modifier);
								// newDepArg.print();
								coordination.argument.conllDependencies = newDepArg.copy();
								coordination.result.conllDependencies = newDepArg.copy();
							}
						}
					}
					break;
				case X1_CC___CC_X2: // X1 -> conj -> X2
					// cat1: X1
					// cat2: conj;
					// System.out.println("OTHER: cat1: " + cat1.heads.asString() +
					// " cat2: " + cat2.heads.asString());
					dConj = new DepList("arg", cat1.heads, cat2.conjHeads, 1, FW, false);
					dConj.next = null;
					if (coordination.filled_conllDependencies == null) {
						coordination.filled_conllDependencies = dConj.copy();
					} else {
						coordination.filled_conllDependencies.append(dConj);
					}
					coordination.setHeadsRec(cat1.heads);
					break;
				case X1_X2___X2_CC: // X2->conj, X2->X1
					// cat1: X1
					// cat2: conj;
					// System.out.println("OTHER: cat1: " + cat1.heads.asString() +
					// " cat2: " + cat2.heads.asString());
					dConj = new DepList("arg", cat1.heads, cat2.heads, 1, FW, false);
					dConj.next = null;
					if (coordination.filled_conllDependencies == null) {
						coordination.filled_conllDependencies = dConj.copy();
					} else {
						coordination.filled_conllDependencies.append(dConj);
					}
					coordination.setHeadsRec(cat1.heads);
					break;
				case X2_X1___X2_CC:// julia

					dConj = new DepList("arg", cat2.heads, cat1.heads, 1, BW, false);
					dConj.next = null;
					if (coordination.filled_conllDependencies == null) {
						coordination.filled_conllDependencies = dConj.copy();
					} else {
						coordination.filled_conllDependencies.append(dConj);
					}
					// coordination.printCat();
					coordination.setHeadsRec(cat2.heads);
					// coordination.printCat();
					break;
				}
			}
		}
		return coordination;
	}

	/** coordinate recursively with another category. Is called by coordinate */
	private void coordinateRec(CCGcat cat) {
		if (argument != null) {
			argument.coordinateRec(cat.argument);
		}
		if (result != null) {
			if (result.id == argument.id) {// coordination
				result = argument.copy();
				if (Configuration.CCGcat_CCG)
					result.ccgDependencies = null;
				if (Configuration.CCGcat_CoNLL)
					result.conllDependencies = null;
			} else
				result.coordinateRec(cat.result);
		}
		appendHeads(cat.heads());
		if (Configuration.CCGcat_CCG)
			appendCCGDeps(cat.CCGdeps());
		if (Configuration.CCGcat_CoNLL)
			appendCoNLLDeps(cat.CoNLLdeps()); // TODO: Should just pass cat (not deps)
	}

	/**
	 * Standard substitution: cat1 = (X\Y)/Z cat2=Y/Z ==> X/Z The result Z is Z1
	 * and Z2 coordinated.
	 */
	public static CCGcat substitute(CCGcat cat1, CCGcat cat2) {
		CCGcat result = null;
		// System.out.println("Substitution:");
		// System.out.println("cat1 = " + cat1.catString());
		// System.out.println("cat2 = " + cat2.catString());
		if (cat1.argument != null && cat1.result.argument != null && cat1.result.result != null
				&& cat2.argument != null && cat2.result != null && cat1.argument.matches(cat2.argument)
				&& cat1.result.argument.matches(cat1.result.result)
				// && cat1.target().matches(S)
				&& cat1.result.argument.matches(cat2.result)) {
			// System.out.println("substitute(" + cat1.catString + ", " +
			// cat2.catString + ")");

			CCGcat functor = cat1.copy();
			CCGcat arg = cat2.copy();
			CCGcat Z1 = functor.argument;
			CCGcat Y1 = functor.result.argument;
			// CCGcat X1 = functor.result.result;
			CCGcat Z2 = arg.argument;
			CCGcat Y2 = arg.result;

			CCGcat Z3 = coordinate(Z1, Z2);
			functor.replace(Z1.id, Z1.headId, Z3);// substitution
			arg.replace(Z2.id, Z2.headId, Z3);// substitution
			CCGcat Y3 = unify(Y1, Y2);
			functor.replace(Y1, Y3);
			arg.replace(Y2, Y3);

			result = functor.result();
			result.argDir = functor.argDir;
			result.argument = functor.argument;
			result.argument.function = result;
			result.catStringReparse();

			if (Configuration.CCGcat_CCG) {
				result.filled_ccgDependencies = Y3.filled_ccgDependencies;
				Y3.filled_ccgDependencies = null;
			}
			if (Configuration.CCGcat_CoNLL) {
				result.filled_conllDependencies = Y3.filled_conllDependencies;
				Y3.filled_conllDependencies = null;
			}

			if (result.result.heads != null) {
				result.heads = result.result.heads.copy();
				result.headId = result.result.headId; // coordination
			}

		}
		return result;
	}

	/**
	 * A special rule for punctuation. Returns a copy of the main category. At the
	 * moment, does not change the yield.
	 */
	public static CCGcat punctuation(CCGcat cat, CCGcat pct) {
		CCGcat result = null;

		try {
			boolean Yonatan_punct = TAGSET.Punct(TAGSET.valueOf(pct.catString));
			if (Yonatan_punct || pct.catString.equals(",") || pct.catString.equals(".")
					|| pct.catString.equals(";") || pct.catString.equals(":") || pct.catString.equals("RRB")
					|| pct.catString.equals("LRB") || pct.catString.equals("``")
					|| pct.catString.equals("\'\'")) {
				result = cat.copy();
				result.filled_ccgDependencies = null;
				result.filled_conllDependencies = null;
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		return result;

	}

	/**
	 * Special rule for the intermediate stage of coordination.
	 */
	public static CCGcat conjunction(CCGcat cat, CCGcat conjCat) {
		CCGcat result = null;
		try {
			boolean Yonatan_conj = TAGSET.CONJ(TAGSET.valueOf(conjCat.catString));
			if (conjCat.catString.equals("conj") || conjCat.catString.equals(",")
					|| conjCat.catString.equals(";") || conjCat.catString.equals(":")
					|| conjCat.catString.equals(".") || conjCat.catString.equals("LRB")
					|| conjCat.catString.equals("RRB") || Yonatan_conj) {
				result = cat.copy();
				if (Configuration.CCGcat_CCG) {
					result.filled_ccgDependencies = null;
				}
				if (Configuration.CCGcat_CoNLL) {// X1<-conj->X2
					CCGcat X2 = cat;

					// TODO: May not need conjHeads except for CoNLL
					/**
					 * Coordination styles: - CCG: Parent->X1, Parent->X2 - CoNLL:
					 * Parent->conj, conj->X1, conj->X2 - Spanish: Parent->X1, X1->conj,
					 * X1->X2 - Other: Parent->X1, X1->conj conj->X2 - Other2: Parent->X1,
					 * X1->X2, X2->conj
					 */
					switch (Configuration.CONJSTYLE) {
					case CC_X1___CC_X2:
						result.filled_conllDependencies = new DepList("arg", conjCat.heads(), X2.heads(), 2,
								FW, false);
						result.conjHeads = conjCat.heads().copy();
						break;
					case X1_CC___X1_X2:
						result.conjHeads = conjCat.heads().copy();
						result.filled_conllDependencies = null;
						break;
					case X1_CC___CC_X2:// conj->X2
						result.filled_conllDependencies = new DepList("arg", conjCat.heads(), X2.heads(), 2,
								FW, false);
						result.conjHeads = conjCat.heads().copy();
						break;
					case X1_X2___X2_CC:
						result.filled_conllDependencies = new DepList("arg", X2.heads(), conjCat.heads(), 2,
								BW, false);
						result.conjHeads = X2.heads().copy();// MAY NEED TO BE FIXED
						break;
					case X2_X1___X2_CC:
						result.filled_conllDependencies = new DepList("arg", X2.heads(), conjCat.heads(), 2,
								BW, false);
						result.conjHeads = null;
						break;
					}
					if (Configuration.CONJSTYLE != ConjStyle.X2_X1___X2_CC)
						result.removeConLLDeps();

				}
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		return result;
	}

	/*
	 * recursively remove ConLL Dependencies - used for conjuncts, since in
	 * dependency grammar there is only a single head
	 */
	private void removeConLLDeps() {
		conllDependencies = null;
		if (argument != null) {
			argument.removeConLLDeps();
		}
		if (result != null) {
			result.removeConLLDeps();
		}
	}

	/**
	 * Type-changing rules: change <tt>dtrCat</tt> to a category with string
	 * representation <tt>cat</tt>. Copies the head and yield of dtrCat. Calls
	 * "assignHeadsDeps".
	 */
	public static CCGcat typeChangingRule(CCGcat dtrCat, String cat) {

		CCGcat newCat = null;

		if (cat != null && dtrCat != null) {

			newCat = parseCat(cat);
			newCat.target().assignHeadsDeps(1, newCat.heads, newCat.headId);// TCR

			if (dtrCat.heads != null) {
				newCat.heads = dtrCat.heads.copy();
			}
			newCat.filled_ccgDependencies = null;
			newCat.filled_conllDependencies = null;

			// if the argument of newCat matches the argument of dtrCat, then they are
			// the same!
			if (newCat.argument != null && dtrCat.argument != null && newCat.argument.catString != null
					&& dtrCat.argument.catString != null
					&& newCat.argument.matches(dtrCat.argument.catString)
					&& !(dtrCat.catString.equals("S[to]\\NP") && cat.equals("NP\\NP"))
					&& !dtrCat.catString.equals("S[dcl]\\NP") && !dtrCat.catString.equals("S[b]\\NP")) {

				if (Configuration.CCGcat_CCG) {
					if (dtrCat.argument.ccgDependencies != null) {
						newCat.argument.ccgDependencies = dtrCat.argument.ccgDependencies.copy();
						if (dtrCat.argDir == FW)
							newCat.argument.ccgDependencies.setToExtractedUnbounded();
						if (dtrCat.argDir == BW)
							newCat.argument.ccgDependencies.setToExtractedBounded();
						// newCat.argument.dependencies.setToExtracted();
						// System.out.println("TYPE-CHANGING: pass argument from" +
						// dtrCat.catString + " ==> " + cat );
						// newCat.printCat();
					}
				}
				// FIXME: Currently Yonatan's code doesn't have typeChanging (needs to
				// be debugged)
				if (Configuration.CCGcat_CoNLL) {
					if (dtrCat.argument.conllDependencies != null) {
						newCat.argument.conllDependencies = dtrCat.argument.conllDependencies.copy();
						if (dtrCat.argDir == FW)
							newCat.argument.conllDependencies.setToExtractedUnbounded();
						if (dtrCat.argDir == BW)
							newCat.argument.conllDependencies.setToExtractedBounded();
					}
				}
			} else {
				// (S\NP)\(S\NP) ==> S\NP
				if (newCat.argument != null && newCat.result != null && dtrCat != null
						&& dtrCat.catString != null && dtrCat.matches(VP)
						&& newCat.argument.matches(dtrCat.catString) && newCat.result.matches(dtrCat.catString)) {

					if (Configuration.CCGcat_CCG) {
						if (dtrCat.argument.ccgDependencies != null) {
							newCat.argument.argument.ccgDependencies = dtrCat.argument.ccgDependencies.copy();
							newCat.argument.argument.ccgDependencies.setToExtractedBounded();
							newCat.result.argument.ccgDependencies = dtrCat.argument.ccgDependencies.copy();
							newCat.result.argument.ccgDependencies.setToExtractedBounded();
							// System.out.println("TYPE-CHANGING (2): pass argument from" +
							// dtrCat.catString + " ==> " + cat );
							// newCat.printCat();
						}
					}

					if (Configuration.CCGcat_CoNLL) {
						if (dtrCat.argument.conllDependencies != null) {
							newCat.argument.argument.conllDependencies = dtrCat.argument.conllDependencies.copy();
							newCat.argument.argument.conllDependencies.setToExtractedBounded();
							newCat.result.argument.conllDependencies = dtrCat.argument.conllDependencies.copy();
							newCat.result.argument.conllDependencies.setToExtractedBounded();
						}
					}
				}
			}
		}
		return newCat;
	}

	private static CCGcat unify(CCGcat cat1, CCGcat cat2) {
		// assumes that cat1, cat2 have the same categories
		CCGcat newCat = null;
		DepList copiedUnfilled = null;

		// System.out.println("ENTER UNIFY " + cat1.catString() + " " +
		// cat2.catString());

		if (cat2.hasFeatures()) {
			// System.out.println("CASE 1");
			newCat = cat2.copy();
			newCat.id = newId();// unification -- case1

			if (Configuration.CCGcat_CCG) {
				// copiedUnfilled is a list of the unfilledDependencis in newCat
				if (newCat.filled_ccgDependencies != null)
					copiedUnfilled = newCat.filled_ccgDependencies.copyUnfilled();
				newCat.filled_ccgDependencies = newCat.mergeWithCCG(cat1, copiedUnfilled);
			}
			if (Configuration.CCGcat_CoNLL) {
				// System.out.println("UNIFY: before: "); newCat.printFilledCoNLLDeps();
				if (newCat.filled_conllDependencies != null)
					copiedUnfilled = newCat.filled_conllDependencies.copyUnfilled();
				newCat.filled_conllDependencies = newCat.mergeWithCoNLL(cat1, copiedUnfilled);
				// System.out.println("UNIFY after: "); newCat.printFilledCoNLLDeps();
			}
		} else {
			if (DEBUG)
				System.out.println("CASE 2");
			newCat = cat1.copy();
			newCat.id = newId();// unification -- case2
			if (Configuration.CCGcat_CCG) {
				// new
				if (newCat.filled_ccgDependencies != null)
					copiedUnfilled = newCat.filled_ccgDependencies.copyUnfilled();
				if (DEBUG) {
					System.out.println("before filledDependenciecs:");
					newCat.printCat();
				}
				newCat.filled_ccgDependencies = newCat.mergeWithCCG(cat2, copiedUnfilled);
				if (DEBUG) {
					System.out.println("after filled_ccgDependencies: ");
					newCat.printCat();
				}
			}
			if (Configuration.CCGcat_CoNLL) {
				if (DEBUG) {
					System.out.println("UNIFY BEFORE: x ");
					newCat.printCat();
				}// newCat.printFilledCoNLLDeps();
				if (newCat.filled_conllDependencies != null)
					copiedUnfilled = newCat.filled_conllDependencies.copyUnfilled();
				newCat.filled_conllDependencies = newCat.mergeWithCoNLL(cat2, copiedUnfilled);
				if (DEBUG) {
					System.out.println("UNIFY AFTER:");
					newCat.printCat();
				}// newCat.printFilledCoNLLDeps();
			}
		}
		return newCat;
	}

	private boolean hasFeatures() {
		if (catString != null && catString.indexOf('[') > -1)
			return true;
		return false;
	}

	/** merge this category with cat recursively */
	private DepList mergeWithCCG(CCGcat cat, DepList currentDeps) {

		/*
		 * System.out.println("ENTER mergeWith: " + catString + " " + this.extracted
		 * + " " + cat.extracted); if (currentDeps != null) currentDeps.print();
		 * this.printCat(System.out); cat.printCat(System.out);
		 */
		if (!this.hasFeatures() && cat.hasFeatures()) {
			catString = new String(cat.catString);
		}
		// RECURSION
		// 1. if the result has the same id as the argument,
		// then treat the argument first, and then copy it over to the result.
		if (argument != null && (argument.id == result.id || cat.argument.id == cat.result.id)) {// mergeWith
			if (argument != null) {
				currentDeps = argument.mergeWithCCG(cat.argument, currentDeps);
			}
			result = argument.copy();
		}
		// 2. otherwise, do the argument and result separately
		else {
			if (argument != null) {
				currentDeps = argument.mergeWithCCG(cat.argument, currentDeps);
			}
			if (result != null) {
				currentDeps = result.mergeWithCCG(cat.result, currentDeps);
			}
		}

		// THE BASE STEP: FOR EACH CATEGORY:

		// prepending:
		if (cat.extracted && ccgDependencies != null) {
			// if ONE of them is unbounded, the result is unbounded
			if (cat.bounded && this.bounded) {
				ccgDependencies.setToExtractedBounded();
			} else {
				ccgDependencies.setToExtractedUnbounded();
			}
		}
		// new -- March 06: prepending the other way!
		if (this.extracted && cat.ccgDependencies != null) {
			// if ONE of them is unbounded, the result is unbounded
			if (cat.bounded && this.bounded) {
				cat.ccgDependencies.setToExtractedBounded();
			} else {
				cat.ccgDependencies.setToExtractedUnbounded();
			}
		}

		// COPY THE HEADS (fill the ccgDependencies if they are there)
		if (heads == null && cat.heads != null) {
			heads = cat.heads.copy();
			headId = cat.headId; // TCR
			if (ccgDependencies != null) {
				currentDeps = applyCCGDependencies(CCGdeps(), heads(), currentDeps);
			}
		} else {
			appendHeads(cat.heads);
			if (ccgDependencies != null && heads != null) {
				currentDeps = applyCCGDependencies(CCGdeps(), heads(), currentDeps);
			}
		}

		// COPY THE DEPENDENCIES (fill the ccgDependencies if they are there)
		if (ccgDependencies == null && cat.ccgDependencies != null) {
			ccgDependencies = cat.ccgDependencies.copy();

			if (heads != null) {
				// System.out.print("### COPY DEPENDENCIES"); deps().print();
				currentDeps = applyCCGDependencies(CCGdeps(), heads(), currentDeps);
			}
		} // IF THERE ARE MULTIPLE DEPENDENCIES: APPEND, THEN APPLY
		else {
			if (ccgDependencies != null && cat.ccgDependencies != null) {
				if (extracted) {// the multiple ccgDependencies are extracted!!!
					// cat.ccgDependencies.setToExtracted();
					if (bounded && cat.bounded) {
						cat.ccgDependencies.setToExtractedBounded();
					} else {
						cat.ccgDependencies.setToExtractedUnbounded();
					}
				}
				appendCCGDeps(cat.ccgDependencies);
				if (heads != null) {
					// System.out.print("### APPEND DEPENDENCIES"); deps().print();
					currentDeps = applyCCGDependencies(CCGdeps(), heads(), currentDeps);
				}
			}
		}
		return currentDeps;
	}

	private DepList mergeWithCoNLL(CCGcat cat, DepList currentDeps) {

		if (DEBUG) {
			System.out.println("ENTER mergeWithCoNLL: " + catString + " " + this.extracted + " "
					+ cat.extracted);
			System.out.print("CurrentDeps:");
			if (currentDeps != null)
				currentDeps.print();
			System.out.println();
			System.out.println("This category:");
			this.printCat(System.out);
			System.out.println("The other category (\'cat\'):");
			cat.printCat(System.out);
		}
		if (!this.hasFeatures() && cat.hasFeatures()) {
			catString = new String(cat.catString);
		}
		// RECURSION
		// 1. if the result has the same id as the argument,
		// then treat the argument first, and then copy it over to the result.
		if (argument != null && (argument.id == result.id || cat.argument.id == cat.result.id)) {// mergeWith
			if (argument != null) {
				currentDeps = argument.mergeWithCoNLL(cat.argument, currentDeps);
			}
			result = argument.copy();
		}
		// 2. otherwise, do the argument and result separately
		else {
			if (argument != null) {
				currentDeps = argument.mergeWithCoNLL(cat.argument, currentDeps);
			}
			if (result != null) {
				currentDeps = result.mergeWithCoNLL(cat.result, currentDeps);
			}

		}

		// THE BASE STEP: FOR EACH CATEGORY:

		// prepending:
		if (cat.extracted && conllDependencies != null) {
			// if ONE of them is unbounded, the result is unbounded
			if (cat.bounded && this.bounded) {
				conllDependencies.setToExtractedBounded();
			} else {
				conllDependencies.setToExtractedUnbounded();
			}
		}
		// new -- March 06: prepending the other way!
		if (this.extracted && cat.conllDependencies != null) {
			// if ONE of them is unbounded, the result is unbounded
			if (cat.bounded && this.bounded) {
				cat.conllDependencies.setToExtractedBounded();
			} else {
				cat.conllDependencies.setToExtractedUnbounded();
			}
		}

		// COPY THE HEADS (fill the conllDependencies if they are there)
		if (heads == null && cat.heads != null) {
			heads = cat.heads.copy();
			headId = cat.headId; // TCR
			if (DEBUG) {
				System.out.println("copied the heads: " + heads);
			}
			if (conllDependencies != null) {
				if (DEBUG) {
					System.out.println("applying CoNLLdependencies: ");
					currentDeps.print();
				}
				currentDeps = applyConLLDependencies(CoNLLdeps(), heads(), currentDeps);
				if (DEBUG)
					currentDeps.print();
			}

		} else {
			// heads().print(System.out);
			if (!Configuration.CCGcat_CCG) {
				appendHeads(cat.heads);
			}
			if (conllDependencies != null && heads != null) {
				// heads().print(System.out);
				if (DEBUG) {
					System.out.println("applying CoNLLdependencies: ");
					if (currentDeps != null)
						currentDeps.print();
				}
				currentDeps = applyConLLDependencies(CoNLLdeps(), heads(), currentDeps);
				if (DEBUG)
					currentDeps.print();
			}
		}

		// COPY THE DEPENDENCIES (fill the conllDependencies if they are there)
		if (conllDependencies == null && cat.conllDependencies != null) {
			conllDependencies = cat.conllDependencies.copy();

			if (heads != null) {
				if (DEBUG) {
					System.out.print("### APPLY CONLL DEPENDENCIES");
					CoNLLdeps().print();
				}
				currentDeps = applyConLLDependencies(CoNLLdeps(), heads(), currentDeps);
				if (DEBUG)
					currentDeps.print();
			}
		} // IF THERE ARE MULTIPLE DEPENDENCIES: APPEND, THEN APPLY
		else {
			if (conllDependencies != null && cat.conllDependencies != null) {
				if (extracted) {// the multiple conllDependencies are extracted!!!
					// cat.conllDependencies.setToExtracted();
					if (bounded && cat.bounded) {
						cat.conllDependencies.setToExtractedBounded();
					} else {
						cat.conllDependencies.setToExtractedUnbounded();
					}
				}

				this.appendCoNLLDeps(cat.conllDependencies);

				if (heads != null) {
					if (DEBUG) {
						System.out.print("### APPLY CONLL DEPENDENCIES");
						CoNLLdeps().print();
					}
					currentDeps = applyConLLDependencies(CoNLLdeps(), heads(), currentDeps);
					if (DEBUG)
						currentDeps.print();
				}
			}
		}
		return currentDeps;
	}

	/**
	 * recursive replacement of all the parts of cat1 with the corresponding parts
	 * of cat2
	 */
	private void replace(CCGcat cat1, CCGcat unifiedCat1) {
		if (cat1 != null && unifiedCat1 != null) {
			if (cat1.argument != null) {
				replace(cat1.argument, unifiedCat1.argument);
			}
			if (cat1.result != null) {
				replace(cat1.result, unifiedCat1.result);
			}
			replace(cat1.id, cat1.headId, unifiedCat1);// recursion in replace
		}
	}

	/**
	 * REPLACING catId with newCat and headID with newCat.head or deps
	 */
	private void replace(int catId, int headID, CCGcat newCat) {

		/*
		 * If the catId matches the old catID: - replace old catId with new catId -
		 * replace old headId with the new headId - replace cat string with
		 * newCat.catString if that ONE has features - copy the heads across from
		 * newCat if newCat provides the head. COPY the ccgDependencies if newCat
		 * provides the ccgDependencies, else set them to null (debug?) If the
		 * headId matches the old headId: - replace old headId with the new headId -
		 * copy the heads across from newCat if newCat provides the head. ADD the
		 * ccgDependencies from newCat if the categories match (and otherwise???)
		 */
		// Case 1: it could be the category itself!
		if (this.id == catId) {// replace: the category id matches the ONE to be
			// replaced

			this.id = newCat.id;// replace old by new
			this.headId = newCat.headId;// replace old by new

			if (!hasFeatures() && newCat.hasFeatures()) {
				catString = newCat.catString;
			}

			if (newCat.heads != null) {
				this.heads = newCat.heads.copy();
			}

			if (Configuration.CCGcat_CCG) {
				if (newCat.ccgDependencies != null) {
					this.ccgDependencies = newCat.ccgDependencies.copy();
				} else
					ccgDependencies = null;
			}

			if (Configuration.CCGcat_CoNLL) {
				if (newCat.conllDependencies != null) {
					this.conllDependencies = newCat.conllDependencies.copy();
				} else
					conllDependencies = null;
			}

			if (newCat.extracted) {
				if (newCat.bounded)
					this.setToExtractedBounded();
				else
					this.setToExtractedUnbounded();
			}
		} else {
			// If the categories have the same headId, but aren't the same category:
			// then instantiate the head if newCat's head is instantiated:

			if (this.headId == headID // replace: (old) head ids are the same
					&& this.id != newCat.id) {// replace: heads same, but cats not
				// copy the head across
				if (newCat.heads != null && this.heads == null) {
					this.heads = newCat.heads.copy();
				}
				headId = newCat.headId; // replace old headId with new

				// add the ccgDependencies, but don't change the 'extracted' feature on
				// these cate here.
				if (this.matches(newCat.catString)) {
					if (Configuration.CCGcat_CCG)
						appendCCGDeps(newCat.ccgDependencies);
					if (Configuration.CCGcat_CoNLL)
						appendCoNLLDeps(newCat.conllDependencies);
				}
			}

			// recursion on argument:
			if (argument != null) {
				argument.replace(catId, headID, newCat);// replace: recursion on
				// argument
			}
			// recursion on result:
			if (result != null) {
				result.replace(catId, headID, newCat);// replace: recursion on result
			}
		}
	}

	/**
	 * A testsuite for different kinds of lexical categories.
	 */
	public static void testsuite() {
		System.out.println("========================================");
		System.out.println("   TEST SUITE FOR LEXICAL CATEGORIES   ");
		System.out.println("========================================\n");

		// CONJSTYLE = ConjStyle.X1_CC___CC_X2; // crashes in substitution
		// (coordinate)
		// CONJSTYLE = ConjStyle.CC_X1___CC_X2;// correct, except for complex
		// coordinations (not worth fixing for now)
		// CONJSTYLE = ConjStyle.X1_CC___X1_X2;// also crashes in substitution
		// (coordinate)
		Configuration.CONJSTYLE = ConjStyle.X1_X2___X2_CC;// also crashes in
		// substitution
		// (coordinate)
		// B
		CCGcat a = lexCat("eat", "S/N");
		// a.printCat();
		CCGcat b = lexCat("very", "(N/N)/(N/N)");
		// b.printCat();
		System.out.println("\"eat very\" compose S/N (N/N)/(N/N) => (S/N)/(N/N)");
		CCGcat c = compose(a, b);
		c.printCat();
		CCGcat d = lexCat("spicy", "N/N");
		CCGcat e = lexCat("food", "N");
		// DEBUG = true;

		CCGcat f = apply(c, d);
		System.out.print("\" eat very spicy\" apply (S/N)/(N/N) N/N => S/N: ");
		f.printFilledCoNLLDeps();
		f.printCat();
		// System.exit(0);
		CCGcat g = apply(f, e);
		System.out.print("\" eat very spicy food\" apply S/N N => S: ");
		g.printFilledCoNLLDeps();
		// g.printCat();

		// C
		CCGcat conj = lexCat("but", "conj");
		// conj.printCat();
		CCGcat a2 = lexCat("cook", "S/N");
		// a2.printCat();
		CCGcat b2 = lexCat("rather", "(N/N)/(N/N)");
		// b2.printCat();
		CCGcat c2 = compose(a2, b2);
		// c2.printCat();

		// FW_CONJOIN
		CCGcat CCright = conjunction(c2, conj);
		System.out.print("@@@ (S/N)/(N/N)[conj] (but cook rather): ");
		CCright.printFilledCoNLLDeps();
		// CCright.printCat();
		// BW_CONJOIN
		System.out.println("@@@ \"eat very  but cook rather\" :");
		// DEBUG = true;
		CCGcat out = coordinate(c, CCright);

		System.out.print("(S/N)/(N/N) -> (S/N)/(N/N) (S/N)/(N/N)[conj] (eat very  but cook rather): ");
		out.printFilledCoNLLDeps();
		out.printCat();
		CCGcat x = apply(out, d);
		System.out.print("@@@ eat very but cook rather spicy: ");
		x.printFilledCoNLLDeps();
		x.printCat();
		// DEBUG = false;
		CCGcat y = apply(x, e);
		System.out.print("@@@ eat very but cook rather spicy food: ");
		y.printFilledCoNLLDeps();
		y.printCat();

		// System.exit(0);

		//
		// CCGcat a = lexCat("VBZ", "(S/N)/N");
		// CCGcat b = lexCat("VBN", "S\\(S/N)");
		// CCGcat c = compose(b,a);
		// c.printCat();
		// c.printFilledDeps();
		// CCGcat d = lexCat("RB", "S/S");
		// CCGcat e = compose(d, c);
		// e.printCat();
		// e.printFilledDeps();
		// CCGcat f = lexCat("X", "(S\\N)/(S/N)");
		// CCGcat g = apply(f, e);
		// g.printCat();
		// g.printFilledDeps();
		// System.exit(0);

		CCGcat likes = lexCat("likes", "(S[dcl]\\NP)/(S[ng]\\NP)");

		CCGcat sleeps = lexCat("sleeps", "S[dcl]\\NP");
		CCGcat sleeping = lexCat("sleeping", "S[ng]\\NP");
		CCGcat dreaming = lexCat("dreaming", "S[ng]\\NP");
		// sleeps.printCat();
		CCGcat gives = lexCat("gives", "((S[dcl]\\NP)/NP)/NP");
		CCGcat writes = lexCat("writes", "(S[dcl]\\NP)/NP");
		CCGcat reading = lexCat("reading", "(S[ng]\\NP)/NP");
		CCGcat reads = lexCat("reads", "(S[dcl]\\NP)/NP");
		// gives.printCat();
		CCGcat john = lexCat("John", NP);
		CCGcat john2 = john.typeRaise(S, FW);
		CCGcat mary = lexCat("Mary", NP);
		CCGcat mary2 = mary.typeRaise("(S\\NP)/NP", BW);
		CCGcat mary3 = mary.typeRaise(S, FW);
		CCGcat sue = lexCat("Sue", NP);
		CCGcat sue2 = sue.typeRaise("(S\\NP)/NP", BW);
		CCGcat flowers = lexCat("flowers", NP);
		CCGcat flowers2 = flowers.typeRaise(VP, BW);
		CCGcat books = lexCat("books", NP);
		CCGcat books2 = books.typeRaise(VP, BW);

		// System.out.println();
		CCGcat well = lexCat("well", "(S\\NP)\\(S\\NP)");
		// System.out.println(well.print());
		CCGcat very1 = lexCat("very", "(N/N)/(N/N)");
		CCGcat very = lexCat("very", "((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP))");
		// System.out.println(very1.print());
		// System.out.println(very.print());
		// System.exit(0);
		CCGcat sleepswell = apply(well, sleeps);
		// sleepswell.printGeneratedDependencies(well.heads);

		CCGcat fast = lexCat("fast", "(S\\NP)\\(S\\NP)");
		CCGcat happily = lexCat("happily", "(S\\NP)\\(S\\NP)");

		CCGcat on = lexCat("on", "(NP\\NP)/NP");
		CCGcat without = lexCat("without", "((S\\NP)\\(S\\NP))/(S[ng]\\NP)");

		// Configuration.CCGcat_CoNLL = true;
		CCGcat and = lexCat("and", "conj");

		System.out.println("conjunction (FW_CONJOIN) - conjunction and 2nd conjunct");
		CCGcat andMary = conjunction(mary, and);
		andMary.printCat();
		System.out.println("coordinate (BW_CONJOIN) - 1st conjunct and conjunction");
		CCGcat johnmary = coordinate(andMary, john);
		johnmary.printCat();
		// System.exit(0);
		System.out.println("STANDARD FUNCTION APPLICATION");
		System.out.println("=============================");
		System.out.println("apply(sleeps:" + sleeps.catString + ", john:" + john.catString + ")");
		CCGcat johnsleeps = apply(sleeps, john);
		johnsleeps.printCat();
		System.out.println();
		System.out.println("FUNCTION APPLICATION WITH TYPE-RAISED SUBJECT");
		System.out.println("=============================================");
		System.out.println("apply(john:" + john2.catString + ", sleeps:" + sleeps.catString + ")");
		CCGcat john2sleeps = apply(john2, sleeps);
		john2sleeps.printCat();
		System.out.println();
		System.out.println("FUNCTION APPLICATION WITH NP-ADJUNCT");
		System.out.println("=====================================");
		System.out.println("apply(on:" + on.catString + ",  flowers:NP)");
		CCGcat onflowers = apply(on, flowers);
		onflowers.printCat();
		System.out.println("apply(onFlowers:NP\\NP, books:NP)");
		CCGcat booksOnFlowers = apply(onflowers, books);
		booksOnFlowers.printCat();

		System.out.println("apply(reads:" + reads.catString + ", booksOnFlowers:"
				+ booksOnFlowers.catString + ")");
		CCGcat readsBooksOnFlowers = apply(reads, booksOnFlowers);
		readsBooksOnFlowers.printCat();

		System.out.println();
		System.out.println("FUNCTION APPLICATION WITH VP-ADJUNCT");
		System.out.println("=============================");
		// System.out.println("apply(well:"+ well.catString
		// +", sleeps:"+sleeps.catString+")");
		CCGcat sleepsWell = apply(well, sleeps);
		sleepsWell.printCat();
		System.out.println("apply(sleepsWell:" + sleepsWell.catString + ", john:" + john.catString
				+ ")");
		CCGcat johnsleepsWell = apply(sleepsWell, john);
		johnsleepsWell.printCat();
		System.out.println("apply(very:" + very.catString + ", well:" + well.catString + ")");
		CCGcat veryWell = apply(very, well);
		veryWell.printCat();

		System.out.println("apply(veryWell:" + veryWell.catString + ", sleeps:" + sleeps.catString
				+ ")");
		CCGcat sleepsVeryWell = apply(veryWell, sleeps);
		sleepsVeryWell.printCat();

		System.out.println("apply(sleepsVeryWell:" + sleepsVeryWell.catString + ", john:"
				+ john.catString + ")");
		CCGcat johnsleepsVeryWell = apply(sleepsVeryWell, john);
		johnsleepsVeryWell.printCat();
		System.out.println("compose(veryWell:" + veryWell.catString + ", reads:" + reads.catString);

		CCGcat readsVeryWell = compose(veryWell, reads);
		readsVeryWell.printCat();

		// flowers2.printCat();
		System.out.println();
		System.out.println("COMPOSITION OF ARGUMENTS");
		System.out.println("========================");
		System.out.println("compose(flowers:" + flowers2.catString + ", mary:" + mary2.catString + ")");

		CCGcat maryflowers = compose(flowers2, mary2);
		maryflowers.printCat();

		// System.out.println();
		System.out.println("apply(maryflowers:" + maryflowers.catString + ", gives:" + gives.catString
				+ ")");
		CCGcat givesmaryflowers = apply(maryflowers, gives);
		givesmaryflowers.printCat();

		System.out.println("apply(john:" + john2.catString + ", givesmaryflowers:"
				+ givesmaryflowers.catString + ")");
		CCGcat john2givesmaryflowers = apply(john2, givesmaryflowers);
		john2givesmaryflowers.printCat();
		System.out.println();
		System.out.println("COMPOSITION WITH VP-ADJUNCT");
		System.out.println("============================");
		CCGcat with = lexCat("with", "((S\\NP)\\(S\\NP))/NP");
		// System.out.println();
		System.out.println("apply(with:" + with.catString + ", john:" + john.catString + ")");
		CCGcat withJohn = apply(with, john);
		// System.out.println();
		System.out.println("compose(withJohn:" + withJohn.catString + ", reading:" + reading.catString
				+ ")");
		CCGcat readingwithJohn = compose(withJohn, reading);
		readingwithJohn.printCat();
		System.out.println();

		System.out.println("VP COORDINATION");
		System.out.println("===============");
		System.out.println("coordinate(writes" + writes.catString + ", reads" + reads.catString + ")");
		CCGcat writesAndreads = coordinate(writes, reads);
		writesAndreads.printCat();

		// System.out.println();
		System.out.println("apply(writesAndreads:" + writesAndreads.catString + ", books:"
				+ books.catString + ")");
		CCGcat writesAndreadsbooks = apply(writesAndreads, books);
		writesAndreadsbooks.printCat();
		System.out.println("compose(john:" + john2.catString + ", writesandreads:"
				+ writesAndreads.catString + ")");

		CCGcat johnwritesandreads = compose(john2, writesAndreads);
		johnwritesandreads.printCat();

		// System.out.println();
		System.out.println("apply(johnWritesAndReads:" + johnwritesandreads.catString + ", books:"
				+ books.catString + ")");

		CCGcat johnwritesandreadsbooks = apply(johnwritesandreads, books);
		johnwritesandreadsbooks.printCat();
		System.out.println();
		System.out.println("ARGUMENT CLUSTER COORDINATION");
		System.out.println("=============================");
		System.out.println("compose(books:" + books2.catString + ", sue:" + sue2.catString + ")");

		CCGcat suebooks = compose(books2, sue2);
		System.out.println("coordinate(SueBooks, MaryFlowers)");
		CCGcat suebooksandmaryflowers = coordinate(suebooks, maryflowers);
		suebooksandmaryflowers.printCat();
		System.out.println("apply(SueBooksAndMaryFlowers:" + suebooksandmaryflowers.catString
				+ ", gives:" + gives.catString + ")");
		CCGcat givessuebooksandmaryflowers = apply(suebooksandmaryflowers, gives);
		givessuebooksandmaryflowers.printCat();
		System.out.println();
		System.out.println("COORDINATION OF ADJUNCTS");
		System.out.println("=========================");
		System.out.println("coordinate(well:" + well.catString + ", fast:" + fast.catString + ")");
		CCGcat wellandFast = coordinate(well, fast);
		// wellandFast.printCat();
		System.out.println("compose(wellAndFast:" + wellandFast.catString + ", writes:"
				+ writes.catString + ")");
		CCGcat writeswellandFast = compose(wellandFast, writes);
		writeswellandFast.printCat();
		System.out.println();
		System.out.println("NP COORDINATION");
		System.out.println("===============");
		System.out.println("coordinate(john:NP, mary:NP)");
		CCGcat johnmary2 = coordinate(john, mary);
		johnmary2.printCat();
		System.out.println("apply(writesAndReadsBooks:" + writesAndreadsbooks.catString
				+ ", johnandmary:" + johnmary.catString + ")");
		// writesAndreadsbooks.printCat();
		CCGcat johnmarywritesandreadsbooks = apply(writesAndreadsbooks, johnmary);
		johnmarywritesandreadsbooks.printCat();
		System.out.println();
		System.out.println("ARGUMENT PASSING: VP-ADJUNCTS");
		System.out.println("============================");

		System.out.println("apply(without:" + without.catString + ", dreaming:" + dreaming.catString
				+ ")");
		CCGcat withoutdreaming = apply(without, dreaming);
		withoutdreaming.printCat();

		System.out.println("apply(withoutDreaming:" + withoutdreaming.catString + ", sleeps:"
				+ sleeps.catString + ")");
		CCGcat sleepsWithoutDreaming = apply(withoutdreaming, sleeps);
		sleepsWithoutDreaming.printCat();
		System.out.println("apply(sleepsWithoutDreaming:" + sleepsWithoutDreaming.catString + ", mary:"
				+ mary.catString + ")");
		CCGcat marySleepsWithoutDreaming = apply(sleepsWithoutDreaming, mary);
		marySleepsWithoutDreaming.printCat();

		System.out.println();
		System.out.println("PARASITIC GAPS: SUBSTITUTION");
		System.out.println("============================");
		System.out.println("compose(without:" + without.catString + ",  reading:" + reading.catString
				+ ")");

		CCGcat withoutreading = compose(without, reading);
		withoutreading.printCat();

		System.out.println("substitute(without_reading:" + withoutreading.catString + ", writes:"
				+ writes.catString + ")");

		CCGcat writeswithoutreading = substitute(withoutreading, writes);
		writeswithoutreading.printCat();

		System.out.println("apply(writesWithoutreading:" + writeswithoutreading.catString + ", books:"
				+ books.catString + ")");
		CCGcat writeswithoutreadingbooks = apply(writeswithoutreading, books);
		writeswithoutreadingbooks.printCat();
		System.out.println("apply(writesWithoutreadingbooks:" + writeswithoutreading.catString
				+ ", mary:" + mary.catString + ")");
		CCGcat marywriteswithoutreadingbooks = apply(writeswithoutreadingbooks, mary);
		marywriteswithoutreadingbooks.printCat();

		System.out.println();
		System.out.println("RELATIVE PRONOUNS -- OBJECT EXTRACTION");
		System.out.println("======================================");

		CCGcat which = lexCat("which", "(NP\\NP)/(S/NP)");
		System.out.println("Lexical entry for \"which\":");
		which.printCat();
		CCGcat johnwrites = compose(john2, writes);
		System.out.println("The category for \"John writes\":");
		johnwrites.printCat();
		CCGcat whichjohnwrites = apply(which, johnwrites);
		System.out.println("The category for \"which John writes\":");
		whichjohnwrites.printCat();
		// System.out.println();
		System.out.println("apply(which:" + which.catString + ", johnwritesandreads:"
				+ johnwritesandreads.catString + ")");
		CCGcat whichJohnWritesAndReads = apply(which, johnwritesandreads);
		whichJohnWritesAndReads.printCat();

		System.out.println();
		System.out.println("apply(whichJohnWritesAndReads:" + whichJohnWritesAndReads.catString
				+ ", books:" + books.catString + ")");
		CCGcat booksWhichJohnWritesAndReads = apply(whichJohnWritesAndReads, books);
		booksWhichJohnWritesAndReads.printCat();
		System.out.println("apply(reads:" + reads.catString + ", booksWhichJohnWritesAndReads:"
				+ booksWhichJohnWritesAndReads.catString + ")");

		CCGcat readsbooksWhichJohnWritesAndReads = apply(reads, booksWhichJohnWritesAndReads);
		readsbooksWhichJohnWritesAndReads.printCat();
		System.out.println();
		System.out.println("RELATIVE PRONOUNS & SBJ CONTROL       ");
		System.out.println("======================================");

		// likes is recognised as adjunct category, which is wrong!

		System.out.println("compose(likes:" + likes.catString + ", reading:" + reading.catString + ")");
		CCGcat likesreading = compose(likes, reading);
		likesreading.printCat();
		System.out.println("compose(mary:" + mary3.catString + ", likesReading:"
				+ likesreading.catString + ")");
		CCGcat marylikesreading = compose(mary3, likesreading);
		marylikesreading.printCat();
		System.out.println("apply(which:" + which.catString + ", MarylikesReading:"
				+ marylikesreading.catString + ")");
		CCGcat whichmarylikesreading = apply(which, marylikesreading);
		whichmarylikesreading.printCat();
		System.out.println("apply(whichMaryLikesReading:" + whichmarylikesreading.catString + ", books"
				+ books.catString + ")");
		CCGcat bookswhichmarylikesreading = apply(whichmarylikesreading, books);
		bookswhichmarylikesreading.printCat();
		System.out.println("apply(writes:" + writes.catString + ", booksWhichMaryLikesReading:"
				+ bookswhichmarylikesreading.catString + ")");
		CCGcat writesbookswhichmarylikesreading = apply(writes, bookswhichmarylikesreading);
		writesbookswhichmarylikesreading.printCat();
		System.out.println("compose(well, fast):");
		CCGcat wellfast = compose(well, fast);
		// System.out.println("EXIT compose(well, fast):");

		wellfast.printCat();
		System.out.println("compose(wellfast, happily):");
		CCGcat wellfasthappily = compose(wellfast, happily);
		// System.out.println("EXIT compose(wellfast, happily):");
		wellfasthappily.printCat();
		System.out.println("compose(wellfast, writes)");
		CCGcat writeswellfast = compose(wellfast, writes);
		// System.out.println("EXIT compose(wellfast, writes)");
		writeswellfast.printCat();
		System.out.println("compose(wellfasthappily, writes)");
		CCGcat writeswellfasthappily = compose(wellfasthappily, writes);
		// System.out.println("EXIT compose(wellfasthappily, writes)");
		writeswellfasthappily.printCat();
		System.out.println("apply(wellfasthappily, sleeping)");
		CCGcat sleepingwellfasthappily = apply(wellfasthappily, sleeping);
		// System.out.println("EXIT apply(wellfasthappily, sleeping)");
		sleepingwellfasthappily.printCat();
		System.out.println("apply(likes, sleepingwellfasthappily)");
		CCGcat likessleepingwellfasthappily = apply(likes, sleepingwellfasthappily);
		// System.out.println("EXIT apply(likes, sleepingwellfasthappily)");
		likessleepingwellfasthappily.printCat();
		System.out.println("apply(likessleepingwellfasthappily, john)");
		CCGcat johnlikessleeping2 = apply(likessleepingwellfasthappily, john);
		// System.out.println("EXIT apply(likessleepingwellfasthappily, john)");
		johnlikessleeping2.printCat();

		CCGcat likessleeping = apply(likes, sleeping);
		likessleeping.printCat();

		// System.out.println("EXIT apply(likes, sleeping)");
		CCGcat johnlikessleeping = apply(likessleeping, john);
		johnlikessleeping.printCat();

		CCGcat veryN = lexCat("very", "(N/N)/(N/N)");
		// veryN.printCat();
		CCGcat big = lexCat("BIG", "N/N");
		// BIG.printCat();
		CCGcat red = lexCat("red", "N/N");
		// red.printCat();
		CCGcat book = lexCat("book", NOUN);
		System.out.println();
		System.out.println("COMPOSITION OF N-MODIFIERS:");
		System.out.println("===========================");
		System.out.println("compose(BIG, red):");
		CCGcat bigred = compose(big, red);
		bigred.printCat();
		System.out.println("apply(very, bigred):");
		CCGcat verybigred = apply(veryN, bigred);
		verybigred.printCat();
		System.out.println("apply(verybigred, book):");
		CCGcat verybigredbook = apply(verybigred, book);
		verybigredbook.printCat();

		System.out.println();
		System.out.println("COMPOSITION OF VP-MODIFIERS:");
		System.out.println("===========================");
		System.out.println("compose(well, fast):");
		CCGcat wellfastB = compose(well, fast);
		wellfastB.printCat();
		System.out.println("apply(very, wellfast):");
		CCGcat verywellfast = apply(very, wellfastB);
		verywellfast.printCat();
		System.out.println("apply(verywellfast, sleep):");
		CCGcat sleepverywellfast = apply(verywellfast, sleeps);
		sleepverywellfast.printCat();

	}

	private static void readLexicon(String lexFile) {
		BufferedReader reader = null;
		String entryString = null;
		try {
			reader = new BufferedReader(new FileReader(lexFile));
		} catch (Exception E) {
			E.printStackTrace();
		}
		try {
			entryString = reader.readLine();
		} catch (Exception E) {
			E.printStackTrace();
		}
		while (entryString != null) {
			addLexEntry(entryString);
			resetCounters();
			try {
				entryString = reader.readLine();
			} catch (Exception E) {
				E.printStackTrace();
			}
		}
	}

	/** just like StatCCGModel.addLexEntry */
	private static void addLexEntry(String entryString) {
		StringTokenizer tokenizer = new StringTokenizer(entryString);
		String word = null;
		String cat = null;
		// HashMap lexicon = new HashMap();
		// System.err.println("ENTRY" + entryString);

		if (tokenizer.hasMoreTokens()) {
			word = tokenizer.nextToken();
		}
		if (tokenizer.hasMoreTokens()) {
			cat = tokenizer.nextToken();
		}
		Double prob = null;
		if (tokenizer.hasMoreTokens()) {
			prob = new Double(tokenizer.nextToken());
			if (prob.doubleValue() > 1) {
				// System.err.println("lexical probability: " + prob);
			}
		}
		CCGcat newCat = lexCat(word, cat);
		if (DEBUG && !newCat.isAtomic())
			System.out.println("@@@ " + word + " " + newCat.indexedCatString() + " " + prob.intValue());
	}

	public String returnHTML() {
		if (result != null) {
			StringBuffer buffer = new StringBuffer();
			CCGcat tmp = this;
			// System.err.println("returnHTML: thisCat -- " + this.catString());
			while (tmp.result != null) {
				// System.err.println("tmp.result: -- " + tmp.result.catString()); !=
				// thisd)

				buffer.append('(');
				tmp = tmp.result;
			}
			buffer.append(tmp.catString());
			while (tmp != this) {

				tmp = tmp.function();
				// System.err.println("return HTML: " + tmp.catString());
				if (tmp.argument != null) {
					if (tmp.argDir == FW)
						buffer.append('/');
					else if (tmp.argDir == BW)
						buffer.append('\\');

					String argString = tmp.argument.createHTML();
					buffer.append(argString);
					buffer.append(')');
				}
			}
			return buffer.toString();
		} else {// System.err.println("result is null: " + catString());
			return catString();
		}
	}

	/**
	 * returns html version of the current category as the ith argument. Not
	 * recursive.
	 */
	private String createHTML() { // TODO: Ignores conll
		int arg = -1;
		if (ccgDependencies != null)
			arg = ccgDependencies.argPos;

		StringBuffer htmlBuffer = new StringBuffer();

		switch (arg) {
		case -1:
			break;
		case 0:
			break;
		case 1:
			htmlBuffer.append("<font color=\"firebrick\">");
			break;
		case 2:
			htmlBuffer.append("<font color=\"mediumblue\">");
			break;
		case 3:
			htmlBuffer.append("<font color=\"green\">");
			break;
		case 4:
			htmlBuffer.append("<font color=\"orchid\">");
			break;
		default:
			htmlBuffer.append("<font color=\"slategray\">");
		}
		if (this.result != null) {
			htmlBuffer.append('(');
		}
		htmlBuffer.append(this.catString());
		if (this.result != null) {
			htmlBuffer.append(')');
		}
		if (arg >= 1)
			htmlBuffer.append("</font>");
		return htmlBuffer.toString();
	}

	// #####################################################

	public static void main(String[] args) throws IOException {
		// Configuration.CCGcat_CCG = false;
		Configuration.CCGcat_CCG = true;
		Configuration.CCGcat_CoNLL = true;
		if (args.length > 0) {
			String lexFile = args[0];
			DEBUG = true;
			readLexicon(lexFile);
		} else {
			// testsuite();
			testsuiteConj();
		}
	}

	public static void testsuiteConj() {
		for (ConjStyle c : ConjStyle.values()) {
			Configuration.CONJSTYLE = c;
			CCGcat sleep = lexCat("sleep", "S[dcl]\\NP");
			CCGcat john = lexCat("John", NP);

			// CCGcat john2 = john.typeRaise(S, FW);
			CCGcat mary = lexCat("Mary", NP);

			CCGcat and = lexCat("and", "conj");
			CCGcat writes = lexCat("writes", "(S\\NP)/NP");
			CCGcat reads = lexCat("reads", "(S\\NP)/NP");
			CCGcat books = lexCat("books", NP);
			System.out
			.println("================================================================================");
			System.out.println("   TEST SUITE FOR COORDINATION: " + Configuration.CONJSTYLE);
			System.out
			.println("================================================================================\n");
			System.out.println("NP COORDINATION: \"John and Mary sleep\" " + Configuration.CONJSTYLE);
			// System.out.println("\"and Mary\"" );
			CCGcat andMary = conjunction(mary, and);// ... and mary
			// andMary.printCat();
			System.out.print("\t\"and Mary\": \t\t");
			andMary.printFilledCoNLLDeps();
			CCGcat johnmary = coordinate(john, andMary);
			System.out.print("\t\"John and Mary\": \t");
			johnmary.printFilledCoNLLDeps();
			// johnmary.printCat();
			CCGcat johnandmarysleep = apply(sleep, johnmary);
			System.out.print("\t\"John and Mary sleep\": \t");
			johnandmarysleep.printFilledCoNLLDeps();
			// johnandmarysleep.printCat();

			System.out.println("VP COORDINATION: \"john writes and reads books\" "
					+ Configuration.CONJSTYLE);

			CCGcat andReads = conjunction(reads, and);
			System.out.print("\t\"and reads\": \t\t\t\t");
			andReads.printFilledCoNLLDeps();
			// andReads.printCat();

			CCGcat writesAndreads = coordinate(writes, andReads);
			System.out.print("\t\"writes and reads\": \t\t\t");
			writesAndreads.printFilledCoNLLDeps();
			// writesAndreads.printCat();

			CCGcat writesAndreadsbooks = apply(writesAndreads, books);
			System.out.print("\t\"writes and reads books\": \t\t");
			writesAndreadsbooks.printFilledCoNLLDeps();
			// writesAndreadsbooks.printCat();

			CCGcat johnwritesAndreadsbooks = apply(writesAndreadsbooks, john);
			System.out.print("\t\"john writes and reads books\": \t\t");
			johnwritesAndreadsbooks.printFilledCoNLLDeps();
			// johnwritesAndreadsbooks.printCat();

			System.out.println("MODIFIERS: ADJECTIVE COORDINATION: \"little and BIG books\" "
					+ Configuration.CONJSTYLE);

			CCGcat little = lexCat("little", "NP/NP");
			CCGcat big = lexCat("BIG", "NP/NP");
			CCGcat andBig = conjunction(big, and);
			System.out.print("\t\"and BIG\": \t\t\t");
			andBig.printFilledCoNLLDeps();
			// andBig.printCat();
			CCGcat littleAndBig = coordinate(little, andBig);
			System.out.print("\t\"little and BIG\": \t\t");
			littleAndBig.printFilledCoNLLDeps();
			// littleAndBig.printCat();
			CCGcat littleAndBigBook = apply(littleAndBig, books);
			System.out.print("\t\"little and BIG books\": \t");
			littleAndBigBook.printFilledCoNLLDeps();
			// littleAndBigBook.printCat();

			System.out.println("MODIFIERS: ADVERB COORDINATION: \"sleeps/writes well and fast\" "
					+ Configuration.CONJSTYLE);
			CCGcat well = lexCat("well", "(S\\NP)\\(S\\NP)");
			// CCGcat very1 = lexCat("very", "(N/N)/(N/N)");
			CCGcat very2 = lexCat("very", "((S\\NP)\\(S\\NP))/((S\\NP)\\(S\\NP))");
			CCGcat fast = lexCat("fast", "(S\\NP)\\(S\\NP)");
			CCGcat andFast = conjunction(fast, and);
			System.out.print("\t\"and fast\": \t\t\t\t");
			andFast.printFilledCoNLLDeps();
			// andFast.printCat();

			CCGcat wellandFast = coordinate(well, andFast);
			System.out.print("\t\"well and fast\": \t\t\t");
			wellandFast.printFilledCoNLLDeps();
			// wellandFast.printCat();
			CCGcat verywellandFast = apply(very2, wellandFast);
			System.out.print("\t\"very well and fast\": \t\t\t");
			verywellandFast.printFilledCoNLLDeps();
			// verywellandFast.printCat();
			CCGcat sleepwellandFast = apply(wellandFast, sleep);
			System.out.print("\t\"sleeps well and fast\" (application): \t");
			sleepwellandFast.printFilledCoNLLDeps();
			// sleepwellandFast.printCat();
			CCGcat writeswellandFast = compose(wellandFast, writes);
			System.out.print("\t\"writes well and fast\" (composition): \t");
			writeswellandFast.printFilledCoNLLDeps();
			// writeswellandFast.printCat();

			/*
			 * System.out.println(
			 * "ARGUMENT CLUSTER COORDINATION: \" gives mary flowers and sue books\" "
			 * + CONJSTYLE); CCGcat gives = lexCat("gives", "((S\\NP)/NP)/NP"); CCGcat
			 * mary2 = mary.typeRaise("(S\\NP)/NP", BW); CCGcat sue = lexCat("Sue",
			 * NP); CCGcat sue2 = sue.typeRaise("(S\\NP)/NP", BW); CCGcat flowers =
			 * lexCat("flowers", NP); CCGcat flowers2 = flowers.typeRaise(VP, BW);
			 * CCGcat books2 = books.typeRaise(VP, BW); CCGcat suebooks =
			 * compose(books2, sue2); CCGcat maryflowers = compose(flowers2, mary2);
			 * System.out.print("\t\"mary flowers\": \t\t");
			 * maryflowers.printFilledCoNLLDeps(); //maryflowers.printCat(); CCGcat
			 * andmaryflowers = conjunction(maryflowers, and);
			 * System.out.print("\t\"and Mary Flowers\": \t\t\t");
			 * andmaryflowers.printFilledCoNLLDeps(); //andmaryflowers.printCat();
			 * CCGcat suebooksandmaryflowers = coordinate(suebooks, andmaryflowers);
			 * System.out.print("\t\"sue books and mary flowers\": \t\t");
			 * suebooksandmaryflowers.printFilledCoNLLDeps(); CCGcat
			 * givessuebooksandmaryflowers = apply(suebooksandmaryflowers, gives);
			 * System.out.print("\t\"gives sue books and mary flowers\": \t");
			 * givessuebooksandmaryflowers.printFilledCoNLLDeps();
			 * //givessuebooksandmaryflowers.printCat();
			 */
			System.out.println();

		}
	}

}

// --- END OF FILE:
// /home/julia/CCG/code/StatisticalParser/code/CurrentCVSed/StatCCG/CCGcat.java

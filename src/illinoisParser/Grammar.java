package illinoisParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines an abstract grammar which must be extended to actually create rules
 * 
 * @author bisk1
 */
public class Grammar implements Serializable {
	private static final long serialVersionUID = 5162012L;

	/*
	 * Rules
	 */
	/**
	 * Rules used 
	 */
	IntegerMapping<Rule> Rules;
	/**
	 * Cache of right-hand-sides to rules that can generate them
	 */
	ConcurrentHashMap<IntPair, ConcurrentHashMap<Rule, Boolean>> rhsToRuleMap;
	/**
	 * A map from word IDs to the IDs of the categories that can generate them
	 */
	ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Boolean>> wordToLexicalCategoryMap;

	/*
	 * Nonterminal Symbols
	 */
	/**
	 * String values for non-terminals
	 */
	public final IntegerMapping<String> Symbols;

	/**
	 * Const for TOP
	 */
	int TOP;

	/*
	 * Words
	 */
	/**
	 * Words that can be produced by the grammar or the model
	 */
	IntegerMapping<String> Words;
	/**
	 * List of in-vocabulary words
	 */
	IntegerMapping<String> learnedWords;
	/**
	 * Generic UNK constant for unknown words
	 */
	final Integer UNK;



	/**
	 * Default constructor: Initializes variables, creates initial rules, etc.
	 */
	public Grammar() {
		// Initialize data structures for mapping grammar fields to Integer IDs
		this.Words = new IntegerMapping<String>("Words", String.class);
		this.learnedWords = new IntegerMapping<String>("Known Words", String.class);
		this.Symbols = new IntegerMapping<String>("Symbols", String.class);
		this.Rules = new IntegerMapping<Rule>("Rules", Rule.class);
		this.rhsToRuleMap = new ConcurrentHashMap<IntPair, ConcurrentHashMap<Rule, Boolean>>();
		this.wordToLexicalCategoryMap = new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Boolean>>();

		// Set values for TOP and UNK
		this.TOP = getCatID("TOP");
		this.UNK = this.Words.getIDAndAddIfAbsent("#UNK#");
	}

	public void addKnownWord(String word) {
		learnedWords.getIDAndAddIfAbsent(word);
		this.getLexID(word);
	}

	/**
	 * Given a LexicalToken, returns the ID of the word (if known) or the ID of the POS tag (if not)
	 */
	public Integer getWordOrPOSID(LexicalToken lt) {
		String word = lt.getWord();
		if(learnedWords.contains(word)) {
			Integer lexID = this.checkLexID(word);
			if(lexID != null) {
				return lexID;
			}
		}
		return this.getLexID("UNK_"+lt.getPOS());
	}

	/**
	 * Given a POS tag, return the lexical ID of that POS tag
	 */
	public Integer getPOSID(POS pos) {
		return this.getLexID(pos.toString());
	}
	
	public Integer getPOSID(String pos) {
		return this.getLexID(pos);
	}

	/**
	 * Maps a lexical string (w) to an integer
	 * 
	 * @param w: word string
	 * @return 
	 */
	private final Integer getLexID(String lex) {
		return Words.getIDAndAddIfAbsent(lex);
	}

	/**
	 * Returns a word's lexical ID if it already exists in the map, else null
	 * 
	 * @param w: word string
	 * @return 
	 */
	private final Integer checkLexID(String w) {
		return Words.checkID(w);
	}

	/**
	 * Maps a word string (w) to an integer
	 * 
	 * @param w: word string
	 * @return 
	 */
	public final Integer getWordID(String word) {
		return this.getLexID(word);
	}


	/**
	 * Map non-terminals (cat) to integer (formerly called "NT")
	 * 
	 * @param cat: category symbol string
	 * @return Integer id of this category
	 */
	public final Integer getCatID(String cat) {
		Integer id = Symbols.getIDAndAddIfAbsent(cat); 
		return id;
	}

	/**
	 * Cat to string
	 * 
	 * @param cat
	 * @return String
	 */
	public String prettyCat(Integer cat) {
		return Symbols.getItemByID(cat);
	}

	/**
	 * Symbol to string
	 * 
	 * @param sym
	 * @return String
	 */
	public String getCatFromID(Integer catID) {
		return Symbols.getItemByID(catID);
	}

	/**
	 * word to String
	 * 
	 * @param cat
	 * @return String
	 */
	public final String getLexFromID(Integer wordID) {
		return Words.getItemByID(wordID);
	}

	/**
	 * Abstract: Creates a rule a -> b c w/ type,dir,arity
	 * 
	 * @param a
	 *          Parent
	 * @param b
	 *          L child
	 * @param c
	 *          R child
	 * @param type
	 *          Type
	 * @param dir
	 *          Head Direction
	 * @param rule_arity
	 *          arity
	 * @return Binary
	 * @throws Exception
	 */
	public Binary createBinaryRule(Integer a, Integer b, Integer c, Rule_Type type,
			Rule_Direction dir, int rule_arity) {
		Binary temp = new Binary(a, b, c, type, rule_arity, dir);
		return (Binary) addRuleIfAbsent(temp, new IntPair(b, c));
	}

	/**
	 * Abstract: Creates a rule a -> b w/ type
	 * 
	 * @param a
	 *          Parent
	 * @param b
	 *          Child
	 * @param type
	 *          Type
	 * @return Unary
	 * @throws Exception
	 */
	protected Unary createLexicalRule(String category, LexicalToken lt) {
		Integer catID = this.getCatID(category);
		Integer lexID = this.getWordOrPOSID(lt);
		return this.createUnaryRule(catID, lexID, Rule_Type.PRODUCTION);
	}

	/**
	 * Abstract: Creates a rule a -> b w/ type
	 * 
	 * @param a
	 *          Parent
	 * @param b
	 *          Child
	 * @param type
	 *          Type
	 * @return Unary
	 * @throws Exception
	 */
	protected Unary createUnaryRule(Integer a, Integer b, Rule_Type type) {
		Unary temp = new Unary(a, b, type);
		if(type == Rule_Type.PRODUCTION) {
			this.wordToLexicalCategoryMap.putIfAbsent(b, new ConcurrentHashMap<Integer, Boolean>());
			this.wordToLexicalCategoryMap.get(b).put(a, true);
		}
		return (Unary) addRuleIfAbsent(temp, new IntPair(b));
	}

	/**
	 * If rule is new, add to datastructures, else return instance
	 * 
	 * @param r
	 * @param BC
	 * @return Rule
	 * @throws Exception
	 */
	protected final Rule addRuleIfAbsent(Rule r, IntPair BC) {
		this.Rules.getIDAndAddIfAbsent(r);
		if(r.getType() != Rule_Type.PRODUCTION) { // we store production rules in wordToLexicalCategoryMap instead
			this.rhsToRuleMap.putIfAbsent(BC, new ConcurrentHashMap<Rule,Boolean>());
			this.rhsToRuleMap.get(BC).put(r, true);
		}
		return r;
	}

	// TODO: this is weird, but probably works (and is only called once)
	private static final Set<Rule> empty = new ConcurrentHashMap<Rule,Boolean>().keySet();

	public Set<Rule> getRules(int b, int c) {
		return getRules(new IntPair(b, c));
	}

	public Set<Rule> getRules(IntPair BC) {
		ConcurrentHashMap<Rule, Boolean> ruleCache = this.rhsToRuleMap.get(BC);
		if(ruleCache != null) {
			return ruleCache.keySet();
		}
		return empty;
	}

	protected static void addLexTree(CoarseChartItem b, Tree L) {
		if (b.topK == null) {
			b.topK = new Tree[0];
		}
		for (Tree T : b.topK) {
			if (T.equals(L)) {
				return;
			}
		}
		b.topK = new Tree[] { L };
	}

	public boolean isCoordination(Rule r) {
		return Rule_Type.isCoordination(r.getType());
	}

	/**
	 * Performs the parsing action in Cell A of combining categories from B and C
	 * if possible
	 * 
	 * @param A
	 * @param b
	 * @param B
	 * @param c
	 * @param C
	 * @param Test
	 * @throws Exception
	 */
	void combine(Cell A, int b, Collection<CoarseChartItem> B, int c, Collection<CoarseChartItem> C) throws Exception {
		// TODO: move this functionality to Chart or Cell? Should the grammar modify the cell, or just return a set of rules?
		if (B == null || C == null) {
			return;
		}
		Binary r;
		for (Rule rule : getRules(b,c)) {
			r = (Binary) rule;
			for (CoarseChartItem b_cat : B) {
				for (CoarseChartItem c_cat : C) {
					if (NF.binaryNF(r.Type, r.arity, b_cat.type(), b_cat.arity(),
							c_cat.type(), c_cat.arity())) {
						A.addCat(r, b_cat, c_cat);
					}
				}
			}
		}
	}

	private ConcurrentHashMap<Integer, Set<Integer>> leftCatToRightCatMap;

	public void buildLeftToRightMap() {
		leftCatToRightCatMap = new ConcurrentHashMap<Integer, Set<Integer>>();
		for (Rule r : Rules.items()) {
			if (r instanceof Binary) {
				leftCatToRightCatMap.putIfAbsent(r.B, new HashSet<Integer>());
				leftCatToRightCatMap.get(r.B).add(((Binary) r).C);
			}
		}
	}

	public Set<Integer> rightCats(Integer leftCat) {
		return leftCatToRightCatMap.get(leftCat);
	}

	/**
	 * Prints the grammar (one rule per line) to a file. 
	 * @param file_name
	 *  Target file
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	void print(String file_name) throws UnsupportedEncodingException,
	FileNotFoundException, IOException {
		Util.timestamp("Printing rules in Grammar");
		BufferedWriter writer = Util.TextFileWriter(Configuration.Folder + "/"
				+ file_name);
		for (IntPair BC : this.rhsToRuleMap.keySet()) {
			for (Rule r : this.rhsToRuleMap.get(BC).keySet()) {
				writer.write(r.toString(this));
				writer.write("\n");
			}
		}
		writer.close();
	}

	public void printGrammar(PrintWriter ps) {
		ps.println("Words: " + Words.size());
		for (String w : Words.items()) {
			ps.println("  " + w);
		}

		ps.println("------------");
		ps.println("CCG Categories: " + Symbols.size());
		for (String c : Symbols.items()) {
			ps.println("  " + c);
		}

		ps.println("------------");
		ps.println("Rules: " + Rules.size());
		for (Rule r : this.Rules.items()){
			ps.println("  " + r.toString(this));
		}
	}

	public Collection<Integer> getLexicalCategories(Integer lexID) {
		ConcurrentHashMap<Integer, Boolean> map =  this.wordToLexicalCategoryMap.get(lexID);
		if( map == null) {
			return new ArrayList<Integer>();
		}
		return map.keySet();
	}

	public Integer getTypeID(String type) {
		return this.getLexID(type);
	}

	public String prettyRule(Rule r) {
		if(r.getType() == Rule_Type.PRODUCTION) {
			return getCatFromID(r.A)+"  -->  "+getLexFromID(r.B)+"    (PRODUCTION)";
		}
		if(r instanceof Unary) {
			return getCatFromID(r.A)+"  -->  "+getCatFromID(r.B)+"    ("+r.getType()+")";
		}
		else {
			return getCatFromID(r.A)+"  -->  "+getCatFromID(r.B)+"  "+getCatFromID(((Binary)r).C)+"    ("+r.getType()+")";
		}
	}

	/**
	 * Saves this grammar's words, categories, and rules to disk.
	 * 
	 * @param saveDir the location of the directory containing the saved files
	 */
	public void save(File saveDir) {
		try {
			// Write words to file
			File wordFile = new File(saveDir.getCanonicalPath()+"/words.txt");
			PrintWriter wpw = new PrintWriter(wordFile);
			for(String word : this.Words.items()) {
				wpw.println(this.Words.checkID(word)+" "+word);
			}
			wpw.close();
			// Write known word IDs to file
			File knownWordFile = new File(saveDir.getCanonicalPath()+"/known_words.txt");
			PrintWriter kwpw = new PrintWriter(knownWordFile);
			for(String knownWord : this.learnedWords.items()) {
				// Note: just save ID of known word in Words list
				kwpw.println(this.Words.checkID(knownWord));
			}
			kwpw.close();
			// Write categories to file
			File categoryFile = new File(saveDir.getCanonicalPath()+"/categories.txt");
			PrintWriter cpw = new PrintWriter(categoryFile);
			for(String category : this.Symbols.items()) {
				cpw.println(this.Symbols.checkID(category)+" "+category);
			}
			cpw.close();
			// Write rules to file
			File ruleFile = new File(saveDir.getCanonicalPath()+"/rules.txt");
			PrintWriter rpw = new PrintWriter(ruleFile);
			for(Rule rule : this.Rules.items()) {
				String ruleLine = this.Rules.checkID(rule)+" "+rule.N+" "+rule.getType()+" "+rule.A+" "+rule.B;
				if(rule instanceof Binary) {
					Binary brule = (Binary) rule;
					ruleLine += " "+brule.C+" "+brule.arity+" "+brule.head;
				}
				rpw.println(ruleLine);
			}
			rpw.close();
		}
		catch(IOException e) {
			System.out.println("Failed to save grammar; " +
					"IOException while attempting to write to disk.\nExiting...");
			System.exit(1);
		}
	}

	/**
	 * Loads this grammar's words, categories, and rules from disk.
	 * 
	 * @param loadDir the location of the directory containing the data files
	 * @return a new Grammar object with parameters defined by the load directory 
	 */
	public static Grammar load(File loadDir) {
		try {
			// Initialize new Grammar object
			Grammar grammar = new Grammar();
			// Load words from file
			File wordFile = new File(loadDir.getCanonicalPath()+"/words.txt");
			Scanner wsc = new Scanner(wordFile);
			while(wsc.hasNextLine()) {
				String line = wsc.nextLine().trim();
				if(!line.isEmpty()) {
					int oldID = Integer.parseInt(line.split("\\s+")[0]);
					String word = line.split("\\s+")[1];
					int newID = grammar.Words.getIDAndAddIfAbsent(word);
					if(newID != oldID) {
						//TODO: throw and handle exception
						System.out.println("Warning: " +
								"mismatched IDs for word "+word+" while loading grammar.");
					}
				}
			}
			wsc.close();
			// Load known words from file
			File knownWordFile = new File(loadDir.getCanonicalPath()+"/known_words.txt");
			Scanner kwsc = new Scanner(knownWordFile);
			while(kwsc.hasNextLine()) {
				String line = kwsc.nextLine().trim();
				if(!line.isEmpty()) {
					int id = Integer.parseInt(line);
					String word = grammar.Words.getItemByID(id);
					grammar.learnedWords.getIDAndAddIfAbsent(word);
				}
			}
			kwsc.close();
			// Load categories from file
			File categoryFile = new File(loadDir.getCanonicalPath()+"/categories.txt");
			Scanner csc = new Scanner(categoryFile);
			while(csc.hasNextLine()) {
				String line = csc.nextLine().trim();
				if(!line.isEmpty()) {
					int oldID = Integer.parseInt(line.split("\\s+")[0]);
					String category = line.split("\\s+")[1];
					int newID = grammar.Symbols.getIDAndAddIfAbsent(category);
					if(newID != oldID) {
						//TODO: throw and handle exception
						System.out.println("Warning: " +
								"mismatched IDs for category "+category+" while loading grammar.");
					}
				}
			}
			csc.close();
			// Load rules from file
			File ruleFile = new File(loadDir.getCanonicalPath()+"/rules.txt");
			Scanner rsc = new Scanner(ruleFile);
			while(rsc.hasNextLine()) {
				String line = rsc.nextLine().trim();
				if(!line.isEmpty()) {
					// See save() above for format of line/toks
					String[] toks = line.split("\\s+");
					int oldID = Integer.parseInt(toks[0]);
					int N = Integer.parseInt(toks[1]);
					Rule_Type type = Rule_Type.valueOf(toks[2]);
					int A = Integer.parseInt(toks[3]);
					int B = Integer.parseInt(toks[4]);
					Rule rule = null;
					if(N == 1) {
						// Note: also adds rule to rhsToRuleMap and wordToLexCatMap
						rule = grammar.createUnaryRule(A, B, type);
					}
					else if(N == 2) {
						int C = Integer.parseInt(toks[5]);
						int arity = Integer.parseInt(toks[6]);
						Rule_Direction head = Rule_Direction.valueOf(toks[7]);
						// Note: also adds rule to rhsToRuleMap
						rule = grammar.createBinaryRule(A, B, C, type, head, arity);
					}
					int newID = grammar.Rules.getIDAndAddIfAbsent(rule);
					if(newID != oldID) {
						//TODO: throw and handle exception
						System.out.println("Warning: " +
								"mismatched IDs for rule "+oldID+" while loading grammar.");
					}
				}
			}
			rsc.close();
			grammar.buildLeftToRightMap();
			return grammar;
		}
		catch(FileNotFoundException e) {
			System.out.println("Failed to load grammar; " +
					"missing data files while attempting to load from: "
					+ loadDir.getAbsolutePath()+"\nExiting...");
			System.exit(1);
			return null;
		}
		catch(IOException e) {
			System.out.println("Failed to load grammar; " +
					"IOException while attempting to read from disk.\nExiting...");
			System.exit(1);
			return null;
		}
	}

	/**
	 * Returns a Rule object for a particular lexical category-word pair.
	 * 
	 * @param category		index of the lexical category
	 * @param realWordID	index of the word
	 * @return
	 */
	public Rule getProductionRule(int category, int realWordID) {
		return new Unary(category, realWordID, Rule_Type.PRODUCTION);
	}

	/**
	 * Returns the integer ID of the TOP category (root of a parse).
	 */
	public int getTopCatID() {
		return TOP;
	}

	/**
	 * Returns the integer ID of the specified Rule.
	 * @param rule	the Rule to look up
	 * @return	the rule's ID
	 */
	public int getRuleID(Rule rule) {
		return this.Rules.checkID(rule);
	}

	/**
	 * Removes from this grammar the Rules that belong to disallowedRules.
	 */
	public void pruneRules(HashSet<Rule> disallowedRules) {

		IntegerMapping<Rule> keptRules = new IntegerMapping<Rule>("Rules", Rule.class); 
		ConcurrentHashMap<IntPair, ConcurrentHashMap<Rule, Boolean>> keptRHSMap
		= new ConcurrentHashMap<IntPair, ConcurrentHashMap<Rule, Boolean>>();
		ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Boolean>> keptWordLexcatMap
		= new ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, Boolean>>();

		for(Rule rule : this.Rules.items()) {
			if(!disallowedRules.contains(rule)) {
				keptRules.getIDAndAddIfAbsent(rule);				
				if(rule.getType() == Rule_Type.PRODUCTION) {
					keptWordLexcatMap.putIfAbsent(rule.B, new ConcurrentHashMap<Integer, Boolean>());
					keptWordLexcatMap.get(rule.B).put(rule.A, true);
				}
				else {
					IntPair BC;
					if(rule instanceof Unary) {
						BC = new IntPair(rule.B);
					}
					else {
						BC = new IntPair(rule.B, ((Binary)rule).C);
					}
					keptRHSMap.putIfAbsent(BC, new ConcurrentHashMap<Rule,Boolean>());
					keptRHSMap.get(BC).put(rule, true);
				}
			}
		}
		this.Rules = keptRules;
		this.rhsToRuleMap = keptRHSMap;
		this.wordToLexicalCategoryMap = keptWordLexcatMap;
		this.buildLeftToRightMap();
	}

	/**
	 * Return true iff the gold parse for the sentence is licensed by this Grammar.
	 */
	public boolean licenses(Sentence sentence) {
		AutoDecoder decoder = new AutoDecoder(sentence, sentence.getCCGbankParse());
		Grammar grammar = new Grammar();
		grammar.Words = this.Words;
		grammar.learnedWords = this.learnedWords;
		Chart chart = decoder.getCoarseChart(grammar);
		return licensesRecurse(chart.coarseRoot, sentence, grammar);
		/*
		for(Rule r : grammar.Rules.items()) {
			if(!this.Rules.contains(convertRule(r, grammar))) {
				System.out.println("\nGold rule:    "+r.toString(grammar)+"\nGrammar rule: "+convertRule(r, grammar).toString(this));
				return false;
			}
		}
		return true;
		 */
	}

	/**
	 * Returns true iff the the subtree rooted at the ChartItem ci 
	 * (which uses indices defined in the Grammar other) is a parse licensed
	 * by this grammar.
	 */
	private boolean licensesRecurse(CoarseChartItem ci,
			Sentence sentence, Grammar other) {
		if(ci == null) {
			System.out.println("null ci");
		}
		int otherCatID = ci.category();
		String cat = other.getCatFromID(otherCatID);
		int thisCatID = this.Symbols.checkID(cat);
		if(thisCatID == -1) {
			System.out.println("Unlicensed: "+sentence.asWords());
			System.out.println("Missing category: "+cat);
			return false;
		}
		if(ci instanceof CoarseLexicalCategoryChartItem) {

			// If we only care about categories:
			return true; 

			// The code below checks lexical production as well; may produce false negative for rare words, dpeending on the grammar
			/*
			LexicalToken lt = sentence.get(ci.X());
			String word = lt.getWord();
			boolean ruleMatch;
			if(this.learnedWords.contains(word)) {
				int otherWordID = other.getLexID(word);
				int thisWordID = this.Words.checkID(word);
				if(thisWordID == -1) {
					System.out.println("Unlicensed: "+sentence.asWords());
					System.out.println("Missing ID for word \""+word+"\".");
					return false;
				}
				ConcurrentHashMap<Integer,Boolean> temp = this.wordToLexicalCategoryMap.get(thisWordID);
				ruleMatch = (temp != null) 
						&& temp.get(thisCatID);
				if(!ruleMatch) {
					System.out.println("Unlicensed: "+sentence.asWords());
					System.out.println("Missing lexical rule entry for: "+cat+" -> "+word);
					return false;
				}
			}
			else {
				String pos = lt.getPOS().toString();
				int otherPOSID = other.getLexID(pos);
				int thisPOSID = this.Words.checkID(pos);
				int lexID = this.getWordOrPOSID(lt);
				if(thisPOSID == -1) {
					System.out.println("Unlicensed: "+sentence.asWords());
					System.out.println("Missing ID for POS \""+pos+"\".");
					return false;
				}
				ConcurrentHashMap<Integer,Boolean> temp = this.wordToLexicalCategoryMap.get(this.getWordOrPOSID(lt));
				ruleMatch = (temp != null) 
						&& temp.get(thisCatID);
				if(!ruleMatch) {
					System.out.println("Unlicensed: "+sentence.asWords());
					System.out.println("Missing lexical rule entry for: "+cat+" -> "+this.getLexFromID(lexID));
					return false;
				}
			}
			return true;
			 */
		}
		else {
			if(ci.children.isEmpty()) {
				System.out.println("Unlicensed: "+sentence.asWords());
				System.out.println("Missing backpointer in parse.");
				return false;
			}
			BackPointer bp = ci.children.get(0);
			Rule rule = bp.r;
			if(rule instanceof Unary) {
				int A = this.getCatID(other.getCatFromID(rule.A));
				int B = this.getCatID(other.getCatFromID(rule.B));
				if(!this.Rules.contains(new Unary(A, B, rule.Type))) {
					System.out.println("Unlicensed: "+sentence.asWords());
					System.out.println("Missing rule: "+(new Unary(A, B, rule.Type)).toString(this));
				}
				return this.Rules.contains(new Unary(A, B, rule.Type))
						&& licensesRecurse(bp.B, sentence, other);
			}
			else {
				Binary br = (Binary) rule;
				int A = this.getCatID(other.getCatFromID(br.A));
				int B = this.getCatID(other.getCatFromID(br.B));
				int C = this.getCatID(other.getCatFromID(br.C));
				if(!this.Rules.contains(new Binary(A, B, C, br.getType(), br.arity, br.head))) {
					System.out.println("Unlicensed: "+sentence.asWords());
					System.out.println("Missing rule: "+(new Binary(A, B, C, br.getType(), br.arity, br.head)).toString(this));
				}
				return this.Rules.contains(new Binary(A, B, C, br.getType(), br.arity, br.head))
						&& licensesRecurse(bp.B, sentence, other)
						&& licensesRecurse(bp.C, sentence, other);
			}
		}
	}

	/**
	 * Returns the Rule object corresponding to the specified ID
	 * @param id	rule ID
	 * @return	corresponding Rule
	 */
	public Rule getRuleFromID(int id) {
		return this.Rules.getItemByID(id);
	}

	public int getNumberOfCategories() {
		return this.Symbols.size();
	}

	public int getRuleID(String aCat, String bCat, String cCat) {
		int aCatID = this.getCatID(aCat);
		IntPair BC = new IntPair(this.getCatID(bCat), this.getCatID(cCat));
		for(Rule r : this.getRules(BC)) {
			if(r.A == aCatID) {
				return this.getRuleID(r);
			}
		}
		return -1;
	}
}
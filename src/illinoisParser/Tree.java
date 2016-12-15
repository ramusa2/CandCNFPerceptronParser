package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Tree data-structure (e.g. ONE sampled from a parse chart)
 * 
 * @author bisk1
 * @param 
 */
public class Tree<CI extends FineChartItem> implements Comparable<Tree<CI>>, Externalizable {

	/**
	 * Left Child
	 */
	Tree<CI> B = null;
	/**
	 * Right Child
	 */
	Tree<CI> C = null;
	/**
	 * Rule used
	 */
	Rule rule;

	private boolean NormalForm = false;

	/**
	 * Parent category
	 */
	Integer A = -1;
	/**
	 * Rule Type
	 */
	Rule_Type type = null;
	private int arity = -1;

	/**
	 * Head Index of subtree
	 */
	int headIndex = -1;
	/**
	 * Other variables used in chart item representation for eq class. e.g. head
	 * information
	 */
	CI chartItem;
	/**
	 * Reference to child chart items
	 */
	FineBackPointer backPointer;
	/**
	 * Inside probability
	 */
	double prob;

	/**
	 * CCG Dependency Relation
	 */
	DepRel[][] ccgDepRel;
	/**
	 * CCG Cat, used to compute dependencies
	 */
	CCGcat ccgcat;

	/**
	 * Empty Tree
	 */
	public Tree() {}

	/**
	 * Unary Tree branch
	 * 
	 * @param rule_used Rule
	 * @param ci
	 * @param bp
	 * @param p Probability
	 * @param b Child
	 */
	Tree(CI ci, FineBackPointer bp, double p, Tree<CI> b) {
		rule = bp.rule();
		A = rule.A;
		type = rule.getType();
		headIndex = b.headIndex;
		chartItem = ci;
		backPointer = bp;
		prob = p;
		B = b;
		C = null;
	}

	/**
	 * Binary Tree branch
	 * 
	 * @param rule_used Rule
	 * @param ci
	 * @param bp
	 * @param p Probability
	 * @param b Left Child
	 * @param c Right Child
	 */
	Tree(CI ci, FineBackPointer bp, double p, Tree<CI> b, Tree<CI> c) {
		Binary bin = (Binary) bp.rule();
		rule = bin;
		A = rule.A;
		type = rule.getType();
		arity = bin.arity;
		chartItem = ci;
		backPointer = bp;
		if (bp.direction() == Rule_Direction.Right) {
			headIndex = c.headIndex;
		} else {
			headIndex = b.headIndex;
		}

		if (rule.getType().equals(Rule_Type.BW_CONJOIN)) {
			switch (Configuration.CONJSTYLE) {
			case CC_X1___CC_X2:
			case X2_X1___X2_CC:
				headIndex = c.headIndex;
				break;
			case X1_CC___CC_X2:
			case X1_CC___X1_X2:
			case X1_X2___X2_CC:
				headIndex = b.headIndex;
				break;
			default:
				Util.Error("Invalid option: " + Configuration.CONJSTYLE);
			}
		} else if (rule.getType().equals(Rule_Type.FW_CONJOIN)) {
			switch (Configuration.CONJSTYLE) {
			case CC_X1___CC_X2:
			case X1_CC___CC_X2:
			case X1_CC___X1_X2: // ??
				headIndex = b.headIndex;
				break;
			case X1_X2___X2_CC:
			case X2_X1___X2_CC:
				headIndex = c.headIndex;
				break;
			}
		}
		Set(p, b, c);
	}

	private void Set(double p, Tree<CI> b, Tree<CI> c) {
		prob = p;
		B = b;
		C = c;
	}

	/**
	 * Create Lexical Tree
	 * 
	 * @param a
	 *          Category
	 * @param hI
	 *          Head Index
	 * @param t
	 *          Rule Type
	 * @param ci
	 */
	Tree(Integer a, int hI, Rule_Type t, CI ci) {
		A = a;
		type = t;
		headIndex = hI;
		rule = null;
		prob = Log.ONE;
		chartItem = ci;
		backPointer = null;
	}

	public static Tree<FineChartItem> getLexicalCategoryTree(FineChartItem lexCatCI, Rule r) {
		Tree<FineChartItem> t = new Tree<FineChartItem>();
		t.A = lexCatCI.category();
		t.type = Rule_Type.PRODUCTION;
		t.headIndex = lexCatCI.headIndex();
		t.chartItem = lexCatCI;
		t.rule = r;
		t.prob = lexCatCI.getViterbiProb();
		t.backPointer = null;
		return t;
	}

	@Override
	public String toString() {
		String catstring;
		if (ccgcat != null) {
			catstring = ccgcat.catString();
		} else {
			catstring = "(null CCGcat so far)";
		}
		return prob + "\t" + catstring;
	}
	
	public String toBracketedString(Sentence sen, Grammar g) {
		if(this.B == null) {
			return "{"+g.getCatFromID(this.A)+" "+sen.asWords(this.headIndex, this.headIndex)+"}";
		}
		if(this.C == null) {
			return "{"+g.getCatFromID(this.A)+" "+this.B.toBracketedString(sen, g)+"}"; 
		}
		return "{"+g.getCatFromID(this.A)+" "+this.B.toBracketedString(sen, g)+" "+this.C.toBracketedString(sen, g)+"}"; 
	}
	
	/**
	 * Return String representation based on Model and Grammar
	 * 
	 * @param model
	 * @param depth
	 * @return String
	 */
	public String toString(Grammar g, int depth) {
		String s = "";
		for(int i=0; i<depth; i++) {
			s+=" ";
		}
		s += "( " + g.prettyCat(A) + "|" + type + " "+prob;
		if (B != null) {
			s += "\n"+B.toString(g, depth + 2);
			if (C != null) {
				s += C.toString(g, depth + 2);
			}
			for(int i=0; i<depth; i++) {
				s+=" ";
			}
		}
		s += ")\n";
		return s;
	}

	/**
	 * Return String representation based on CCGCat values
	 * 
	 * @param model
	 * @return String
	 */
	public String ccgString() {
		if (B == null) {
			return "{ " + ccgcat.catString() + "__" + ccgcat.heads().index() + " }";
		}
		String s = "{ " + ccgcat.catString() + "__" + ccgcat.heads().index();
		if (B != null) {
			s += " " + B.ccgString() + " ";
		}
		if (C != null) {
			s += " " + C.ccgString() + " ";
		}
		return s + " }";
	}



	private CCGcat getCCGbankProdCat() {
		String word = this.chartItem.coarseItem.cell.chart.getSentence().get(headIndex).getWord();
		String category = this.chartItem.coarseItem.cell.chart.getGrammar().getCatFromID(A);
		String pos = this.chartItem.coarseItem.cell.chart.getSentence().get(headIndex).getPOS().toString();
		return CCGcat.lexCat(word, category, pos, headIndex);
	}
	
	/**
	 * Set of rules used in parse
	 * 
	 * @return HashSet<Integer>
	 */
	HashSet<Rule> getUsedRules() {
		HashSet<Rule> rules = new HashSet<Rule>();
		if (rule != null) {
			rules.add(rule);
		}
		if (B != null) {
			rules.addAll(B.getUsedRules());
		}
		if (C != null) {
			rules.addAll(C.getUsedRules());
		}
		return rules;
	}

	/**
	 * Indicates number of NF violations a parse contains
	 * 
	 * @return boolean
	 */
	int NFpenalty() {
		if (C == null && B != null && B.type != null) {
			NormalForm = NF.unaryNF(B.type, type);
		} else if (B != null && B.type != null && C != null && C.type != null) {
			NormalForm = NF.binaryNF(type, arity, B.type, B.arity, C.type, C.arity);
		} else if (B == null && C == null) {
			NormalForm = true;
		}
		int score = 0;
		if (!NormalForm) {
			score = 1;
		}

		if (B != null) {
			score += B.NFpenalty();
		}
		if (C != null) {
			score += C.NFpenalty();
		}
		return score;
	}

	@Override
	public int compareTo(Tree<CI> obj) {
		if (Log.equal(obj.prob, prob)) { // Tied, choose head right
			return obj.headIndex - headIndex; // Still has ties (btw)
		}
		return (int) Math.signum(obj.prob - prob);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		Tree<?> O = (Tree<?>) o;
		return O.A == A && O.type == type
				&& O.arity == arity && O.rule == rule
				&& ((O.chartItem == null && chartItem == null)
						|| O.chartItem.equals(chartItem))
						&& ((O.backPointer == null && backPointer == null)
								|| O.backPointer.equals(backPointer))
								&& ((B == null && O.B == null) || O.B.equals(B))
								&& ((C == null && O.C == null) || O.C.equals(C));
	}

	@Override
	public int hashCode() {
		int i = A + rule.hashCode();
		if (B != null) {
			i += B.hashCode();
		}
		if (C != null) {
			i += C.hashCode();
		}
		return i;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		rule = (Rule) in.readObject();
		headIndex = in.readInt();
		A = in.readInt();
		type = (Rule_Type) in.readObject();
		arity = in.readInt();
		chartItem = (CI) in.readObject();
		backPointer = (FineBackPointer) in.readObject();
		prob = in.readDouble();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(rule);
		out.writeInt(headIndex);
		out.writeLong(A);
		out.writeObject(type);
		out.writeInt(arity);
		out.writeObject(chartItem);
		out.writeObject(backPointer);
		out.writeDouble(prob);
	}

	public Tree<CI> B() { return B; }

	public Tree<CI> C() { return C; }
	
	public int category() { return A; }
	
	public double probability() { return prob; }
	
	public int getX() { return chartItem.X(); }
	
	public int getY() { return chartItem.Y(); }
	
	public int headIndex() { return headIndex; }

	public int[] getLexicalCategoryIDs() {
		HashMap<Integer, Integer> lexCatIDs = new HashMap<Integer, Integer>();
		this.getLexicalCategoryIDsRecurse(lexCatIDs);
		int[] ids = new int[lexCatIDs.size()];
		for(int i=0; i<ids.length; i++) {
			ids[i] = lexCatIDs.get(i);
		}
		return ids;
	}

	private void getLexicalCategoryIDsRecurse(
			HashMap<Integer, Integer> lexCatIDs) {
		if(this.chartItem.coarseItem instanceof CoarseLexicalCategoryChartItem) {
			int index = this.chartItem.X();
			lexCatIDs.put(index, this.A);
		}
		else {
			this.B.getLexicalCategoryIDsRecurse(lexCatIDs);
			if(this.C != null) {
				this.C.getLexicalCategoryIDsRecurse(lexCatIDs);
			}
		}
	}

	public static String buildPargString(Tree tree, Sentence sen) {
		if (tree == null) {
			return "<s> 0\n<\\s>\n";
		}
		DepRel[][] depr = dpCCGRecurse(tree, sen);
		return printDepRel(depr, sen) + "\n";
	}
	
	public String buildPargString(Sentence sen) {
		return buildPargString(this, sen);
	}


	private static DepRel[][] dpCCGRecurse(Tree a, Sentence sentence) {
		a.ccgDepRel = new DepRel[sentence.length()][sentence.length()];
		if (a.C == null && a.B != null) {
			SemanticTuple.copyDepRel(a.ccgDepRel, dpCCGRecurse(a.B, sentence));
		} else if (a.C != null && a.B != null) {
			// Left Traverse
			SemanticTuple.copyDepRel(a.ccgDepRel, dpCCGRecurse(a.B, sentence));
			// Right Traverse
			SemanticTuple.copyDepRel(a.ccgDepRel, dpCCGRecurse(a.C, sentence));
		}

		if (a.ccgcat != null) {
			DepList filled = a.ccgcat.filled_ccgDependencies();
			if (filled != null) {
				while (filled != null) {
					SemanticTuple.addDependency(a.ccgDepRel, filled);
					filled = filled.next();
				}
			}
		}
		return a.ccgDepRel;
	}

	private static String printDepRel(DepRel[][] depRel, Sentence sentence) {
		int count = 0;
		String[] docStrip = new String[sentence.length()];
		for (int i = 0; i < sentence.length(); i++) {
			docStrip[i] = sentence.get(i).getWord();
		}

		String out = "";
		if (depRel != null) {
			for (int i = 0; i < depRel.length; i++) {  // was chart.length
				for (int j = 0; j < depRel.length; j++) { // was chart.length
					if (depRel[i][j] != null) {
						if (j == depRel.length - 1) {} else { // was chart.length
							if (depRel[i][j].extracted) {
								if (depRel[i][j].bounded) {
									// arg, head, cat, slot, argWord, headWord
									out += i + " \t " + j + " \t " + depRel[i][j].cat
											+ " \t " + depRel[i][j].slot + " \t " + docStrip[i]
													+ " " + docStrip[j] + " <XB>" + "\n";
									count += 1;
								}
								if (!(depRel[i][j].bounded)) {
									// arg, head, cat, slot, argWord, headWord
									out += i + " \t " + j + " \t " + depRel[i][j].cat
											+ " \t " + depRel[i][j].slot + " \t " + docStrip[i]
													+ " " + docStrip[j] + " <XU>" + "\n";
									count += 1;
								}
							} else {
								out += i + " \t " + j + " \t " + depRel[i][j].cat
										+ " \t " + depRel[i][j].slot
										+ " \t " + docStrip[i] + " " + docStrip[j] + "\n";
								count += 1;
							}
						}
					}
				}
			}
			out = "<s> " + count + "\n" + out + "<\\s>";
		} else {
			out += "depRel is null: ";// + catString());
		}
		return out;
	}
}

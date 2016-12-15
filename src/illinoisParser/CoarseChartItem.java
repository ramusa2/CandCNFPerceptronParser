package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines a chartitem for CYK parsing
 * 
 * @author bisk1
 * @param 
 */
strictfp public class CoarseChartItem implements Externalizable {
  private static final int PRIMEONE = 13249,
      PRIMETWO = 18061, PRIMETHREE = 23311;

  /** Cell that contains the chart item */
  public Cell cell;
  /** CCG Category for the chart item */
  private int category;
  /** Cell's position X */
  private int X;
  /** Cell's position Y */
  private int Y;
  /** Indicates if chart item is part of successful parse */
  boolean used = false;
  
  private int hashCode;

  /*
   * COARSE EQUIVALENCE CLASS
   */
  private Rule_Type type;
  private Punctuation punc = Punctuation.None;
  private int arity = -1;

  /**
   * Rule type used for construction of ChartItem (used by NF)
   * 
   * @return Rule type
   */
  //final Rule_Type type() {
  //  return type;
  //}

  /**
   * Return punctuation type if relevant
   * @return punc
   */
  final Punctuation punc(){return punc;}
  /**
   * Arity of rule used during construction (used by NF)
   * 
   * @return Arity
   */
  final int arity() {
    return arity;
  }

  /** Number of child parses */
  public double parses;
  /** Backpointers */
  public ArrayList<BackPointer> children;
  private ArrayList<String> strings;

  // -- trainK Best Training -- //
  /** Top K Parses */
  Tree[] topK;
  private int nextK = 0;
  private PriorityQueue<Tree_bp_ij> frontier;
  /** Fine grained ( different equivalence class ) Chart Items */
  private ConcurrentHashMap<FineChartItem, FineChartItem> FineGrained;

  // -- Testing -- //
  private Double viterbiProb = Double.NaN;
  private CCGcat ccg_cat;

  /** Default Constructor for externalization */
  public CoarseChartItem() {}

  /**
   * Constructor without rule type (rule type is set to Rule_Type.NULL)
   * or punctuation information
   */
  public CoarseChartItem(int cat, Cell c) throws Exception {
    this(c, cat, Rule_Type.NULL, -1, Punctuation.None);
  }

  /**
   * Constructor with rule type, but without or punctuation information
   */
  public CoarseChartItem(int cat, Rule_Type Type, Cell c) throws Exception {
    this(c, cat, Type, -1, Punctuation.None);
  }
  /**
   * Constructor without punctuation information
   */
  public CoarseChartItem(Cell c, int cat, Rule_Type Type, int ar) {
    this(c, cat, Type, ar, Punctuation.None);
  }
  /**
   * No-punctuation constructor, from a Rule
   */
  public CoarseChartItem(Cell c, Rule rule) {
    this(c, rule.A, rule.getType(), -1, Punctuation.None);
  }
  /**
   * Full constructor
   */
  public CoarseChartItem(Cell c, int cat, Rule_Type Type, int ar, Punctuation punc) {
    this.category = cat;
    this.cell = c;
    this.X = c.X();
    this.Y = c.Y();

    this.children = new ArrayList<BackPointer>();
    
    // TODO: revisit setting the type and punc fields
    this.type = Type;
    //this.type = Rule_Type.NULL;
    this.punc = punc;
    this.punc = Punctuation.None;
    
    this.arity = ar;
    this.children = new ArrayList<BackPointer>();
    this.FineGrained = new ConcurrentHashMap<FineChartItem, FineChartItem>();

    if (Configuration.DEBUG) {
      this.strings = new ArrayList<String>(1);
    }
    
    this.hashCode = 17;
    this.hashCode = this.hashCode * 31 + this.category;
    this.hashCode = this.hashCode * 31 + this.X;
    this.hashCode = this.hashCode * 31 + this.Y;
    if(type != null) {
        this.hashCode = this.hashCode * 31 + this.type.hashCode();
    }
  }
  
  /**
   * If a fine chart item ci' equivalent to ci already exists in FineGrained, return ci'; otherwise add ci to FineGrained and return ci.
   */
  public FineChartItem addFineChartItem(FineChartItem ci) {
	  FineChartItem temp = this.FineGrained.putIfAbsent(ci, ci);
	  if(temp == null) {
		  return ci;
	  }
	  return temp;
  }


	public void setFineGrained(Collection<FineChartItem> retainedItems) {
		this.FineGrained.clear();
		for(FineChartItem fci : retainedItems) {
			this.FineGrained.putIfAbsent(fci, fci);
		}
	}

  /**
   * Get probability of the viterbi parse
   * 
   * @return double
   */
  public final double viterbiP() {
    return topK[0].prob;
  }
  
  public Rule_Type type() {
	  return this.type;
  }

  @Override
  public String toString() {
    String ret = cell.chart.getGrammar().prettyCat(category);
    if (this.viterbiProb != null) {
      ret += " " + this.viterbiProb;
    }
    return ret;
  }
  public String toString(Grammar g) {
	    String ret = g.prettyCat(category);
	    if (this.viterbiProb != null
	    		&& !Double.isNaN(this.viterbiProb)) {
	      ret += " " + this.viterbiProb;
	    }
	    return ret;
	  }

  public void logEquivClass() {
    String c = cell.chart.getGrammar().prettyCat(category);
    if (Configuration.NF.equals(Normal_Form.Full)) {
      Util.log(String.format("%-25s %-12s %-2s", c, type(), arity()));
    	Util.log(String.format("%-25s %-2s", c, arity()));
    } else {
      Util.log(String.format("%-25s", c));
    }
    Util.log(String.format(" (->%5d\tT:%5d)\n", children.size(), (int) parses));
  }

  @Override
  public final int hashCode() {
	  return hashCode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    CoarseChartItem obj = (CoarseChartItem) o;

    return this.category == obj.category
        && this.X == obj.X
        && this.Y == obj.Y
        && this.arity == obj.arity
        && (obj.type == this.type || this.type.equals(obj.type))
        && (obj.punc == this.punc || this.punc.equals(obj.punc));
  }

  public final boolean addChild(Rule r, CoarseChartItem B) { 
	  return this.addChild(r,  B, null);
  }

  public final boolean addChild(Rule r, CoarseChartItem B,
      CoarseChartItem C) {
    BackPointer bp;
    // Add a DepStructure for children
    if (C == null) {
        bp = new BackPointer((Unary) r, B);
      if (bp.r != null) {
        if (Configuration.DEBUG) {
          for (String s : B.strings) {
            this.strings.add("{" +
            		cell.chart.getGrammar().prettyCat(this.category)
                + " " + s + "}");
          }
        }
        children.add(bp);
        return true;
      }
      return false;
    }
    bp = new BackPointer((Binary) r, B, C);
    if (bp.r != null) {
      if (Configuration.DEBUG) {
        for (String bs : B.strings) {
          for (String cs : C.strings) {
            this.strings.add("{" +
            		cell.chart.getGrammar().prettyCat(this.category)
                + " " + bs
                + " " + cs + "}");
          }
        }
      }
      children.add(bp);
      return true;
    }
    return false;

  }
  
  /**
   * Compute the topK parses
   * 
   * @throws Exception
   */
   /*
  final void populateTopK(Model model) throws Exception {
    int K = Configuration.trainK;
    //if (cell.chart.test) {
      K = Configuration.testK;
      if(Configuration.DEBUG) {
        K = (int)cell.chart.parses;
      }
    //}
    ArrayList<Tree> successful = new ArrayList<Tree>();
    int count = 0;
    Tree newest = new Tree();
    while (successful.size() < K) {
      newest = getTopK(count, model);

      // We might run out of trees
      if (newest == null) {
        break;
      }
      if (Configuration.NF.equals(Normal_Form.None)
          || newest.NFpenalty() == 0) {
        if (successful.size() >= K) {
          break;
        }

        successful.add(newest);
      }
      count += 1;
    }
    topK = new Tree[successful.size()];
    for (int i = successful.size() - 1; i >= 0; i--) {
      topK[i] = successful.get(i);
    }

    if(Configuration.DEBUG) {
      Util.Println(cell.chart.sentence.toString());
      for(Tree T : topK){
        Util.Println(Math.exp(T.prob) + "\t" + T.toString(cell.chart.getGrammar(), 0));
      }
    }

    if (Configuration.NF.equals(Normal_Form.None) && cell.chart.parses < K
        && topK.length < cell.chart.parses) {
      Util.logln("Only: " + topK.length + "/" + cell.chart.parses);
    }
  }
  */
  
  // TODO: move topK stuff to FineChartItem

  /**
   * Returns the i'th best tree
   * 
   * @param index
   * @return Tree
   * @throws Exception
   */
  /*
  private final Tree getTopK(int index, Model model) throws Exception {
    if (frontier == null) {
      topK = new Tree[2 * (index + 1)];
      nextK = 0;
      frontier = new PriorityQueue<Tree_bp_ij>();
      for (BackPointer bp : children) {
        Tree temp = null;
        if (bp.isUnary()) {
          Unary u = (Unary) bp.r;
          Tree B;
          if (bp.B().children.isEmpty()) {
            B = bp.B().topK[0];
          } else {
            B = bp.B().getTopK(0, model);
          }
          if (B != null) {
            double prob = model.getLogProbabilityOfFineUnaryChartItem(this, bp.B());
            temp = new Tree(u, this, bp, Log.mul(B.prob, prob), B);
          }
        } else {
          Binary b = (Binary) bp.r;
          Tree B = bp.B().getTopK(0, model);
          Tree C = bp.C().getTopK(0, model);
          if (B != null && C != null) {
            double p = model.getLogProbabilityOfFineBinaryChartItem(this, bp, coarseChart);
            temp = new Tree(b, this, bp, Log.mul(B.prob, C.prob, p), B, C);
          }
        }
        if (temp != null) {
          temp.X = X;
          temp.Y = Y;
          Tree_bp_ij newT = new Tree_bp_ij(temp, bp, 0, 0);
          if (frontier.contains(newT) || Util.contains(topK, temp)) {
            Util.Println("Already: ");
            for (Tree_bp_ij T : frontier) {
              Util.Println(T.bp.r.toString(cell.chart.getGrammar()));
              Util.Println(T.T.toString(cell.chart.getGrammar(),0));
            }
            Util.Println("New");
            Util.Println(newT.bp.r.toString(cell.chart.getGrammar()));
            cell.chart.debugChart();
            Util.log(temp.toString(cell.chart.getGrammar(),0));
            throw new Exception("Adding tokK tree to frontier when it already is in frontier");
          }
          frontier.add(newT);
        }
      }
    }
    if (nextK > index) {
      return topK[index];
    }
    double p;
    while (!frontier.isEmpty() && (nextK <= index)) {
      // T = sup(Q)
      Tree_bp_ij Tbpij = frontier.poll();
      // C.push(T)
      if (Configuration.NF.equals(Normal_Form.None)
          || Tbpij.T.NFpenalty() == 0) {
        if (!Util.contains(topK, Tbpij.T)) {
          if (nextK >= topK.length) {
            topK = Arrays.copyOf(topK, topK.length * 2);
          }
          topK[nextK] = Tbpij.T;
          nextK++;
        }
      }

      if (Tbpij.bp.isUnary()) {
        Unary u = (Unary) Tbpij.bp.r;
        p = model.prob(this, Tbpij.bp, cell.Test);
        // p = cell.chart.model.prob(Tbpij.bp.context, cell.Test);
        addUnaryShoulder(Tbpij.i, Tbpij.bp, u, p, model);
      } else {
        Binary r = (Binary) Tbpij.bp.r;
        p = model.prob(this, Tbpij.bp, cell.Test);
        // p = cell.chart.model.prob(Tbpij.bp.context, cell.Test);
        addBinaryShoulders(Tbpij.i, Tbpij.j, Tbpij.bp, r, p, model);
      }

      if (nextK > index) {
        break;
      }
    }
    if (nextK > index) {
      return topK[index];
    }
    return null;
  }

  private final void addUnaryShoulder(int i, BackPointer bp,
      Unary u, double p, Model model) throws Exception {
    Tree B = null;
    if (!bp.B().children.isEmpty()) {
      B = bp.B().getTopK(i + 1, model);
    }
    if (B != null) {
      Tree temp = new Tree(u, this, bp, Log.mul(B.prob, p), B);
      temp.X = X;
      temp.Y = Y;
      Tree_bp_ij newT = new Tree_bp_ij(temp, bp, i + 1, 0);
      if (!frontier.contains(newT)) {
        frontier.add(newT);
      }
    }
  }

  private final void addBinaryShoulders(int i, int j, BackPointer bt,
      Binary r, double p, Model model) throws Exception {
    Tree L, R, temp;
    // Q.push(L_{Ti}R_{Tj+1})
    L = bt.B().getTopK(i, model);
    R = bt.C().getTopK(j + 1, model);
    if (L != null && R != null) {
      temp = new Tree(r, this, bt, Log.mul(L.prob, R.prob, p), L, R);
      temp.X = X;
      temp.Y = Y;
      Tree_bp_ij newT = new Tree_bp_ij(temp, bt, i, j + 1);
      if (!frontier.contains(newT)) {
        frontier.add(newT);
      }
    }
    // Q.push(L_{Ti+1}R_{Tj})
    L = bt.B().getTopK(i + 1, model);
    R = bt.C().getTopK(j, model);
    if (L != null && R != null) {
      temp = new Tree(r, this, bt, Log.mul(L.prob, R.prob, p), L, R);
      temp.X = X;
      temp.Y = Y;
      Tree_bp_ij newT = new Tree_bp_ij(temp, bt, i + 1, j);
      if (!frontier.contains(newT)) {
        frontier.add(newT);
      }
    }
  }
  */

  
  @Override
  public void readExternal(ObjectInput in)
      throws IOException, ClassNotFoundException {
    category = in.readInt();
    parses = in.readDouble();
    children = (ArrayList<BackPointer>) in.readObject();
    type = (Rule_Type) in.readObject();
    punc = (Punctuation) in.readObject();
    arity = in.readInt();
    X = in.readInt();
    Y = in.readInt();
    hashCode = in.readInt();
    FineGrained = (ConcurrentHashMap<FineChartItem, FineChartItem>) in.readObject();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeInt(category);
    out.writeDouble(parses);
    out.writeObject(children);
    out.writeObject(type);
    out.writeObject(punc);
    out.writeInt(arity);
    out.writeInt(X);
    out.writeInt(Y);
    out.writeInt(hashCode);
    out.writeObject(FineGrained);
  }
  
  public boolean setCell(Cell c) {
	  if(this.cell != null) {
		  return false;
	  }
	  this.cell = c;
	  c.addCat(this);
	  return true;
  }

  private class Tree_bp_ij implements Comparable<Object> {
    final Tree T;
    final int i, j;
    final BackPointer bp;

    public Tree_bp_ij(Tree t, BackPointer bp2, int I, int J) {
      T = t;
      i = I;
      j = J;
      bp = bp2;
    }

    @Override
    public int compareTo(Object o) {
      Tree_bp_ij obj = (Tree_bp_ij) o;
      return T.compareTo(obj.T);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      Tree_bp_ij obj = (Tree_bp_ij) o;
      // TODO: check that bp is equal, as well?
      return i == obj.i && j == obj.j && T.equals(obj.T) && bp.equals(obj.bp);
    }
  }

	public int category() {
		return category;
	}

	public int X() {
		return X;
	}

	public int Y() {
		return Y;
	}

	public Collection<FineChartItem> fineItems() {
		return this.FineGrained.keySet();
	}

	public void setCCGcat(CCGcat cat) {
		this.ccg_cat = cat;
	}
	
	public CCGcat getCCGcat() {
		return this.ccg_cat;
	}

	public String getBracketedParse() {
		Grammar g = this.cell.chart.getGrammar();
		String cat = g.getCatFromID(this.category);
		if(this instanceof CoarseLexicalCategoryChartItem) {
			return "{ "+cat+" "+this.cell.chart.getSentence().asWords(this.X, this.X)+" }";
		}
		String ret = "{ "+cat+" ";
		BackPointer bp = this.children.get(0);
		ret += bp.B.getBracketedParse();
		if(bp.C != null) {
			ret += " "+bp.C.getBracketedParse();
		}
		return ret +"}";
	}

}

enum Punctuation {
  None, FW, BW;
}

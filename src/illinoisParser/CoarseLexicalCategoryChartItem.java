package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * A LexicalCategoryChartItem has an empty list of children (the list of BackPointers in a normal/internal ChartItem).
 * Instead, it stores the production rule that generates a particular lexical item from its category. 
 * 
 * @author ramusa2
 *
 */
public class CoarseLexicalCategoryChartItem extends CoarseChartItem implements Externalizable {

	  /** Default Constructor for externalization */
	  public CoarseLexicalCategoryChartItem() {}
	
	public CoarseLexicalCategoryChartItem(Cell c, Integer cat) {
		super(c, cat, Rule_Type.PRODUCTION, -1);
		this.parses = 1.0;
	}

	public int index() {
		return super.X();
	}
	
	  @Override
	  public void readExternal(ObjectInput in)
	      throws IOException, ClassNotFoundException {
		  super.readExternal(in);
	  }
	  @Override
	  public void writeExternal(ObjectOutput out) throws IOException {
			super.writeExternal(out);
	  }
}

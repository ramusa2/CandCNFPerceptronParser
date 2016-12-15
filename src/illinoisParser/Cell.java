package illinoisParser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

//public class Cell implements Externalizable {
public class Cell implements Serializable {
	private static final long serialVersionUID = 11112010;
	private ConcurrentHashMap<Integer,ConcurrentHashMap<CoarseChartItem, CoarseChartItem>> cats
	= new ConcurrentHashMap<Integer,ConcurrentHashMap<CoarseChartItem, CoarseChartItem>>();
	private int X;
	private int Y;
	public Chart chart;

	public Cell() {}

	public Cell(Chart c, int xy) {
		this.X = xy;
		this.Y = xy;
		this.chart = c;
	}

	Cell(Chart c, int x, int y) {
		this.X = x;
		this.Y = y;
		this.chart = c;
	}

	final Collection<Integer> cats() {
		return cats.keySet();
	}

	final Collection<CoarseChartItem> values(Integer cat) {
		return this.cats.get(cat).values();
	}

	public final Collection<CoarseChartItem> values() {
		HashSet<CoarseChartItem> values = new HashSet<CoarseChartItem>();
		for (ConcurrentHashMap<CoarseChartItem, CoarseChartItem> map : this.cats.values()) {
			values.addAll(map.values());
		}
		return values;
	}

	public CoarseChartItem addCat(CoarseChartItem ci) {
		return addCatHelper(ci);
	}

	private CoarseChartItem addCatHelper(CoarseChartItem newC) {
		ConcurrentHashMap<CoarseChartItem, CoarseChartItem> temp;
		if ((temp = cats.get(newC.category())) != null) {
			CoarseChartItem cat;
			if ((cat = temp.get(newC)) != null) {
				return cat;
			}
			temp.put(newC, newC);
			return newC;
		}
		temp = new ConcurrentHashMap<CoarseChartItem, CoarseChartItem>();
		temp.put(newC, newC);
		cats.put(newC.category(), temp);
		return newC;
	}

	CoarseChartItem addCat(Binary v, CoarseChartItem B, CoarseChartItem C) throws Exception {
		CoarseChartItem newC = null;
		switch (Configuration.NF) {
		case None:
			newC = addCatHelper(new CoarseChartItem(v.A, this));
			break;
		case Eisner_Orig:
			Rule_Type ternary = (Rule_Type.isCoordination(v.Type)) ? v.Type : Rule_Type.NULL;
			newC = addCatHelper(new CoarseChartItem(v.A, ternary, this));
			break;
		case Eisner:
		case Full_noPunct:
		case Full:
			if(v.Type == Rule_Type.FW_PUNCT || v.Type == Rule_Type.FW_PUNCT_TC) {
				newC = addCatHelper(new CoarseChartItem(this, v.A, C.type(), C.arity(), Punctuation.FW));
			} else if (v.Type == Rule_Type.BW_PUNCT || v.Type == Rule_Type.BW_PUNCT_TC) {
				newC = addCatHelper(new CoarseChartItem(this, v.A, B.type(), B.arity(), Punctuation.BW));
			} else if (v.Type == Rule_Type.FW_CONJOIN) {
				newC = addCatHelper(new CoarseChartItem(this, v.A, C.type(), C.arity(), Punctuation.None));
			} else if (v.Type == Rule_Type.BW_CONJOIN && Rule_Type.TR(B.type())) {
				newC = addCatHelper(new CoarseChartItem(this, v.A, B.type(), B.arity(), Punctuation.None));
			} else {
				newC = addCatHelper(new CoarseChartItem(this, v.A, v.Type, v.arity, Punctuation.None));
			}
		}

		// BackPointers
		if (newC.addChild(v, B, C)) {
			// Number of parses
			newC.parses += (B.parses * C.parses);
		}
		return newC;
	}

	/*
	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		X = in.readInt();
		Y = in.readInt();
		if (X == -1 || Y == -1) {
			Util.Error("-1");
		}
		cats = (ConcurrentHashMap<Integer,
				ConcurrentHashMap<CoarseChartItem, CoarseChartItem>>) in.readObject();
		if (cats != null) {
			for (ConcurrentHashMap<CoarseChartItem, CoarseChartItem> map : cats.values()) {
				for (CoarseChartItem cat : map.values()) {
					if (X == Y) {
						recurse(cat);
					} else {
						cat.cell = this;
					}
				}
			}
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeInt(X);
		out.writeInt(Y);
		out.writeObject(cats);
	}

	private void recurse(CoarseChartItem cat) {
		cat.cell = this;
		if (!cat.children.isEmpty()) {
			for (BackPointer t : cat.children) {
				if(t.isUnary()) {
					recurse(t.B());
				}
			}
		}
	}
	 */

	@Override
	public String toString() {
		String ret = "Cell [" + X + ", " + Y + "]\n";
		ret += "Cats:\n";
		if (cats != null) {
			for (ConcurrentHashMap<CoarseChartItem, CoarseChartItem> map : cats.values()) {
				for (CoarseChartItem ci : map.values()) {
					ret += " \n" + ci;
				}
			}
		}
		return ret;
	}

	public void removeUnusedCats() {
		ArrayList<CoarseChartItem> AL;
		for (ConcurrentHashMap<CoarseChartItem, CoarseChartItem> map : cats.values()) {
			AL = new ArrayList<CoarseChartItem>(map.keySet());
			for (CoarseChartItem ci : AL) {
				if (!ci.used) {
					map.remove(ci);
				}
			}
		}
	}

	public boolean isEmpty() {
		return cats.isEmpty();
	}

	public void addAllCats(Collection<CoarseChartItem> newCats) {
		for (CoarseChartItem ci : newCats) {
			addCatHelper(ci);
		}
	}

	public void addAllCats(HashMap<CoarseChartItem, CoarseChartItem> newCats) {
		addAllCats(newCats.values());
	}

	//  public boolean contains(long cat) {
	//    return cats.containsKey(cat);
	//  }

	public Collection<CoarseChartItem> getCats(Integer cat) {
		if (cats.containsKey(cat)) {
			return cats.get(cat).values();
		}
		return null;
	}

	public CoarseChartItem getCat(CoarseChartItem c) {
		if (cats.containsKey(c.category())) {
			return cats.get(c.category()).get(c);
		}
		return null;
	}

	public int X() {
		return X;
	}

	public int Y() {
		return Y;
	}
}

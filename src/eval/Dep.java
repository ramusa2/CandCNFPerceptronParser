package eval;

public class Dep {
	int source, target, arg;
	String category;

	public Dep(String line) {
		String[] args = line.split("\\s+");
		target = Integer.parseInt(args[0]);
		source = Integer.parseInt(args[1]);
		category = args[2];
		arg = Integer.parseInt(args[3]);

	}

	boolean matchesLabeledDirected(Dep oth) {
		return this.source == oth.source && this.target == oth.target 
				&& this.arg == oth.arg && this.category.equals(oth.category);
	}

	boolean matchesUnlabeledUndirected(Dep oth) {
		return (this.source == oth.source && this.target == oth.target)
				|| (this.source == oth.target && this.target == oth.source);
	}

	public String toString() {
		return source+"\t"+target+"\t"+category+"\t\t"+arg;
	}

	public String desc() {
		return pad(category, 50)+" arg="+arg;
	}

	private String pad(String s, int l) {
		while(s.length() < l) {
			s += " ";
		}
		return s+" ";
	}
}

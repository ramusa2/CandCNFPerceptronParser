package multitagger.nettester;

import java.util.ArrayList;
import java.util.HashSet;

public class DataItem {
	
	private double label;
	
	private String str;
	
	public DataItem(String input) {
		this.str = input;
		this.label = this.labelShouldBe();
	}
	
	public double getLabel() {
		return label;
	}
	
	public String str() {
		return this.str;
	}
	
	public static ArrayList<DataItem> getData(int numItems, double noisePct) {
		ArrayList<DataItem> list = new ArrayList<DataItem>();
		for(int i=0; i<numItems; i++) {
			DataItem item = new DataItem(getString());
			if(Math.random() < noisePct) {
				item.switchLabel();
			}
			list.add(item);
		}
		return list;
	}
	
	private void switchLabel() {
		if(this.label == 1.0) {
			this.label = 0.0;
		}
		else {
			this.label = 1.0;
		}
	}

	private static String getString() {
		String[] chars = new String[]{"a", "b", "c", "d", "e"};
		int maxLength = 5;
		String ret = "";
		int length = (int) (Math.random()*(maxLength+1));
		for(int c=0; c<length; c++) {
			ret += getRandomCharacter(chars);
		}
		return ret;
	}

	private static String getRandomCharacter(String[] chars) {
		return chars[Math.min((int) (Math.random()*chars.length), chars.length-1)];
	}

	// Function: has at least three unique letters 
	public double labelShouldBe() {
		HashSet<String> characters = new HashSet<String>();
		for(int c=0; c<this.str.length(); c++) {
			characters.add(this.str.charAt(c)+"");
		}
		return (characters.size() >= 3) ? 1.0 : 0.0;
	}
	
	public boolean isFaithful() {
		return this.label == this.labelShouldBe();
	}

}

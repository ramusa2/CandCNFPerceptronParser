package supertagger.lsbeta;

import illinoisParser.Sentence;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import supertagger.SupertagAssignment;

public class MultitaggerTrainingSentence implements Externalizable {
	
	private Sentence sentence;
	
	private ArrayList<MultitaggerTrainingItem> items;
	
	private ArrayList<Integer> itemsToTest;
	
	public MultitaggerTrainingSentence() {
		this.sentence = null;
		this.items = new ArrayList<MultitaggerTrainingItem>();
		this.itemsToTest = new ArrayList<Integer>();
	}
	
	public MultitaggerTrainingSentence(SupertagAssignment tags, int topK) {
		this.sentence = tags.sentence();
		this.items = new ArrayList<MultitaggerTrainingItem>();
		this.itemsToTest = new ArrayList<Integer>();
		for(int i=0; i<this.sentence.length(); i++) {
			this.items.add(new MultitaggerTrainingItem(tags, i, topK));
			this.itemsToTest.add(i);
		}
	}
	
	public void addItem(MultitaggerTrainingItem item) {
		this.items.add(item);
	}
	
	public Sentence sentence() {
		return this.sentence;
	}
	
	public ArrayList<MultitaggerTrainingItem> getItems() {
		return this.items;
	}
	
	public ArrayList<Integer> getTestingIndices() {
		return this.itemsToTest;
	}
	
	public void addItemIndex(int index) {
		this.itemsToTest.add(index);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(sentence);
		out.writeObject(this.items);
		out.writeObject(this.itemsToTest);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		this.sentence = (Sentence) in.readObject();
		this.items = (ArrayList<MultitaggerTrainingItem>) in.readObject();
		this.itemsToTest = (ArrayList<Integer>) in.readObject();
	}

	public void shuffleItemIndices() {
		Collections.shuffle(this.itemsToTest);
	}

}

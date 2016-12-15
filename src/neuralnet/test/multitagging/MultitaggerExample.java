package neuralnet.test.multitagging;

import java.util.ArrayList;

import illinoisParser.Sentence;
import supertagger.lsbeta.MultitaggerTrainingData;
import supertagger.lsbeta.MultitaggerTrainingItem;
import supertagger.lsbeta.MultitaggerTrainingSentence;

public class MultitaggerExample {
	
	protected String word;
	
	protected MultitaggerTrainingItem item;
	
	protected MultitaggerTrainingSentence sentence;
	
	protected int index;
	
	public MultitaggerExample() {}
	
	public MultitaggerExample(MultitaggerTrainingSentence multitaggedSentence, int i) {
		this.index = i;
		this.sentence = multitaggedSentence;
		this.item = multitaggedSentence.getItems().get(i);
		this.word = this.sentence.sentence().wordAt(i);
	}
	
	public String getWord() {
		return word;
	}
	
	public MultitaggerTrainingItem getItem() {
		return item;
	}
	
	public int getIndex() {
		return index;
	}
	
	public MultitaggerTrainingSentence getMultitaggedSentence() {
		return this.sentence;
	}
	
	public Sentence getSentence() {
		return this.sentence.sentence();
	}
	
	public static ArrayList<MultitaggerExample> getExamples(MultitaggerTrainingData data) {
		ArrayList<MultitaggerExample> list = new ArrayList<MultitaggerExample>();
		for(MultitaggerTrainingSentence sen : data.getData()) {
			for(int i=0; i<sen.sentence().length(); i++) {
				list.add(new MultitaggerExample(sen, i));
			}
		}
		return list;
	}

}

package perceptron.parser;

import java.util.ArrayList;

import perceptron.parser.ccnormalform.NormalFormChartItem;

import illinoisParser.AutoDecoder;
import illinoisParser.Chart;
import illinoisParser.LexicalToken;
import illinoisParser.Sentence;
import illinoisParser.variables.ConditioningVariables;
import supertagger.SupertagAssignment;

public class SupertaggedSentence {
	
	private Sentence sentence;
	private SupertagAssignment supertags;
	private boolean parseable;
	
	private NormalFormChartItem goldRoot;
	private ArrayList<ConditioningVariables> goldFeatures;

	public SupertaggedSentence(Sentence sen) {
		sentence = sen;
		supertags = null;
		parseable = true;
	}
	
	public SupertaggedSentence(Sentence sen, SupertagAssignment tags) {
		this(sen);
		supertags = tags;
	}
	
	public boolean isTagged() {
		return supertags == null;
	}
	
	public void assignTags(SupertagAssignment tags) {
		supertags = tags;
	}
	
	public Sentence sentence() {
		return sentence;
	}
	
	public SupertagAssignment tags() {
		return supertags;
	}
	
	public String toString() {
		String str = sentence.getCCGbankParse()+"\n";
		if(supertags != null) {
			str += supertags.toString();
		}
		else {
			for(LexicalToken lt : sentence.getTokens()) {
				str += lt.getWord()+" "+lt.getPOS()+"\n";
			}
		}
		return str.trim();
	}

	public static SupertaggedSentence fromString(String input) {
		String[] lines = input.split("\n");
		if(lines.length == 0) {
			return null;
		}
		SupertagAssignment tags = SupertagAssignment.fromString(input);
		return new SupertaggedSentence(tags.sentence(), tags);
	}

	public NormalFormChartItem getGoldRoot(PerceptronParser parser) {
		if(goldRoot != null) {
			return goldRoot;
		}
		AutoDecoder auto = new AutoDecoder(sentence, sentence.getCCGbankParse());
		Chart chart = auto.getFineChart(parser);
		goldRoot = (NormalFormChartItem) chart.fineRoot();
		return goldRoot;
	}

	public ArrayList<ConditioningVariables> getGoldFeatures(PerceptronParser parser) {
		if(goldFeatures != null) {
			return goldFeatures;
		}
		NormalFormChartItem root = this.getGoldRoot(parser);
		goldFeatures = parser.collateFeatures(root);
		return goldFeatures;
	}

	public boolean parseable() {
		return parseable;
	}
	
	public void markUnparseable() {
		parseable = false;
	}
}

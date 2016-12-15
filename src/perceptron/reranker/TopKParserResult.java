package perceptron.reranker;

import java.util.ArrayList;

import illinoisParser.ParseResult;
import illinoisParser.Sentence;

public class TopKParserResult {
	
	private Sentence sentence;
	
	private ParseResult gold;
	
	private ArrayList<Double> probs;
	
	private ArrayList<ParseResult> results; 
	
	int size;
	
	public TopKParserResult(Sentence sen, ParseResult goldTree) {
		sentence = sen;
		gold = goldTree;
		probs = new ArrayList<Double>();
		results = new ArrayList<ParseResult>();
		size = 0;
	}
	
	public ParseResult gold() {
		return gold;
	}
	
	public void addResult(ParseResult r) {
		probs.add(r.getViterbiProbability());
		results.add(r);
		size++;
	}
	
	public int size() {
		return size;
	}
	
	public Sentence sentence() {
		return sentence;
	}
	
	public ArrayList<Double> probs() {
		return probs;
	}
	
	public ArrayList<ParseResult> results() {
		return results;
	}
}

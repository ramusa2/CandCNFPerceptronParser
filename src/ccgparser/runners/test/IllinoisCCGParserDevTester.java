package ccgparser.runners.test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import perceptron.parser.PerceptronParser;
import perceptron.parser.ccnormalform.NormalFormPerceptronParser;

import illinoisParser.CCGbankReader;
import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Sentence;
import illinoisParser.SupervisedParsingConfig;
import illinoisParser.Tree;
import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;
import util.Util;

public class IllinoisCCGParserDevTester {

	static int MAX = 0;
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		//testSupertagger();
		testCandCParser();
	}
	
	private static void testCandCParser() throws Exception {
		System.out.println("#################################");
		System.out.println("Testing C&C normal-form parser:");
		Grammar grammar = Grammar.load(new File("grammar"));
		LSSupertagger supertagger = loadExistingSupertagger();
		Collection<Sentence> sentences = CCGbankReader.getSentencesFromAutoFile("data/wsj0.auto");
		Collection<SupertagAssignment> supertagged = tagSentences(supertagger, sentences);
		System.out.println("Finished supertagging sentences.");
		
		
		String candCWeightFile = "";
		
		
		SupervisedParsingConfig config = SupervisedParsingConfig.getDefaultConfig();
		MAX = config.getMaxSentenceLength();
		
		//MAX = 15;
		
		
		PerceptronParser parser = NormalFormPerceptronParser.load(grammar, config, candCWeightFile);
		System.out.println("Loaded parser.");
		Collection<Tree<? extends FineChartItem>> parseTrees = parseSentences(supertagged, parser);
		evaluateSupertaggedSentences(supertagged);
		evaluateParsedSentences(sentences, parseTrees);
		System.out.println("Finished testing C&C normal-form parser.");
	}

	private static void evaluateParsedSentences(Collection<Sentence> sentences,
			Collection<Tree<? extends FineChartItem>> parseTrees) {
		// TODO Auto-generated method stub
		
	}

	private static Collection<Tree<? extends FineChartItem>> parseSentences(
			Collection<SupertagAssignment> supertagged,
			PerceptronParser parser) {
		Collection<Tree<? extends FineChartItem>> parseTrees = new ArrayList<Tree<? extends FineChartItem>>();
		int tooLong = 0;
		int failures = 0;
		int total = supertagged.size();
		for(SupertagAssignment tagged : supertagged) {
			Tree<? extends FineChartItem> viterbi = null;
			if(tagged.sentence().length() > MAX) {
				failures++;
				parseTrees.add(viterbi);
				continue;
			}
			try {
				parseTrees.add(parser.parse(tagged));
			}
			catch(Exception e) {
				System.out.println("Failed to parse sentence: \""+tagged.sentence().asWords()+"\"");
				failures++;
			}
			parseTrees.add(viterbi);
		}
		System.out.println("Out of "+total+" sentences, "+(total - tooLong - failures)+" were successfully parsed; "+tooLong+" were too long and "+failures+" had parse failures.");
		return parseTrees;
	}

	public static void testSupertagger() {
		System.out.println("#################################");
		System.out.println("Testing supertagger:");
		LSSupertagger supertagger = loadExistingSupertagger();
		Collection<Sentence> sentences = CCGbankReader.getSentencesFromAutoFile("data/wsj0.auto");
		Collection<SupertagAssignment> supertagged = tagSentences(supertagger, sentences);
		evaluateSupertaggedSentences(supertagged);
		System.out.println("Finished testing supertagger.");
	}

	private static void evaluateSupertaggedSentences(
			Collection<SupertagAssignment> supertagged) {
		int numSens = supertagged.size();
		double numWords = 0.0;
		double correct = 0.0;
		for(SupertagAssignment tagged : supertagged) {
			int len = tagged.sentence().length();
			numWords += len;
			for(int i=0; i<len; i++) {
				if(tagged.getGold(i).equals(tagged.getBest(i).category())) {
					correct++;
				}
			}
		}
		System.out.println("Evaluated "+numSens+" sentences with "+((int) numWords)+" words.");
		System.out.println("Viterbi supertag accuracy: "+(correct/numWords));
	}

	private static Collection<SupertagAssignment> tagSentences(
			LSSupertagger supertagger, Collection<Sentence> sentences) {
		System.out.println("Supertagging "+sentences.size()+" sentences");
		ArrayList<SupertagAssignment> tagged = new ArrayList<SupertagAssignment>();
		for(Sentence sentence : sentences) {
			tagged.add(supertagger.tagSentence(sentence));
		}
		return tagged;
	}

	private static LSSupertagger loadExistingSupertagger() {
		try {
			return Util.loadTagger();
		} catch (Exception e) {
			System.out.println("Failed to load tagger.");
			e.printStackTrace();
			System.exit(1);
		}
		return null;
	}

}

package eval;

import illinoisParser.CCGbankReader;
import illinoisParser.Grammar;
import illinoisParser.Model;
import illinoisParser.Parse;
import illinoisParser.Sentence;
import illinoisParser.models.BaselineModel;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

public class Evaluator {

	private Grammar grammar;
	private Model model;

	private ConfusionMatrix lexcats;
	private PredArgDepResults deps;

	private ArrayList<Parse> goldParses;
	private ArrayList<Parse> guessParses;

	public Evaluator(String goldAuto, String guessAuto) {
		grammar = new Grammar();
		model = new BaselineModel(grammar);
		goldParses = this.getParsesFromAutoFile(goldAuto);
		guessParses = this.getParsesFromAutoFile(guessAuto);
		checkForParseFailures();
		lexcats = new ConfusionMatrix();
		deps = new PredArgDepResults();
	}

	private void checkForParseFailures() {
		ArrayList<Parse> filledParses = new ArrayList<Parse>();
		int index = 0;
		for(Parse guessParse : this.guessParses) {
			boolean matches = false;
			while(!matches) {
				Parse goldParse = this.goldParses.get(index);
				index++;
				if(!goldParse.matches(guessParse)) {
					filledParses.add(Parse.getEmptyParse(model));
				}
				else {
					filledParses.add(guessParse);
					matches = true;
				}
			}
		}
		guessParses = filledParses;
	}

	private ArrayList<Parse> getParsesFromAutoFile(String auto) {
		Collection<Sentence> autoSens = CCGbankReader.getSentencesFromAutoFile(auto);
		ArrayList<Parse> parses = new ArrayList<Parse>();
		for(Sentence sen : autoSens) {
			Parse parse;
			if(sen.getCCGbankParse().equals("TOO_LONG")
					|| sen.getCCGbankParse().equals("PARSE_FAILURE")) {
				parse = new Parse(model); // do nothing
			}
			else {
				parse = new Parse(sen.getCCGbankParse(), model, sen);
			}
			parses.add(parse);
		}
		return parses;
	}

	public void runEval() {
		for(int s=0; s<goldParses.size(); s++) {
			Parse gold = goldParses.get(s);
			Parse guess = guessParses.get(s);
			if(guess.isFailure()) {
				// Lexcats
				for(String lexcat : gold.getLexicalCategories()) {
					this.lexcats.add(lexcat, "FAILURE");
				}
				// Pred-arg deps
				this.deps.addResult(gold.getPredArgDeps(), new DepSet(0));
			}
			else {
				// Lexcats
				String[] goldLCs = gold.getLexicalCategories();
				String[] guessLCs = guess.getLexicalCategories();
				for(int i=0; i<goldLCs.length; i++) {
					if(i < guessLCs.length) {
						this.lexcats.add(goldLCs[i], guessLCs[i]);
					}
					else {
						this.lexcats.add(goldLCs[i], "FAILURE");
					}
				}
				// Pred-arg deps
				this.deps.addResult(gold.getPredArgDeps(), guess.getPredArgDeps());
			}
		}
	}

	public void writeResultsToDir(String outputDir) {
		if(!outputDir.endsWith("/")) {
			outputDir += "/";
		}
		File dir = new File(outputDir);
		dir.mkdirs();
		// Lexical categories
		lexcats.writeConfusionsToDir(dir, "");
		// Pred-arg dependencies
		deps.writeDepResultsToDir(dir, "");
		// Summary
		String sumName = outputDir+"summary.txt";
		try {
			PrintWriter summary = new PrintWriter(new File(sumName));
			summary.println("Pred-arg deps:");
			summary.println(deps.getSummaryString());
			summary.println("\nLexcats:");
			summary.println(lexcats.getSummaryString());
			summary.close();
		}
		catch(Exception e) {
			System.out.println("Failed to write evaluator summary file.");
		}
	}

}

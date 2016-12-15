package ccgparser.core;

import illinoisParser.FineChartItem;
import illinoisParser.Grammar;
import illinoisParser.Sentence;
import illinoisParser.Tree;

import java.io.File;
import java.io.IOException;

import ccgparser.core.config.Constants;

import perceptron.parser.PerceptronParser;
import supertagger.SupertagAssignment;
import supertagger.lewissteedman.LSSupertagger;

/**
 * Wrapper class for the Illinois CCG parser, which combines a neural-net unigram supertagger
 * with additional parsing models.
 * 
 * @author ramusa2
 *
 */
public class IllinoisCCGSystem {

	private File baseDirectory;
	
	private Grammar grammar;
	
	private LSSupertagger supertagger;
	
	private PerceptronParser parser;
	
	private IllinoisCCGSystem(File base_directory) {
		this.baseDirectory = base_directory;
	}
	
	public static IllinoisCCGSystem createNewIllinoisCCGParser(File base_directory) {
		base_directory.mkdirs();
		IllinoisCCGSystem ccg = new IllinoisCCGSystem(base_directory);
		ccg.createFileStructure();
		return ccg;
	}
	
	private void createFileStructure() {
		this.createFile(Constants.GRAMMAR_DIR, true);
		this.createFile(Constants.SUPERTAGGER_MODEL_DIR, true);
		this.createFile(Constants.CONFIG, false);
		this.createFile(Constants.CANDC_PARSER_DIR, true);
		this.createFile(Constants.TRAINING_DIR, true);
	}

	private File createFile(String pathUnderBaseDirectory, boolean mkdir) {
		File created = null;
		try {
			String path = this.baseDirectory.getCanonicalPath()+File.separator+pathUnderBaseDirectory;
			created = new File(path);
			if(mkdir) {
				created.mkdir();
			}
		}
		catch(IOException e) {
			System.err.println("Failed to create file for parser based in "+this.baseDirectory.getPath()
					+".\nUnable to open sub-file: "+pathUnderBaseDirectory);
		}
		return created;
	}

	public static IllinoisCCGSystem loadIllinoisCCGParser(File base_directory) {
		IllinoisCCGSystem ccg = new IllinoisCCGSystem(base_directory);
		ccg.readConfig();
		ccg.loadGrammar();
		ccg.loadSupertagger();
		ccg.loadCandCParser();
		return ccg;
	}

	private void readConfig() {
		// TODO Auto-generated method stub
		
	}

	private void loadGrammar() {
		// TODO Auto-generated method stub
		
	}

	private void loadSupertagger() {
		// TODO Auto-generated method stub
		
	}

	private void loadCandCParser() {
		// TODO Auto-generated method stub
		
	}
	
	public void setGrammar(Grammar g) {
		
		// TODO: clear/update filesystem
	}
	
	public void setSupertagger(LSSupertagger st) {
		this.supertagger = st;
		// TODO: clear/update filesystem
	}
	
	public void setParser(PerceptronParser p) {
		this.parser = p;
		// TODO: clear/update filesystem
	}
	
	public void setTrainingData() {
		
	}
	
	public void clearTrainingData() {
		
	}
	
	public void trainSupertagger() {
		
	}
	
	public void trainParser() {
		
	}
	
	public SupertagAssignment supertagSentence(Sentence sentence) {
		return null;
	}
	
	public Tree<? extends FineChartItem> parseSupertaggedSentence(SupertagAssignment supertaggedSentence) {
		return null;
	}
	
	public Tree<? extends FineChartItem> parseSentence(Sentence sentence) {
		return this.parseSupertaggedSentence(this.supertagSentence(sentence));
	}
	
}

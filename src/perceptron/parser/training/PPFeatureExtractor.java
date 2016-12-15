package perceptron.parser.training;

import illinoisParser.variables.ConditioningVariables;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;

import perceptron.core.representations.parsing.PackedFeatureForest;
import perceptron.parser.PerceptronChart;
import perceptron.parser.PerceptronParser;
import util.serialization.SerializedData;

public class PPFeatureExtractor {

	private static String PARSER_FILE_NAME = "parser";
	private static String FREQ_FILE_NAME = "parser_feature_freqs";

	private File directory;

	private PerceptronParser parser;

	private HashMap<ConditioningVariables, Integer> featureFreqs;

	private SerializedData<PerceptronChart> coarseForests;

	private SerializedData<PackedFeatureForest> fineForests;

	public PPFeatureExtractor(File dir, PerceptronParser par, SerializedData<PerceptronChart> coarseForestsCache,
			SerializedData<PackedFeatureForest> fineForestsCache) {
		this.directory = dir;
		this.parser = par;
		this.parser.setSaveDirectory(dir.getPath());
		this.loadFeatureFrequenciesIfPossible();
		this.coarseForests = coarseForestsCache;
		this.fineForests = fineForestsCache;
	}
	
	public PPFeatureExtractor(File dir) {
		this.directory = dir;
		this.parser = PerceptronParser.loadIntermediateParser(dir, PARSER_FILE_NAME, false);
		if(this.parser != null) {
			this.parser.setSaveDirectory(dir.getPath());
		}
		this.loadFeatureFrequenciesIfPossible();
		this.coarseForests = null;
		this.fineForests = null;
	}

	public void extractFeaturesAndBuildFeatureForests() {
		PerceptronChart coarse;
		while((coarse = this.coarseForests.next()) != null) {			
			PackedFeatureForest unpruned = this.parser.extractAndAddFeatures(coarse, this, true);			
			this.fineForests.addObject(unpruned);
		}
		// Save parser and feature frequency information
		this.parser.save(PARSER_FILE_NAME);
		this.saveFeatureFrequencies();
	}

	private void saveFeatureFrequencies() {
		try {
			File freqFile = new File(this.directory.getPath()+File.separator+FREQ_FILE_NAME);
			PrintWriter pw = new PrintWriter(freqFile);
			for(ConditioningVariables vars : this.featureFreqs.keySet()) {
				pw.println(this.parser.getFeatureIndex(vars)+" "+this.featureFreqs.get(vars)+"  "+vars);
			}
			pw.close();
		}
		catch(Exception e) {
			System.err.println("Failed to write feature frequencies to disk.");
			e.printStackTrace();
		}
	}
	
	private void loadIntermediateParserIfPossible(boolean cacheVariables) {
		PerceptronParser loadedParser;
		try {
			loadedParser = PerceptronParser.loadIntermediateParser(directory, PARSER_FILE_NAME, cacheVariables);
			this.parser = loadedParser;
		}
		catch(Exception e) {
			System.out.println("Warning: failed to load intermediate parser for feature extraction.");
		}
	}

	private void loadFeatureFrequenciesIfPossible() {
		try {
			this.featureFreqs = new HashMap<ConditioningVariables, Integer>();
			File freqFile = new File(this.directory.getPath()+File.separator+FREQ_FILE_NAME);
			if(freqFile.exists()) {
				Scanner sc = new Scanner(freqFile);
				this.featureFreqs = new HashMap<ConditioningVariables, Integer>();
				String line;
				while(sc.hasNextLine()) {
					line = sc.nextLine().trim();
					if(!line.isEmpty()) {
						String[] toks = line.split("\\s+");
						int featureIndex= Integer.parseInt(toks[0]);
						int freq = Integer.parseInt(toks[1]);
						String varString = line.substring((featureIndex+"").length() + 1 + (freq+"").length() + 1);
						varString = varString.replaceAll("\\[", "");
						varString = varString.replaceAll("\\]", "");
						varString = varString.replaceAll(",", "");
						ConditioningVariables vars = ConditioningVariables.loadFromString(varString.trim(), false);
						this.featureFreqs.put(vars, freq);
					}
				}
				sc.close();
			}
		}
		catch(Exception e) {
			System.err.println("Failed to load feature frequencies from disk.");
			e.printStackTrace();
		}
	}

	public PerceptronParser getIntermediateParser() {
		return this.parser;
	}

	public HashMap<ConditioningVariables, Integer> getFeatureFrequencies() {
		return this.featureFreqs;
	}

	public void pruneFeatureFromForests(PerceptronParser finalParser, 
			SerializedData<PackedFeatureForest> intermediateForestsCache,
			SerializedData<PackedFeatureForest> finalForestsCache) {
		HashMap<Integer, Integer> featureMap = getFeatureMap(finalParser);		
		PackedFeatureForest unpruned;
		while((unpruned = intermediateForestsCache.next()) != null) {
			PackedFeatureForest pruned = unpruned.pruneFeatureForest(unpruned, featureMap);
			finalForestsCache.addObject(pruned);
		}
		intermediateForestsCache.reset();
	}
	
	private HashMap<Integer, Integer> getFeatureMap(PerceptronParser finalParser) {
		if(this.parser == null
				|| this.parser.features().isEmpty()) {
			this.loadIntermediateParserIfPossible(true);
		}
		HashMap<Integer, Integer> featureMap = new HashMap<Integer, Integer>();
		for(ConditioningVariables feature : finalParser.features()) {
			Integer tempFeatIndex = this.parser.getFeatureIndex(feature);
			if(tempFeatIndex != null) {
				featureMap.put(tempFeatIndex, finalParser.getFeatureIndex(feature));
			}
		}
		return featureMap;
	}

	public void incrementFeatureCount(ConditioningVariables feature) {
		Integer oldFreq = this.featureFreqs.get(feature);
		if(oldFreq==null) {
			oldFreq = 0;
		}
		this.featureFreqs.put(feature, oldFreq+1);
	}

}

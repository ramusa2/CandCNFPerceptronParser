package perceptron.parser.training;

import illinoisParser.Grammar;
import illinoisParser.Sentence;

import java.io.File;
import java.util.Collection;

import perceptron.core.representations.parsing.PackedFeatureForest;
import perceptron.parser.PerceptronChart;
import perceptron.parser.PerceptronParser;
import perceptron.parser.SupertaggedTrainingData;

import supertagger.lewissteedman.LSSupertagger;
import util.CoarseParseGenerator;
import util.serialization.CompressionType;
import util.serialization.SerializedData;
import util.serialization.SerializedFSTDiskData;

public class PPTrainingFold {

	private static CompressionType COMPRESSION_TYPE = CompressionType.LZ4;

	private static String SENTENCE_FILE_NAME = "sentences.auto.gz";
	private static String MULTITAGGED_FILE_NAME = "sentences.multitagged.txt";
	private static String COARSEPARSE_DIR_NAME = "coarse_parses";
	private static String EXTRACTION_DIR_NAME = "feature_extraction";
	private static String INTER_FORESTS_DIR_NAME = "intermediate_fine_parses";
	private static String INTER_PARSER_DIR_NAME = "intermediate_parser";
	private static String INTER_PARSER_FILE_NAME = "parser";

	private File directory;	

	public PPTrainingFold(File dir) {
		this.directory = dir;
	}

	public void setSentences(Collection<Sentence> sens) {
		Sentence.writeToGZIPFile(
				new File(this.directory.getPath()+File.separator+SENTENCE_FILE_NAME), sens);
	}

	public void multitag(LSSupertagger supertagger, double beta) {
		Collection<Sentence> sentences = Sentence.readFromGZIPFile(
				new File(this.directory.getPath()+File.separator+SENTENCE_FILE_NAME));
		SupertaggedTrainingData data = new SupertaggedTrainingData();
		for(Sentence sen : sentences) {
			data.addSentence(sen, supertagger.tagSentence(sen, beta));			
		}
		data.save(this.directory.getPath()+File.separator+MULTITAGGED_FILE_NAME);
	}

	public void coarseParse(Grammar grammar) {
		File cpDir = new File(this.directory.getPath()+File.separator+COARSEPARSE_DIR_NAME);
		cpDir.mkdir();
		@SuppressWarnings("unchecked")
		SerializedData<PerceptronChart> parses = (SerializedData<PerceptronChart>) SerializedFSTDiskData.createNew(
				cpDir, COMPRESSION_TYPE);
		SupertaggedTrainingData data = this.getSupertaggedSentences();
		CoarseParseGenerator.generateCoarseParses(data, grammar, parses);
	}

	public void extractFeatures(PerceptronParser intermediateParser) {
		// Create file structure
		File feDir = new File(this.getFeatureExtractionDirPath());
		feDir.mkdir();
		for(File child : feDir.listFiles()) {
			child.delete();
		}
		File interParserDir = new File(this.getIntermediateParserDirPath());
		interParserDir.mkdirs();
		File interForestDir = new File(this.getIntermediateForestsDirPath());
		interForestDir.mkdirs();

		// Extract features from coarse parses, and save (intermediate) packed feature forests
		@SuppressWarnings("unchecked")
		SerializedData<PackedFeatureForest> intermediateForests = 
				(SerializedData<PackedFeatureForest>) SerializedFSTDiskData.createNew(interForestDir, COMPRESSION_TYPE);
		SerializedData<PerceptronChart> coarseForests = this.getCoarseForests();

		PPFeatureExtractor extractor = new PPFeatureExtractor(feDir, intermediateParser, coarseForests,
				intermediateForests);
		extractor.extractFeaturesAndBuildFeatureForests();

		// Save intermediate parser to disk
		(new File(interParserDir.getPath())).mkdir();
		intermediateParser.setSaveDirectory(interParserDir.getPath());
		intermediateParser.save(INTER_PARSER_FILE_NAME);
	}

	private String getIntermediateForestsDirPath() {
		return this.getFeatureExtractionDirPath()+File.separator+INTER_FORESTS_DIR_NAME;
	}

	private String getIntermediateParserDirPath() {
		return this.getFeatureExtractionDirPath()+File.separator+INTER_PARSER_DIR_NAME;
	}

	private String getFeatureExtractionDirPath() {
		return this.directory.getPath()+File.separator+EXTRACTION_DIR_NAME;
	}

	public PerceptronParser getIntermediateParser() {
		File dir = this.getParserDir();
		String filename = getParserFilepath();
		return PerceptronParser.loadIntermediateParser(dir, filename, false);
	}

	public void pruneFeaturesFromForests(PerceptronParser finalParser,
			SerializedData<PackedFeatureForest> finalForests) {
		@SuppressWarnings("unchecked")
		SerializedData<PackedFeatureForest> intermediateForests = 
				(SerializedData<PackedFeatureForest>) SerializedFSTDiskData.useExisting(new File(this.getIntermediateForestsDirPath()), COMPRESSION_TYPE);
		PPFeatureExtractor extractor = this.getFeatureExtractor();
		extractor.pruneFeatureFromForests(finalParser, intermediateForests, finalForests);
	}

	public PPFeatureExtractor getFeatureExtractor() {
		return new PPFeatureExtractor(this.getFeatureExtractionDir());
	}

	public boolean successfullyBuiltIntermediateForests() {
		String parserFilePath = getParserFilepath();
		File parserFile = new File(parserFilePath);
		return parserFile.exists();
	}

	private File getParserDir() {
		return new File(this.directory + File.separator + EXTRACTION_DIR_NAME + File.separator
				+ INTER_PARSER_DIR_NAME);
	}



	private File getFeatureExtractionDir() {
		return new File(this.directory + File.separator + EXTRACTION_DIR_NAME);
	}

	private String getParserFilepath() {
		return this.getParserDir().getPath() + File.separator + INTER_PARSER_FILE_NAME;
	}

	public SupertaggedTrainingData getSupertaggedSentences() {

		File cpDir = new File(this.directory.getPath()+File.separator+COARSEPARSE_DIR_NAME);
		cpDir.mkdir();
		for(File child : cpDir.listFiles()) {
			child.delete();
		}
		return SupertaggedTrainingData.load(
				this.directory.getPath()+File.separator+MULTITAGGED_FILE_NAME);
	}

	@SuppressWarnings("unchecked")
	public SerializedData<PerceptronChart> getCoarseForests() {
		File cpDir = new File(this.directory.getPath()+File.separator+COARSEPARSE_DIR_NAME);
		return (SerializedData<PerceptronChart>) SerializedFSTDiskData.useExisting(cpDir, COMPRESSION_TYPE);
	}

	public void buildTrainingForests(PerceptronParser parser,
			SerializedFSTDiskData<PackedFeatureForest> finalForests) {
		SerializedData<PerceptronChart> coarseForests = this.getCoarseForests();
		PerceptronChart chart;
		while((chart=coarseForests.next())!= null) {
			if(chart.successfulCoarseParse()) {
				chart.grammar = parser.grammar();
				chart.fineParseWithPerceptronModel(parser, false);
				PackedFeatureForest pruned = new PackedFeatureForest(chart, parser, false);
				finalForests.addObject(pruned);
			}

			// TODO: we need to either:
			//	a) construct a packed feature forest using only the features in parser
			//	b) construct an unpruned feature forest, then prune it using the features in parse
		}
		coarseForests.reset();
	}

}

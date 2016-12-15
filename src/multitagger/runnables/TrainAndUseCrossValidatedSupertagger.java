package multitagger.runnables;

import illinoisParser.CCGbankReader;
import illinoisParser.Sentence;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

import supertagger.lewissteedman.LSSupertagger;
import supertagger.lewissteedman.WordEmbeddings;
import supertagger.lsbeta.MultitaggerTrainingData;

public class TrainAndUseCrossValidatedSupertagger {

	private static final String AUTO_DIR = "data/CCGbank/AUTO/";

	private static File outputDir;

	private static String categoryFile;

	private static int[] omitSecs;

	private static int LOW = 2;

	private static int HIGH = 21;

	public static void main(String[] args) throws Exception {		
		System.out.println("Make sure CCGbank data is at data/CCGbank/");
		if(args.length <2) {
			System.out.println("Requires at least two arguments: "
					+"\n  1) output directory"
					+"\n  2) category file"
					+"\n  3...) indices of sections to omit from 2-21");
			return;
		}
		outputDir = new File(args[0]);
		outputDir.mkdirs();
		System.out.println("Output directory: "+args[0]);
		categoryFile = args[1];
		System.out.println("Category file: "+categoryFile);
		System.out.println("Omitting sections: ");
		omitSecs = new int[args.length-2];
		for(int i=2; i<args.length; i++) {
			omitSecs[i-2] = Integer.parseInt(args[i]);
			System.out.print(" "+args[i]);
		}
		System.out.println();
		Collection<Sentence> data = CCGbankReader.getCCGbankDataOmittingSections(LOW, HIGH, AUTO_DIR, omitSecs);


		int startIter = 0;

		// Context window 	(numbers of words to consider when tagging "center" word,
		//                 	i.e. context window is word-to-tag and the |(CW-1)/2| words 
		//					to the left and right of that word):
		int CW = 7;
		// Number of features each word will map to (embedding features + suffic/capitalization features, for instance)
		int Fs = 60;
		// Dimension of the pre-trained word embeddings:
		int WE = 50;
		LSSupertagger net = null;
		try {
			File[] pastIters = outputDir.listFiles();
			for(File f : pastIters) {
				if(f.isDirectory()) {
					Integer i = Integer.parseInt(f.getName().split("=")[1]);
					startIter = Math.max(startIter, i);
				}			
			}
			for(File f : pastIters) {
				if(f.isDirectory()) {
					Integer i = Integer.parseInt(f.getName().split("=")[1]);
					if(i == startIter) {
						net = LSSupertagger.load(f);
					}
				}			
			}
		}
		catch(Exception e) {
			System.out.println("No previous iterations to load.");
		}
		if(net == null) {
			System.out.println("Starting from iteration 1.");
			WordEmbeddings embeddings = new WordEmbeddings(50, false);
			System.out.println("Reading 50-dimensional embeddings from embeddings/turian/embeddings-scaled.EMBEDDING_SIZE=50.txt");
			embeddings.loadEmbeddingsFromFile(new File("embeddings/turian/embeddings-scaled.EMBEDDING_SIZE=50.txt"));
			System.out.println("Loaded embeddings.");
			net = new LSSupertagger(CW, Fs, WE, getCatList());
			net.initializeLookupFeatures(embeddings);
			
		}



		System.out.println("Initialized net with a context window of "+CW+", "+Fs+
				" features per context word, and "+WE+"-dimensional word embeddings.");
		double learningRate = 0.01;
		int T = 15;
		System.out.println("Starting training for "+T+" iterations with learning rate of "+learningRate);
		net.train(data, T, learningRate, outputDir.getAbsolutePath(), startIter);
		System.out.println("Finished training.");
		System.out.println("Saving file to "+outputDir);
		net.save(outputDir);
		System.out.println("Done.");

		System.out.println("Tagging omitted sections.");
		Collection<Sentence> newData = CCGbankReader.getCCGbankData(omitSecs, AUTO_DIR);
		int topK = 20;
		String dir = "multitagger_training_data";
		(new File(dir)).mkdirs();
		String omit = "omit";
		if(omitSecs.length > 0) {
			omit += omitSecs[0];
		}
		for(int o=1; o<omitSecs.length; o++) {
			omit+=","+omitSecs[o];
		}
		String file = dir+File.separator+""+omit+".ser";
		File trainingFile = new File(file);
		System.out.println("Saving top k="+topK+" tags to "+file);
		MultitaggerTrainingData trainingData2 = new MultitaggerTrainingData(net, newData, topK);
		trainingData2.saveToFile(trainingFile);
	}


	private static ArrayList<String> getCatList() throws Exception {
		Scanner sc = new Scanner(new File(categoryFile));
		ArrayList<String> cats = new ArrayList<String>();
		while(sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			if(!line.isEmpty()) {
				cats.add(line);
			}
		}
		sc.close();
		return cats;
	}

}

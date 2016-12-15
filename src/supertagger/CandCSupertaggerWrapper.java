package supertagger;

import illinoisParser.LexicalToken;
import illinoisParser.Sentence;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;


/**
 * This class interfaces with the (official) C&C supertagger binaries in candc-1.00/bin
 * 
 * @author ramusa2
 *
 */
public class CandCSupertaggerWrapper {

	/**
	 * The supertagger object
	 */
	private static CandCSupertaggerWrapper tagger;

	/**
	 * The directory storing the executables for the C&C tagger
	 */
	@SuppressWarnings("unused")
	private final String binDir;

	/**
	 * Executable for training a C&C supertagger
	 */
	private final String trainBin;

	/**
	 * Executable for running a C&C supertagger
	 */
	private final String superBin;

	/**
	 * Executable for running a C&C multitagger
	 */
	private final String multiBin;

	/**
	 * Address of directory for data files
	 */
	private final String dataDir;

	/**
	 * Address of directory for scratch files
	 */
	private final String scratchDir;

	/**
	 * Address of directory for saved model parameters
	 */
	private final String modelDir;

	private CandCSupertaggerWrapper(String binDirectory, String dataDirectory, String scratchDirectory,
			String superBinary, String multiBinary, String trainBinary, String modelDirectory, boolean trained) {
		binDir = binDirectory;
		superBin = superBinary;
		multiBin = multiBinary;
		trainBin = trainBinary;
		dataDir = dataDirectory;
		scratchDir = scratchDirectory;
		modelDir = modelDirectory;
	}

	/**
	 * 
	 */
	private CandCSupertaggerWrapper(String binDirectory, String dataDirectory, String scratchDirectory,
			String superBinary, String multiBinary, String trainBinary) {
		binDir = binDirectory;
		superBin = superBinary;
		multiBin = superBinary;
		trainBin = trainBinary;
		dataDir = dataDirectory;
		scratchDir = scratchDirectory;
		modelDir = getModelDir();
	}

	/**
	 * Generates and returns a new model directory 
	 * (scratch/model0/, scratch/model1/, scratch/model2/, ...)
	 */
	private String getModelDir() {
		File scrDir = new File(scratchDir);
		File[] existing = scrDir.listFiles();
		int m = 0;
		for(File md : existing) {
			if(md.getName().matches("model[0-9]+")) {
				int old = Integer.parseInt(md.getName().substring(6));
				m = Math.max(m, old+1);
			}
		}
		String name = this.scratchDir+"model"+m;
		(new File(name)).mkdirs();
		return name;
	}

	/**
	 * Returns a tagger interface with the default directories
	 * @return
	 */
	public static CandCSupertaggerWrapper getTagger() {
		String binDirectory = "candc-1.00/bin/";
		String superBinary = binDirectory+"super";
		String multiBinary = binDirectory+"msuper";
		String trainBinary = binDirectory+"train";
		String dataDirectory = "candc-1.00/data/";
		String scratchDirectory = "candc-1.00/scratch/";
		String modelDirectory = scratchDirectory+"default_wsj";
		return new CandCSupertaggerWrapper(binDirectory, dataDirectory, scratchDirectory, 
				superBinary, multiBinary, trainBinary, modelDirectory, true);
		//return new CandCSupertaggerWrapper(binDirectory, dataDirectory, scratchDirectory, 
		//		superBinary, multiBinary, trainBinary);
	}

	/**
	 * If tagger has been initialized, return it; else initialize it first.
	 */
	private static CandCSupertaggerWrapper tagger() {
		if(tagger == null) {
			tagger = getTagger();
		}
		return tagger;
	}

	/**
	 * Trains the tagger
	 */
	@SuppressWarnings("unused")
	private static void train(String trainingData) {
		CandCSupertaggerWrapper st = tagger();
		String trainData = st.dataDir+"train.txt";
		String format = "\"%w|%p|%s \n\"";
		run(st.trainBin
				+" --comment \"A trained model for the C&C supertagger\""
				+" --input "+trainData
				+" --ifmt "+format
				+" --model "+st.modelDir);
	}

	/**
	 * Translates a C&C viterbi string into a LexicalCategoryAssignment for use in the parser
	 */
	public static SupertagAssignment viterbi(String toTag) {
		String[] tagged = viterbiAsString(toTag).trim().split("\\s+");
		Sentence sen = stringToSentence(toTag);
		SupertagAssignment lexcats = new SupertagAssignment(sen);
		int i=0;
		for(String tok : tagged) {
			String cat = tok.split("\\|")[2];
			lexcats.addLexcat(i, cat, 1.0);
			i++;
		}
		return lexcats;
	}

	/**
	 * Runs the tagger in viterbi mode and returns a multitag assignment
	 */
	public static SupertagAssignment viterbi(Sentence sen) {
		return viterbi(sentenceToString(sen));
	}

	/**
	 * Runs the tagger in Viterbi mode, and returns the input String with tags
	 */
	private static String viterbiAsString(String toTag) {
		CandCSupertaggerWrapper st = tagger();
		String file = writeToRandomFile(st.scratchDir, toTag);
		String outFile = getRandomFile(st.scratchDir);
		boolean success = run(st.superBin+" --model "+st.modelDir+" --input "+file+" --output "+outFile);
		String output = "";
		if(success) {
			output = readFile(outFile);
		}
		deleteFile(file);
		deleteFile(outFile);
		return output;

	}
	
	/**
	 * Runs the tagger in multitag mode with default beta=0.1, and returns a multitag assignment
	 */
	public static SupertagAssignment multi(String toTag) {
		return multi(toTag, 0.1);
	}

	/**
	 * Translates a C&C multitag string into a LexicalCategoryAssignment for use in the parser
	 */
	public static SupertagAssignment multi(String toTag, double beta) {
		String[] tagged = multiAsString(toTag, beta).trim().split("\\n");
		Sentence sen = stringToSentence(toTag);
		SupertagAssignment lexcats = new SupertagAssignment(sen);
		int i=0;
		for(String entry : tagged) {
			String[] toks = entry.split("\\s+");
			for(int j=3; j<toks.length; j+=2) {
				String cat = toks[j];
				double prob = Double.parseDouble(toks[j+1]);
				lexcats.addLexcat(i, cat, prob);
			}
			i++;
		}
		return lexcats;
	}

	/**
	 * Runs the tagger in multitag mode with default beta=0.1, and returns a multitag assignment
	 */
	public static SupertagAssignment multi(Sentence sen) {
		return multi(sentenceToString(sen), 0.1);
	}

	/**
	 * Runs the tagger in multitag mode with parameter beta, and returns a multitag assignment
	 */
	public static SupertagAssignment multi(Sentence sen, double beta) {
		return multi(sentenceToString(sen), beta);
	}

	/**
	 * Runs the tagger in multitag mode with default beta=0.1, and returns the input string with the tags and their probabilities
	 */
	private static String multiAsString(String toTag) {
		return multiAsString(toTag, 0.1);
	}

	/**
	 * Runs the tagger in multitag mode, and returns the input string with the tags and their probabilities
	 */
	private static String multiAsString(String toTag, double beta) {
		CandCSupertaggerWrapper st = tagger();
		String file = writeToRandomFile(st.scratchDir, toTag);
		String outFile = getRandomFile(st.scratchDir);
		boolean success = run(st.multiBin+" --model "+st.modelDir+" --beta "+beta+" --input "+file+" --output "+outFile);
		String output = "";
		if(success) {
			output = readFile(outFile);
		}
		deleteFile(file);
		deleteFile(outFile);
		return output;
	}

	/**
	 * Takes a C&C word|pos tokenized string and returns a Sentence object
	 */
	private static Sentence stringToSentence(String str) {
		Sentence sen = new Sentence();
		String[] toks = str.trim().split("\\s+");
		for(String tok : toks) {
			String[] wordPOS = tok.split("\\|");
			sen.addLexicalItem(new LexicalToken(wordPOS[0], wordPOS[1]));
		}
		return sen;
	}

	/**
	 * Takes a Sentence object and returns a C&C word|pos tokenized string
	 */
	private static String sentenceToString(Sentence sen) {
		String str = "";
		for(LexicalToken lt : sen.getTokens()) {
			str += lt.getWord()+"|"+lt.getPOS()+" ";
		}
		return str.trim();
	}

	private static String readFile(String file) {
		Scanner sc;
		String str = "";
		try {
			sc = new Scanner(new File(file));
			while(sc.hasNextLine()) {
				if(str.isEmpty()) {
					str += sc.nextLine().trim();
				}
				else {
					str += "\n"+sc.nextLine().trim();
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return str;
	}

	private static void deleteFile(String file) {
		File f = new File(file);
		f.delete();
	}

	private static String writeToRandomFile(String prefix, String... lines) {
		String file = getRandomFile(prefix);
		PrintWriter pw;
		try {
			pw = new PrintWriter(new File(file));
			for(String line : lines) {
				pw.println(line);
			}
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return file;
	}

	private static String getRandomFile(String prefix) {
		int length = 8;
		String file = "";
		while(file.length() < length) {
			int rand = (int) (Math.random()*26); 
			file += (char) (rand+97);
		}
		return prefix+file+".txt";
	}

	/**
	 * Runs a command; returns true iff command executed successfully
	 */
	private static boolean run(String command) {
		try {
			Process p = Runtime.getRuntime().exec(command);
			p.waitFor();
			return true;
		} catch (IOException e) {
			System.out.println("Failed to read/write: "+command);
			e.printStackTrace();
		}catch (InterruptedException e) {
			System.out.println("Command interrupted: "+command);
			e.printStackTrace();
		}
		return false;
	}

	public static void main(String[] args) {
		System.out.println(CandCSupertaggerWrapper.viterbiAsString("The|DT boy|NN ran|VB"));
		CandCSupertaggerWrapper.viterbi("The|DT boy|NN ran|VB");
		System.out.println(CandCSupertaggerWrapper.multiAsString("The|DT boy|NN ran|VB"));
		CandCSupertaggerWrapper.multi("The|DT boy|NN ran|VB");
	}

}

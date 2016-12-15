package illinoisParser;


import illinoisParser.CCGcat.ConjStyle;
import illinoisParser.TAGSET.TAG_TYPE;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Arrays;

/**
 * @author bisk1 Set of configuration variables read from mandatory Config.txt
 *         argument at runtime
 */
public strictfp final class Configuration implements Serializable {
  private static final long serialVersionUID = 5162012L;
  /**
   * Includes/Ignores punctuation from data
   */
  public static boolean ignorePunctuation = false;

  // // GRAMMAR ////
  /**
   * POS tag file location
   */
  static String TAG_SET = "data/Languages/POS/english.txt";
  /**
   * Parse with {Coarse,Fine,Universal} tags
   */
  static TAG_TYPE tagType = TAG_TYPE.Fine;
  /**
   * Add column for NAACL Shared Task input/output
   */
  static boolean hasUniversalTags = true;
  /**
   * Parse with NF: {Full,Full_noPunct,Eisner,Eisner_Orig,None}
   */
  public static Normal_Form NF = Normal_Form.None; // Full, Full_noPunct, Eisner,
  // Eisner_Orig, None
  /**
   * Allow TypeRaising
   */
  static boolean typeRaising = true;
  /**
   * Restrict TypeRaising to lexical items
   */
  boolean lexTROnly = false;
  /**
   * Treat all X|X as modifiers
   */
  public static boolean modXbX = false;

  // // Grammar Induction ////
  /**
   * EM init w/ Uniform Trees
   */
  public static boolean uniformPrior = false;
  /**
   * Maximum lexical arity
   */
  static int maxArity = 3;
  /**
   * Maximum lexical arity for Modifiers
   */
  static int maxModArity = 2;
  static boolean induceValidOnly = true;
  /**
   * Allow verbs to be modifiers
   */
  static boolean modVerb = true; // Can verb be modifier
  /**
   * Allow S to be lexical
   */
  static boolean lexicalS = true; // Can S -> LEX
  /**
   * Restrict categories with English knowledge
   */
  boolean useEnglishKnowledge = false; // Non-English categories (S\S)\N and
  // (S\N)\N
  /**
   * Allow complex arguments
   */
  public static boolean complexArgs = false;
  /**
   * Allow TOP to complex arguments
   */
  public static boolean complexTOP = false;
  /**
   * Raise variational hyperparameter to n
   */
  static double[] alphaPower = new double[] { 0.0, 0.0, 0.0 };
  /**
   * PY Discount factor 0 <= d < 1
   */
  static double discount = 0.5; // PY Discount factor
  /**
   * Log form of PY Discount
   */
  static double logDiscount = Math.log(discount); // PY Discount factor
  // // Tag Set Induction ////
  /**
   * Restrict HMM to upper triangular
   */
  static boolean Diagonal = false;
  /**
   * Specify # of hidden states
   */
  int TagSetSize = 10;

  // // TRAINING ////
  String modelFile = "";
  /**
   * Hard vs Soft EM
   */
  static boolean viterbi = false; // Hard vs Soft EM
  boolean viterbiOnSwap = false; // Should first iter after swap be vierbi
  boolean viterbiAfterSwap = false; // Should remainder of training be viterbi
  private int[] leapFrog = new int[0];
  /**
   * Max # of EM/BW Iterations
   */
  static int maxItr = 2000; // Maximum iterations default:2000
  /**
   * EM/BW convergence threshold
   */
  static double threshold = 0.001; // EM/SGD Convergence threshold
  /**
   * Used instead of zeros for small values
   */
  final static double EPSILON = -20;
  boolean merged = true;

  // // TRAINING MODEL ////
  private int swapPoint = -1; // When to move to complex model
  /**
   * TopK parses to be computed during training
   */
  public static int trainK = 1;
  /**
   * Minimum prob/value allowed for a rule
   */
  static double smallRule = -25;
  /**
   * When interpolating models, lambda cannot grow larger than 1-smallRule
   */
  static double largeRule;
  private static double nonLogLambda = 0.5;
  /**
   * Logspace lambda (topK upweight)
   */
  static double lambda = Math.log(nonLogLambda);
  /**
   * One Minus lambda (topK upweight)
   */
  static double OneMinusLambda = Math.log(1 - nonLogLambda);

  // // TRAINING DATA ////
  /**
   * Training file(s), comma delimited
   */
  String[][] trainFile = new String[][] {{"data/Corpus.PTB"}};
  /** Current subset of the training files being used */
  static int training_subset = 0;
  private boolean lowerCaseWords = false;
  /**
   * Shortest sentence to consider
   */
  static int shortestSentence = 1;
  /**
   * Longest sentence to consider
   */
  public static int longestSentence = 10;

  // // SAVE ////
  /**
   * Folder for output files
   */
  public static String Folder = "";
  /**
   * Files to read model from
   */
  static String loadModelFile = "Model.gz";
  /**  Load a lexicon   */
  static String savedLexicon = "Lexicon.txt.gz";
  /** Threshold for discarding lexical categories */
  static double KL_threshold = 0.01;
  /** Threshold for discarding categories from conditional distribution */
  static double CondProb_threshold = 0.01;

  // // DEBUG ////
  /**
   * Debug mode
   */
  public static boolean DEBUG = true;

  // // CCGBank ////
  /**
   * if true, read in counts from CCGbank without smoothing
   */
  public static boolean supervisedTraining = false;
  /**
   * directory containing gzipped CCGbank AUTO (parse) files
   */
  public static String autoDir = "";
  /**
   * directory containing gzipped CCGbank PARG (dependency) files
   */
  public static String pargDir = "";
  /**
   * file name of the various Discourse-mode result files
   */
  static String ccgResults = "";
  /**
   * use a beam search during fine parsing?
   */
  public boolean useBeam = false;
  /**
   * the beam factor non-Viterbi ChartItems must exceed to be retained
   */
  public double logBeamWidth = Math.log(1.0/10000.0);
  /**
   * number of TIMES a word must occur in the training corpus to be seen
   */
  public static int freqCutoff = 30;
  /**
   * CCGbank section we begin building folds over
   */
  int lowFoldSec = 2;
  /**
   * CCGbank section we finish building folds over
   */
  int highFoldSec = 21;
  /**
   * CCGbank section we begin training on
   */
  public static int lowTrainSec = 2;
  /**
   * CCGbank section we finish training on
   */
  public static int highTrainSec = 21;
  /**
   * CCGbank section we test on
   */
  public static int testSec = 23;
  /**
   * condition all words on their POS tag (not just unseen words)?
   */
  static boolean useLexTag = false;
  /**
   * train and use a lexical-category tagger?
   */
  static boolean useSupertagger = false;
  /**
   * only add the gold supertags to the lexical cells?
   */
  public static boolean useGoldSupertags = false;

  // / State-Splitting ///
  public static double alpha = 0.01;

  // //Reranker ////
  /**
   * reranker input directory, containing section directories (00, 02, 03 ...
   * 23) with .test, .parg, .auto, .ccg, and .parse files
   */
  public static String rerankerInputDir = "CandCgold_max20";
  /**
   * the identifier for the .ccg, .parg, and .parse files
   */
  public static String rerankerInputName = "lexcat";
  /**
   * the number of candidate parses to consider per sentence
   */
  public static int rerankerK = 500;
  /**
   * the number of iterations through the data to train all features
   */
  public static int rerankerIterCount1 = 1;
  /**
   * the number of iterations to train over only filtered features
   */
  public static int rerankerIterCount2 = 3;
  /**
   * the number of sentences we read-in ahead of time
   */
  public static int rerankerMaxQueueSize = 50;
  /**
   * the percentage of seen features that we retain, if filtering
   */
  public static double rerankerFeatureCountLimit = 0.5;
  /**
   * if true, only keep important features
   */
  public static boolean rerankerFilterFeatures = true;
  /**
   * if true, identify important features by the numer of updates they
   * participate in
   */
  public static boolean rerankerFilterFeaturesByUpdates = true;
  /**
   * if true, average the learned weight vector over the course of training
   */
  public static boolean rerankerUseAveragedPerceptron = true;
  /**
   * if true, allow the reranker to select gold-standard candidates during
   * training
   */
  public static boolean rerankerUseGS = false;

  // // Lexical Learning ////
  /**
   * # or percentage of words to learn
   */
  double lexFreq = 0;
  /**
   * # or percentage of nouns to learn
   */
  double nounFreq = 0;
  /**
   * # or percentage of verbs to learn
   */
  double verbFreq = 0;
  /**
   * # or percentage of function words to learn
   */
  double funcFreq = 0;

  // // TESTING ////
  /**
   * List of test files ( comma delimited )
   */
  String[] testFile = new String[] { "data/CCGbank.00.PTB" };
  static boolean CCGcat_CoNLL = false;
  /**
   * how to print conjunction dependencies
   */
  static ConjStyle CONJSTYLE = ConjStyle.CC_X1___CC_X2;
  public static boolean CCGcat_CCG = true;
  static boolean PRINT_SYNTACTIC_PARSES = true;
  boolean evaluate = false;
  /**
   * Longest allowable test sentence (else: right branch)
   */
  public static int longestTestSentence = 25;
  static int testK = 1;

  // // SYSTEM ////
  /**
   * Max allowable threads for parallelization
   */
  public static int threadCount = 100;
  /**
   * Whether logged values should also be sent to stdout
   */
  public static boolean printToScreen = false;

  /**
   * Default constructor
   */
  public Configuration() {}

  /**
   * Use command line arguments var=val to set parameters
   * 
   * @param cmdLine
   * @throws Exception
   */
  Configuration(String[] cmdLine) throws Exception {
    this(cmdLine[0]);
    if (cmdLine.length > 1) {
      String[] split;
      for (String arg : cmdLine) {
        split = arg.split("=", 2);
        if (split.length == 2) {
          // Util.logln(String.format("%-20s          %-50s\n", split[0],
          // split[1]));
          process(split[0], split[1]);
        }
      }
    }
  }

  /**
   * Read parameters from file. Ignore comments (#)
   * 
   * @param file
   */
  public Configuration(String file) {
    try {
      // Read in Documents
      FileInputStream fstream = new FileInputStream(file);
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      String[] args;
      while ((strLine = br.readLine()) != null) {
        if (!strLine.isEmpty()
            && (strLine.length() == 0 || strLine.charAt(0) != '#')) {
          args = strLine.split("#")[0].trim().split("\\s+", 2);
          if (args.length > 1 && !args[0].equals("#") && !args[1].equals("#")) {
            // Util.logln(String.format("%-20s          %-50s\n", args[0],
            // args[1]));
            process(args[0], args[1]);
          }
        }
      }
      br.close();
      largeRule = Log.subtract(Log.ONE, smallRule);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Configuration malformed");
      System.exit(1);
    }
  }

  /**
   * Assign value (val) to variable (v)
   * 
   * @param v
   * @param val
   * @throws Exception
   */
  private void process(String v, String val) throws Exception {
    String var = v.toLowerCase();
    if (var.equals("threshold")) {
      threshold = Double.parseDouble(val);
    } else if (var.equals("debug")) {
      DEBUG = Boolean.parseBoolean(val);
    } else if (var.equals("viterbi")) {
      viterbi = Boolean.parseBoolean(val);
    } else if (var.equals("viterbionswap")) {
      this.viterbiOnSwap = Boolean.parseBoolean(val);
    } else if (var.equals("viterbiafterswap")) {
      this.viterbiAfterSwap = Boolean.parseBoolean(val);
    } else if (var.equals("trainfile")) {
      String[] chunks = val.split("\\|");
      this.trainFile = new String[chunks.length][];
      for(int i = 0; i < chunks.length; ++i) {
        this.trainFile[i] = chunks[i].split(",");
      }
    } else if (var.equals("testfile")) {
      this.testFile = val.split(",");
    } else if (var.equals("maxitr")) {
      maxItr = Integer.parseInt(val);
    } else if (var.equals("nf")) {
      NF = Normal_Form.valueOf(val);
    } else if (var.equals("evaluate")) {
      this.evaluate = Boolean.parseBoolean(val);
    } else if (var.equals("lexfreq")) {
      this.lexFreq = Double.parseDouble(val);
    } else if (var.equals("folder")) {
      Folder = val;
    } else if (var.equals("loadmodelfile")) {
      loadModelFile = val;
    } else if (var.equals("savedlexicon")) {
      savedLexicon = val;
    } else if (var.equals("kl_threshold")) {
      KL_threshold = Double.valueOf(val);
    } else if (var.equals("condprob_threshold")) {
      CondProb_threshold = Double.valueOf(val);
    } else if (var.equals("tag_set")) {
      TAG_SET = val;
    } else if (var.equals("shortestsentence")) {
      shortestSentence = Integer.parseInt(val);
    } else if (var.equals("typeraising")) {
      typeRaising = Boolean.parseBoolean(val);
    } else if (var.equals("traink")) {
      trainK = Integer.parseInt(val);
    } else if (var.equals("testk")) {
      testK = Integer.parseInt(val);
    } else if (var.equals("threadcount")) {
      threadCount = Math.min(Integer.parseInt(val),
          Runtime.getRuntime().availableProcessors());
    } else if (var.equals("autodir")) {
      autoDir = val;
    } else if (var.equals("pargdir")) {
      pargDir = val;
    } else if (var.equals("ccgresults")) {
      ccgResults = val;
    } else if (var.equals("ignorepunctuation")) {
      ignorePunctuation = Boolean.parseBoolean(val);
    } else if (var.equals("supervisedtraining")) {
      supervisedTraining = Boolean.parseBoolean(val);
    } else if (var.equals("uselextag")) {
      useLexTag = Boolean.parseBoolean(val);
    } else if (var.equals("usesupertagger")) {
      useSupertagger = Boolean.parseBoolean(val);
    } else if (var.equals("usegoldsupertags")) {
      useGoldSupertags = Boolean.parseBoolean(val);
    } else if (var.equals("inducevalidonly")) {
      induceValidOnly = Boolean.parseBoolean(val);
    } else if (var.equals("swappoint")) {
      this.swapPoint = Integer.parseInt(val);
    } else if (var.equals("longestsentence")) {
      longestSentence = Integer.parseInt(val);
    } else if (var.equals("modverb")) {
      modVerb = Boolean.parseBoolean(val);
    } else if (var.equals("ccgcat_conll")) {
      CCGcat_CoNLL = Boolean.valueOf(val);
    } else if (var.equals("ccgcat_ccg")) {
      CCGcat_CCG = Boolean.valueOf(val);
    } else if (var.equals("print_syntactic_parses")) {
      PRINT_SYNTACTIC_PARSES = Boolean.valueOf(val);
    } else if (var.equals("lexicals")) {
      lexicalS = Boolean.parseBoolean(val);
    } else if (var.equals("nounfreq")) {
      this.nounFreq = Double.parseDouble(val);
    } else if (var.equals("verbfreq")) {
      this.verbFreq = Double.parseDouble(val);
    } else if (var.equals("funcfreq")) {
      this.funcFreq = Double.parseDouble(val);
    } else if (var.equals("lowercasewords")) {
      this.lowerCaseWords = Boolean.parseBoolean(val);
    } else if (var.equals("lextronly")) {
      this.lexTROnly = Boolean.parseBoolean(val);
    } else if (var.equals("useenglishknowledge")) {
      this.useEnglishKnowledge = Boolean.parseBoolean(val);
    } else if (var.equals("modxbx")) {
      modXbX = Boolean.parseBoolean(val);
    } else if (var.equals("usebeam")) {
      this.useBeam = Boolean.parseBoolean(val);
    } else if (var.equals("beamwidth")) {
      this.logBeamWidth = Double.parseDouble(val);
    } else if (var.equals("alpha")) {
      alpha = Double.parseDouble(val);
    } else if (var.equals("complexargs")) {
      complexArgs = Boolean.parseBoolean(val);
    } else if (var.equals("complextop")) {
      complexTOP = Boolean.parseBoolean(val);
    } else if (var.equals("freqcutoff")) {
      freqCutoff = Integer.parseInt(val);
    } else if (var.equals("lowfoldsec")) {
      this.lowFoldSec = Integer.parseInt(val);
    } else if (var.equals("highfoldsec")) {
      this.highFoldSec = Integer.parseInt(val);
    } else if (var.equals("lowtrainsec")) {
      lowTrainSec = Integer.parseInt(val);
    } else if (var.equals("hightrainsec")) {
      highTrainSec = Integer.parseInt(val);
    } else if (var.equals("testsec")) {
      testSec = Integer.parseInt(val);
    } else if (var.equals("longesttestsentence")) {
      longestTestSentence = Integer.parseInt(val);
    } else if (var.equals("tagtype")) {
      tagType = TAG_TYPE.valueOf(val);
    } else if (var.equals("hasuniversaltags")) {
      hasUniversalTags = Boolean.parseBoolean(val);
    } else if (var.equals("merged")) {
      merged = Boolean.parseBoolean(val);
    } else if (var.equals("modelfile")) {
      modelFile = val;
    } else if (var.equals("diagonal")) {
      Diagonal = Boolean.parseBoolean(val);
    } else if (var.equals("tagsetsize")) {
      TagSetSize = Integer.parseInt(val);
    } else if (var.equals("printtoscreen")) {
      printToScreen = Boolean.parseBoolean(val);
    } else if (var.equals("uniformprior")) {
      uniformPrior = Boolean.parseBoolean(val);
    } else if (var.equals("discount")) {
      discount = Double.parseDouble(val);
      logDiscount = Math.log(discount);
    } else if (var.equals("alphapower")) {
      String[] vals = val.trim().split(",");
      alphaPower = new double[vals.length];
      for (int i = 0; i < vals.length; i++) {
        alphaPower[i] = Double.parseDouble(vals[i]);
      }
    } else if (var.equals("rerankerinputdir")) {
      rerankerInputDir = val;
    } else if (var.equals("rerankerinputname")) {
      rerankerInputName = val;
    } else if (var.equals("rerankerK")) {
      rerankerK = Integer.parseInt(val);
    } else if (var.equals("rerankeritercount1")) {
      rerankerIterCount1 = Integer.parseInt(val);
    } else if (var.equals("rerankeritercount2")) {
      rerankerIterCount2 = Integer.parseInt(val);
    } else if (var.equals("rerankermaxqueuesize")) {
      rerankerMaxQueueSize = Integer.parseInt(val);
    } else if (var.equals("rerankerfeaturecountlimit")) {
      rerankerFeatureCountLimit = Double.parseDouble(val);
    } else if (var.equals("rerankerfilterfeatures")) {
      rerankerFilterFeatures = Boolean.parseBoolean(val);
    } else if (var.equals("rerankerfilterfeaturesbyupdates")) {
      rerankerFilterFeaturesByUpdates = Boolean.parseBoolean(val);
    } else if (var.equals("rerankeruseaveragedperceptron")) {
      rerankerUseAveragedPerceptron = Boolean.parseBoolean(val);
    } else if (var.equals("rerankerusegs")) {
      rerankerUseGS = Boolean.parseBoolean(val);
    } else if (var.equals("maxarity")) {
      maxArity = Integer.parseInt(val);
    } else if (var.equals("maxmodarity")) {
      maxModArity = Integer.parseInt(val);
    } else if (var.equals("conjstyle")) {
      CONJSTYLE = ConjStyle.valueOf(val);
    } else if (var.equals("leapfrog")) {
      String[] vals = val.trim().split(",");
      leapFrog = new int[vals.length];
      for (int i = 0; i < vals.length; i++) {
        leapFrog[i] = Integer.parseInt(vals[i]);
      }
    } else if (var.equals("smallrule")) {
      smallRule = Double.parseDouble(val);
      largeRule = Log.subtract(Log.ONE, smallRule);
    } else {
      if (var.length() > 0 && (var.length() == 0 || var.charAt(0) != '#')) {
        Util.Error("Unknown variable: \t\"" + var
            + "\tConfiguration malformed");
      }
    }
  }

  /**
   * Write all variables to screen and log file
   */
  void print() {
    printConfig("printToScreen", printToScreen,
        "Toggle printing output to screen or only log");
    printConfig("ignorePunctuation", ignorePunctuation,
        "Includes/Ignores punctuation from data");

    Util.logln("----- Grammar -----");
    // // GRAMMAR ////
    printConfig("TAGSET", TAG_SET, "POS tag file location");
    printConfig("tagType", tagType.toString(),
        "Parse with {Coarse,Fine,Universal} tags");
    printConfig("hasUniversalTags", hasUniversalTags,
        "Add column for NAACL Shared Task input/output");
    printConfig("NF", NF.toString(),
        "Parse with NF: {Full,Full_noPunct,Eisner,Eisner_Orig,None}");
    printConfig("typeRaising", typeRaising, "Allow TypeRaising");
    printConfig("lexTROnly", lexTROnly,
        "Restrict TypeRaising to lexical items");
    printConfig("modXbX", modXbX, "Treat all X|X as modifiers");

    Util.logln("----- Grammar Induction -----");
    // // Grammar Induction ////
    printConfig("uniformPrior", uniformPrior, "EM init w/ Uniform Trees");
    printConfig("maxArity", maxArity, "Maximum lexical arity");
    printConfig("maxModArity", maxModArity,
        "Maximum lexical arity for Modifiers");
    printConfig("induceValidOnly", induceValidOnly, "");
    printConfig("modVerb", modVerb, "Allow verbs to be modifiers");
    printConfig("lexicalS", lexicalS, "Allow S to be lexical");
    printConfig("useEnglishKnowledge", useEnglishKnowledge,
        "Restrict categories with English knowledge");
    printConfig("complexArgs", complexArgs, "Allow complex arguments");
    printConfig("complexTOP", complexTOP, "Allow TOP to complex arguments ");
    printConfig("alphaPower", Arrays.toString(alphaPower),
        "Raise variational hyperparameter to n");
    printConfig("discount", discount, "PY Discount factor 0 <= d < 1");

    Util.logln("----- Tagset Induction -----");
    // // TagSet Induction ////
    printConfig("Diagonal", Diagonal, "Restrict HMM to upper triangular");
    printConfig("TagSetSize", TagSetSize, "Specify # of hidden states");

    Util.logln("----- Training -----");
    // // TRAINING ////
    printConfig("modelFile", modelFile, "");
    printConfig("viterbi", viterbi, "");
    printConfig("viterbiOnSwap", viterbiOnSwap, "");
    printConfig("viterbiAfterSwap", viterbiAfterSwap, "");
    printConfig("leapFrog", Arrays.toString(leapFrog), "");
    printConfig("maxItr", maxItr, "Max # of EM/BW Iterations");
    printConfig("threshold", threshold, "EM/BW convergence threshold");
    printConfig("EPSILON", EPSILON, "");
    printConfig("merged", merged, "");
    printConfig("supervisedTraining", supervisedTraining,
        "if true, read in counts from CCGbank without smoothing");

    Util.logln("----- Training Model -----");
    // // TRAINING MODEL ////
    printConfig("swapPoint", swapPoint, "");
    printConfig("trainK", trainK, "TopK parses to be computed during training");
    printConfig("smallRule", smallRule, "Minimum prob/val allowed for a rule");
    printConfig("largeRule", largeRule,
        "When interpolating models, lambda cannot grow larger than " +
        "1-smallRule");
    printConfig("nonLogLambda", nonLogLambda,
        "User's input lambda (topK upweight)");
    printConfig("lambda", lambda, "Logspace lambda (topK upweight)");
    printConfig("OneMinusLambda", OneMinusLambda,
        "One Minus lambda (topK upweight)");

    Util.logln("----- Training Data -----");
    // // TRAINING DATA ////
    String files = "";
    for(String[] arr : trainFile) {
      files += Arrays.toString(arr) + "|";
    }
    printConfig("trainFile", files, "Training file(s), comma delimited ");
    printConfig("lowerCaseWords", lowerCaseWords, "");
    printConfig("shortestSentence", shortestSentence,
        "Shortest sentence to consider");
    printConfig("longestSentence", longestSentence,
        "Longest sentence to consider");

    Util.logln("----- Misc -----");
    // // SAVE ////
    printConfig("Folder", Folder, "Folder for output files");
    printConfig("loadModelFile", loadModelFile, "file to read model from");
    printConfig("savedLexicon", savedLexicon, "Lexicon to load");
    printConfig("KL_threshold", KL_threshold, "Threshold for discarding lexical categories");
    printConfig("CondProb_threshold", CondProb_threshold , "Threshold for discarding categories based on cond prob");
    // // DEBUG ////
    printConfig("DEBUG", DEBUG, "");
    // // SYSTEM ////
    printConfig("threadCount", threadCount, "");

    Util.logln("----- CCGBank -----");
    // // CCGBank ////
    printConfig("autoDir", autoDir,
        "directory containing gzipped CCGbank AUTO (parse) files");
    printConfig("pargDir", pargDir,
        "directory containing gzipped CCGbank PARG (dependency) files");
    printConfig("ccgResults", ccgResults,
        "file name of the various Discourse-mode result files");
    printConfig("useBeam", useBeam,
        "use a beam search during fine parsing?");
    printConfig("beamWidth", logBeamWidth,
        "the beam factor non-Viterbi ChartItems must exceed to be " +
        "retained");
    printConfig("freqCutoff", freqCutoff,
        "number of TIMES a word must occur in the training corpus to " +
        "be seen");
    printConfig("lowFoldSec", lowFoldSec,
        "CCGbank section we begin building folds over");
    printConfig("highFoldSec", highFoldSec,
        "CCGbank section we finish building folds over");
    printConfig("lowTrainSec", lowTrainSec,
        "CCGbank section we begin training on");
    printConfig("highTrainSec", highTrainSec,
        "CCGbank section we finish training on");
    printConfig("TESTSEC", testSec, "CCGbank section we test on");
    printConfig("useLexTag", useLexTag,
        "condition all words on their POS tag(not just unseen words)?");
    printConfig("useSupertagger", useSupertagger,
        "train and use a lexical-category tagger?");
    printConfig("useGoldSupertags", useGoldSupertags,
        "only add the gold supertags to the lexical cells?");

    Util.logln("----- State-Splitting -----");
    // / State-Splitting ///
    printConfig("alpha", alpha,
        "interpolation constant for coarse-grammar backoff");

    Util.logln("----- Reranker -----");
    // // Reranker ////
    printConfig("rerankerInputDir", rerankerInputDir,
        "reranker input directory, containing section directories " +
            "(00, 02, 03 ... 23) with .test, .parg, .auto, .ccg, and " +
        ".parse files");
    printConfig("rerankerInputname", rerankerInputName,
        "the identifier for the .ccg, .parg, and .parse files");
    printConfig("rerankerK", rerankerK,
        "the number of candidate parses to consider per sentence");
    printConfig("rerankerIterCount1", rerankerIterCount1,
        "the number of iterations through the data to train all " +
        "features");
    printConfig("rerankerIterCount2", rerankerIterCount2,
        "the number of iterations to train over only filtered " +
        "features");
    printConfig("rerankerMaxQueueSize", rerankerMaxQueueSize,
        "the number of sentences we read-in ahead of time");
    printConfig("rerankerFeatureCountLimit", rerankerFeatureCountLimit,
        "the percentage of seen features that we retain, if filtering");
    printConfig("rerankerFilterFeatures", rerankerFilterFeatures,
        "if true, only keep important features");
    printConfig("rerankerFilterFeaturesByUpdates",
        rerankerFilterFeaturesByUpdates,
        "if true, identify important features by the numer of " +
        "updates they participate in");
    printConfig("rerankerUseAveragedPerceptron", rerankerUseAveragedPerceptron,
        "if true, average the learned weight vector over the course " +
        "of training");
    printConfig("rerankerUseGS", rerankerUseGS,
        "if true, allow the reranker to select gold-standard " +
        "candidates during training");

    Util.logln("----- Induction's Lexical Learning -----");
    // // Lexical Learning ////
    printConfig("lexFreq", lexFreq, "# or percentage of words to learn");
    printConfig("nounFreq", nounFreq, "# or percentage of nouns to learn");
    printConfig("verbFreq", verbFreq, "# or percentage of verbs to learn");
    printConfig("funcFreq", funcFreq,
        "# or percentage of function words to learn");

    Util.logln("----- Testing -----");
    // // TESTING ////
    printConfig("testFile", Arrays.toString(testFile),
        "List of test files ( comma delimited )");
    printConfig("CCGcat_CoNLL", CCGcat_CoNLL,
        "Whether CoNLL style dependencies should be printed");
    printConfig("CCGcat_CCG", CCGcat_CCG,
        "Whether CCG style dependencies should be printed");
    printConfig("PRINT_SYNTACTIC_PARSES", PRINT_SYNTACTIC_PARSES,
        "Whether viterbi parses should be printed");
    printConfig("evaluate", evaluate, "");
    printConfig("longestTestSentence", longestTestSentence,
        "Longest allowable test sentence (else: right branch)");
    printConfig("testK", testK, "");
    printConfig("CONJSTYLE", CONJSTYLE.toString(),
        "how to print conjunction dependencies");
  }

  /**
   * Prints String vals to log with description
   * 
   * @param var
   * @param val
   * @param des
   *          description
   */
  static void printConfig(String var, String val, String des) {
    Util.logln(String.format("%-25s  %-30s  %-50s", var, val, des));
  }

  /**
   * Prints int vals to log
   * 
   * @param var
   * @param val
   * @param des
   *          description
   */
  static void printConfig(String var, int val, String des) {
    printConfig(var, Integer.toString(val), des);
  }

  /**
   * Prints double vals to log
   * 
   * @param var
   * @param val
   * @param des
   *          description
   */
  static void printConfig(String var, double val, String des) {
    printConfig(var, Double.toString(val), des);
  }

  /**
   * Prints boolean vals to log
   * 
   * @param var
   * @param val
   * @param des
   *          description
   */
  static void printConfig(String var, boolean val, String des) {
    printConfig(var, Boolean.toString(val), des);
  }
}

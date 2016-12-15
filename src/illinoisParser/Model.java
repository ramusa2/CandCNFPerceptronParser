package illinoisParser;

import illinoisParser.models.BaselineModel;
import illinoisParser.models.HWDepDistModel;
import illinoisParser.models.HWDepModel;
import illinoisParser.models.LexCatModel;
import illinoisParser.variables.ConditioningVariables;
import illinoisParser.variables.VariablesFactory;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Abstract class implemented by others which creates conditioning variable
 * contexts and requires inherited models implement a way to assign
 * probabilities, initialize/update distributions and accumulate counts.
 * Additionally, these counts can be computed via Inside-Outside or reading from
 * CCGbank
 * 
 * @author bisk1
 * @param 
 */
public strictfp abstract class Model implements Externalizable {
	private static final long serialVersionUID = 5142012L;

	/**
	 * Model name (static)
	 */
	protected static String MODEL_NAME;

	/**
	 * The model's grammar
	 */
	protected Grammar grammar;

	/**
	 * Distributions used by this model
	 */
	protected ArrayList<Distribution> Distributions = new ArrayList<Distribution>();

	/**
	 * Finalize all probability distributions
	 * 
	 * @throws Exception
	 */
	void finalizeProbabilityDistributions() throws Exception {
		for (Distribution D : Distributions) {
			D.finalizeProbabilityDistribution();
		}
	}

	/**
	 * The methods below are abstract; subclasses should use the getFine...ChartItem
	 * methods to create the fine equivalence classes, and the addCountsFromFine...ChartItem
	 * methods to add observed conditioning variable/outcome pairs to the model's distributions.
	 * Similarly, the setProbabilityOfFine...ChartItem methods should return the fine item's
	 * probability according to this probability model
	 */

	// Beam search methods
	abstract public void addBeamPriorCounts(FineChartItem fineCI, Chart coarseChart) throws Exception ;
	abstract public double getBeamPriorLogProbability(FineChartItem fineCI, Chart coarseChart) throws Exception ;

	// Lexical chart item methods
	abstract public FineChartItem getFineLexicalChartItem(CoarseLexicalCategoryChartItem ci, Chart coarseChart);
	abstract public void addCountsFromFineLexicalChartItem(FineChartItem fineLexicalCI, Chart coarseChart);	
	abstract public void setLogProbabilityOfLexicalChartItem(FineChartItem fineLexicalCI, Chart coarseChart) throws Exception;

	// Unary chart item methods
	abstract public FineChartItem getFineUnaryChartItem(CoarseChartItem ci, FineChartItem fineChildCI);
	abstract public void addCountsFromFineUnaryChartItem(FineChartItem fineParentCI, 
			FineChartItem fineChildCI, Chart coarseChart) throws Exception;
	public final double getLogProbabilityOfFineUnaryChartItem(FineChartItem fineParentCI,
			FineChartItem fineChildCI, Chart coarseChart) throws Exception {
		return getLogUnaryRuleProbability(fineParentCI, fineChildCI, coarseChart) 
				+ fineChildCI.getViterbiProb();
	}
	abstract public double getLogUnaryRuleProbability(FineChartItem fineParentCI,
			FineChartItem fineChildCI, Chart coarseChart) throws Exception;

	// Binary chart item methods
	abstract public FineChartItem getFineBinaryChartItem(CoarseChartItem ci, BackPointer bp,
			FineChartItem fineLeftChildCI, FineChartItem fineRightChildCI);
	abstract public void addCountsFromFineBinaryChartItem(FineChartItem fineParentCI, FineBackPointer bp,
			Chart coarseChart) throws Exception;
	public final double getLogProbabilityOfFineBinaryChartItem(FineChartItem fineParentCI, FineBackPointer bp,
			Chart coarseChart) throws Exception {
		return getLogBinaryRuleProbability(fineParentCI, bp, coarseChart)
				+ bp.B().getViterbiProb() + bp.C().getViterbiProb();
	}
	abstract public double getLogBinaryRuleProbability(FineChartItem fineParentCI, FineBackPointer bp,
			Chart coarseChart) throws Exception;

	protected void addObservedCount(Distribution dist,
			ConditioningVariables cond, int res) throws Exception {
		dist.accumulateCount(cond, res, 1.0);
	}

	/**
	 * Returns the figure of merit (for beam search)
	 */
	public double getFigureOfMerit(FineChartItem fineCI, Chart chart) throws Exception {
		return Log.ONE;
	}

	public String prettyCond(ConditioningVariables cond,
			Distribution distribution) {
		return "null";
	}

	public Object prettyOutcome(ConditioningVariables conditioningVariables, int outcome, Distribution distribution) {
		return "null";
	}

	/**
	 * Returns this model's grammar
	 * @return the grammar (words, categories, rules, etc.) used by this parsing model
	 */
	public Grammar getGrammar() {
		return this.grammar;
	}
	/**
	 * Saves this model's distributions (and conditioning variables) to disk.
	 * 
	 * @param saveDir the location of the directory containing the saved files
	 */
	public void save(File saveDir) {
		// Write summary file, storing model type and names of distributions
		File summaryFile = new File(saveDir.getAbsolutePath()+"/summary.txt");
		try {
			PrintWriter pw = new PrintWriter(summaryFile);
			pw.println(MODEL_NAME);
			for(Distribution d : this.Distributions) {
				pw.println(d.identifier);
			}
			pw.close();
			// Serialize cached conditioning variables
			ObjectOutput cvout = getGZIPObjectOutput(saveDir.getAbsolutePath()+"/cond_vars.gz");
			VariablesFactory.writeCache(cvout);	
			cvout.flush();
			cvout.close();
			// Serialize distributions
			for(Distribution dist : this.Distributions) {
				ObjectOutput dout = getGZIPObjectOutput(saveDir.getAbsolutePath()+"/"+dist.identifier+".gz");
				dout.writeObject(dist);	
				cvout.flush();
				cvout.close();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Loads this model's distributions (and conditioning variables) from disk
	 * 
	 * @param loadDir the location of the directory containing the data files
	 * @return a new Model object with parameters defined by the load directory 
	 */
	public static Model load(Grammar loadedGrammar, File loadDir) {
		try {
			// Read model summary
			File summaryFile = new File(loadDir.getAbsolutePath()+"/summary.txt");
			Scanner sc = new Scanner(summaryFile);
			String name = sc.nextLine().trim();
			Model model = getEmptyModel(name);
			ArrayList<String> dists = new ArrayList<String>();
			while(sc.hasNextLine()) {
				String distName = sc.nextLine().trim();
				if(!distName.isEmpty()) {
					dists.add(distName);
				}
			}
			sc.close();
			// Read serialized cached conditioning variables
			ObjectInput cvIn = getGZIPObjectInput(loadDir.getAbsolutePath()+"/cond_vars.gz");
			VariablesFactory.readCache(cvIn);
			cvIn.close();
			// Read serialized distributions
			for(String distName : dists) {
				ObjectInput distIn = getGZIPObjectInput(loadDir.getAbsolutePath()+"/"+distName+".gz");
				Distribution dist = (Distribution) distIn.readObject();
				distIn.close();
				model.Distributions.add(dist);
			}
			return model;
		}
		catch(Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Return a new instance of a subclass of Model, based on that class's MODEL_NAME 
	 */
	private static Model getEmptyModel(String name) {
		if(name.equals(LexCatModel.MODEL_NAME)) {
			return new LexCatModel(new Grammar());
		}
		if(name.equals(HWDepModel.MODEL_NAME)) {
			return new HWDepModel(new Grammar());
		}
		if(name.equals(HWDepDistModel.MODEL_NAME)) {
			return new HWDepDistModel(new Grammar());
		}
		return new BaselineModel(new Grammar());
	}

	/**
	 * Get a gzipped object output stream for serializing data
	 */
	private ObjectOutput getGZIPObjectOutput(String filename) {
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			GZIPOutputStream gos = new GZIPOutputStream(fos);
			return new ObjectOutputStream(gos);
		}
		catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Get a gzipped object intput stream for reading serialized data
	 */
	private static ObjectInput getGZIPObjectInput(String filename) {
		try {
			FileInputStream fos = new FileInputStream(filename);
			GZIPInputStream gos = new GZIPInputStream(fos);
			return new ObjectInputStream(gos);
		}
		catch (IOException e) {
			// TODO: handle exception
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Writes a log/summary of this model to a target file on disk
	 * 
	 * @param summaryFile the target file
	 */
	public void writeSummary(File summaryFile) {
		PrintWriter pw;
		try {
			pw = new PrintWriter(summaryFile);
			pw.println(MODEL_NAME);
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		grammar = (Grammar) in.readObject();
		// This next step is important; it allows for shared Cond.Var. references across distributions
		VariablesFactory.readCache(in);
		Distributions = (ArrayList<Distribution>) in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(grammar);
		// This next step is important; it allows for shared Cond.Var. references across distributions
		VariablesFactory.writeCache(out);
		out.writeObject(Distributions);
	}
	
	public ArrayList<Distribution> distributions() {
		return Distributions;
	}

}

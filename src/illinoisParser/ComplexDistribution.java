package illinoisParser;

import illinoisParser.variables.ComplexConditioningVariables;
import illinoisParser.variables.ConditioningVariables;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ComplexDistribution extends Distribution {

	private static final long serialVersionUID = 1796732203374724527L;
	protected final Distribution backoff;
	protected final ConcurrentHashMap<CondOutcomePair, Double> pCache;
	protected final ConcurrentHashMap<ConditioningVariables, double[]> logLambdas;
	protected final ConcurrentHashMap<ConditioningVariables, HashSet<Integer>> lengths;
	protected final static double LOGFIVE = Math.log(5);

	public ComplexDistribution(Model mod, String id, Distribution backoffDist) {
		super(mod, id);
		pCache = new ConcurrentHashMap<CondOutcomePair, Double>();
		backoff = backoffDist;
		logLambdas = new ConcurrentHashMap<ConditioningVariables, double[]>();
		lengths = new ConcurrentHashMap<ConditioningVariables, HashSet<Integer>>();
	}

	public Distribution backoff() {
		return backoff;
	}

	/**
  @Override
  public void print(String s) throws Exception {
    super.print(s);
    backoff.print(s);
  }
	 **/

	@Override
	public synchronized void addContext(ConditioningVariables cond, Integer res)
			throws Exception {
		super.addContext(cond, res);
		backoff.addContext(((ComplexConditioningVariables)cond).getBackoff(), res);
		lengths.putIfAbsent(cond, new HashSet<Integer>());
		lengths.get(cond).add(res);
	}

	@Override
	public boolean contains(CondOutcomePair pair) {
		return this.contains(pair.conditioning_variables(), pair.outcome());
	}
	
	@Override
	public boolean contains(ConditioningVariables cond, Integer res) {
		return backoff.contains(((ComplexConditioningVariables)cond).getBackoff(), res);
	}



	/**
	 * Print distributions and corresponding counts to a file
	 * 
	 * @param s
	 * @throws Exception
	 */
	public void print(String s) throws Exception {
		this.printWithLambdas(s);
		backoff.print(s);
	}

	public void printWithLambdas(String s) throws Exception {
		// Print
		String name = Configuration.Folder + "/";
		if (!s.isEmpty()) {
			name += s + ".";
		}
		name += toString();
		Util.logln("Printing " + name);
		Writer output = Util.TextFileWriter(name+".txt");

		// Build data structures
		double total = 0.0;
		HashMap<ConditioningVariables, HashMap<CondOutcomePair, Double>> mapCounts 
		= new HashMap<ConditioningVariables, HashMap<CondOutcomePair, Double>>();
		HashMap<ConditioningVariables, HashMap<CondOutcomePair, Double>> mapProbs 
		= new HashMap<ConditioningVariables, HashMap<CondOutcomePair, Double>>();
		HashMap<ConditioningVariables, Double> condCounts = new HashMap<ConditioningVariables, Double>();
		for (CondOutcomePair pair : Counts.keySet()) {
			ConditioningVariables cond = pair.conditioning_variables();
			int res = pair.outcome();
			// Update cond counts
			Double old = condCounts.get(cond);
			if(old == null) {
				old = 0.0;
			}
			double added = Counts.get(pair);
			old += added;
			total += added;
			condCounts.put(cond, old);
			// Update cond counts
			HashMap<CondOutcomePair, Double> temp = mapProbs.get(cond);
			if(temp == null) {
				temp = new HashMap<CondOutcomePair, Double>();
			}
			temp.put(pair, Math.exp(this.logProb(pair)));
			mapProbs.put(cond,  temp);
			// Update cond-outcome counts
			HashMap<CondOutcomePair, Double> temp2 = mapCounts.get(cond);
			if(temp2 == null) {
				temp2 = new HashMap<CondOutcomePair, Double>();
			}
			temp2.put(pair, this.Counts.get(pair));
			mapCounts.put(cond,  temp2);
		}

		ArrayList<SortedPair> condKeys = new ArrayList<SortedPair>();
		for(ConditioningVariables cond : condCounts.keySet()) {
			condKeys.add(new SortedPair(condCounts.get(cond), cond));
		}
		Collections.sort(condKeys);
		for(SortedPair sp : condKeys) {
			ConditioningVariables cond = (ConditioningVariables) sp.obj;
			double condCount = sp.val;
			HashMap<CondOutcomePair, Double> countTemp = mapCounts.get(cond);
			HashMap<CondOutcomePair, Double> probTemp = mapProbs.get(cond);
			ArrayList<SortedPair> pairKeys = new ArrayList<SortedPair>();
			for(CondOutcomePair pair : countTemp.keySet()) {
				pairKeys.add(new SortedPair(countTemp.get(pair), pair));
			}
			Collections.sort(pairKeys);
			output.write(String.format("%-19s %1.5f    %5.5f\n",
					model.prettyCond(cond, this),
					condCount/total,
					condCount));
			for(SortedPair sp2 : pairKeys) {
				CondOutcomePair pair = (CondOutcomePair) sp2.obj;
				double pairCount = countTemp.get(pair);
				double unsmoothedProb = pairCount/condCount;
				//double smoothedProb = probTemp.get(pair);
				double smoothedProb = Math.exp(this.calculateInterpolatedProb((ComplexConditioningVariables) pair.conditioning_variables(), pair.outcome()));
				double lambda = Math.exp(this.logLambdas.get(cond)[0]);
				output.write(String.format("%-10s%-30s:%-10.1f%-10s%-7.6f%-10s%-7.6f%-10s%-7.6f\n", "",
						model.prettyOutcome(pair.conditioning_variables(), pair.outcome(), this),
						pairCount, "", unsmoothedProb, "", smoothedProb, "", lambda));
			}
			output.write("\n");
		}
		output.close();
	}

	/**
	 * Probability
	 * 
	 * @param cond
	 * @param outcome
	 * @return P( outcome | cond )
	 * @throws Exception
	 */
	@Override
	public double logProb(ConditioningVariables cond, int outcome) throws Exception {
		return this.logProb(new CondOutcomePair(cond, outcome));
	}

	@Override
	double logProb(CondOutcomePair pair) throws Exception {
		Double cachedProb = this.pCache.get(pair);
		if(cachedProb != null) {
			return cachedProb.doubleValue();
		}
		else {
			ConditioningVariables cond = pair.conditioning_variables();
			double backoffProb = backoff.logProb(((ComplexConditioningVariables)cond).getBackoff(), pair.outcome());
			if (logLambdas.containsKey(cond)) {
				double[] lambdaVals = logLambdas.get(cond);
				double oneMinusLambda = lambdaVals[1];
				return Log.mul(oneMinusLambda, backoffProb);
			} else {
				return backoffProb;
			}
		}
		//return calculateInterpolatedProb((ComplexConditioningVariables) pair.conditioning_variables(), pair.outcome());
	}

	protected double calculateInterpolatedProb(ComplexConditioningVariables cond, int outcome) throws Exception {
		CondOutcomePair pair = new CondOutcomePair(cond, outcome);
		double backoffProb = backoff.logProb(((ComplexConditioningVariables)cond).getBackoff(), outcome);
		double[] lambdaVals = logLambdas.get(cond);
		double lambda = lambdaVals[0];
		double oneMinusLambda = lambdaVals[1];
		double complexProb = super.logProb(pair);
		return Log.sloppy_add(Log.mul(lambda, complexProb),
				Log.mul(oneMinusLambda, backoffProb));
	}

	/**
	 * Update probabilities using CountsNew Transfer CountsNew -> Counts
	 * 
	 * @throws Exception
	 */
	@Override
	public void finalizeProbabilityDistribution() throws Exception {
		backoff.finalizeProbabilityDistribution();
		this.setLambdas();
		super.finalizeProbabilityDistribution(); // warning: super.finalizeProbabilityDistributions()
		// reset counts (which we need to set the lambdas)
		this.cacheProbabilities();
	}

	protected void cacheProbabilities() throws Exception {
		for (CondOutcomePair pair : this.logProbabilities.keySet()) {
			double prob = this.calculateInterpolatedProb((ComplexConditioningVariables) pair.conditioning_variables(), pair.outcome());
			this.pCache.put(pair, prob);
		}
	}

	protected void setLambdas() throws Exception {
		for (ConditioningVariables cond : conditioning_contexts.keySet()) {
			double total = 0.0;
			for (Integer res : conditioning_contexts.get(cond).keySet()) {
				CondOutcomePair pair = new CondOutcomePair(cond, res);
				total += this.Counts.get(pair);
			}
			double f = Math.log(total);
			double u = Math.log(conditioning_contexts.get(cond).keySet().size());
			double lambda = Log.div(f,
					Log.sloppy_add(f,
							Log.mul(LOGFIVE, u)));
			double oneMinusLambda = Log.subtract(Log.ONE, lambda);
			logLambdas.put((ComplexConditioningVariables) cond, new double[] { lambda, oneMinusLambda });
		}
	}

	/**
	 * Add counts for a given distribution
	 * 
	 * @param cond
	 *          conditioning variable
	 * @param j
	 *          outcome variable
	 * @param v
	 *          value
	 * @throws Exception
	 */
	@Override
	public synchronized void accumulateCount(CondOutcomePair pair, double v)
			throws Exception {
		super.accumulateCount(pair, v);
		backoff.accumulateCount(
				((ComplexConditioningVariables)pair.conditioning_variables()).getBackoff(), 
				pair.outcome(), v);
	}

	@Override
	synchronized void accumulateCount(ConditioningVariables cond, int res, double v) throws Exception{
		super.accumulateCount(new CondOutcomePair(cond, res), v);
		backoff.accumulateCount(((ComplexConditioningVariables)cond).getBackoff(), res, v);
	}
}

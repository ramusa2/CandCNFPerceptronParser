package illinoisParser;

import illinoisParser.variables.ComplexConditioningVariables;
import illinoisParser.variables.ConditioningVariables;

import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class ManualComplexDistribution extends ComplexDistribution {

	private static final long serialVersionUID = -7441183050915925460L;

	ManualComplexDistribution(Model mod, String id, Distribution backoffDist) {
		super(mod, id, backoffDist);
	}

	@Override
	public synchronized void addContext(ConditioningVariables cond, Integer res)
			throws Exception {
		Util.Error("Can't use addContext for ManualComplexDistribution without providing a backoff context");
	}

	public synchronized void addContext(ConditioningVariables cond, ConditioningVariables backoffCond, Integer res)
			throws Exception {
		// Distribution's addContext functionality:
		conditioning_contexts.putIfAbsent(cond,
				new ConcurrentHashMap<Integer,Boolean>());
		conditioning_contexts.get(cond).put(res, true);
		// ManualComplexDistribution's addContext functionality, different from ComplexDistribution's
		backoff.addContext(backoffCond, res);
		lengths.putIfAbsent(cond, new HashSet<Integer>());
		lengths.get(cond).add(res);
	}
	
	@Override
	public boolean contains(ConditioningVariables cond, Integer res) {
		Util.Error("Can't use contains for ManualComplexDistribution without providing a backoff context");
		return false;
	}
	
	public boolean contains(ConditioningVariables cond, ConditioningVariables backoffCond, Integer res) {
		return backoff.contains(backoffCond, res);
	}
	
	@Override
	public
	synchronized void accumulateCount(CondOutcomePair pair, double v)
			throws Exception {
		Util.Error("Can't use accumulateCount for ManualComplexDistribution without providing a backoff context");
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
	public
	double logProb(ConditioningVariables cond, int outcome) throws Exception {
		Util.Error("Can't use logProb for ManualComplexDistribution without providing a backoff context");
		return Double.NaN;
	}


	@Override
	double logProb(CondOutcomePair pair) throws Exception {
		Util.Error("Can't use logProb for ManualComplexDistribution without providing a backoff context");
		return Double.NaN;
	}

	double logProb(ConditioningVariables cond, ConditioningVariables backoffCond, int outcome) throws Exception {
		CondOutcomePair pair = new CondOutcomePair(cond, outcome);
		Double cachedProb = this.pCache.get(pair);
		if(cachedProb != null) {
			return cachedProb.doubleValue();
		}
		else {
			double backoffProb = backoff.logProb(backoffCond, pair.outcome());
			if (logLambdas.containsKey(cond)) {
				double[] lambdaVals = logLambdas.get(cond);
				double oneMinusLambda = lambdaVals[1];
				return Log.mul(oneMinusLambda, backoffProb);
			} else {
				return backoffProb;
			}
		}
	}

	@Override
	protected double calculateInterpolatedProb(ComplexConditioningVariables cond, int outcome) throws Exception {
		Util.Error("Can't use calculateInterpolatedProb for ManualComplexDistribution without providing a backoff context");
		return Double.NaN;
	}

	protected double calculateInterpolatedProb(ConditioningVariables cond, ConditioningVariables condBackoff, int outcome) throws Exception {
		CondOutcomePair pair = new CondOutcomePair(cond, outcome);
		double backoffProb = backoff.logProb(condBackoff, outcome);
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

		if (conditioning_contexts.isEmpty()) {
			Util.logln("Warning: creating empty distribution:\t" + this.identifier);
		}
		logProbabilities.clear();

		for (ConditioningVariables context : conditioning_contexts.keySet()) {
			HashMap<CondOutcomePair, Double> full = new HashMap<CondOutcomePair, Double>();
			double sum = 0.0;
			for (Integer outcome : conditioning_contexts.get(context).keySet()) {
				CondOutcomePair pair = new CondOutcomePair(context, outcome);
				double c = Counts.get(pair);
				sum += c;
				full.put(pair, Math.log(c));
			}
			double logSum = Math.log(sum);
			for (CondOutcomePair pair : full.keySet()) {
				logProbabilities.put(pair, full.get(pair) - logSum);
			}
		}
		// TODO: will we ever need to add more counts?
		//Counts.clear();
	}

	@Override
	protected void cacheProbabilities() throws Exception {
		Util.Error("Can't cache probabilities for ManualComplexDistribution (no stored backoff contexts)");
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
			logLambdas.put(cond, new double[] { lambda, oneMinusLambda });
		}
	}
	
	@Override
	synchronized void accumulateCount(ConditioningVariables cond, int res, double v) throws Exception{
		Util.Error("Can't use accumulateCount for ManualComplexDistribution without providing a backoff context");
	}

	synchronized void accumulateCount(ConditioningVariables cond, ConditioningVariables backoffCond, int res, double v) throws Exception{
		super.accumulateCount(new CondOutcomePair(cond, res), v);
		backoff.accumulateCount(backoffCond, res, v);
	}
}

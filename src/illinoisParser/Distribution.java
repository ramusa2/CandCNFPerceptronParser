package illinoisParser;

import illinoisParser.variables.ConditioningVariables;

import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create a distribution object
 * 
 * @author bisk1
 */
public class Distribution implements Serializable {
	private static final long serialVersionUID = 5162012L;
	/** Name of the distribution */
	public final String identifier;


	/**
	 * Map a conditioning context to all possible context-outcome pairs
	 */
	protected final ConcurrentHashMap<ConditioningVariables,
	ConcurrentHashMap<Integer,Boolean>> conditioning_contexts =
	new ConcurrentHashMap<ConditioningVariables,ConcurrentHashMap<Integer,Boolean>>();

	/**
	 * Reference to the model that uses the distribution. Needed for printing.
	 */
	protected Model model;
	/** Previously accumulated counts **/
	protected final ConcurrentHashMap<CondOutcomePair,Double> Counts
	= new ConcurrentHashMap<CondOutcomePair,Double>();
	/** Distribution's probabilities */
	protected final ConcurrentHashMap<CondOutcomePair,Double> logProbabilities
	= new ConcurrentHashMap<CondOutcomePair,Double>();

	/**
	 * Constructor requires a model and distribution identifier string (e.g.
	 * "pDistType")
	 * 
	 * @param mod
	 * @param id
	 */
	public Distribution(Model mod, String id) {
		model = mod;
		identifier = id;
	}

	/**
	 * Initialize distributions.
	 * 
	 * @throws Exception
	 */
	public synchronized void finalizeProbabilityDistribution() throws Exception {
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

		//Util.log(String.format("%-20s initialized as %6d x %6d array\n",
		//		this.identifier, conditioning_contexts.size(),
		//		conditioning_contexts.size()));
	}

	/**
	 * Return Probability
	 * @param cond_outcome
	 * @return P( outcome | cond )
	 * @throws Exception
	 */
	double logProb(CondOutcomePair cond_outcome) throws Exception {
		Double logProb = logProbabilities.get(cond_outcome);
		if(logProb == null) {
			/*
			for(CondOutcomePair pair : logProbabilities.keySet()) {
				if(pair.equals(cond_outcome)) {
					System.out.println("Hash is messed up.");
				}
				else if(pair.conditioning_variables().equals(cond_outcome.conditioning_variables())
						&& pair.outcome() == cond_outcome.outcome()) {
					System.out.println("Equals is messed up.");
				}
			}
			*/
			return Double.NEGATIVE_INFINITY;
		}
		return logProb.doubleValue();
	}

	/**
	 * Return probability of outcome | cond
	 * @param cond conditioning variable
	 * @param l outcome value
	 * @return probability
	 * @throws Exception
	 */
	public double logProb(ConditioningVariables cond, int outcome) throws Exception {
		return logProb(new CondOutcomePair(cond, outcome));
	}

	/**
	 * Add counts for a given distribution
	 * 
	 * @param pair conditioning variable + outcome pair
	 * @param v value
	 * @throws Exception
	 */
	public synchronized void accumulateCount(CondOutcomePair pair, double v)
			throws Exception {
		this.addContext(pair.conditioning_variables(), pair.outcome());
		Double old = Counts.get(pair);
		if(old == null) {
			Counts.put(pair, v);
		}
		else {
			Counts.put(pair, old+v);
		}
	}

	synchronized void accumulateCount(ConditioningVariables cond, int res, double v)
			throws Exception {
		this.accumulateCount(new CondOutcomePair(cond, res), v);
	}

	/**
	 * Adds a conditioning context and outcome to the distribution object
	 * @param cond context
	 * @param res  outcome
	 * @throws Exception
	 */
	public void addContext(ConditioningVariables cond, Integer res)
			throws Exception {
		conditioning_contexts.putIfAbsent(cond,
				new ConcurrentHashMap<Integer,Boolean>());
		conditioning_contexts.get(cond).put(res, true);
	}

	/**
	 * Print distributions and corresponding counts to a file
	 * 
	 * @param s
	 * @throws Exception
	 */
	/**
	public void print(String s) throws Exception {
		// Print
		String name = Configuration.Folder + "/";
		if (!s.isEmpty()) {
			name += s + ".";
		}
		name += toString();
		Util.logln("Printing " + name);
		Writer output = Util.TextFileWriter(name + ".gz");

		// Build data structures
		HashMap<ConditioningVariables, Outcomes> mapCounts = new HashMap<ConditioningVariables, Outcomes>();
		HashMap<ConditioningVariables, Outcomes> mapProbs = new HashMap<ConditioningVariables, Outcomes>();
		HashMap<ConditioningVariables, Double> condCounts = new HashMap<ConditioningVariables, Double>();
		for (ConditioningVariables cond : conditioning_contexts.keySet()) {
			Outcomes out_count = new Outcomes(conditioning_contexts.get(cond).size());
			Outcomes out_prob = new Outcomes(conditioning_contexts.get(cond).size());
			for (Integer res : conditioning_contexts.get(cond).keySet()) {
				CondOutcomePair pair = new CondOutcomePair(cond, res);
				double count = Counts.get(pair).value();
				out_count.add(pair, count);
				out_prob.add(pair, Probabilities.get(pair));
			}
			out_count.sort();
			out_prob.sort();

			mapCounts.put(cond, out_count);
			mapProbs.put(cond, out_prob);
			condCounts.put(cond, out_count.sum());
		}

		// Print distributions
		for (ConditioningVariables cond : condCounts.keySet()) {
			output.write(String.format("%-19s \n",
					model.prettyCond(cond, this)));
			Outcomes cout = mapCounts.get(cond);
			Outcomes pout = mapProbs.get(cond);
			for (int j = 0; j < cout.length(); j++) {
				output.write(String.format("%-10s%-30s:%-17.15f%-10s%-10.9f\n", "",
						model.prettyOutcome(pout.pairs[j].outcome(), this),
						Math.exp(pout.vals[j]), "", Math.exp(cout.vals[j])));
			}
			output.write("\n");
		}
		output.close();

		// Also print a lexicon
		if (this.identifier.contains("p_Tag") || this.identifier.contains("p_Word") || this.identifier.contains("Emission")) {
			output = Util.TextFileWriter(name + ".lex.gz");
			// Probability of Cat
			HashMap<String, Double> p_of_Cat = new HashMap<String, Double>();
			HashMap<String, ConditioningVariables> cat_to_int = new HashMap<String, ConditioningVariables>();
			HashMap<String, Integer> w_to_int = new HashMap<String, Integer>();
			// Probability of Word
			HashMap<String, Double> p_of_word = new HashMap<String, Double>();
			HashMap<String, HashSet<CondOutcomePair>> cats_for_word
			= new HashMap<String, HashSet<CondOutcomePair>>();
			Double word_sum = 0.0;
			ArrayList<ObjectDoublePair<String>> wordProbs =
					new ArrayList<ObjectDoublePair<String>>();
			for (ConditioningVariables cond : condCounts.keySet()) {
				String cat = model.prettyCond(cond, this);
				cat_to_int.put(cat, cond);
				p_of_Cat.put(cat, mapProbs.get(cond));
				Outcomes cout = mapCounts.get(cond);
				for (int i = 0; i < cout.length(); i++) {
					String w = model.prettyOutcome(cout.pairs[i].outcome(), this);
					w_to_int.put(w, cout.pairs[i].outcome());

					if (!cats_for_word.containsKey(w)) {
						cats_for_word.put(w, new HashSet<CondOutcomePair>());
					}
					cats_for_word.get(w).add(conds.pairs[c]);

					if (!p_of_word.containsKey(w)) {
						p_of_word.put(w, 0.0);
					}
					p_of_word.put(w, p_of_word.get(w) + Math.exp(cout.vals[i]));
					word_sum += Math.exp(cout.vals[i]);
				}
			}
			for (String w : p_of_word.keySet()) {
				wordProbs.add(
						new ObjectDoublePair<String>(w, p_of_word.get(w) / word_sum));
			}
			Collections.sort(wordProbs);
			for (ObjectDoublePair<String> st : wordProbs) {
				String w = st.content();
				// p(c|w) = p(w|c)*p(c)/p(w)
				output.write(String.format("%-19s %1.5f   %5.5f\n",
						w, p_of_word.get(w) / word_sum,
						p_of_word.get(w))); // p(w)
				Outcomes out = new Outcomes(cats_for_word.get(w).size());
				for (CondOutcomePair cat : cats_for_word.get(w)) {
					String c = model.prettyCond(cat.conditioning_variables(), this);
					out.add(cat, Math.exp(P(cat_to_int.get(c).conditioning_variables(), w_to_int.get(w)))
							* p_of_Cat.get(c)
							/ (p_of_word.get(w) / word_sum));
				}
				out.sort();
				for (int i = 0; i < out.length(); i++) {
					CondOutcomePair cat = out.pairs[i];
					if(out.vals[i] != 0.0) {
						output.write(String.format("\t%-40s|\t%-20s%10.9f\n",
								model.prettyCond(cat.conditioning_variables(), this), w, out.vals[i]));
					}
				}
				output.write("\n");
			}
			output.close();

		}
	}
	**/

//	/**
//	 * Use newCounts to compute prob values for conditioning var cond
//	 * 
//	 * @param outs
//	 * @throws Exception
//	 */
//	protected void normalize(Outcomes outs) throws Exception {
//		if (outs == null) {
//			return;
//		}
//		if (outs.sum() == Log.ZERO) {
//			throw new Exception("Divisor in normalization == 0");
//		}
//
//		double divisor = outs.sum();
//		// +1 smoothing
//		for (int var = 0; var < outs.pairs.length; var++) {
//			// Real + smooth
//			double v = Log.div(outs.vals[var], divisor);
//
//			// Round
//			// If || val - 1 || < EPSILON val = 1
//			if (Log.One(v)) {
//				v = Log.ONE;
//			}
//
//			outs.vals[var] = v;
//		}
//
//		if (Math.abs(outs.sum() - 0.0) > .0001) {
//			throw new Exception("Doesn't sum to ONE: " + Arrays.toString(outs.vals));
//		}
//	}

	/**
	 * For use during testing if there is a probability defined for the given
	 * conditioning and outcome variables.
	 * 
	 * @param context
	 * @param outcome
	 * @return boolean
	 */
	public boolean contains(ConditioningVariables cond, Integer outcome) {
		return this.contains(new CondOutcomePair(cond, outcome));
	}

	/**
	 * For use during testing if there is a probability defined for the given
	 * conditioning and outcome variables.
	 * 
	 * @param context
	 * @param outcome
	 * @return boolean
	 */
	public boolean contains(CondOutcomePair pair) {
		return logProbabilities.containsKey(pair);
	}

	@Override
	public String toString() {
		return identifier;
	}
	
	/**
	   * Print distributions and corresponding counts to a file
	   * 
	   * @param s
	   * @throws Exception
	   */
	  public void print(String s) throws Exception {
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
			    	  double smoothedProb = probTemp.get(pair);
			          output.write(String.format("%-10s%-30s:%-10.1f%-10s%-7.6f%-10s%-7.6f\n", "",
				              model.prettyOutcome(pair.conditioning_variables(), pair.outcome(), this),
				              pairCount, "", unsmoothedProb, "", smoothedProb));
			      }
			        output.write("\n");
		      }
		      output.close();
	  }

	  public boolean isEmpty() {
		  return conditioning_contexts.isEmpty();
	  }
}

class SortedPair implements Comparable {
	Double val;
	Object obj;
	
	public SortedPair(double v, Object o) {
		val = v;
		obj = o;
	}
	
	@Override public int compareTo(Object other) {
		if(other instanceof SortedPair) {
			return ((SortedPair)other).val.compareTo(val);
		}
		return -1;
	}
}

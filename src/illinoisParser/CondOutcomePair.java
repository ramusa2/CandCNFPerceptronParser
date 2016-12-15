package illinoisParser;

import illinoisParser.variables.ConditioningVariables;

import java.io.Serializable;

/**
 * An integer array
 * 
 * @author bisk1
 */
public strictfp class CondOutcomePair implements Serializable {
  private static final long serialVersionUID = 5142012L;

  /**
   * Array of conditioning variables
   */
  private final ConditioningVariables conditioning_variables;
  /**
   * Outcome variable
   */
  private final int outcome;

  /**
   * Create a pair using another pair that's missing it's outcome
   * @param l
   * @param cond
   */
  CondOutcomePair(ConditioningVariables cond, int res) {
	    conditioning_variables = cond;
	    outcome = res;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if(o == null || !(o instanceof CondOutcomePair)) {
    	return false;
    }
    return conditioning_variables.equals(((CondOutcomePair) o).conditioning_variables)
        && outcome == ((CondOutcomePair) o).outcome;
  }

  @Override
  public int hashCode() {
    return (conditioning_variables.hashCode() + 5851*outcome);
  }

  @Override
  public String toString() {
    return outcome + " | "+conditioning_variables.toString();
  }

  /**
   * Return conditioning variables
   * 
   * @return Conditioning Variables
   */
  final ConditioningVariables conditioning_variables() {
    return conditioning_variables;
  }

  /**
   * Return the outcome
   */
  final int outcome() {
    return outcome;
  }
}

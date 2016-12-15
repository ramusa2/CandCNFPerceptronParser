package illinoisParser;

/**
 * @author bisk1 Provides logspace operations.
 */
public final strictfp class Log {

  /*
   * Unlike some of the numeric methods of class StrictMath, all implementations
   * of the equivalent functions of class Math are not defined to return the
   * bit-for-bit same results. This relaxation permits better-performing
   * implementations where strict reproducibility is not required. By default
   * many of the Math methods simply call the equivalent method in StrictMath
   * for their implementation. Code generators are encouraged to use
   * platform-specific native libraries or microprocessor instructions, where
   * available, to provide higher-performance implementations of Math methods.
   * Such higher-performance implementations still must conform to the
   * specification for Math. And I repeat:
   * "Code generators are encouraged to use platform-specific native libraries"
   */

  private static final double BIG = StrictMath.log(
      StrictMath.pow(StrictMath.E, 40));
  /**
   * Log ZERO constant
   */
  public static final double ZERO = StrictMath.log(0.0);
  /**
   * Log ONE constant
   */
  public static final double ONE = StrictMath.log(1.0);

  /**
   * Performs a log add using the Manning and Schutze algorithm
   * 
   * @param a
   * @param b
   * @return double
   * @throws Exception
   */
  public static final double add(double a, double b) throws Exception {
    return sloppy_add(a,b);
    // We are currently disregarding precision issues
    //    if (Double.isNaN(a) || Double.isNaN(b)) {
    //      throw new Exception("NaN: " + a + "\t" + b);
    //    }
    //    /*
    //     * Manning and Schutze funct log_add =
    //     * if y - x > log_big then y
    //     * elif x - y > log BIG then x
    //     * else min(x,y) + log(exp(x - min(x,y)) + exp(y - min(x,y)))
    //     *
    //     * Scratch work
    //     *  Let min = min(x,y)
    //     *  Let max = max(x,y)
    //     *  min + log(exp(min - min) + exp(max - min))
    //     *  min + log( 1 + exp(max-min) )
    //     *  min + log1p( exp(max-min) )
    //     */
    //    double min = Math.min(a, b);
    //    double max = Math.max(a, b);
    //    if (min == Log.ZERO) {
    //      return max;
    //    }
    //
    //    if (max - min > BIG) {
    //      return max;
    //    }
    //
    //    //double newV = min + Math.log1p(Math.exp(max - min));
    //    double newV = min + Math.log(1 + Math.exp(max - min)); // FIXME: PRECISION
    //    if (newV <= max) {
    //      throw new Exception("Add Unsuccessful " + min + "\t+\t"
    //          + max + "\t=\t" + newV);
    //    }
    //    if (Double.isNaN(newV)) {
    //      throw new Exception("NaN: " + newV);
    //    }
    //    return newV;
  }

  /**
   * Attempts to add two values, returning the larger if precision doesn't allow
   * it
   * 
   * @param a
   * @param b
   * @return double
   * @throws Exception
   */
  public static final double sloppy_add(double a, double b) throws Exception {
    double min, max;
    if ( a < b ) {
      min = a;   max = b;
    } else {
      min = b;   max = a;
    }
    if (min == Log.ZERO) {
      return max;
    } else if (max - min > BIG) {
      return max;
    } else {
      // This has more precision
      //double newV = min + Math.log1p(Math.exp(max - min));
      double newV = min + Math.log(1 + Math.exp(max - min));
      if (newV <= max) {
        return max;
      }
      if (Double.isNaN(newV)) {
        throw new Exception("NaN: " + newV);
      }
      return newV;
    }
  }

  /**
   * Divides. Returning <a>/<b>
   * 
   * @param a
   * @param b
   * @return double
   * @throws Exception
   */
  static final double div(double a, double b) throws Exception {
    if (Double.isNaN(a)
        || Double.isNaN(b)
        || Double.isNaN(a - b)
        || b == Log.ZERO) {
      throw new Exception("Invalid: " + a + "\t" + b);
    }
    return a - b;
  }

  /**
   * Takes in (logspace) doubles and multiplies them.
   * 
   * @param numbers
   * @return double
   * @throws Exception
   */
  public static final double mul(double... numbers) throws Exception {
    double v = 0.0;
    for (double d : numbers) {
      if (d == Log.ZERO) {
        return Log.ZERO;
      }
      if (Double.isNaN(d)) {
        throw new Exception("NaN: " + d);
      }
      v += d;
    }
    // TODO / FIXME : Check that multiply was successful
    return v;
  }

  /**
   * Attempts to subtract <b> from <a> but does not throw an exception on
   * failure.
   * 
   * @param a
   * @param b
   * @return double
   * @throws Exception
   */
  static final double trySubtract(double a, double b) throws Exception {
    if (a < b) {
      throw new Exception("Undefined Log for: " + a + " - " + b);
    }
    try {
      double v = subtract(a, b);
      return v;
    } catch (Exception e) {
      Util.SimpleError(e.getMessage());
      return a;
    }
  }

  /**
   * Subtracts <b> from <a>
   * 
   * @param a
   * @param b
   * @return double
   * @throws Exception
   */
  public static final double subtract(double a, double b) throws Exception {
    if (equal(a,b)) {
      return Log.ZERO;
    }
    if (a - b > BIG || b == Log.ZERO) {
      return a;
    } else if (a < b) {
      throw new Exception("Undefined Log for: " + a + " - " + b);
    }
    double newV = b + /* Strict */Math.log(/* Strict */Math.exp(a - b) - 1);
    //    if (newV > a) {
    //      throw new Exception("Subtract Unsucessful " + a + "\t-\t"
    //          + b + "\t=\t" + newV);
    //    }
    return newV;
  }

  /**
   * Sums a double array
   * 
   * @param ll
   * @return double
   * @throws Exception
   */
  static final double sum(double[] ll) throws Exception {
    // Stolen from Berkeley Parser / Ch16 Numerical Recipes
    if (ll.length == 0) {
      throw new Exception("Can't sum nothing");
    }

    // Find Max
    double max = ll[0];
    for (double d : ll) {
      if (d > max) {
        max = d;
      }
    }

    if (equal(max, Log.ZERO)) {
      return Log.ZERO;
    }

    double sum = 0.0;
    for (double d : ll) {
      sum += /* Strict */Math.exp(d - max);
    }

    double d = max + /* Strict */Math.log(sum);
    if (equal(d, Log.ONE)) {
      d = Log.ONE;
    }
    return d;
  }

  /**
   * Sums a LogDouble array
   * @param ll
   * @return sum
   * @throws Exception
   */
  static final double sum(LogDouble[] ll) throws Exception {
    double d = Log.ZERO;
    for (LogDouble L : ll) {
      d = Log.add(d, L.value());
    }
    return d;
  }

  /**
   * Returns the absolute value of the difference of two numbers (<v> and <v2>)
   * 
   * @param v
   * @param v2
   * @return double
   * @throws Exception
   */
  static double ABSsubtract(double v, double v2) throws Exception {
    if (v > v2) {
      return subtract(v, v2);
    }
    return subtract(v2, v);
  }

  /**
   * Performs a ``soft" equals check.
   * 
   * @param A
   * @param B
   * @return double
   */
  static final boolean equal(double A, double B) {
    double maxRelativeError = 0.00000001;
    if (A == B) {
      return true;
    }
    double relativeError;
    if (Math.abs(B) > Math.abs(A)) {
      relativeError = Math.abs((B - A) / B);
    } else {
      relativeError = Math.abs((A - B) / A);
    }
    if (relativeError <= maxRelativeError) {
      return true;
    }
    return false;
  }

  /**
   * Check if A soft-equals One
   * @param A
   * @return if equal to one
   */
  static final boolean One(double A) {
    return (A >= 0 && A < 1E-12) || (A < 0 && (-1 * A) < 1E-12);
  }

  /**
   * Check if A soft-equals Zero
   * @param A
   * @return if equal to zero
   */
  static final boolean Zero(double A) {
    return A < -15;
  }

  /**
   * Exponentiate an array of log-space doubles
   * 
   * @param vals
   * @return double[]
   */
  static double[] exp(double... vals) {
    double[] newV = new double[vals.length];
    for (int i = 0; i < vals.length; i++) {
      newV[i] = Math.exp(vals[i]);
    }
    return newV;
  }
}

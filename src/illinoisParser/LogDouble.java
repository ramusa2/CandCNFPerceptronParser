package illinoisParser;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Data-structure to replace Double where all operations are in Logspace
 * 
 * @author bisk1
 */
strictfp class LogDouble implements Serializable {
  private static final long serialVersionUID = 1272014L;
  private final static int LENGTH = 8;
  private final double[] da = new double[] {
      Log.ZERO, Log.ZERO, Log.ZERO, Log.ZERO,
      Log.ZERO, Log.ZERO, Log.ZERO, Log.ZERO };
  private double sum = Log.ZERO;
  private boolean validSum = true;

  /**
   * Create LogDouble with value ZERO
   */
  public LogDouble() {}

  /**
   * Create LogDouble with value v
   * 
   * @param v
   * @throws Exception
   */
  LogDouble(double v) throws Exception {
    this();
    da[LENGTH - 1] = v;
    sum = v;
  }

  /**
   * Assigns a specific value to the data-structure
   * @param v value
   */
  final void set(double v) {
    zero();
    da[LENGTH - 1] = v;
    sum = v;
  }

  /**
   * Increment value
   * 
   * @param v
   * @throws Exception
   */
  final void add(double v) throws Exception {
    // TODO: Profile this and check if precision is necessary/efficient
    // Binary search for where to insert (i.e. between the closest two)
    int i = Arrays.binarySearch(da, v);
    if (i < 0) {
      i = (-1) * i - 1;
    }

    // which is closer?
    i = (i == LENGTH) ? i - 1 : i;
    int closer = (i == 0) ? i :
      (da[i - 1] == Log.ZERO || v - da[i - 1] < da[i] - v)
      ? i - 1 : i;

    // merge to closest
    try {
      da[closer] = Log.add(da[closer], v);
    } catch (Exception e) {
      Util.SimpleError(e.getMessage());
      // Find closest pair
      double closest = Double.MAX_VALUE;
      int index = 0;
      for (int j = 0; j < LENGTH - 1; j++) {
        if (da[j + 1] - da[j] < closest) {
          closest = da[j + 1] - da[j];
          index = j;
        }
      }
      // These are the closes so it's sloppy but least precision is lost
      da[index + 1] = Log.sloppy_add(da[index + 1], da[index]);
      da[index] = v;
      Arrays.sort(da);
    }

    // propogate
    double tmp;
    i = closer;
    while (i < LENGTH - 1 && da[i + 1] < da[i]) {
      tmp = da[i + 1];
      da[i + 1] = da[i];
      da[i] = tmp;
      i++;
    }

    // Sum no longer valid
    validSum = false;
  }

  /**
   * Get value
   * 
   * @return double
   * @throws Exception
   */
  final double value() throws Exception {
    if (!validSum) {
      updateSum();
    }
    return sum;
  }

  private final void updateSum() throws Exception {
    sum = Log.sum(da);
    validSum = true;
  }

  private final void zero() {
    Arrays.fill(da, Log.ZERO);
    sum = Log.ZERO;
    validSum = true;
  }

  /**
   * Zero out data-structure
   */
  final void clear() {
    zero();
  }

  @Override
  public String toString() {
    try {
      return Double.toString(value());
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      System.exit(1);
      return null;
    }
  }

  /**
   * Get hidden values that define value
   * 
   * @return double[]
   */
  final double[] vals() {
    return da;
  }

  /**
   * Add to another LogDouble
   * 
   * @param other
   * @throws Exception
   */
  final void merge(LogDouble other) throws Exception {
    for (double d : other.vals()) {
      add(d);
    }
  }
}

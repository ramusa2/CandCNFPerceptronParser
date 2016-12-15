package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

/**
 * Provides basic Double array functionality while maintaining the small storage
 * size of doubles rather than Doubles
 * 
 * @author bisk1
 */
class DoubleArray implements Externalizable {

  private double[] da = new double[] {};
  private int size = 0;
  private final static double INCREMENT = 1.5;
  private int length = 0;
  private double sum = Log.ZERO;
  private boolean valid = true;

  /**
   * Default constructor
   */
  public DoubleArray() {}

  /**
   * Create empty array of length s
   * 
   * @param s
   *          length
   */
  public DoubleArray(int s) {
    da = new double[s];
    Arrays.fill(da, Log.ZERO);
    size = s;
    length = s;
  }

  /**
   * Return primitive double array
   * 
   * @return double[]
   */
  final double[] vals() {
    return da;
  }

  private final void set(int i, double v) {
    if (i >= length) {
      da = Arrays.copyOf(da, (int) ((i * INCREMENT) + 1));
      Arrays.fill(da, length, da.length, Log.ZERO);
      length = da.length;
    }
    da[i] = v;
    valid = false;
  }

  /**
   * Adds element v to the end of the array
   * 
   * @param v
   */
  final synchronized void add(double v) {
    set(size, v);
    size++;
  }

  /**
   * Sums values (assumes logspace) and returns a ONE element array
   * 
   * @return DoubleArray
   * @throws Exception
   */
  final synchronized DoubleArray collapse() throws Exception {
    double t = sum();
    clear();
    DoubleArray newA = new DoubleArray();
    newA.add(t);
    return newA;
  }

  /**
   * Retrieve index i
   * 
   * @param i
   * @return double
   */
  final double get(int i) {
    return da[i];
  }

  /**
   * Sums (assumes log space) the value of the array
   * 
   * @return double
   * @throws Exception
   */
  final double sum() throws Exception {
    if (!valid) {
      sum = Log.sum(da);
      valid = true;
    }
    return sum;
  }

  /**
   * Returns the product (assumes logspace) of the values in the array
   * 
   * @return double
   * @throws Exception
   */
  final double prod() throws Exception {
    Arrays.sort(da);
    double product = Log.ONE;
    // for(int i = da.length-1; i >= 0; i--){
    for (int i = 0; i < da.length; i++) {
      if (da[i] != Log.ZERO) {
        product = Log.mul(product, da[i]);
      }
    }
    return product;
  }

  /**
   * Log space ZERO-ing out
   */
  final void clear() {
    zero();
    size = 0;
  }

  private final void zero() {
    Arrays.fill(da, Log.ZERO);
    sum = Log.ZERO;
    valid = true;
  }

  @Override
  public void readExternal(ObjectInput in) throws IOException,
  ClassNotFoundException {
    da = (double[]) in.readObject();
    length = da.length;
    size = in.readInt();
    sum = in.readDouble();
    valid = in.readBoolean();
  }

  @Override
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(da);
    out.writeInt(size);
    out.writeDouble(sum);
    out.writeBoolean(valid);
  }

  /**
   * Merges a given double[]
   * 
   * @param arr
   */
  public void addAll(double[] arr) {
    // TODO(bisk1): This should be a single operation
    for (double d : arr) {
      add(d);
    }
  }

  @Override
  public String toString() {
    return Arrays.toString(Arrays.copyOf(da, size));
  }
}

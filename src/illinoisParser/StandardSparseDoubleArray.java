package illinoisParser;

import java.io.Serializable;
import java.util.Arrays;

public strictfp class StandardSparseDoubleArray implements Serializable {
  private static final long serialVersionUID = 5142012L;

  private double[] da = new double[] {};
  private int[] ia = new int[] {};
  int size = 0;
  private double sum = 0;
  private boolean valid = true;
  private int max_v = Integer.MIN_VALUE;
  private int min_v = Integer.MAX_VALUE;

  public StandardSparseDoubleArray() {}

  final void add(int i, double v) {
    if (i > max_v)
      max_v = i;
    if (i < min_v)
      min_v = i;
    if (ia.length == 0) {
      ia = new int[] { i };
      da = new double[] { v };
      size = 1;
      valid = false;
      return;
    }

    int iI = (-1) * Util.binarySearch(ia, i) - 1;

    int[] nia = new int[ia.length + 1];
    double[] nda = new double[da.length + 1];

    System.arraycopy(ia, 0, nia, 0, iI);
    System.arraycopy(da, 0, nda, 0, iI);

    System.arraycopy(ia, iI, nia, iI + 1, ia.length - iI);
    System.arraycopy(da, iI, nda, iI + 1, da.length - iI);

    ia = nia;
    da = nda;

    size = ia.length;

    ia[iI] = i;
    da[iI] = v;
    valid = false;
  }

  final void set(int i, double v) {
    int ind = getI(i);
    if (ind > -1)
      da[ind] = v;
    else
      add(i, v);
    valid = false;
  }

  public final void increment(int i, double v) throws Exception {
    if (contains(i))
      set(i, get(i) + v);
    else
      add(i, v);
  }

  private final int getI(int ind) {
    return Util.binarySearch(ia, ind);
  }

  public final double get(int i) {
    return da[getI(i)];
  }

  final double sum() throws Exception {
    if (!valid) {
      sum = 0;
      for (double d : da)
        sum += d;
      valid = true;
    }
    return sum;
  }

  final void zero() {
    Arrays.fill(da, 0.0);
  }

  public final int size() {
    return size;
  }

  public int[] indices() {
    return ia;
  }

  boolean contains(int b) {
    if (b > max_v || b < min_v)
      return false;
    return getI(b) >= 0;
  }

  String toStringExp() {
    String val = "";
    if (ia.length == 0)
      return val;
    for (int i = 0; i <= ia[ia.length - 1]; i++) {
      if (contains(i))
        val += Math.exp(get(i)) + " ";
    }
    return val;
  }

  public String toString() {
    String val = "";
    if (ia.length == 0)
      return val;
    for (int i = 0; i <= ia[ia.length - 1]; i++) {
      if (contains(i))
        val += get(i) + " ";
      else
        val += " x ";
    }
    return val;
  }

  void remove(int i) {
    System.err.println("This isn't efficient (see next two lines)");
    min_v = Integer.MAX_VALUE;
    max_v = Integer.MIN_VALUE;

    int iI = getI(i);
    int[] nia = new int[ia.length - 1];
    double[] nda = new double[da.length - 1];

    if (iI < 0)
      Util.Error("Invalid Index: " + iI);

    if (nia.length != 0) {
      System.arraycopy(ia, 0, nia, 0, iI);
      System.arraycopy(da, 0, nda, 0, iI);

      System.arraycopy(ia, iI + 1, nia, iI, ia.length - iI - 1);
      System.arraycopy(da, iI + 1, nda, iI, da.length - iI - 1);
    }
    ia = nia;
    da = nda;
    size = ia.length;
    valid = false;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  double[] vals() {
    return da;
  }

  public void normalize() throws Exception {
    double s = sum();
    for (int i = 0; i < da.length; i++)
      da[i] /= s;
  }

}

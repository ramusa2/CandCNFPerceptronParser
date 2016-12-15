package illinoisParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * File provides printing/logging utilities and array operations
 * 
 * @author bisk1
 */
public class Util {
	/**
	 * Regex Pattern for whitespace
	 */
	static Pattern whitespace_pattern = Pattern.compile("\\s+");
	private static final long[] byteTable = createLookupTable();
	private static final long HSTART = 0xBB40E64DA205B064L;
	private static final long HMULT = 7664345821815920749L;

	/**
	 * Throws an exception with message o.tostring()
	 * 
	 * @param o
	 */
	public static void Error(Object o) {
		try {
			throw new Exception(o.toString());
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Print array <sA> to String
	 * 
	 * @param sA
	 * @return String
	 **/
	static String toString(String[] sA) {
		String s = "";
		for (String e : sA) {
			s += e + " ";
		}
		return s;
	}

	/**
	 * Print array <dA> to String, w/ full/limited precision
	 * 
	 * @param dA
	 * @param full
	 * @return String
	 **/
	static String toString(double[] dA, boolean full) {
		if (full) {
			return Arrays.toString(dA);
		}
		String s = "[";
		for (Double d : dA) {
			if (d == 0) {
				s += String.format("%9d, ", 0);
			} else {
				s += String.format("%1.6f, ", d);
			}
		}
		s = s.substring(0, s.lastIndexOf(","));
		return s + "]";
	}

	/**
	 * Print array <dA> to String, w/ full/limited precision
	 * 
	 * @param dA
	 * @return String
	 * @throws Exception
	 **/
	static String toString(LogDouble[] dA) throws Exception {
		String s = "[";
		for (LogDouble d : dA) {
			if (d.value() == 0) {
				s += String.format("%9d, ", 0);
			} else {
				s += String.format("%1.6f, ", d.value());
			}
		}
		s = s.substring(0, s.lastIndexOf(","));
		return s + "]";
	}

	/**
	 * Print array <dA> to String, w/ full/limited precision exponentiated
	 * 
	 * @param dA
	 * @param full
	 * @return String
	 **/
	static String toExpString(double[] dA, boolean full) {
		String s = "[";
		for (Double d : dA) {
			if (d == 0) {
				s += "1, ";
			} else if (!full) {
				s += String.format("%1.5f, ", Math.exp(d));
			} else {
				s += String.format("%f, ", Math.exp(d));
			}
		}
		s = s.substring(0, s.lastIndexOf(","));
		return s + "]";
	}

	/**
	 * Print array <dA> to String, w/ full/limited precision exponentiated
	 * 
	 * @param dA
	 * @param full
	 * @return String
	 * @throws Exception
	 **/
	static String toExpString(LogDouble[] dA, boolean full) throws Exception {
		String s = "[";
		for (LogDouble d : dA) {
			if (d == null) {
				s += "null,";
			} else if (d.value() == 0) {
				s += "1, ";
			} else if (!full) {
				s += String.format("%1.5f, ", Math.exp(d.value()));
			} else {
				s += String.format("%f, ", Math.exp(d.value()));
			}
		}
		s = s.substring(0, s.lastIndexOf(","));
		return s + "]";
	}

	/**
	 * Print o.toString()
	 * 
	 * @param o
	 **/
	static void Println(Object o) {
		System.out.println(o.toString());
	}

	/**
	 * Print an array os of objects to screen
	 * 
	 * @param os
	 */
	static void Println(Object... os) {
		System.out.println(Arrays.toString(os));
	}

	/**
	 * Print o.toString()
	 * 
	 * @param o
	 **/
	static void Print(Object o) {
		System.out.print(o.toString());
	}

	public static long START;
	static ArrayList<String> STAMPS = new ArrayList<String>();
	static ArrayList<Double> TIMES = new ArrayList<Double>();

	/**
	 * Add a time stamp
	 * 
	 * @param msg
	 */
	static void timestamp(String msg) {
		STAMPS.add(msg);
		TIMES.add(
				new Double(
						new java.sql.Timestamp(
								Calendar.getInstance().getTime().getTime()).getTime()
								- START) / 60000);
	}

	public static void printTimes() {
		Util.logln("");
		Util.timestamp("Finish");
		for (int i = 0; i < STAMPS.size(); i++) {
			Util.logln(String.format("%-30s %f", STAMPS.get(i), TIMES.get(i)));
		}

		Util.logln("\nDistribution of Time");
		HashMap<String, Double> diffs = new HashMap<String, Double>();
		int count = 1; // For Yonatan
		double pd = 0.0;
		String pS = "Startup";
		diffs.put("Startup", 0.0);
		for (int i = 0; i < STAMPS.size(); i++) {
			String stamp = STAMPS.get(i);
			Double time = TIMES.get(i);

			if (stamp.contains("Map")) {
				stamp += " " + count;
			}
			if (stamp.equals("Test")) {
				count += 1;
			}

			if (!diffs.containsKey(stamp)) {
				diffs.put(stamp, 0.0);
			}
			diffs.put(pS, diffs.get(pS) + (time - pd));

			pS = stamp;
			pd = time;
		}
		ArrayList<ObjectDoublePair<String>> pairs =
				new ArrayList<ObjectDoublePair<String>>();
		for (String label : diffs.keySet()) {
			pairs.add(new ObjectDoublePair<String>(label, diffs.get(label)));
		}
		Collections.sort(pairs);
		Util.logln(String.format("%-30s %4s %s", "Action", "%", "  Min"));
		Util.logln("------------------------------------------");
		for (ObjectDoublePair<String> pair : pairs) {
			Util.logln(String.format("%-30s %3.2f %2.5f", pair.content(),
					100 * pair.value() / pd, pair.value()));
		}
	}

	public static PrintWriter OUTPUT_FILE;

	/**
	 * Prints error o.toString() but does not throw an exception
	 * 
	 * @param o
	 **/
	static void SimpleError(Object o) {
		System.err.println(o);
		if (Configuration.DEBUG) {
			OUTPUT_FILE.print("SIMPLE ERROR: " + o.toString() + "\n");
		}
	}

	/**
	 * Checks if <arr> contains <a>
	 * 
	 * @param arr
	 * @param a
	 * @return boolean
	 **/
	public static final boolean contains(int[] arr, int a) {
		for (int i : arr) {
			if (i == a) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Prints dots to screen depending on <cur>
	 * 
	 * @param cur
	 */
	static final void stat(int cur) {
		if (cur > 0 && cur % 100 == 0) {
			log(".");
		}
		if (cur > 0 && cur % 1000 == 0) {
			log("\t");
		}
		if (cur > 0 && cur % 10000 == 0) {
			log("\n");
		}
	}

	static int total = 0;
	static int percent = 1;
	/**
	 * Print percentage
	 * @param cur count
	 */
	static final void percent(float cur) {
		if(cur % 100 == 0)
		{
			//Util.Print("\r" + new String(new char[(int)(100*cur/(total-1))]).replace("\0", "="));
			Util.Print("\r" + (int)(100*cur/(total-1)) + "%");
		}
		if (cur == total-1) {
			Util.Print("\r100%\t");
		}
	}

	/**
	 * Prints a <string> both to screen and the log file
	 * 
	 * @param string
	 */
	public static void log(String string) {
		//if (Configuration.printToScreen) {
		System.out.print(string);
		//}
		//OUTPUT_FILE.print(string);
	}

	/**
	 * log(string) with newline
	 * 
	 * @param string
	 **/
	public static void logln(String string) {
		log(string + "\n");
	}

	/**
	 * Prints string to log only, avoiding screen
	 * 
	 * @param string
	 */
	static void logOnly(String string) {
		OUTPUT_FILE.print(string);
	}

	/**
	 * Checks if array arr contains test value
	 * 
	 * @param arr
	 * @param test
	 * @return boolean
	 */
	static final boolean contains(Object[] arr, Object test) {
		for (Object o : arr) {
			if (o != null && test.equals(o)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Escapes characters for printing to LaTeX
	 * 
	 * @param s
	 *          input
	 * @return String
	 */
	static final String escape_chars(String s) {
		if (s.isEmpty()) {
			return s;
		}
		String n = s.replaceAll("\\$", "\\\\\\$").replaceAll("_", "\\\\_")
				.replaceAll("%", "\\\\%").replaceAll("&", "\\\\&")
				.replaceAll("#", "\\\\#").replaceAll("\\^", "caret")
				.replaceAll("\\{", "\\\\\\$\\\\\\{\\\\\\$").replaceAll("\\}", "\\\\\\$\\\\\\}\\\\\\$");
		return n;
	}

	/**
	 * http://ochafik.com/blog/?p=106 Searches a sorted int array for a specified
	 * value, using an optimized binary search algorithm (which tries to guess
	 * smart pivots).<br />
	 * The result is unspecified if the array is not sorted.<br />
	 * The method returns an index where key was found in the array. If the array
	 * contains duplicates, this might not be the first occurrence.
	 * 
	 * @see #java.util.Arrays.sort(int[])
	 * @see #java.util.Arrays.binarySearch(int[])
	 * @param array
	 *          sorted array of integers
	 * @param key
	 *          value to search for in the array
	 * @return index of an occurrence of key in array, or -(insertionIndex + 1) if
	 *         key is not contained in array (<i>insertionIndex</i> is then the
	 *         index at which key could be inserted).
	 */
	static final int binarySearch(int[] array, int key) {// min, int max) {
		if (array.length == 0) {
			return -1;
		}
		int min = 0, max = array.length - 1;
		int minVal = array[min], maxVal = array[max];

		int nPreviousSteps = 0;

		// Uncomment these two lines to get statistics about the average number of
		// steps in the test report :
		// totalCalls++;
		for (;;) {
			// totalSteps++;

			// be careful not to compute key - minVal, for there might be an integer
			// overflow.
			if (key <= minVal) {
				return key == minVal ? min : -1 - min;
			}
			if (key >= maxVal) {
				return key == maxVal ? max : -2 - max;
			}

			assert min != max;

			int pivot;
			// A typical binarySearch algorithm uses pivot = (min + max) / 2.
			// The pivot we use here tries to be smarter and to choose a pivot close
			// to the expectable location of the key.
			// This reduces dramatically the number of steps needed to get to the key.
			// However, it does not work well with a logaritmic distribution of
			// values, for instance.
			// When the key is not found quickly the smart way, we switch to the
			// standard pivot.
			if (nPreviousSteps > 2) {
				pivot = (min + max) >> 1;
				// stop increasing nPreviousSteps from now on
			} else {
				// NOTE: We cannot do the following operations in int precision, because
				// there might be overflows.
				// long operations are slower than float operations with the hardware
				// this was tested on (intel core duo 2, JVM 1.6.0).
				// Overall, using float proved to be the safest and fastest approach.
				pivot = min + (int) ((key - (float) minVal)
						/ (maxVal - (float) minVal) * (max - min));
				nPreviousSteps++;
			}

			int pivotVal = array[pivot];

			// NOTE: do not store key - pivotVal because of overflows
			if (key > pivotVal) {
				min = pivot + 1;
				max--;
			} else if (key == pivotVal) {
				return pivot;
			} else {
				min++;
				max = pivot - 1;
			}
			maxVal = array[max];
			minVal = array[min];
		}
	}

	/**
	 * Determines if a file is GZIPed or not based on filename and then creates
	 * a buffered reader
	 * @param filename
	 * @return
	 *    A BufferedReader
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	static BufferedReader TextFileReader(String filename)
			throws UnsupportedEncodingException,
			FileNotFoundException,
			IOException{
		if(filename.endsWith(".gz")) {
			return new BufferedReader(new InputStreamReader(new GZIPInputStream(
					new FileInputStream(new File(filename))), "UTF-8"));
		}
		return new BufferedReader(new InputStreamReader(new FileInputStream(
				new File(filename)), "UTF-8"));
	}
	/**
	 * @param filename
	 * @return A new buffered Writer
	 * @throws UnsupportedEncodingException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static BufferedWriter TextFileWriter(String filename)
			throws UnsupportedEncodingException,
			FileNotFoundException,
			IOException{
		if(filename.endsWith(".gz")) {
			return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
					new FileOutputStream(new File(filename))), "UTF-8"));
		}
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
				new File(filename)), "UTF-8"));

	}

	/**
	 * A 64 bit String hash
	 * @param value
	 * @return string's hash value
	 */
	static final long hash(String value) {
		byte[] data;
		try {
			data = value.getBytes("UTF-8");
			long h = HSTART;
			final long hmult = HMULT;
			final long[] ht = byteTable;
			for (int len = data.length, i = 0; i < len; i++) {
				h = (h * hmult) ^ ht[data[i] & 0xff];
			}
			return h;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			Util.Error("Hash failures on encoding");
		}
		return -1;
	}
	private static final long[] createLookupTable() {
		long[] Table = new long[256];
		long h = 0x544B2FBACAAF1684L;
		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 31; j++) {
				h = (h >>> 7) ^ h;
				h = (h << 11) ^ h;
				h = (h >>> 10) ^ h;
			}
			Table[i] = h;
		}
		return Table;
	}

	public static void openLog(String file) {
		try {
			Util.OUTPUT_FILE = new PrintWriter(new File(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Util.START = new java.sql.Timestamp(
				Calendar.getInstance().getTime().getTime()).getTime();
	}

	public static void closeLog() {
		Util.printTimes();
		Util.OUTPUT_FILE.close();
	}

	/**
	 * Recursively construct an AUTO format Parse
	 * @param model   Model with hashes
	 * @param tree    Tree to recurse
	 */
	public static void buildAUTORecurse(StringBuilder AUTOparse, Sentence sentence,
			Model model, Tree tree) {
		buildAUTORecurse(AUTOparse, sentence, model.grammar, tree); 
	}

	public static void buildAUTORecurse(StringBuilder AUTOparse, Sentence sentence,
			Grammar grammar, Tree tree) {
		//	    if (tree.B != null) {
			//	      // LEX
		//	      if (tree.B.B == null) {

		// Q: why is LEX checking for tree's child child to be null? modified

		if (tree.B == null) {

			// Standard AUTO
			// (<L ccgcategory tag tag word indexed>)
			// TODO: get actual word if word token is labeled "UNK"
			String word = sentence.get(tree.getX()).getWord();

			//String word = model.grammar.Words.get(sentence.get(tree.getX()).getWord());
			//if (sentence.get(tree.getX()).tag().isNum()) {
			//  word = model.grammar.Words.get(sentence.get(tree.getX()).lemma());
			//}

			AUTOparse.append(" (<L ").append(grammar.prettyCat(tree.A).replace("\\.", "\\").replace("/.", "/"))
			.append(' ').append(sentence.get(tree.getX()).getPOS())
			.append(' ').append(sentence.get(tree.getX()).getPOS())
			.append(' ').append(word)
			.append(' ').append(tree.ccgcat.catStringIndexed())
			.append(">");

		} else {
			if (tree.A != grammar.TOP) {
				AUTOparse.append(" (<T ").append(grammar.prettyCat(tree.A).replace(".", ""));

				// UNARY
				if (tree.C == null) {
					AUTOparse.append(" 0 1>");
				} else {
					if (((Binary)tree.rule).head.equals(Rule_Direction.Left)) {
						AUTOparse.append(" 0 2>");
					} else {
						AUTOparse.append(" 1 2>");
					}
				}
			}
		}


		if (tree.B != null) {
			buildAUTORecurse(AUTOparse, sentence, grammar, tree.B);
		}

		if (tree.C != null) {
			buildAUTORecurse(AUTOparse, sentence, grammar, tree.C);
		}

		AUTOparse.append(")");
	}

	
	public static <T> boolean arrayContains(T[] array, T v) {
	    for (final T e : array)
	        if (e == v || v != null && v.equals(e))
	            return true;

	    return false;
	}

	
	public static boolean arrayContains(int[] array, int v) {
	    for (int e : array)
	        if (e == v) {
	            return true;
	        }
	    return false;
	}
}

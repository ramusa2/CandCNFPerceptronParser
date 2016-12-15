package illinoisParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Specifices the tagset. This includes defining classes.
 * 
 * @author bisk1
 */
public class TAGSET {
  /**
   * String representations of the tags
   */
  final static ArrayList<String> STRINGS = new ArrayList<String>();
  /**
   * A None tag.
   */
  final static POS NONE = new POS("NONE");
  /**
   * Verbs
   */
  static POS[] verbs = new POS[0];
  /**
   * Verbs allowed to be produced by TOP
   */
  static POS[] verbsTOP = new POS[0];
  /**
   * Function words
   */
  static POS[] func = new POS[0];
  /**
   * Nouns
   */
  static POS[] nouns = new POS[0];
  /**
   * Conjunctions
   */
  static POS[] conj = new POS[] {}; //new POS(","),new POS(";"), new POS(":")
  /**
   * Punctuation
   */
  static POS[] punct = new POS[] { new POS(":"), // new POS(",")
    new POS("."), new POS("?"), new POS("!"),
    //  Unidcode for new POS("»"), new POS("«"), new POS("“"), new POS("”"),
    new POS(String.valueOf(Character.toChars('\u00BB'))),
    new POS(String.valueOf(Character.toChars('\u00AB'))),
    new POS(String.valueOf(Character.toChars('\u201C'))),
    new POS(String.valueOf(Character.toChars('\u201D'))),
    new POS("("), new POS(")"),
    new POS("{"), new POS("}"), new POS("["),
    new POS("]"), new POS("<"), new POS(">"),
    new POS("-"), new POS("--"), new POS("``"),
    new POS("\'\'"), new POS("\"") };
  /**
   * Full tagset
   */
  static POS[] tags = new POS[0];
  /**
   * Prepositions
   */
  static POS[] prep = new POS[0];

  /**
   * Blank constructor used with Induced tagsets
   */
  TAGSET() {}

  /**
   * Reads tagset from (filename). Each line is of the form: <br>
   * Format:
   * <table>
   * <tr>
   * <td><b>tag</b></td>
   * <td><b>class</b></td>
   * <td><b>comment/information</b></td>
   * </tr>
   * <tr>
   * <td>NNP</td>
   * <td>noun</td>
   * <td>// Proper noun, singular/td>
   * </tr>
   * </table>
   * where class = noun, verb, func, punct
   * 
   * @param filename
   * @throws IOException
   */
  public TAGSET(String filename) throws IOException {
    BufferedReader br = Util.TextFileReader(filename);
    String strLine;
    while ((strLine = br.readLine()) != null) {
      strLine = strLine.split("//")[0];
      if (strLine.length() > 0) {
        String[] vals = strLine.split("\\s+");

        // Case 1: Bracketing [ ] bracket
        if (vals.length == 3 && vals[2].equals("bracket")) {
          POS left = new POS(vals[0]);
          POS right = new POS(vals[1]);
          punct = add(punct, left);
          punct = add(punct, right);
          tags = add(tags, left);
          tags = add(tags, right);
        } else {
          POS tag = new POS(vals[0]);
          tags = add(tags, tag);
          for (int i = 1; i < vals.length; i++) {
            if (vals[i].equals("verb")) {
              verbs = add(verbs, tag);
            }
            if (vals[i].equals("top")) {
              verbsTOP = add(verbsTOP, tag);
            }
            if (vals[i].equals("func")) {
              func = add(func, tag);
            }
            if (vals[i].equals("noun")) {
              nouns = add(nouns, tag);
            }
            if (vals[i].equals("conj")) {
              conj = add(conj, tag);
            }
            if (vals[i].equals("punct")) {
              punct = add(punct, tag);
            }
            if (vals[i].equals("prep")) {
              prep = add(prep, tag);
            }
          }
        }
      }
    }
    // Allow all verbs as TOP if none are specifically specified
    if (verbsTOP.length == 0) {
      verbsTOP = Arrays.copyOf(verbs, verbs.length);
    }
    // print();
    br.close();
  }

  /**
   * Prints the current tagset and mappings to screen.
   */
  static void print() {
    Util.logln("POS Mapping:");
    Util.logln("verbs:\t" + Arrays.toString(verbs));
    Util.logln("nouns:\t" + Arrays.toString(nouns));
    Util.logln("TOP:\t" + Arrays.toString(verbsTOP));
    Util.logln("func:\t" + Arrays.toString(func));
    Util.logln("conj:\t" + Arrays.toString(conj));
    Util.logln("punct:\t" + Arrays.toString(punct));
    Util.log("\n");
    Util.logln("prep:\t" + Arrays.toString(prep));
    Util.logln("tags:\t" + Arrays.toString(tags));
  }

  /**
   * Adds the given tag (t) to the provided list (addTo) if it isn't already
   * present
   * 
   * @param addTo
   * @param t
   * @return POS[]
   */
  static POS[] add(POS[] addTo, POS t) {
    if (!contains(addTo, t)) {
      POS[] temp = Arrays.copyOf(addTo, addTo.length + 1);
      temp[temp.length - 1] = t;
      return temp;
    }
    return addTo;
  }

  private static POS[] remove(POS[] takeFrom, POS t) {
    if (contains(takeFrom, t)) {
      POS[] temp = new POS[takeFrom.length - 1];
      int i = 0;
      for (POS test : takeFrom) {
        if (!test.equals(t)) {
          temp[i] = test;
          i++;
        }
      }
      return temp;
    }
    return takeFrom;
  }

  /**
   * Adds tag as punctuation
   * 
   * @param tag
   */
  static void addPunctuationTag(POS tag) {
    punct = add(punct, tag);
    tags = add(tags, tag);
  }

  /**
   * Creates a new tag from String.
   * 
   * @param val
   * @return int
   */
  public static int add(String val) {
    int i = STRINGS.indexOf(val);
    if (i == -1) {
      STRINGS.add(val);
      return STRINGS.size() - 1;
    }
    return i;
  }

  /**
   * Returns the POS value of a string if present.  Returns null otherwise.
   * @param val human readable POS
   * @return POS
   */
  static POS valueOf(String val){
	  /*
    if (!STRINGS.contains(val)) {
      return null;
    }
    */
    return new POS(val);
  }

  /**
   * Checks if a given array of tags contains the query
   * 
   * @param check
   * @param t
   * @return boolean
   */
  static boolean contains(POS[] check, POS t) {
    if (t == null) {
      return false;
    }
    for (POS comp : check) {
      if (comp.equals(t)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns array of verbs
   * 
   * @return POS[]
   */
  static POS[] verb() {
    return verbs;
  }

  /**
   * Checks if a tag is a verb
   * 
   * @param t
   * @return boolean
   */
  public static boolean verb(POS t) {
    return contains(verbs, t);
  }

  /**
   * Returns array of nouns
   * 
   * @return POS[]
   */
  static POS[] noun() {
    return nouns;
  }

  /**
   * Checks if a tag is a noun
   * 
   * @param t
   * @return boolean
   */
  static boolean noun(POS t) {
    return contains(nouns, t);
  }

  /**
   * Returns array of function words
   * 
   * @return POS[]
   */
  static POS[] func() {
    return func;
  }

  /**
   * Checks if a tag is a function word
   * 
   * @param t
   * @return boolean
   */
  static boolean func(POS t) {
    return contains(func, t);
  }

  /**
   * Returns the full set of tags
   * 
   * @return POS[]
   */
  static POS[] tags() {
    return tags;
  }

  /**
   * Checks if a tag is a conjunction
   * 
   * @param t
   * @return boolean
   */
  static boolean CONJ(POS t) {
    return contains(conj, t);
  }

  /**
   * Checks if a tag is punctuation
   * 
   * @param t
   * @return boolean
   */
  public static boolean Punct(POS t) {
    return contains(punct, t);
  }

  /**
   * Removes a tag from all categories
   * 
   * @param tag
   */
  static void remove(POS tag) {
    tags = remove(tags, tag);
    verbs = remove(verbs, tag);
    verbsTOP = remove(verbsTOP, tag);
    func = remove(func, tag);
    nouns = remove(nouns, tag);
    conj = remove(conj, tag);
    punct = remove(punct, tag);
    prep = remove(prep, tag);
  }

  /**
   * Converts a String to a tag
   * 
   * @param tag
   * @return POS
   */
  static POS convert(String tag) {
    // Added by Ryan:
    if (tag.equals("(null)")) {
      return new POS("BUG");
    }
    return new POS(tag);
  }

  /**
   * Checks if a verb is allows to be produced by TOP
   * 
   * @param t
   * @return boolean
   */
  static boolean verbTOP(POS t) {
    return contains(verbsTOP, t);
  }

  /**
   * checks if an array of tags contains a noun
   * 
   * @param local_tags
   * @return boolean
   * @throws Exception
   */
  static boolean containsNoun(POS[] local_tags) throws Exception {
    boolean contains = false;
    for (POS T : local_tags) {
      contains = contains || noun(T);
    }
    return contains;
  }

  /**
   * Converts a number to a simplified form
   * 
   * @param word
   * @return String
   */
  static final String convertNumber(String word) {
    if (word.length() == 4 && (word.startsWith("19") || word.startsWith("20"))) {
      return "X-YEAR-X";
    } else if (word.contains("/")) {
      return "X-FRAC-X";
    } else if (word.contains(":")) {
      return "X-TIME-X";
    } else {
      return word.replaceAll("[0-9]", "X");
    }
  }

  /**
   * Types of Tags
   * 
   * @author bisk1
   */
  enum TAG_TYPE {
    /**
     * Reduced tagset
     */
    Coarse,
    /**
     * Standard tagset
     */
    Fine,
    /**
     * Defined by Slav Petrov
     */
    Universal,
    /**
     * Induced via Baum-Welch code
     */
    Induced,
    /**
     * Non-Standard
     */
    Custom
  }
}

package illinoisParser;

import java.io.Externalizable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Data-structure for data read in that stores all information given.
 * 
 * @author bisk1
 */
public class Sentence implements Externalizable {
	/**
	 * Full sentence
	 */
	private LexicalToken[] sentence;

	/**
	 * Unique (within a Sentences collection) indicator for a sentence
	 */
	private int id;

	/** 
	 * The CCGbank parse, in AUTO format (for training)
	 */
	protected String autoString;

	/**
	 * Empty sentence
	 */
	public Sentence() {
		sentence = new LexicalToken[0];
		id = -1;
		autoString = "";
	}


	/**
	 * Sentence from tokenized string (words/POS/lexcats can be delimited with _ or |)
	 */
	public Sentence(String tokenized) {
		this();
		String[] toks = tokenized.trim().split("\\s+");
		for(String tok : toks) {
			String[] fields = tok.split("[_\\|]");
			if(fields.length >= 2) {

				LexicalToken lt;
				if(fields.length == 3) {
					lt = new LexicalToken(fields[0], fields[1], fields[2]);
				}
				else {
					lt = new LexicalToken(fields[0], fields[1]);
				}
				this.addLexicalItem(lt);
			}
		}
	}

	/**
	 * FST constructor
	 */
	public Sentence(LexicalToken[] tokens, int ID, String auto) {
		sentence = tokens;
		id = ID;
		autoString = auto;
	}

	/**
	 * Add a word to a sentence ( requires knowledge of the grammar ). Adds all
	 * tag types and lemma.
	 * 
	 * @param line
	 * @param g
	 * @param source
	 * @throws Exception
	 */
	public final void addLexicalItem(LexicalToken lt) {
		sentence = Arrays.copyOf(sentence, sentence.length + 1);
		sentence[sentence.length - 1] = lt;
	}

	/**
	 * Returns the length of the sentence
	 * 
	 * @return int
	 */
	public final int length() {
		return sentence.length;
	}

	/**
	 * Return the lexicaltoken at a specific index
	 * 
	 * @param i
	 * @return LexicalToken
	 */
	public final LexicalToken get(int i) {
		return sentence[i];
	}

	/**
	 * Returns sentence yield
	 */
	final String toString(int i, int j) {
		String toRet = "";
		for (; i <= j; i++) {
			toRet += get(i).toString() + " ";
		}
		return toRet.trim();
	}

	public String toString() {
		return toString(0, this.sentence.length - 1);
	}

	public String asWords() {
		return asWords(0, this.sentence.length-1);  
	}

	public final String asWords(int i, int j) {
		String toRet = "";
		for (; i <= j; i++) {
			toRet += get(i).getWord() + " ";
		}
		return toRet.trim();
	}

	public final String wordAt(int i) {
		return get(i).getWord();
	}

	/**
	 * Prints to stream ONE word per line
	 * 
	 * @param g
	 *          Grammar
	 * @param w
	 *          Writer
	 * @throws IOException
	 */
	final void print(Writer w) throws IOException {
		for (LexicalToken lt : sentence) {
			w.write(lt.toString()+"\n");
		}
		w.write("\n");
	}

	/**
	 * Associates a parse with this sentence
	 * @param auto the gold CCGbank parse, in AUTO format
	 */
	public void addCCGbankParse(String auto) {
		autoString = auto;
	}

	/**
	 * Returns the gold CCGbank parse
	 * 
	 * @return String
	 */
	public String getCCGbankParse() {
		return autoString;
	}

	/**
	 * Set the sentence's ID
	 */
	public void setID(int i) {
		id = i;
	}

	/**
	 * Retrieve the sentence's ID
	 */
	public int getID() {
		return id;
	}

	public POS[] getPOSTags() {
		POS[] tags = new POS[this.length()];
		for(int i=0; i<tags.length; i++) {
			tags[i] = this.get(i).getPOS();
		}
		return tags;
	}

	public LexicalToken[] getTokens() {
		return this.sentence;
	}


	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(sentence);
		out.writeInt(id);
		out.writeObject(autoString);
	}


	@Override
	public void readExternal(ObjectInput in) throws IOException,
	ClassNotFoundException {
		sentence = (LexicalToken[]) in.readObject();
		id = in.readInt();
		autoString = (String) in.readObject();
	}


	public void setSentence(LexicalToken[] tokens) {
		sentence = tokens;		
	}


	public static void writeToGZIPFile(File file, Collection<Sentence> sentences) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new GZIPOutputStream(new FileOutputStream(file)));
			out.writeInt(sentences.size());
			for(Sentence sen : sentences) {
				out.writeObject(sen);
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			System.err.println("Failed to write sentences (.auto.gz) file: "+file.getPath());
			e.printStackTrace();
		}
	}


	public static Collection<Sentence>readFromGZIPFile(File file) {
		ArrayList<Sentence> list = new ArrayList<Sentence>();
		ObjectInputStream in;
		try {
			in = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
			int numSens = in.readInt();
			for(int s=0; s<numSens; s++) {
				list.add((Sentence) in.readObject());
			}
			in.close();
		} catch (IOException e) {
			System.err.println("Failed to read sentences (.auto.gz) file: "+file.getPath());
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			System.err.println("Malformed sentences (.auto.gz) file: "+file.getPath());
			e.printStackTrace();
		}
		return list;
	}
}

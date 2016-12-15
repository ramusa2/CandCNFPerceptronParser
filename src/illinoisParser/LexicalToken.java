package illinoisParser;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 * A Lexical item which contains part-of-speech tags, word forms and optional
 * category
 * 
 * @author bisk1
 */
public class LexicalToken implements Externalizable {
	
	private String word;
	private POS pos;
	private String category;

	public static final String EMPTY_CAT = "EMPTY_CATEGORY_FIELD";

	public LexicalToken() {}
	
	/**
	 * Define a lexical item (with an "EMPTY" 
	 */
	public LexicalToken(String w, String posTag) {
		this(w, posTag, EMPTY_CAT);
	}

	/**
	 * Define a lexical item (with a category)
	 */
	public LexicalToken(String w, String posTag, String cat) {
		word = w;
		pos = TAGSET.valueOf(posTag);
		category = cat;
	}

	/**
	 * Retrieves the word
	 */
	public String getWord() {
		return word;
	}


	/**
	 * Retrieves the POS tag
	 */
	public POS getPOS() {
		return pos;
	}
	
	/**
	 * Returns the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Prints to stream a lexical line word lemma coarse fine universal induced
	 * 
	 * @param g
	 * @param w
	 * @throws IOException
	 */
	public String toString() {
		if(this.category.equals(EMPTY_CAT))
			return word+"_"+pos;
		return word+"_"+pos+"_"+category;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(word);
		out.writeObject(pos);
		out.writeObject(category);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		word = (String) in.readObject();
		pos = (POS) in.readObject();
		category = (String) in.readObject();
	}
}

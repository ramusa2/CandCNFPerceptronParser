package supertagger.lewissteedman;

public class LSVariableEntry {
	
	private final String word;
	
	private final int embeddingIndex;
	
	private final int capitalizationIndex;
	
	private final int suffixIndex;
	
	public LSVariableEntry(String myWord, int myEmbeddingIndex, int myCapitalizationIndex, int mySuffixIndex) {
		this.word = myWord;
		this.embeddingIndex = myEmbeddingIndex;
		this.capitalizationIndex = myCapitalizationIndex;
		this.suffixIndex = mySuffixIndex;
	}

	public String getWord() {
		return word;
	}

	public int getEmbeddingIndex() {
		return embeddingIndex;
	}

	public int getCapitalizationIndex() {
		return capitalizationIndex;
	}

	public int getSuffixIndex() {
		return suffixIndex;
	}

}

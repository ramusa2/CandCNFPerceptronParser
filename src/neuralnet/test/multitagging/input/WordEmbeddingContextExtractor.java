package neuralnet.test.multitagging.input;

import neuralnet.test.multitagging.MultitaggerExample;
import supertagger.nn.StringEmbeddings;

public class WordEmbeddingContextExtractor extends StringEmbeddingContextExtractor {

	public WordEmbeddingContextExtractor(int embeddingDimension,
			StringEmbeddings stringEmbeddings) {
		super(embeddingDimension, stringEmbeddings);
	}

	@Override
	protected String getKey(MultitaggerExample context) {
		return context.getWord();
	}
}

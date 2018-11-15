package org.easy.search.analysis;

import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.easy.search.analysis.PinyinNGramTokenFilter.OutputDirection;

public class PinyinNGramTokenFilterFactory extends TokenFilterFactory {
	private final int maxGramSize;
	private final int minGramSize;
	private final OutputDirection outputDirection;

	/**
	 * 
	 * @param args init param
	 */
	public PinyinNGramTokenFilterFactory(Map<String, String> args) {
		super(args);
		minGramSize = getInt(args, "minGram", PinyinNGramTokenFilter.DEFAULT_MIN_GRAM_SIZE);
		maxGramSize = getInt(args, "maxGram", PinyinNGramTokenFilter.DEFAULT_MAX_GRAM_SIZE);
		this.outputDirection = OutputDirection.getOutputDirection(
				get(args, "outputDirection", PinyinNGramTokenFilter.DEFAULT_OUTPUT_DIRECTION.getLabel()));
		if (!args.isEmpty()) {
			throw new IllegalArgumentException("Unknown parameters: " + args);
		}
	}

	@Override
	public TokenFilter create(TokenStream input) {
		return new PinyinNGramTokenFilter(input, minGramSize, maxGramSize, outputDirection);
	}
}

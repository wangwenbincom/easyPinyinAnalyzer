package org.easy.search.analysis;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(com.carrotsearch.randomizedtesting.RandomizedRunner.class)
public class PinyinNGramTokenFilterFactoryTest {

	private PinyinNGramTokenFilter filter;

	@Before
	public void before() throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("minGram", "1");
		params.put("maxGram", "20");
		params.put("outputDirection", "front");

		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.setReader(new StringReader("zhongguoren shixi xuexi"));
		this.filter = (PinyinNGramTokenFilter) new PinyinNGramTokenFilterFactory(params).create(tokenizer);
	}

	@Test
	public void test() throws IOException {
		this.filter.reset();
		int position = 0;
		while (this.filter.incrementToken()) {
			CharTermAttribute termAtt = this.filter.getAttribute(CharTermAttribute.class);
			String token = termAtt.toString();
			int increment = this.filter.getAttribute(PositionIncrementAttribute.class).getPositionIncrement();
			position += increment;
			OffsetAttribute offset = this.filter.getAttribute(OffsetAttribute.class);
			TypeAttribute type = this.filter.getAttribute(TypeAttribute.class);
			System.out.println(position + "[" + offset.startOffset() + "," + offset.endOffset() + "] (" + type.type()
					+ ")" + token);
		}

	}
}

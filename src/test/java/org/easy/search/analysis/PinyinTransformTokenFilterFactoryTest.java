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
public class PinyinTransformTokenFilterFactoryTest {

	private PinyinTransformTokenFilter filter;

	@Before
	public void before() throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put("outputFormat", "both");
		params.put("outOriginal", "false");
		params.put("mixShort", "3");

		MockTokenizer tokenizer = new MockTokenizer();
		tokenizer.setReader(new StringReader("科华万象 hello"));
		this.filter = (PinyinTransformTokenFilter) new PinyinTransformTokenFilterFactory(params).create(tokenizer);
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
			System.out.println(position + "[" + offset.startOffset() + "," + offset.endOffset() + "} (" + type.type()
					+ ") " + token);
		}

	}

}

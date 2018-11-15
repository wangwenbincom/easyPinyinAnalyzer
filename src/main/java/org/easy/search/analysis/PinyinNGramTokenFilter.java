package org.easy.search.analysis;

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.analysis.util.CharacterUtils;

public class PinyinNGramTokenFilter extends TokenFilter {

	public static final int DEFAULT_MAX_GRAM_SIZE = 10;
	public static final int DEFAULT_MIN_GRAM_SIZE = 1;

	/**
	 * 默认输出方向
	 */
	public static final OutputDirection DEFAULT_OUTPUT_DIRECTION = OutputDirection.BOTH;

	private final int minGram;
	private final int maxGram;
	private OutputDirection outputDirection;

	private final CharacterUtils charUtils;

	private char[] curTermBuffer;
	private int curTermLength;
	private int curCodePointCount;
	private int curGramSize;

	private int savePosIncr;
	private int savePosLen;

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
	private final PositionLengthAttribute posLenAtt = addAttribute(PositionLengthAttribute.class);
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	private boolean front = false;

	protected PinyinNGramTokenFilter(TokenStream input) {
		super(input);
		this.charUtils = CharacterUtils.getInstance();
		this.minGram = DEFAULT_MIN_GRAM_SIZE;
		this.maxGram = DEFAULT_MAX_GRAM_SIZE;
	}

	/**
	 * Creates EdgeNGramTokenFilter that can generate n-grams in the sizes of
	 * the given range
	 * 
	 * @param input
	 *            {@link TokenStream} holding the input to be tokenized	
	 * @param minGram
	 *            the smallest n-gram to generate
	 * @param maxGram
	 *            the largest n-gram to generate
	 * @param direction
	 *            输出方向
	 */
	public PinyinNGramTokenFilter(TokenStream input, int minGram, int maxGram, OutputDirection direction) {
		super(input);

		if (minGram < 1) {
			throw new IllegalArgumentException("minGram must be greater than zero");
		}

		if (minGram > maxGram) {
			throw new IllegalArgumentException("minGram must not be greater than maxGram");
		}

		this.charUtils = CharacterUtils.getInstance();
		this.minGram = minGram;
		this.maxGram = maxGram;
		this.outputDirection = direction;
	}

	@Override
	public final boolean incrementToken() throws IOException {
		while (true) {
			if (curTermBuffer == null) {
				if (!input.incrementToken()) {
					return false;
				} else {
					curTermBuffer = termAtt.buffer().clone();
					curTermLength = termAtt.length();
					curCodePointCount = charUtils.codePointCount(termAtt);
					curGramSize = minGram;

					savePosIncr += posIncrAtt.getPositionIncrement();
					savePosLen = posLenAtt.getPositionLength();
				}
			}
			if (curGramSize <= maxGram) { // if we have hit the end of our
											// n-gram
				// size range, quit
				if (curGramSize <= curCodePointCount) { // if the remaining
														// input is
					// too short, we can't
					// generate any n-grams
					// grab gramSize chars from front or back
					clearAttributes();
					// offsetAtt.setOffset(tofStart, tofEnd);
					// first ngram gets increment, others don't
					if (curGramSize == minGram) {
						posIncrAtt.setPositionIncrement(savePosIncr);
						savePosIncr = 0;
					} else {
						posIncrAtt.setPositionIncrement(0);
					}
					posLenAtt.setPositionLength(savePosLen);

					int start = 0, end = 0;
					
					switch (outputDirection) {
					case FRONT:
						front = false;
						start = 0;
						end = start + curGramSize;
						offsetAtt.setOffset(start, end);
						termAtt.copyBuffer(curTermBuffer, start, curGramSize);
						this.typeAtt.setType("NGram");
						break;
					case BACK:
						front = true;
						// back
						start = curTermLength - curGramSize;
						end = start + curGramSize;
						offsetAtt.setOffset(start, end);
						termAtt.copyBuffer(curTermBuffer, start, curGramSize);
						this.typeAtt.setType("NGram");
						break;
					default:
						if (!front) {
							front = true;
							// back
							start = curTermLength - curGramSize;
							end = start + curGramSize;
							offsetAtt.setOffset(start, end);
							termAtt.copyBuffer(curTermBuffer, start, curGramSize);
							this.typeAtt.setType("NGram");
							return true;
						} else {
							// front
							front = false;
							start = 0;
							end = start + curGramSize;
							offsetAtt.setOffset(start, end);
							termAtt.copyBuffer(curTermBuffer, start, curGramSize);
							this.typeAtt.setType("NGram");
						}
						break;
					}

					curGramSize++;
					return true;
				}
			}

			curTermBuffer = null;
		}
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		curTermBuffer = null;
		savePosIncr = 0;
	}

	/**
	 * 输出方向
	 */
	public static enum OutputDirection {

		/** 向前输出，左到右 */
		FRONT {
			@Override
			public String getLabel() {
				return "front";
			}
		},
		/** 向后输出，右到左 */
		BACK {
			@Override
			public String getLabel() {
				return "back";
			}
		},

		/** 正反同时输出 */
		BOTH {
			@Override
			public String getLabel() {
				return "both";
			}
		};

		public abstract String getLabel();

		// Get the appropriate OutFormat from a string
		public static OutputDirection getOutputDirection(String direction) {
			if (FRONT.getLabel().equals(direction)) {
				return FRONT;
			}
			if (BACK.getLabel().equals(direction)) {
				return BACK;
			}
			if (BOTH.getLabel().equals(direction)) {
				return BOTH;
			}
			return null;
		}

	}
}

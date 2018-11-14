package org.easy.search.analysis;

import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.easy.search.analysis.PinyinTransformTokenFilter.OutputFormat;

public class PinyinTransformTokenFilterFactory extends TokenFilterFactory {

	/**
	 * 输出原词元标识
	 */
	private boolean isOutOriginal = PinyinTransformTokenFilter.DEFAULT_IS_OUT_ORIGINAL;

	/**
	 * 中文词组最小转换长度，默认大于2的中文进行转换拼音
	 */
	private int minTermLength = PinyinTransformTokenFilter.DEFAULT_MIN_TERM_LENGTH;

	/**
	 * 输出格式，默认简拼和全拼全部输出
	 */
	private OutputFormat outputFormat = PinyinTransformTokenFilter.DEFAULT_OUTPUT_FORMAT;

	/**
	 * 混合输出，从左边开始，简拼的长度,默认为0，不输出混合拼音
	 */
	private int mixShortLength = PinyinTransformTokenFilter.DEFAULT_MIX_SHORT_LENGTH;

	/**
	 * 
	 * 
	 * @param args
	 */
	public PinyinTransformTokenFilterFactory(Map<String, String> args) {
		super(args);
		this.isOutOriginal = getBoolean(args, "outOriginal", PinyinTransformTokenFilter.DEFAULT_IS_OUT_ORIGINAL);
		this.outputFormat = OutputFormat
				.getOutFormat(get(args, "outputFormat", PinyinTransformTokenFilter.DEFAULT_OUTPUT_FORMAT.getLabel()));
		this.minTermLength = getInt(args, "minTerm", PinyinTransformTokenFilter.DEFAULT_MIN_TERM_LENGTH);
		this.mixShortLength = getInt(args, "mixShort", PinyinTransformTokenFilter.DEFAULT_MIX_SHORT_LENGTH);
		if (!args.isEmpty())
			throw new IllegalArgumentException("Unknown parameters: " + args);
	}

	public TokenFilter create(TokenStream input) {
		return new PinyinTransformTokenFilter(input, this.outputFormat, this.minTermLength, this.isOutOriginal,
				mixShortLength);
	}
}

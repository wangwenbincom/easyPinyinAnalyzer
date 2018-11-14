package org.easy.search.analysis;

import java.util.Map;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.easy.search.analysis.PinyinTransformTokenFilter.OutputFormat;

public class PinyinTransformTokenFilterFactory extends TokenFilterFactory {

	/**
	 * ���ԭ��Ԫ��ʶ
	 */
	private boolean isOutOriginal = PinyinTransformTokenFilter.DEFAULT_IS_OUT_ORIGINAL;

	/**
	 * ���Ĵ�����Сת�����ȣ�Ĭ�ϴ���2�����Ľ���ת��ƴ��
	 */
	private int minTermLength = PinyinTransformTokenFilter.DEFAULT_MIN_TERM_LENGTH;

	/**
	 * �����ʽ��Ĭ�ϼ�ƴ��ȫƴȫ�����
	 */
	private OutputFormat outputFormat = PinyinTransformTokenFilter.DEFAULT_OUTPUT_FORMAT;

	/**
	 * ������������߿�ʼ����ƴ�ĳ���,Ĭ��Ϊ0����������ƴ��
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

package org.easy.search.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

public class PinyinTransformTokenFilter extends TokenFilter {

	/**
	 * 中文原文是否输出
	 */
	public static final boolean DEFAULT_IS_OUT_ORIGINAL = true;

	/**
	 * 简拼、全拼、简拼全拼三种格式
	 */
	public static final OutputFormat DEFAULT_OUTPUT_FORMAT = OutputFormat.BOTH;

	/**
	 * 中文词组转拼音长度最小长度，默认最小长度为2的转拼音
	 */
	public static final int DEFAULT_MIN_TERM_LENGTH = 2;

	/**
	 * 简拼全拼混合时，简拼的长度,输出模式含有简拼时，中文词组最小长度最小3时为混合模式的有效长度， 否则最小长度为2时为有效转换长度，最大不能超过10
	 */
	public static final int DEFAULT_MIX_SHORT_LENGTH = 0;

	/**
	 * 原词元输出标识
	 */
	private boolean isOutOriginalTerm;

	/**
	 * 输出格式
	 */
	private OutputFormat outputFormat;

	/**
	 * 简拼全拼混合时，简拼长度
	 */
	private int mixShortLength;

	/**
	 * 转换拼音的最小中文词组长度
	 */
	private int minTermLength;

	/**
	 * 拼音转接输出格式
	 */
	private HanyuPinyinOutputFormat pinyinOutputFormat = new HanyuPinyinOutputFormat();

	/**
	 * 当前处理词元（即原词元，例如输入的词元为中国）
	 */
	private char[] curTermBuffer;

	/**
	 * 当前处理词元长度
	 */
	private int curTermLength;

	/**
	 * 词元记录
	 */
	private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);

	/**
	 * 位置增量属性
	 */
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

	/**
	 * 类型属性
	 */
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	/**
	 * 当前输入是否已输出
	 */
	private boolean hasCurOut = false;

	/**
	 * 拼音结果集
	 */
	private Collection<String> terms = new ArrayList<String>();

	/**
	 * 拼音结果集迭代器
	 */
	private Iterator<String> termIte = null;

	/**
	 * 构造器。默认保留原中文词元
	 * 
	 * @param input
	 *            词元
	 * @param outputFormat
	 *            输出格式：全拼、简拼、全拼和简拼{@link OutputFormat}
	 * @param minTermLength
	 *            中文词组最小转换长度
	 */
	public PinyinTransformTokenFilter(TokenStream input, String outputFormat, int minTermLength) {
		this(input, outputFormat, minTermLength, true);
	}

	/**
	 * 构造器
	 * 
	 * @param input
	 *            词元
	 * @param outputFormat
	 *            输出格式：全拼、简拼、全拼和简拼{@link OutputFormat}
	 * @param minTermLength
	 *            中文词组最小转换长度
	 * @param isOutOriginalTerm
	 *            原词元输出标识
	 */
	public PinyinTransformTokenFilter(TokenStream input, String outputFormat, int minTermLength,
			boolean isOutOriginalTerm) {

		this(input, OutputFormat.getOutFormat(outputFormat), minTermLength, isOutOriginalTerm, 0);
	}

	/**
	 * 构造器
	 * 
	 * @param input
	 *            词元
	 * @param outputFormat
	 *            输出格式：全拼、简拼、全拼和简拼 {@link OutputFormat}
	 * @param minTermLength
	 *            中文词组最小转换长度
	 * @param isOutOriginalTerm
	 *            原词元输出标识
	 * 
	 * @param mixShortLength
	 *            简拼和全拼混合模式时，简拼的长度 ，中文长度大于等于3，且此值应当大于1小于中文长度-1
	 */
	public PinyinTransformTokenFilter(TokenStream input, OutputFormat outputFormat, int minTermLength,
			boolean isOutOriginalTerm, int mixShortLength) {

		super(input);
		this.minTermLength = minTermLength;
		if (this.minTermLength < 1) {
			this.minTermLength = 1;
		}
		this.isOutOriginalTerm = isOutOriginalTerm;
		this.pinyinOutputFormat.setCaseType(HanyuPinyinCaseType.LOWERCASE);
		this.pinyinOutputFormat.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
		this.outputFormat = outputFormat;
		this.mixShortLength = mixShortLength;
		addAttribute(OffsetAttribute.class); // 偏移量属性
	}

	/**
	 * 中文字符统计
	 * 
	 * @param str input string
	 * @return return count result
	 */
	public static int countChineseChar(String str) {
		int count = 0;
		if ((null == str) || ("".equals(str.trim())))
			return count;
		for (int i = 0; i < str.length(); i++) {
			if (isChinese(str.charAt(i)))
				count++;
		}
		return count;
	}

	/**
	 * 判断字符是否为中文
	 * 
	 * @param a chinese char
	 * @return chinese char return true else false
	 */
	public static boolean isChinese(char a) {
		int v = a;
		return (v >= 19968) && (v <= 171941);
	}

	/**
	 * 分词过滤 该方法被上层循环调用，直到处理完成返回false
	 */
	public final boolean incrementToken() throws IOException {
		while (true) {

			// 当前无处理词元，则查找下一词元
			if (this.curTermBuffer == null) {

				if (!this.input.incrementToken()) {
					// 没有待处理词元
					return false;
				}
				// 缓存词元
				this.curTermBuffer = this.termAtt.buffer().clone();
				this.curTermLength = this.termAtt.length();
			}
			// 输出原词元
			if ((this.isOutOriginalTerm) && (!this.hasCurOut) && (this.termIte == null)) {
				// 原词元已经输出，标识设置为true
				this.hasCurOut = true;
				// 写入原输入词元
				this.termAtt.copyBuffer(this.curTermBuffer, 0, this.curTermLength);
				this.posIncrAtt.setPositionIncrement(this.posIncrAtt.getPositionIncrement());
				// 进入下次循环
				return true;
			}
			String chinese = this.termAtt.toString();

			int chineseCount = countChineseChar(chinese);

			// 判断是否符合处理长度
			if (chineseCount >= this.minTermLength) {

				try {
					this.terms.clear();

					// 获取拼音结果
					List<String[]> pinyinList = getPinyinList(chinese);

					// 简拼集合
					Collection<String> shortList = null;
					// 全拼集合
					Collection<String> fullList = null;

					switch (outputFormat) {
					case BOTH:// 简拼和全拼
						shortList = getShort(pinyinList);
						fullList = getFull(pinyinList);
						this.typeAtt.setType("both_pinyin");
						break;
					case SHORT:// 简拼
						shortList = getShort(pinyinList);
						this.typeAtt.setType("short_pinyin");
						break;
					case FULL:// 全拼
						fullList = getFull(pinyinList);
						this.typeAtt.setType("full_pinyin");
						break;
					default:
						shortList = getShort(pinyinList);
						fullList = getFull(pinyinList);
						this.typeAtt.setType("both_pinyin");
						break;
					}

					if (shortList != null)
						this.terms.addAll(shortList);
					if (fullList != null) {
						this.terms.addAll(fullList);
					}

					// 在有简拼的情况下中文长度应最小为3
					if (mixShortLength > 0) {
						if (shortList == null) {
							// 在无简拼的情况下中文长度应最小为2
							if (fullList != null && chineseCount > 1) {
								Collection<String> minShortList = getMix(pinyinList, mixShortLength);
								if (null != minShortList)
									this.terms.addAll(minShortList);
							}
						} else {
							if (chineseCount > 2) {
								Collection<String> mixList = getMix(pinyinList, mixShortLength);
								if (null != mixList)
									this.terms.addAll(mixList);
							}
						}
					}

					if (this.terms != null && this.terms.size() > 0) {
						this.termIte = this.terms.iterator();
					}
				} catch (BadHanyuPinyinOutputFormatCombination badHanyuPinyinOutputFormatCombination) {
					badHanyuPinyinOutputFormatCombination.printStackTrace();
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}

			}
			if (this.termIte != null) {
				// 未处理的拼音结果集
				while (this.termIte.hasNext()) {
					String pinyin = this.termIte.next();
					this.termAtt.copyBuffer(pinyin.toCharArray(), 0, pinyin.length());
					this.posIncrAtt.setPositionIncrement(this.posIncrAtt.getPositionIncrement());
					return true;
				}
			}

			// 清理缓存
			this.curTermBuffer = null;
			this.termIte = null;
			// 重置原词元输出标识
			this.hasCurOut = false;

		}
	}

	/**
	 * 获取拼音缩写
	 * 
	 * @param pinyinList
	 *            拼音集合
	 * @return 转换后的文本
	 * @throws BadHanyuPinyinOutputFormatCombination
	 */
	private Collection<String> getShort(List<String[]> pinyinList) throws BadHanyuPinyinOutputFormatCombination {

		Set<String> pinyins = null;
		for (String[] array : pinyinList) {
			if (pinyins == null || pinyins.isEmpty()) {
				pinyins = new HashSet<String>();

				for (String charPinpin : array) {
					pinyins.add(charPinpin.substring(0, 1));
				}
			} else {
				Set<String> pres = pinyins;
				pinyins = new HashSet<String>();
				for (String pre : pres) {
					for (String charPinyin : array) {
						pinyins.add(pre + charPinyin.substring(0, 1));
					}
				}
			}
		}
		return pinyins;
	}

	public void reset() throws IOException {
		super.reset();
		this.curTermBuffer = null;
		this.termIte = null;
		// 重置原词元以输出标识
		this.hasCurOut = false;
	}

	/**
	 * 
	 * @param str
	 *            字符串
	 * @return 返回中文字符拼音，非中文忽略
	 * @throws BadHanyuPinyinOutputFormatCombination
	 */
	private List<String[]> getPinyinList(String str) throws BadHanyuPinyinOutputFormatCombination {
		List<String[]> pinyinList = new ArrayList<String[]>();
		for (int i = 0; i < str.length(); i++) {
			String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(str.charAt(i), this.pinyinOutputFormat);
			if (pinyinArray != null && pinyinArray.length > 0) {
				pinyinList.add(pinyinArray);
			}
		}
		return pinyinList;
	}

	/**
	 * 获取全拼
	 * 
	 * @param pinyinList
	 *            拼音集合
	 * @return 转换后的文本集合
	 * @throws BadHanyuPinyinOutputFormatCombination
	 */
	private Collection<String> getFull(List<String[]> pinyinList) throws BadHanyuPinyinOutputFormatCombination {

		Set<String> pinyins = null;
		for (String[] array : pinyinList) {
			if (pinyins == null || pinyins.isEmpty()) {
				pinyins = new HashSet<String>();
				for (String charPinyin : array) {

					pinyins.add(charPinyin);
				}
			} else {
				Set<String> pres = pinyins;
				pinyins = new HashSet<String>();
				for (String pre : pres) {
					for (String charPinyin : array) {

						pinyins.add(pre + charPinyin);
					}
				}
			}
		}
		return pinyins;
	}

	/**
	 * 
	 * * @param pinyinList 拼音集合
	 * 
	 * @param shortLength
	 *            从左边开始简拼的长度
	 * @return 转换后的文本集合
	 * @throws BadHanyuPinyinOutputFormatCombination
	 */
	private Collection<String> getMix(List<String[]> pinyinList, int shortLength)
			throws BadHanyuPinyinOutputFormatCombination {
		if (shortLength <= 0) {
			return new HashSet<String>();
		}

		//最大长度为拼音长度-1，最后一个首字母始终链接，因此最后一个进行首字母没有意义
		shortLength = Math.min(shortLength, pinyinList.size() - 1);

		if (shortLength > 10) {
			shortLength = 10;
		}

		Set<String> pinyins = null;
		for (int index = 1; index <= shortLength; index++) {

			Set<String> tmppinyins = null;
			int currLen = 1;

			for (String[] array : pinyinList) {

				if (tmppinyins == null || tmppinyins.isEmpty()) {
					tmppinyins = new HashSet<String>();

					for (String charPinpin : array) {
						tmppinyins.add(charPinpin.substring(0, 1));
					}
				} else {
					Set<String> pres = tmppinyins;
					tmppinyins = new HashSet<String>();
					for (String pre : pres) {

						for (String charPinyin : array) {

							if (currLen <= index) {
								tmppinyins.add(pre + charPinyin.substring(0, 1));

							} else {
								tmppinyins.add(pre + charPinyin);

							}

						}
					}

				}

				currLen++;

			}

			if (pinyins == null || pinyins.isEmpty()) {
				pinyins = tmppinyins;
			} else {
				pinyins.addAll(tmppinyins);
			}
		}

		return pinyins;
	}

	/**
	 * 输出格式
	 */
	public static enum OutputFormat {

		/** 全拼 */
		FULL {
			@Override
			public String getLabel() {
				return "full";
			}
		},
		/** 简拼 */
		SHORT {
			@Override
			public String getLabel() {
				return "short";
			}
		},

		/** 全拼和简拼 */
		BOTH {
			@Override
			public String getLabel() {
				return "both";
			}
		};

		public abstract String getLabel();

		// Get the appropriate OutFormat from a string
		public static OutputFormat getOutFormat(String format) {
			if (FULL.getLabel().equals(format)) {
				return FULL;
			}
			if (SHORT.getLabel().equals(format)) {
				return SHORT;
			}
			if (BOTH.getLabel().equals(format)) {
				return BOTH;
			}
			return null;
		}

	}

}

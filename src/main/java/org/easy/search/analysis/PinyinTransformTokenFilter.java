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
	 * ����ԭ���Ƿ����
	 */
	public static final boolean DEFAULT_IS_OUT_ORIGINAL = true;

	/**
	 * ��ƴ��ȫƴ����ƴȫƴ���ָ�ʽ
	 */
	public static final OutputFormat DEFAULT_OUTPUT_FORMAT = OutputFormat.BOTH;

	/**
	 * ���Ĵ���תƴ��������С���ȣ�Ĭ����С����Ϊ2��תƴ��
	 */
	public static final int DEFAULT_MIN_TERM_LENGTH = 2;

	/**
	 * ��ƴȫƴ���ʱ����ƴ�ĳ���,���ģʽ���м�ƴʱ�����Ĵ�����С������С3ʱΪ���ģʽ����Ч���ȣ� ������С����Ϊ2ʱΪ��Чת�����ȣ�����ܳ���10
	 */
	public static final int DEFAULT_MIX_SHORT_LENGTH = 0;

	/**
	 * ԭ��Ԫ�����ʶ
	 */
	private boolean isOutOriginalTerm;

	/**
	 * �����ʽ
	 */
	private OutputFormat outputFormat;

	/**
	 * ��ƴȫƴ���ʱ����ƴ����
	 */
	private int mixShortLength;

	/**
	 * ת��ƴ������С���Ĵ��鳤��
	 */
	private int minTermLength;

	/**
	 * ƴ��ת�������ʽ
	 */
	private HanyuPinyinOutputFormat pinyinOutputFormat = new HanyuPinyinOutputFormat();

	/**
	 * ��ǰ�����Ԫ����ԭ��Ԫ����������Ĵ�ԪΪ�й���
	 */
	private char[] curTermBuffer;

	/**
	 * ��ǰ�����Ԫ����
	 */
	private int curTermLength;

	/**
	 * ��Ԫ��¼
	 */
	private final CharTermAttribute termAtt = (CharTermAttribute) addAttribute(CharTermAttribute.class);

	/**
	 * λ����������
	 */
	private final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);

	/**
	 * ��������
	 */
	private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);

	/**
	 * ��ǰ�����Ƿ������
	 */
	private boolean hasCurOut = false;

	/**
	 * ƴ�������
	 */
	private Collection<String> terms = new ArrayList<String>();

	/**
	 * ƴ�������������
	 */
	private Iterator<String> termIte = null;

	/**
	 * ��������Ĭ�ϱ���ԭ���Ĵ�Ԫ
	 * 
	 * @param input
	 *            ��Ԫ
	 * @param outputFormat
	 *            �����ʽ��ȫƴ����ƴ��ȫƴ�ͼ�ƴ{@link OutputFormat}
	 * @param minTermLength
	 *            ���Ĵ�����Сת������
	 */
	public PinyinTransformTokenFilter(TokenStream input, String outputFormat, int minTermLength) {
		this(input, outputFormat, minTermLength, true);
	}

	/**
	 * ������
	 * 
	 * @param input
	 *            ��Ԫ
	 * @param outputFormat
	 *            �����ʽ��ȫƴ����ƴ��ȫƴ�ͼ�ƴ{@link OutputFormat}
	 * @param minTermLength
	 *            ���Ĵ�����Сת������
	 * @param isOutOriginalTerm
	 *            ԭ��Ԫ�����ʶ
	 */
	public PinyinTransformTokenFilter(TokenStream input, String outputFormat, int minTermLength,
			boolean isOutOriginalTerm) {

		this(input, OutputFormat.getOutFormat(outputFormat), minTermLength, isOutOriginalTerm, 0);
	}

	/**
	 * ������
	 * 
	 * @param input
	 *            ��Ԫ
	 * @param outFormat
	 *            �����ʽ��ȫƴ����ƴ��ȫƴ�ͼ�ƴ {@link OutputFormat}
	 * @param minTermLength
	 *            ���Ĵ�����Сת������
	 * @param isOutOriginalTerm
	 *            ԭ��Ԫ�����ʶ
	 * 
	 * @param mixShortLength
	 *            ��ƴ��ȫƴ���ģʽʱ����ƴ�ĳ��� �����ĳ��ȴ��ڵ���3���Ҵ�ֵӦ������1С�����ĳ���-1
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
		addAttribute(OffsetAttribute.class); // ƫ��������
	}

	/**
	 * �����ַ�ͳ��
	 * 
	 * @param str
	 * @return
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
	 * �ж��ַ��Ƿ�Ϊ����
	 * 
	 * @param a
	 * @return
	 */
	public static boolean isChinese(char a) {
		int v = a;
		return (v >= 19968) && (v <= 171941);
	}

	/**
	 * �ִʹ��� �÷������ϲ�ѭ�����ã�ֱ��������ɷ���false
	 */
	public final boolean incrementToken() throws IOException {
		while (true) {

			// ��ǰ�޴����Ԫ���������һ��Ԫ
			if (this.curTermBuffer == null) {

				if (!this.input.incrementToken()) {
					// û�д������Ԫ
					return false;
				}
				// �����Ԫ
				this.curTermBuffer = this.termAtt.buffer().clone();
				this.curTermLength = this.termAtt.length();
			}
			// ���ԭ��Ԫ
			if ((this.isOutOriginalTerm) && (!this.hasCurOut) && (this.termIte == null)) {
				// ԭ��Ԫ�Ѿ��������ʶ����Ϊtrue
				this.hasCurOut = true;
				// д��ԭ�����Ԫ
				this.termAtt.copyBuffer(this.curTermBuffer, 0, this.curTermLength);
				this.posIncrAtt.setPositionIncrement(this.posIncrAtt.getPositionIncrement());
				// �����´�ѭ��
				return true;
			}
			String chinese = this.termAtt.toString();

			int chineseCount = countChineseChar(chinese);

			// �ж��Ƿ���ϴ�����
			if (chineseCount >= this.minTermLength) {

				try {
					this.terms.clear();

					// ��ȡƴ�����
					List<String[]> pinyinList = getPinyinList(chinese);

					// ��ƴ����
					Collection<String> shortList = null;
					// ȫƴ����
					Collection<String> fullList = null;

					switch (outputFormat) {
					case BOTH:// ��ƴ��ȫƴ
						shortList = getShort(pinyinList);
						fullList = getFull(pinyinList);
						this.typeAtt.setType("both_pinyin");
						break;
					case SHORT:// ��ƴ
						shortList = getShort(pinyinList);
						this.typeAtt.setType("short_pinyin");
						break;
					case FULL:// ȫƴ
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

					// ���м�ƴ����������ĳ���Ӧ��СΪ3
					if (mixShortLength > 0) {
						if (shortList == null) {
							// ���޼�ƴ����������ĳ���Ӧ��СΪ2
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
				// δ�����ƴ�������
				while (this.termIte.hasNext()) {
					String pinyin = this.termIte.next();
					this.termAtt.copyBuffer(pinyin.toCharArray(), 0, pinyin.length());
					this.posIncrAtt.setPositionIncrement(this.posIncrAtt.getPositionIncrement());
					return true;
				}
			}

			// ������
			this.curTermBuffer = null;
			this.termIte = null;
			// ����ԭ��Ԫ�����ʶ
			this.hasCurOut = false;

		}
	}

	/**
	 * ��ȡƴ����д
	 * 
	 * @param pinyinList
	 *            ƴ������
	 * @return ת������ı�
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
		// ����ԭ��Ԫ�������ʶ
		this.hasCurOut = false;
	}

	/**
	 * 
	 * @param str
	 *            �ַ���
	 * @return ���������ַ�ƴ���������ĺ���
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
	 * ��ȡȫƴ
	 * 
	 * @param pinyinList
	 *            ƴ������
	 * @return ת������ı�����
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
	 * * @param pinyinList ƴ������
	 * 
	 * @param shortLength
	 *            ����߿�ʼ��ƴ�ĳ���
	 * @return ת������ı�����
	 * @throws BadHanyuPinyinOutputFormatCombination
	 */
	private Collection<String> getMix(List<String[]> pinyinList, int shortLength)
			throws BadHanyuPinyinOutputFormatCombination {
		if (shortLength <= 0) {
			return new HashSet<String>();
		}

		//��󳤶�Ϊƴ������-1�����һ������ĸʼ�����ӣ�������һ����������ĸû������
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
	 * �����ʽ
	 */
	public static enum OutputFormat {

		/** ȫƴ */
		FULL {
			@Override
			public String getLabel() {
				return "full";
			}
		},
		/** ��ƴ */
		SHORT {
			@Override
			public String getLabel() {
				return "short";
			}
		},

		/** ȫƴ�ͼ�ƴ */
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

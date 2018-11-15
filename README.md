# easyPinyinAnalyzer
       solr的中文拼音分词过滤器，支持全拼、简拼、简拼和全拼同时输出、简拼全拼混合输出，同时提供了一个基于NGram
       算法的类似EdgeNGramTokenFilter的过滤器，支持双向过滤。
# Maven
       <dependency>
        <groupId>com.github.wangwenbincom</groupId>
        <artifactId>easyPinyinAnalyzer</artifactId>
        <version>1.0.0-RELEASE</version>
       </dependency>
# 使用说明 
## schema.xml配置
	<fieldType name="text_pinyin" class="solr.TextField">
		<analyzer type="index">
			<charFilter class="solr.HTMLStripCharFilterFactory"/>
			<tokenizer class="solr.WhitespaceTokenizerFactory" />			
			<filter class="solr.LowerCaseFilterFactory"/>
			<filter class="org.easy.search.analysis.PinyinTransformTokenFilterFactory" minTerm="1"  outputFormat="both" outOriginal="false" mixShort="5" />
			<filter class="org.liangbl.solr.analysis.PinyinNGramTokenFilterFactory" minGram="1" maxGram="20" outputDirection="both"/>
		</analyzer>
		<analyzer type="query">
		  <charFilter class="solr.HTMLStripCharFilterFactory"/>
		  <tokenizer class="solr.WhitespaceTokenizerFactory"/> 
		  <filter class="solr.LowerCaseFilterFactory"/>
		  <filter class="org.easy.search.analysis.PinyinTransformTokenFilterFactory" minTerm="1"  outputFormat="both" outOriginal="false" mixShort="5" />
		  <filter class="org.liangbl.solr.analysis.PinyinNGramTokenFilterFactory" minGram="1" maxGram="20" />
		</analyzer>
	</fieldType>
## PinyinTransformTokenFilterFactory参数配置说明
# 版权
       Apache License Version 2.0

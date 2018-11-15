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
<table>
	<tr><th>配置项名称</th><th>功能</th><th>默认值</th></tr>	
	<tr><td>outOriginal</td><td>输出原词元标识，例如：天气不错，为true时分词后的天气不错和拼音一起输出，否则只输出拼音</td><td>true</td></tr>
	<tr><td>outputFormat</td>输出格式，full全拼、short简拼、both两者同时输出<td></td><td>true</td></tr>
	<tr><td>minTerm</td><td>输出中文词元拼音的最小长度，大于此长度的中文转拼音输出，否则不输出</td><td>2</td></tr>
	<tr><td>mixShort</td><td>此值大于0时，输出混合拼音，如果输出格式中有简拼，则中文长度最小为3此值有效，否则长度为2有效</td><td>true</td></tr>	
<table>
# 版权
       Apache License Version 2.0

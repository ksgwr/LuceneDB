package jp.ksgwr.lucene.valuesdb;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;

/**
 * Ngram Analyzer
 *
 * @author ksgwr
 *
 */
public class NgramAnalyzer extends Analyzer {

	/** ngram */
	protected int n;

	/**
	 * constructor
	 * @param matchVersion LUCENE VERSION
	 */
	public NgramAnalyzer(int n) {
		this.n = n;
	}

	@Override
	protected TokenStreamComponents createComponents(String paramString) {
		Tokenizer source = new NGramTokenizer(n, n);
		TokenStream result =  new StandardFilter(source);
		return new TokenStreamComponents(source, result);
	}

}

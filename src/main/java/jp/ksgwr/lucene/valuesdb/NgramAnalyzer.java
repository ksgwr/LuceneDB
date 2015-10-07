package jp.ksgwr.lucene.valuesdb;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.util.Version;

/**
 * Ngram Analyzer
 *
 * @author ksgwr
 *
 */
public class NgramAnalyzer extends Analyzer {

	/** LUCENE VERSION */
	protected final Version matchVersion;

	/** ngram */
	protected int n;

	/**
	 * constructor
	 * @param matchVersion LUCENE VERSION
	 */
	public NgramAnalyzer(int n, Version matchVersion) {
		this.n = n;
		this.matchVersion = matchVersion;
	}

	@Override
	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		Tokenizer source = new NGramTokenizer(matchVersion, reader, n, n);
		TokenStream result =  new StandardFilter(matchVersion, source);
		return new TokenStreamComponents(source, result);
	}

}

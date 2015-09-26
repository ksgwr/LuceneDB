package jp.ksgwr.lucene.kvs;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.similarities.Similarity;

/**
 * KVSSearcherFactory , create IndexSearcher with DummySimilarity
 *
 * @author ksgwr
 *
 */
public class KVSSearcherFactory extends SearcherFactory {

	/** lucene similarity */
	private static final Similarity SIMILARITY = new DummySimilarity();

	/**
	 * Returns a new IndexSearcher over the given reader.
	 */
	@Override
	public IndexSearcher newSearcher(IndexReader reader) throws IOException {
		IndexSearcher searcher = new IndexSearcher(reader);
		// Dummy Similarity(高速化)
		searcher.setSimilarity(SIMILARITY);
		return searcher;
	}
}

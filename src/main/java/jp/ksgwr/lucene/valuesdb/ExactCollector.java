package jp.ksgwr.lucene.valuesdb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.search.TopDocs;

/**
 * exact match collector
 *
 * @author ksgwr
 *
 */
public class ExactCollector extends Collector {

	/** index searcher */
	private IndexSearcher searcher;

	/** document base offset */
	private int docBase;

	/** field name */
	private String fieldName;

	/** query string */
	private String query;

	/** result list */
	private ArrayList<ScoreDoc> scores;

	/**
	 * constructor
	 * @param searcher IndexSearcher
	 * @param fieldName field name
	 * @param query query
	 * @param totalHits totalHits
	 */
	public ExactCollector(IndexSearcher searcher, String fieldName, String query, int totalHits) {
		this.searcher = searcher;
		this.fieldName = fieldName;
		this.query = query;
		this.scores = new ArrayList<>(totalHits);
	}

	/**
	 * create topDocs
	 * @return topDocs
	 */
	public TopDocs topDocs() {
		Collections.sort(scores, new Comparator<ScoreDoc>() {
			@Override
			public int compare(ScoreDoc o1, ScoreDoc o2) {
				return o1.doc > o2.doc ? 1 : -1;
			}
		});
		return new TopDocs(scores.size(), scores.toArray(new ScoreDoc[0]), Float.NaN);
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {

	}

	@Override
	public void collect(int doc) throws IOException {
		int docNum = docBase + doc;

		// TODO: searcherから引きに行くと遅くなる気がするが効率よい方法がないのか？
		String fieldValue = searcher.doc(docNum).get(fieldName);

		if(query.equals(fieldValue)) {
			ScoreDoc score = new ScoreDoc(docNum, Float.NaN);
			scores.add(score);
		}
	}

	@Override
	public void setNextReader(AtomicReaderContext context) throws IOException {
		this.docBase = context.docBase;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return false;
	}

}

package jp.ksgwr.lucene.kvs;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;

/**
 * DummySimilarity (not calculate)
 *
 * @author ksgwr
 *
 */
public class DummySimilarity extends Similarity {

	@Override
	public long computeNorm(FieldInvertState state) {
		return 0;
	}

	@Override
	public SimWeight computeWeight(float queryBoost,
			CollectionStatistics collectionStats, TermStatistics... termStats) {
		return new SimWeight() {

			@Override
			public void normalize(float queryNorm, float topLevelBoost) {
			}

			@Override
			public float getValueForNormalization() {
				return 0;
			}
		};
	}

	@Override
	public SimScorer simScorer(SimWeight weight, AtomicReaderContext context)
			throws IOException {
		return new SimScorer() {

			@Override
			public float score(int doc, float freq) {
				return 0;
			}

			@Override
			public float computeSlopFactor(int distance) {
				return 0;
			}

			@Override
			public float computePayloadFactor(int doc, int start, int end,
					BytesRef payload) {
				return 0;
			}
		};
	}

}

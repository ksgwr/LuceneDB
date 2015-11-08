package jp.ksgwr.lucene.kvs;

import java.io.IOException;

import org.apache.lucene.index.FieldInvertState;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.TermStatistics;
import org.apache.lucene.search.similarities.Similarity;

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
	public SimScorer simScorer(SimWeight paramSimWeight,
			LeafReaderContext paramLeafReaderContext) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}

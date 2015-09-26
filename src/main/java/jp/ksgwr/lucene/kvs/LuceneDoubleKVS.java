package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;

/**
 * Double Value KVS
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 */
public class LuceneDoubleKVS<K> extends LuceneKVS<K, Double> {

	/**
	 * constructor
	 * @param directory Lucene Directory
	 * @param file Index File Path
	 * @param isVolatile isVolatile, if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneDoubleKVS(Directory directory, File file, boolean isVolatile) throws IOException {
		super(directory, file, isVolatile);
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneDoubleKVS()  throws IOException {
		super();
	}

	@Override
	public Double valueOf(String val) {

		return Double.valueOf(val);
	}

}

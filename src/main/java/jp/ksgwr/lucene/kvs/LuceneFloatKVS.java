package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;

/**
 * Float Value KVS
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 */
public class LuceneFloatKVS<K> extends LuceneKVS<K, Float> {

	/**
	 * constructor
	 * @param directory Lucene Directory
	 * @param file Index File Path
	 * @param isVolatile if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneFloatKVS(Directory directory, File file, boolean isVolatile) throws IOException {
		super(directory, file, isVolatile);
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneFloatKVS()  throws IOException {
		super();
	}

	@Override
	public Float valueOf(String val) {
		return Float.valueOf(val);
	}

}

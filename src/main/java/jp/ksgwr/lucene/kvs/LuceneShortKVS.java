package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;

/**
 * Short Value KVS
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 */
public class LuceneShortKVS<K> extends LuceneKVS<K, Short> {

	/**
	 * constructor
	 * @param directory Lucene Directory
	 * @param file Index File Path
	 * @param isVolatile if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneShortKVS(Directory directory, File file, boolean isVolatile) throws IOException {
		super(directory, file, isVolatile);
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneShortKVS()  throws IOException {
		super();
	}

	@Override
	public Short valueOf(String val) {
		return Short.valueOf(val);
	}

}

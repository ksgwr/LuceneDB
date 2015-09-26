package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;

/**
 * Long Value KVS
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 */
public class LuceneLongKVS<K> extends LuceneKVS<K, Long> {

	/**
	 * constructor
	 * @param directory Lucene Directory
	 * @param file Index File Path
	 * @param isVolatile if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneLongKVS(Directory directory, File file, boolean isVolatile) throws IOException {
		super(directory, file, isVolatile);
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneLongKVS()  throws IOException {
		super();
	}

	@Override
	public Long valueOf(String val) {
		return Long.valueOf(val);
	}


}

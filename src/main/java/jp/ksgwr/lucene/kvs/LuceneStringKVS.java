package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;

/**
 * String Value KVS
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 */
public class LuceneStringKVS<K> extends LuceneKVS<K, String> {

	/**
	 * constructor
	 * @param directory Lucene Directory
	 * @param file Index File Path
	 * @param isVolatile if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneStringKVS(Directory directory, File file, boolean isVolatile) throws IOException {
		super(directory, file, isVolatile);
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneStringKVS()  throws IOException {
		super();
	}

	@Override
	public String valueOf(String val) {
		return val;
	}


}

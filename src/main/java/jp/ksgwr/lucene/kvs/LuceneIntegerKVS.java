package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.store.Directory;

/**
 * Integer Value KVS
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 */
public class LuceneIntegerKVS<K> extends LuceneKVS<K, Integer> {

	/**
	 * constructor
	 * @param directory Lucene Directory
	 * @param file Index File Path
	 * @param onMemory if true, use RAMDirectory
	 * @throws IOException IOException
	 */
	public LuceneIntegerKVS(Directory directory, File file, boolean isVolatile) throws IOException {
		super(directory, file, isVolatile);
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneIntegerKVS()  throws IOException {
		super();
	}

	@Override
	public Integer valueOf(String val) {
		return Integer.valueOf(val);
	}

}

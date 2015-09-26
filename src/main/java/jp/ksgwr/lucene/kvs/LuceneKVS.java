package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.store.Directory;

/**
 * Lucene Simple Value KVS
 * @author ksgwr
 *
 * @param <K> KeyObject
 * @param <V> ValueObject
 */
public abstract class LuceneKVS<K, V> extends LuceneKVSBase<K, V> {

	/**
	 * constructor
	 * @param directory Lucene Directory
	 * @param file Index File Path
	 * @param isVolatile isVolatile, if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneKVS(Directory directory, File file, boolean isVolatile) throws IOException {
		super(directory, file, isVolatile);
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneKVS()  throws IOException {
		super();
	}

	/**
	 * string to ValueObject
	 * @param val string value
	 * @return value object
	 */
	public abstract V valueOf(String val);

	@Override
	public V readValue(Document doc, String valueFieldName) {
		String val = doc.get(valueFieldName);
		if (val != null) {
			return valueOf(val);
		}
		return null;
	}

	@Override
	public StoredField writeValue(V val, String valueFieldName) {
		return new StoredField(valueFieldName, val.toString());
	}
}

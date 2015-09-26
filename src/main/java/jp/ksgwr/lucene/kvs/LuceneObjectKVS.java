package jp.ksgwr.lucene.kvs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.store.Directory;

/**
 * Lucene Object KVS
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 * @param <V> ValueObject
 */
public class LuceneObjectKVS<K,V> extends LuceneKVSBase<K, V> {

	/**
	 * constructor
	 * @param directory  Lucene Directory
	 * @param file Index File Path
	 * @param isVolatile isVolatile, if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneObjectKVS(Directory directory, File file, boolean isVolatile) throws IOException {
		super(directory, file, isVolatile);
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneObjectKVS()  throws IOException {
		super();
	}

	@SuppressWarnings("unchecked")
	@Override
	public V readValue(Document doc, String valueFieldName) throws IOException, ClassNotFoundException {
		ByteArrayInputStream bis = null;
		byte[] data = doc.getBinaryValue(VAL).bytes;
		V val = null;
		try {
			bis = new ByteArrayInputStream(data);
			ObjectInputStream in = new ObjectInputStream(bis);
			val = (V) in.readObject();
		} finally {
			if (bis != null) {
				bis.close();
			}
		}
		return val;
	}

	@Override
	public StoredField writeValue(V val, String valueFieldName) throws IOException {
		ByteArrayOutputStream bos = null;
		byte[] data;
		try {
			bos = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bos);
			out.writeObject(val);
			data = bos.toByteArray();
		} finally {
			if (bos != null) {
				bos.close();
			}
		}
		return new StoredField(valueFieldName, data);
	}
}

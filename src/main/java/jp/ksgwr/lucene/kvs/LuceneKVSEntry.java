package jp.ksgwr.lucene.kvs;

import jp.ksgwr.lucene.exception.LuceneRuntimeException;

import org.apache.lucene.document.Document;

/**
 * Lucene KVS Entry Object
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 * @param <V> ValueObject
 */
public class LuceneKVSEntry<K, V> implements java.util.Map.Entry<K, V> {

	/** document object */
	protected Document doc;

	/** parent map */
	protected LuceneKVSBase<K, V> parent;

	/**
	 * constructor
	 * @param doc document
	 * @param parent parent map
	 */
	public LuceneKVSEntry(Document doc, LuceneKVSBase<K, V> parent) {
		this.doc = doc;
		this.parent = parent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public K getKey() {
		return (K) doc.get(LuceneKVSBase.KEY);
	}

	@Override
	public V getValue() {
		try {
			return parent.readValue(doc, LuceneKVSBase.VAL);
		} catch (Exception e) {
			throw new LuceneRuntimeException(e);
		}
	}

	@Override
	public V setValue(V value) {
		try {
			this.doc = parent.createDocument(getKey(), value);
		} catch (Exception e) {
			throw new LuceneRuntimeException(e);
		}
		return parent.put(getKey(), value);
	}

}

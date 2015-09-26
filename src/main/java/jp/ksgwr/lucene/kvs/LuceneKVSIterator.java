package jp.ksgwr.lucene.kvs;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;

import jp.ksgwr.lucene.exception.LuceneRuntimeException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;

/**
 * Lucene KVS Iterator
 *
 * @author ksgwr
 *
 * @param <K> KeyObject
 * @param <V> ValueObject
 */
public class LuceneKVSIterator<K,V> implements Iterator<Entry<K,V>>{

	/** Lucene Searcher Manager */
	protected SearcherManager manager;

	/** parent map */
	protected LuceneKVSBase<K, V> parent;

	/** index searcher */
	protected IndexSearcher searcher;

	/** index reader */
	protected IndexReader reader;

	/** max document number */
	protected int maxDoc;

	/** document size */
	protected int size;

	/** current document number */
	protected int docCount;
	/** current procced count */
	protected int entryCount;
	/** current procced entry */
	protected Entry<K, V> currentEntry;

	/**
	 * constructor
	 * @param manager lucene searcher manager
	 * @param parent parent map
	 * @throws IOException IOException
	 */
	public LuceneKVSIterator(SearcherManager manager, LuceneKVSBase<K, V> parent) throws IOException {
		this.manager = manager;
		this.parent = parent;

		manager.maybeRefreshBlocking();

		searcher = manager.acquire();
		reader = searcher.getIndexReader();
		maxDoc = reader.maxDoc();
		size = reader.numDocs();
		docCount = 0;
		entryCount = 0;
	}

	@Override
	public boolean hasNext() {
		boolean hasNext = entryCount < size;
		if (!hasNext) {
			try {
				close();
			} catch (IOException e) {
				throw new LuceneRuntimeException(e);
			}
		}
		return hasNext;
	}

	@Override
	public Entry<K, V> next() {
		try {
			Document doc;
			do {
				if (maxDoc <= docCount) {
					entryCount++;
					hasNext();
					return null;
				}
				doc = reader.document(docCount++);
			} while (doc == null && docCount < maxDoc);
			entryCount++;
			currentEntry = new LuceneKVSEntry<K, V>(doc, parent);
			return currentEntry;
		} catch (IOException e) {
			throw new LuceneRuntimeException(e);
		}
	}

	@Override
	public void remove() {
		parent.remove(currentEntry.getKey());
	}

	/**
	 * release IndexSearcher
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (searcher!=null) {
			manager.release(searcher);
			searcher = null;
			reader = null;
		}
	}

	/**
	 * document size
	 * @return size
	 */
	public int size() {
		return size;
	}

}

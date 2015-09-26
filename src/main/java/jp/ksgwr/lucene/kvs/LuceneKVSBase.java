package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jp.ksgwr.lucene.exception.LuceneRuntimeException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 *
 * TODO: key単位ロック, 全件データ取得、ファイル読み込み=>メモリ展開
 * 無駄なSimilarityの設定などは除いて高速化
 * Mapをimplementsする？
 *
 * @author Kouhei
 *
 */
public abstract class LuceneKVSBase<K, V> implements Map<K, V> {

	/** Key Field Name */
	public static final String KEY = "key";

	/** Value Field Name */
	public static final String VAL = "val";

	/** Lucene Directory */
	protected Directory directory;

	/** Index File */
	protected File file;

	/** Lucene Index Writer */
	protected IndexWriter writer;

	/** Lucene Searcher Manager */
	protected SearcherManager manager;

	/** if true, auto commit in writing */
	protected boolean isAutoCommit;

	/** if true, async reflesh in writing */
	protected boolean isAsyncReflesh;

	/** documents size */
	protected AtomicInteger numDocs;

	/** if true, delete file automatically*/
	protected boolean isVolatile;

	/**
	 * constructor
	 * @param directory Lucene Directory
	 * @param file Index File Path
	 * @param isVolatile isVolatile, if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneKVSBase(Directory directory, File file, boolean isVolatile) throws IOException {
		this.directory = directory;
		this.file = file;
		Analyzer analyzer = new KeywordAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9, analyzer);
		this.writer = new IndexWriter(directory, config);
		// LuceneObjectKVS avoid "no segments* file found in RAMDirectory" Exception
		this.writer.commit();

		this.manager = new SearcherManager(directory, new KVSSearcherFactory());
		this.isAutoCommit = true;
		this.isAsyncReflesh = true;

		this.numDocs = new AtomicInteger(writer.numDocs());
		this.isVolatile = isVolatile;

		@SuppressWarnings("rawtypes")
		final LuceneKVSBase own = this;
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				try {
					close(own);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneKVSBase()  throws IOException {
		this((Directory)new RAMDirectory(), null, true);
	}

	/**
	 * delete directory recursive
	 * @param file directory
	 */
	private static void deleteDirectory(File file) {
		if(file.isFile()) {
			file.delete();
		} else {
			for(File f:file.listFiles()) {
				deleteDirectory(f);
			}
			file.delete();
		}
	}

	/**
	 * close index
	 * @param own own this object
	 * @throws IOException IOException
	 */
	@SuppressWarnings("rawtypes")
	private static void close(LuceneKVSBase own) throws IOException {
		own.writer.close();
		own.manager.close();
		if (own.file != null && own.isVolatile && own.file.exists()) {
			deleteDirectory(own.file);
		}
	}

	/**
	 * kvs close
	 * @throws IOException IOException
	 */
	public void close() throws IOException {
		close(this);
	}

	/**
	 * indexのコピー, RAMからFileの保存も可能
	 * @param directory
	 * @throws IOException
	 */
	public void save(Directory directory) throws IOException {
		for (String file : this.directory.listAll()) {
			this.directory.copy(directory, file, file, IOContext.DEFAULT);
		}
	}

	/**
	 * save index file
	 * @param file file path
	 * @throws IOException IOException
	 */
	public void save(File file) throws IOException {
		save((Directory)FSDirectory.open(file));
	}

	/**
	 * set auto commit
	 * @param isAutoCommit if true, auto commit in writing
	 */
	public void setAutoCommit(boolean isAutoCommit) {
		this.isAutoCommit = isAutoCommit;
	}

	/**
	 * is auto commit
	 * @return if true, auto commit in writing
	 */
	public boolean isAutoCommit() {
		return isAutoCommit;
	}

	/**
	 * set async reflesh
	 * @param isAsyncReflesh if true, async reflesh in writing
	 */
	public void setAsyncReflesh(boolean isAsyncReflesh) {
		this.isAsyncReflesh = isAsyncReflesh;
	}

	/**
	 * is async reflesh
	 * @return if true, async reflesh in writing
	 */
	public boolean isAsyncReflesh() {
		return isAsyncReflesh;
	}

	/**
	 * writer commit
	 */
	public void commit() {
		try {
			numDocs.set(writer.numDocs());
			writer.commit();
			// get時のblock時間を減らすため非同期でrefleshを呼び出しておくオプション
			if(isAsyncReflesh) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							manager.maybeRefreshBlocking();
						} catch (IOException e) {
							throw new LuceneRuntimeException(e);
						}
					}
				}).start();
			}
		} catch (IOException e) {
			throw new LuceneRuntimeException(e);
		}
	}

	/**
	 * search document
	 * @param key keyObject
	 * @return null when not found
	 */
	protected Document getDocument(Object key) {
		try {
			manager.maybeRefreshBlocking();
			IndexSearcher searcher = manager.acquire();
			try {
				TopDocs top = searcher.search(new TermQuery(keyTerm(key, KEY)), 1);
				if(top.totalHits==0 ) { return null; }
				int docId = top.scoreDocs[0].doc;
				return searcher.doc(docId);
			} finally {
				manager.release(searcher);
				searcher = null;
			}
		} catch (IOException e) {
			throw new LuceneRuntimeException(e);
		}
	}

	/**
	 * create key term
	 * @param key keyObject
	 * @param keyFieldName key field name
	 * @return key term
	 */
	public Term keyTerm(Object key, String keyFieldName) {
		return new Term(keyFieldName, key.toString());
	}

	/**
	 * documentからKeyを書き込むための実装 (Store.YESの実装が必要)
	 * @param key key
	 * @return byte array
	 */
	public Field writeKey(K key, String keyFieldName) {
		return new StringField(keyFieldName, key.toString(), Store.YES);
	}

	/**
	 * documentからValueへの復元するための実装
	 * @param doc
	 * @param valueFieldName
	 * @return
	 * @throws Exception
	 */
	public abstract V readValue(Document doc, String valueFieldName) throws Exception;

	/**
	 * documentにValueを書き込むための実装
	 * @param val
	 * @param valueFieldName
	 * @return
	 * @throws Exception
	 */
	public abstract StoredField writeValue(V val, String valueFieldName) throws Exception;

	/**
	 * create doucment
	 * @param key key object
	 * @param val val object
	 * @return document
	 * @throws Exception Exception
	 */
	public Document createDocument(K key, V val) throws Exception {
		Document doc = new Document();
		doc.add(writeKey(key, KEY));
		doc.add(writeValue(val, VAL));
		return doc;
	}

	/**
	 * put Key Value
	 */
	public V put(K key, V val) {
		try {
			Document doc = createDocument(key, val);
			writer.updateDocument(new Term(KEY, key.toString()), doc);
			if (isAutoCommit) {
				this.commit();
			}
		} catch (Exception e) {
			throw new LuceneRuntimeException(e);
		}
		return val;
	}

	/**
	 * get Value
	 */
	public V get(Object key) {
		Document doc = getDocument(key);
		if (doc == null) {
			return null;
		} else {
			try {
				return readValue(doc, VAL);
			} catch (Exception e) {
				throw new LuceneRuntimeException(e);
			}
		}
	}

	@Override
	public int size() {
		return numDocs.get();
	}

	@Override
	public boolean isEmpty() {
		return numDocs.get() <= 0;
	}

	@Override
	public boolean containsKey(Object key) {
		return getDocument(key.toString()) != null;
	}

	@Override
	public boolean containsValue(Object value) {
		LuceneKVSIterator<K, V> ite = iterator();
		while(ite.hasNext()) {
			if (ite.next().getValue().equals(value) ) {
				return true;
			}
		}
		return false;
	}

	@Override
	public V remove(Object key) {
		V val = null;
		try {
			val = get(key);
			if (val != null) {
				writer.deleteDocuments(new Term(KEY, key.toString()));
				if (isAutoCommit) {
					this.commit();
				}
			}
		} catch (IOException e) {
			throw new LuceneRuntimeException(e);
		}
		return val;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		try {
			for(Entry<? extends K, ? extends V> entry: m.entrySet()) {
				K key = entry.getKey();
				Document doc = createDocument(key, entry.getValue());
				writer.updateDocument(new Term(KEY, key.toString()), doc);
			}
			if (isAutoCommit) {
				this.commit();
			}
		} catch (Exception e) {
			throw new LuceneRuntimeException(e);
		}
	}

	@Override
	public void clear() {
		try {
			writer.deleteAll();
			if (isAutoCommit) {
				this.commit();
			}
		} catch (IOException e) {
			throw new LuceneRuntimeException(e);
		}
	}

	@Override
	public Set<K> keySet() {
		LuceneKVSIterator<K, V> ite = iterator();
		Set<K> set = new LinkedHashSet<K>(ite.size());
		while (ite.hasNext()) {
			set.add(ite.next().getKey());
		}
		return set;
	}

	@Override
	public Collection<V> values() {
		LuceneKVSIterator<K, V> ite = iterator();
		ArrayList<V> set = new ArrayList<>(ite.size());
		while(ite.hasNext()) {
			set.add(ite.next().getValue());
		}
		return set;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		LuceneKVSIterator<K, V> ite = iterator();
		Set<Entry<K, V>> set = new LinkedHashSet<Entry<K, V>>(ite.size());
		while (ite.hasNext()) {
			set.add(ite.next());
		}
		return set;
	}

	/**
	 * create iterator
	 * @return iterator
	 */
	public LuceneKVSIterator<K,V> iterator() {
		try {
			return new LuceneKVSIterator<K, V>(manager, this);
		} catch (IOException e) {
			throw new LuceneRuntimeException(e);
		}
	}

}

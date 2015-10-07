package jp.ksgwr.lucene.valuesdb;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SearcherFactory;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

/**
 * Lucene Values DB (Thread Safe)
 *
 * TODO: 改行を含むデータに非対応、BufferedReader,WriterもStrategy化するか、パーサーで改行をエスケープする
 *
 * @author ksgwr
 *
 */
public class LuceneValuesDB {

	/** Lucene Version */
	public static Version LUCENE_VERSION = Version.LUCENE_4_9;

	/** Lucene directory */
	protected Directory directory;

	/** Lucene Index Path */
	protected File indexFile;

	/** IndexWriter */
	protected IndexWriter writer;

	/** SearcherManager */
	protected SearcherManager manager;

	/** if true, delete file automatically */
	protected boolean isVolatile;

	/** if true, file header define fields */
	protected boolean hasHeader;

	/** field name list */
	protected String[] fieldNames;

	/** field type list */
	protected Class<? extends Field>[] fieldTypes;

	/** field name index */
	protected Map<String, Integer> fieldsIndex;

	/** field type enum list */
	protected FieldEnum[] fieldTypeEnums;

	/** field store mode list */
	protected Store[] stores;

	/**
	 * constructor
	 * @param directory LuceneDirectory
	 * @param indexFile Index File Path
	 * @param isVolatile if true, delete file automatically
	 * @param analyzer LuceneIndexAnalyzer
	 * @throws IOException IOException
	 */
	public LuceneValuesDB(Directory directory, File indexFile, boolean isVolatile, Analyzer analyzer) throws IOException {
		this.directory = directory;
		this.indexFile = indexFile;

		IndexWriterConfig config = new IndexWriterConfig(LUCENE_VERSION, analyzer);
		this.writer = new IndexWriter(directory, config);
		this.writer.commit();

		this.manager = new SearcherManager(directory, new SearcherFactory());

		this.isVolatile = isVolatile;

		if (isVolatile) {
			final LuceneValuesDB own = this;
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
	}

	/**
	 * constructor
	 * @param directory LuceneDirectory
	 * @param indexFile Index File Path
	 * @param isVolatile if true, delete file automatically
	 * @throws IOException IOException
	 */
	public LuceneValuesDB(Directory directory, File indexFile, boolean isVolatile) throws IOException {
		this(directory, indexFile, isVolatile, new NgramAnalyzer(1, LUCENE_VERSION));
	}

	/**
	 * constructor
	 * @throws IOException IOException
	 */
	public LuceneValuesDB() throws IOException {
		this(new RAMDirectory(), null, true);
	}

	/**
	 * save index as text file
	 * @param file save file
	 * @param dumper write strategy
	 * @throws IOException IOException
	 */
	public void save(File file, WriteDocumentStrategy dumper) throws IOException {
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
		save(bw, dumper);
	}

	/**
	 * save index as text file
	 * @param bw buffered writer
	 * @param dumper write strategy
	 * @throws IOException IOException
	 */
	public void save(BufferedWriter bw, WriteDocumentStrategy dumper) throws IOException {
		IndexSearcher searcher = manager.acquire();
		IndexReader reader = searcher.getIndexReader();
		int maxDoc = reader.maxDoc();

		int docId = 0;
		List<String> storedFields = new ArrayList<String>(fieldNames.length);
		for(int i=0;i<fieldNames.length;i++) {
			if (stores[i] == Store.YES) {
				storedFields.add(fieldNames[i]);
			}
		}
		if (hasHeader) {
			bw.write(dumper.writeDocument(storedFields.toArray(new String[0])));
			bw.newLine();
		}
		String[] fields = new String[storedFields.size()];
		while(docId<maxDoc) {
			Document doc = reader.document(docId);
			for(int i=0,size=storedFields.size();i<size;i++) {
				fields[i] = doc.get(storedFields.get(i));
			}
			bw.write(dumper.writeDocument(fields));
			bw.newLine();
		}
		bw.close();
	}

	/**
	 * read file, and create index
	 * @param file data file
	 * @param parser line parser
	 * @param creator document creator
	 * @throws IOException IOException
	 */
	public void open(File file, ParseStrategy parser, CreateDocumentStrategy creator) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
		open(br, parser, creator);
	}

	/**
	 * read and create index
	 * @param reader buffered reader
	 * @param parser line parser
	 * @param creator document creator
	 * @throws IOException IOException
	 */
	public void open(BufferedReader reader, ParseStrategy parser, CreateDocumentStrategy creator) throws IOException {
		if(writer.maxDoc()>0) {
			System.err.println("warning: not empty this index."+directory.toString());
		}

		hasHeader = creator.hasHeader();
		fieldNames = null;
		boolean init = true;
		String line;
		while((line = reader.readLine()) != null) {
			String[] fields = parser.parse(line);
			if (fields == null) {
				continue;
			}
			if (init && hasHeader) {
				init = false;
				fieldNames = fields;
			} else {
				fieldNames = creator.createHeaderFields(fieldNames);
				if (fieldNames == null) {
					fieldNames = new String[fields.length];
					for (int i = 0; i < fieldNames.length; i++) {
						fieldNames[i] = String.valueOf(i);
					}
				}
				fieldsIndex = new HashMap<String, Integer>();
				for(int i = 0; i < fieldNames.length; i++) {
					fieldsIndex.put(fieldNames[i], i);
				}

				fieldTypes = creator.createFieldsType(fields);
				fieldTypeEnums = new FieldEnum[fieldTypes.length];
				for (int i=0;i<fieldTypes.length;i++) {
					fieldTypeEnums[i] = FieldEnum.valueOf(fieldTypes[i].getSimpleName());
				};
				stores = creator.createStores(fieldTypes.length);
				addDocument(fields);
				break;
			}
		}
		while((line = reader.readLine()) != null) {
			String[] fields = parser.parse(line);
			if (fields == null) {
				continue;
			}
			addDocument(fields);
		}
		reader.close();
		writer.commit();
		manager.maybeRefreshBlocking();
	}

	/**
	 * add document
	 * @param fields fields values made in creator
	 * @throws IOException IOException
	 */
	protected void addDocument(String[] fields) throws IOException {
		if (fields.length != fieldTypeEnums.length) {
			// throw Exception
		}
		Document doc = new Document();
		for(int i=0;i<fields.length;i++) {
			doc.add(createFields(i, fields[i], fieldTypeEnums[i], stores[i]));
		}
		writer.addDocument(doc);
	}

	/**
	 * create fields with define header type
	 * @param fieldNum field colmun number
	 * @param value field value
	 * @param type field type
	 * @param store field store mode
	 * @return new document field
	 */
	protected Field createFields(int fieldNum, String value, FieldEnum type, Store store) {
		Field field = null;
		String fieldName = fieldNames[fieldNum];
		switch(type) {
		case DoubleField:
			field = new DoubleField(fieldName, Double.valueOf(value), store);
			break;
		case FloatField:
			field = new FloatField(fieldName, Float.valueOf(value), store);
			break;
		case IntField:
			field = new IntField(fieldName, Integer.valueOf(value), store);
			break;
		case LongField:
			field = new LongField(fieldName, Long.valueOf(value), store);
			break;
		case StoredField:
			field = new StoredField(fieldName, value);
			break;
		case StringField:
			field = new StringField(fieldName, value, store);
			break;
		case TextField:
			field = new TextField(fieldName, value, store);
			break;
		}
		return field;
	}

	/**
	 * get SearcherManager
	 * @return SearcherManager
	 */
	public SearcherManager getSearcherManager() {
		return manager;
	}

	/**
	 * get header fields
	 * @return header field names
	 */
	public String[] getFieldNames() {
		return fieldNames;
	}

	/**
	 * get analyzer
	 * @return analyzer
	 */
	public Analyzer getAnalyzer() {
		return writer.getAnalyzer();
	}

	/**
	 * create parser
	 * @param defaultField field name
	 * @return query parser
	 */
	public QueryParser createQueryParser(String defaultField) {
		return new QueryParser(LUCENE_VERSION, defaultField, getAnalyzer());
	}

	/**
	 * create parser
	 * @return query parser
	 */
	public QueryParser createQueryParser() {
		return createQueryParser(fieldNames[0]);
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
	private static void close(LuceneValuesDB own) throws IOException {
		own.writer.close();
		own.manager.close();
		if (own.indexFile != null && own.isVolatile && own.indexFile.exists()) {
			deleteDirectory(own.indexFile);
		}
	}

	/**
	 * lucene close
	 * @throws IOException IOException
	 */
	public void close() throws IOException {
		close(this);
	}

	/**
	 * convert document results
	 * @param documents search result
	 * @return string array results
	 */
	public List<List<String>> convertDocumentList(Document[] documents) {
		ArrayList<List<String>> result = new ArrayList<List<String>>(documents.length);
		for(int i=0;i<documents.length;i++) {
			result.add(convertFields(documents[i]));
		}
		return result;
	}

	/**
	 * convert field values array from document
	 * @param document lucene document
	 * @return field values array
	 */
	public List<String> convertFields(Document document) {
		ArrayList<String> fields = new ArrayList<String>(fieldNames.length);
		for(String field:fieldNames) {
			fields.add(document.get(field));
		}
		return fields;
	}

	/**
	 * create documents result
	 * @param searcher IndexSearcher
	 * @param top TopDocs
	 * @return Document list
	 * @throws IOException IOException
	 */
	private Document[] createDocumentResult(IndexSearcher searcher, TopDocs top) throws IOException {
		Document[] docs = new Document[top.totalHits];
		for(int i=0;i<top.totalHits;i++) {
			docs[i] = searcher.doc(top.scoreDocs[i].doc);
			docs[i].add(new StoredField("_id", String.valueOf(top.scoreDocs[i].doc)));
		}
		return docs;
	}

	/**
	 * exact search
	 * @param fieldName field name
	 * @param fieldValue field value
	 * @param n max results
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] exactSearch(String fieldName, String fieldValue, int n) throws IOException {
		return exactSearch(fieldsIndex.get(fieldName), fieldValue, n);
	}

	/**
	 * exact search
	 * @param fieldNum field num
	 * @param fieldValue field value
	 * @param n max results
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] exactSearch(int fieldNum, String fieldValue, int n) throws IOException {
		IndexSearcher searcher = manager.acquire();
		if (n < 0) {
			n = writer.maxDoc();
		}
		Query query = null;
		TopDocs top = null;
		String fieldName = fieldNames[fieldNum];
		switch(fieldTypeEnums[fieldNum]) {
		case DoubleField:
			double doubleValue = Double.valueOf(fieldValue);
			query = NumericRangeQuery.newDoubleRange(fieldName, doubleValue, doubleValue, true, true);
			break;
		case FloatField:
			float floatValue = Float.valueOf(fieldValue);
			query = NumericRangeQuery.newFloatRange(fieldName, floatValue, floatValue, true, true);
			break;
		case IntField:
			int intValue = Integer.valueOf(fieldValue);
			query = NumericRangeQuery.newIntRange(fieldName, intValue, intValue, true, true);
			break;
		case LongField:
			long longValue = Long.valueOf(fieldValue);
			query = NumericRangeQuery.newLongRange(fieldName, longValue, longValue, true, true);
			break;
		case StoredField:
			break;
		case StringField:
			query = new TermQuery(new Term(fieldName, fieldValue));
			break;
		case TextField:
			// parser is not MTSafe. create new instance
			query = createQueryParser().createPhraseQuery(fieldName, fieldValue);
			ExactCollector collector = new ExactCollector(searcher, fieldName, fieldValue, n);
			searcher.search(query, collector);
			top = collector.topDocs();
			break;
		}
		// not TextField
		if (top == null && query != null) {
			top = searcher.search(query, n, Sort.INDEXORDER);
		}

		Document[] docs = createDocumentResult(searcher, top);
		manager.release(searcher);
		return docs;
	}

	/**
	 * exact search
	 * @param fieldName field name
	 * @param fieldValue field value
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] exactSearch(String fieldName, String fieldValue) throws IOException {
		return exactSearch(fieldName, fieldValue, -1);
	}

	/**
	 * exact search
	 * @param fieldNum field num
	 * @param fieldValue field value
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] exactSearch(int fieldNum, String fieldValue) throws IOException {
		return exactSearch(fieldNum, fieldValue, -1);
	}

	/**
	 * search query
	 * @param query lucene query
	 * @param n max results
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(Query query, int n) throws IOException {
		IndexSearcher searcher = manager.acquire();
		if (n < 0) {
			n = writer.maxDoc();
		}
		TopDocs top = searcher.search(query, n, Sort.INDEXORDER);
		Document[] docs = createDocumentResult(searcher, top);
		manager.release(searcher);
		return docs;
	}

	/**
	 * search query
	 * @param query lucene query
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(Query query) throws IOException {
		return search(query, -1);
	}

	/**
	 * default search
	 * @param fieldNum field num
	 * @param fieldValue field value
	 * @param n max results
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(int fieldNum, String fieldValue, int n) throws IOException {
		if (fieldTypeEnums[fieldNum] == FieldEnum.TextField) {
			Query query = createQueryParser().createPhraseQuery(fieldNames[fieldNum], fieldValue);
			return search(query);
		} else {
			return exactSearch(fieldNum, fieldValue, n);
		}
	}

	/**
	 * default search
	 * @param fieldNum field num
	 * @param fieldValue field value
	 * @return n max results
	 * @throws IOException IOException
	 */
	public Document[] search(int fieldNum, String fieldValue) throws IOException {
		return search(fieldNum, fieldValue, -1);
	}

	/**
	 * default search
	 * @param fieldName field name
	 * @param fieldValue field value
	 * @param n max results
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(String fieldName, String fieldValue, int n) throws IOException {
		return search(fieldsIndex.get(fieldName), fieldValue, n);
	}

	/**
	 * default search
	 * @param fieldName field name
	 * @param fieldValue field value
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(String fieldName, String fieldValue) throws IOException {
		return search(fieldName, fieldValue, -1);
	}

	/**
	 * default search
	 * @param fieldName field name
	 * @param fieldValue field value
	 * @param n max results
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(String fieldName, Object fieldValue, int n) throws IOException {
		return search(fieldName, fieldValue.toString(), n);
	}

	/**
	 * default search
	 * @param fieldName field name
	 * @param fieldValue field value
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(String fieldName, Object fieldValue) throws IOException {
		return search(fieldName, fieldValue, -1);
	}

	/**
	 * default search
	 * @param fieldNum field name
	 * @param fieldValue field value
	 * @param n max results
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(int fieldNum, Object fieldValue, int n) throws IOException {
		return search(fieldNum, fieldValue.toString(), n);
	}

	/**
	 * default search
	 * @param fieldNum field num
	 * @param fieldValue field value
	 * @return document list
	 * @throws IOException IOException
	 */
	public Document[] search(int fieldNum, Object fieldValue) throws IOException {
		return search(fieldNum, fieldValue, -1);
	}
}

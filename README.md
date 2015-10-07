# KVS Library based Lucene
## 特徴

Lucene BaseのMapを提供。
MMapやNIODirectoryなどを利用することで、巨大なデータについても扱うことができる。
さらにJavaのライブラリとして直接利用することで複雑なオブジェクトをValueとすることもできる。
また、よくあるKVS(Memcached)などと違い、全件データのIteratorを取得できることも特徴。
Keyについても任意のオブジェクトが扱えるが内部ではStringに変換して扱われる。
そのため、単純な数値のKeyでも効率が低下する場合があるのは注意が必要。
get時に同期も取っているためマルチスレッドセーフだが効率はそこまで重視していない。

## 使い方
### Simple Use

```Java
Map<String, String> kvs = new LuceneStringKVS<String>(); //RAMDirectory
kvs.put("a", "aval");
kvs.get("a");
```

### Advanced Use
```Java
File indexFile = new File("mmapIndex");
LuceneObjectKVS<Integer, Sample> kvs = new LuceneObjectKVS<>(MMapDirectory.open(indexFile), indexFile, true);
// isVolatile true: delete index on exit.
// support factory class
// LuceneKVSFactory.createInstance(LuceneObjectKVS.class, indexFile, true, true);

Sample sample = new Sample();
sample.name = "hoge";
sample.age = 10;

kvs.put(1, sample);
kvs.put(2, sample);

// get object
Sample actual = kvs.get(1);

// iterator
Iterator<Entry<Integer, Sample>> ite = kvs.iterator();
while(ite.hasNext()) {
	ite.next();
}

kvs.close();

public static class Sample implements Serializable {
  public String name;
  public int age;
}
```

# Values DB based Lucene
## 特徴

Lucene BaseのCSV,TSVなどをデータを簡単に扱うためのDBを提供。
MMapやNIODirectoryなどを利用することで、巨大なデータについても扱うことができる。
RDBと違い、unigramのインデックスを構築することで部分一致も比較的に高速に検索できることが特徴。

## 使い方
### Simple Use

```Java
// RAMDirectoryで初期化
LuceneValuesDB valuesDB = new LuceneValuesDB();
// ファイルのデータ型を自動認識しCSVで読み込み
valuesDB.open(new File("test.csv"), new CSVParser(), new AutoDetectDocumentCreator());

//検索対象のフィールド番号
int fieldNum = 2;
//部分一致の検索クエリ
String query = "aaa";
Document[] docs = valuesDB.search(fieldNum, query);
//読みやすいようにリスト形式に変更
List<List<String>> results = valuesDB.convertDocumentList(docs);

//完全一致の検索クエリ
String query = "aaa";
Document[] docs = valuesDB.exactSearch(fieldNum, query);

//複雑な検索
QueryParser parser = valuesDB.createQueryParser();
Query query = parser.parse("3:\"this\" AND 3:\"pen\"");
Document[] docs = valuesDB.search(query);

// writer, readerをクローズ
valuesDB.close();

```

### Advanced Use

```Java
// 1gramでなく3gramでAnalyzerを定義
LuceneValuesDB valuesDB = new LuceneValuesDB(MMapDirectory.open(indexFile), indexFile, false, new NgramAnalyzer(3, LuceneValuesDB.LUCENE_VERSION));

//インデックス構造を指定する場合
UserDefineDocumentCreator creator = new UserDefineDocumentCreator(new Class[] {
	IntField.class,
	StringField.class,
	FloatField.class,
	TextField.class
}, new String[] {
	"docNum",
	"docType",
	"score",
	"text"
});
valuesDB.open(new File("test.csv"), new CSVParser(), creator);

// Searcherを取得し自由に検索する場合
SearcherManager manager = valuesDB.getSearcherManager();
IndexSearcher searcher = manager.acquire();

TopDocs top = searcher.search(new TermQuery(new Term("docNum", "1")), 10);

manager.release(searcher);
valuesDB.close();

```

# CHANGE Logs

Version 0.0.2 (2015/10/08)  
 	* add LuceneValuesDB  
Version 0.0.1 (2015/09/27)  
 	* add LuceneKVS  

# Licenese
MIT License

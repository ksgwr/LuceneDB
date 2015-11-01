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

# Object Values DB based Lucene
## 特徴

ValuesDBを拡張しTSVなどでなくObjectを追加・検索できるように拡張したもの。
検索もObjectをセットすることで検索できる。ただし、その場合はNullを区別するためprimitiveな型の使用は非推奨。
リフレクションを多用しており、効率性はやや難があるが、巨大な階層型のデータが利用できることが特徴。
Map,List,Arrayなども対応しているが、要素中にprimitiveな型のみが使用可能でカスタムのObjectは非対応。（TODO)
単体のフィールドでpublicで取得できる単純なオブジェクトであれば利用可能で、これを使い階層型を表現する。

## 使い方
### Simple Use

```Java
LuceneObjectValuesDB valuesDB = new LuceneObjectValuesDB<Sample>(Sample.class);

Sample sample = new Sample();
sample.name = "hoge";
sample.age = 10;

// Objectを追加
valuesDB.addObject(sample);
// 書き込み結果を反映
valuesDB.commit();

// queryもObjectを生成, null以外をAND検索する
Sample query = new Sample();
query.name = "hoge";

Document[] docs = valuesDB.search(query);
// 検索結果をObjectに復元
List<Sample> lists = valuesDB.convertObjectList(docs);

```

### Advanced Use

```Java

public static class Sample {
	@TextFieldable //部分一致で検索可能
	public String name;
	@NoIndex // Index化しないが復元可能
	public Integer age;
	// 保存対象でないフィールド
	transient public Integer tmp;
	@TextFieldable(key=false) //valueのみ部分一致検索可能
	public Map<Integer, String> map;
	public Long staticVal = 123456L; //default値はデフォルトコンストラクタなので初期化可能
	public Sample(){}
	public Sample(String name) {
		this.name = name;
	}
}

// sample.name = 'a' or sample.name = 'b' の OR検索
Document[] docs = valuesDB.search(
			new Sample[]{new Sample("a"), new Sample("b")});

// mapのvalueを検索する場合
Sample query = new Sample();
query.map = new HashMap<>();
query.map.put(null, "a");

```

# CHANGE Logs

* Version 0.0.3 (2015/10/25)  
  * add LuceneObjectValuesDB  
* Version 0.0.2 (2015/10/08)  
 	* add LuceneValuesDB  
* Version 0.0.1 (2015/09/27)  
 	* add LuceneKVS  

# Licenese
Apache License 2.0

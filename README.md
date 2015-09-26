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

# Licenese
MIT License

package jp.ksgwr.lucene.kvs;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.lucene.store.MMapDirectory;
import org.junit.Test;

public class LuceneObjectKVSTest {

	@Test
	public void simpleTest() throws IOException {
		LuceneObjectKVS<Integer, Sample> kvs = new LuceneObjectKVS<>();

		Sample sample = new Sample();
		sample.name = "hoge";
		sample.age = 10;

		kvs.put(1, sample);

		Sample actual = kvs.get(1);

		assertEquals(sample.name, actual.name);
		assertEquals(sample.age, actual.age);

		kvs.close();
	}

	@Test
	public void advancedTest() throws IOException {
		File indexFile = new File("data", "test3");
		LuceneObjectKVS<Integer, Sample> kvs = new LuceneObjectKVS<>(MMapDirectory.open(indexFile), indexFile, true);

		Sample sample = new Sample();
		sample.name = "hoge";
		sample.age = 10;

		kvs.put(1, sample);
		kvs.put(2, sample);

		// get object
		Sample actual = kvs.get(1);
		assertEquals(sample.name, actual.name);

		// iterator
		Iterator<Entry<Integer, Sample>> ite = kvs.iterator();
		while(ite.hasNext()) {
			ite.next();
		}

		kvs.close();
	}

	/**
	 * sample pojo class
	 *
	 * @author ksgwr
	 *
	 */
	public static class Sample implements Serializable {
		/** name */
		public String name;
		/** age */
		public int age;
	}
}

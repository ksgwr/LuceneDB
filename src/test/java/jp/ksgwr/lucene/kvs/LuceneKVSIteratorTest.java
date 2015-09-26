package jp.ksgwr.lucene.kvs;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

public class LuceneKVSIteratorTest {

	@Test
	public void simpleTest() throws IOException {
		LuceneKVS<String, String> kvs = new LuceneStringKVS<String>();
		kvs.put("a", "aval");
		kvs.put("b", "bval");

		LuceneKVSIterator<String, String> ite = kvs.iterator();

		assertEquals(2, kvs.size());
		assertEquals(2, ite.size());
		assertEquals(true, ite.hasNext());
		assertEquals("aval", ite.next().getValue());
		assertEquals("bval", ite.next().getValue());
		assertEquals(false, ite.hasNext());

		kvs.close();
	}
}

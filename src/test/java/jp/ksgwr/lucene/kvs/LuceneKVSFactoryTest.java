package jp.ksgwr.lucene.kvs;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import org.junit.Test;

public class LuceneKVSFactoryTest {

	@Test
	public void createInstanceTest() throws InvocationTargetException {
		@SuppressWarnings("unchecked")
		Map<String, String> kvs = LuceneKVSFactory.createInstance(LuceneStringKVS.class);

		assertNotNull(kvs);
	}

	@Test
	public void mmapTest() throws InvocationTargetException, IOException {
		@SuppressWarnings("unchecked")
		Map<String, String> kvs = LuceneKVSFactory.createInstance(LuceneStringKVS.class, new File("data", "test"), true, true);

		kvs.put("a", "aval");

		assertEquals("aval", kvs.get("a"));

	}
}

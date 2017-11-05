package jp.ksgwr.lucene.kvs;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.RAMDirectory;

/**
 * Lucene KVS Factory
 *
 * @author ksgwr
 *
 */
public class LuceneKVSFactory {

	/**
	 * create instance
	 * @param clazz LuceneKVS class
	 * @param <T> value type
	 * @param directory lucene directory(mmap, nios, ram...)
	 * @param file index file
	 * @param isVolatile if true, delete file automatically
	 * @return new instance
	 * @throws InvocationTargetException exception
	 */
	@SuppressWarnings("rawtypes")
	public static <T extends LuceneKVSBase> T createInstance(Class<T> clazz, Directory directory,
			File file, boolean isVolatile) throws InvocationTargetException {
		T t = null;
		try {
			Class<?>[] types = { Directory.class, File.class, boolean.class };
			t = clazz.getConstructor(types).newInstance(
					new Object[] { directory, file, isVolatile });
		} catch (InvocationTargetException e) {
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return t;
	}

	/**
	 * create instance
	 * @param clazz LuceneKVS class
	 * @param <T> value type
	 * @param directory lucene directory(mmap, nios, ram...)
	 * @return newInstance new instance
	 * @throws InvocationTargetException exception
	 */
	@SuppressWarnings("rawtypes")
	public static <T extends LuceneKVSBase> T createInstance(Class<T> clazz, Directory directory) throws InvocationTargetException {
		return createInstance(clazz, directory, null, true);
	}

	@SuppressWarnings("rawtypes")
	public static <T extends LuceneKVSBase> T createInstance(Class<T> clazz, File file) throws InvocationTargetException, IOException {
		return createInstance(clazz, FSDirectory.open(file.toPath()), file, false);
	}

	@SuppressWarnings("rawtypes")
	public static <T extends LuceneKVSBase> T createInstance(Class<T> clazz, File file, boolean onMemory, boolean isVolatile) throws InvocationTargetException, IOException {
		if (onMemory) {
			return createInstance(clazz, new MMapDirectory(file.toPath()), file, isVolatile);
		} else {
			return createInstance(clazz, FSDirectory.open(file.toPath()), file, isVolatile);
		}
	}

	@SuppressWarnings("rawtypes")
	public static <T extends LuceneKVSBase> T createInstance(Class<T> clazz, File file, boolean onMemory) throws InvocationTargetException, IOException {
		return createInstance(clazz, file, onMemory, false);
	}

	@SuppressWarnings("rawtypes")
	public static <T extends LuceneKVSBase> T createInstance(Class<T> clazz) throws InvocationTargetException {
		return createInstance(clazz, new RAMDirectory());
	}
}

package jp.ksgwr.lucene.valuesdb.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * text fieldable (using analyzer)
 * @author ksgwr
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface TextFieldable {
	/** if map field and true, key field set TextField */
	boolean key() default true;
	/** if map field and true, value field set TextField */
	boolean value() default true;
}

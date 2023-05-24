package j.lucene.tutorial.transform;

import java.util.Collections;
import java.util.List;

import org.apache.lucene.document.Field;

/**
 * A document made up of Fields.
 *
 */
public class TransformedDocument {
	
	private final List<Field> fields;
	
	public TransformedDocument(List<Field> fields) {
		this.fields = Collections.unmodifiableList(fields);
	}
	
	public List<Field> getFields() {
		return fields;
	}

}

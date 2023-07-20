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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (Field f : fields) {
			String val;
			if (f.stringValue() != null) {
				val = f.stringValue();
				if (val.length() > 13) {
					val = val.substring(0, 10) + "...";
				}
			} else if (f.numericValue() != null) {
				val = f.numericValue().toString();
			} else {
				val = "?";
			}
			sb.append(sb.length() > 0 ? ", " : "");
			sb.append(f.name() + ": " + val);
		}
		return sb.toString();
	}

}

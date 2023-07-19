package j.lucene.tutorial.search;

import java.util.Collections;
import java.util.Map;

/**
 * A single document returned from a search.
 */
public class SearchResult {
	private final Map<String, Object> values;

	public SearchResult(Map<String, Object> values) {
		this.values = Collections.unmodifiableMap(values);
	}

	/**
	 * The fields and their values as returned by the search.
	 * 
	 * @return the map of values
	 */
	public Map<String, Object> getValues() {
		return values;
	}
}

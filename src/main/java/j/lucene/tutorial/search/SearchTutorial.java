package j.lucene.tutorial.search;

/**
 * Each type implementing this interface illustrates the use of a particular
 * Lucene query.
 *
 * @param <T> the input specifying what we are querying
 */
public interface SearchTutorial<T> {

	/**
	 * 
	 * 
	 * @param queryFor
	 * @param maxResults
	 * @return
	 */
	public SearchResults query(T queryFor, int maxResults);
}

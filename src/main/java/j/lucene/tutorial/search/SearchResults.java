package j.lucene.tutorial.search;

import java.util.Collections;
import java.util.List;

/**
 * The results of a query.
 */
public class SearchResults {

	private final long totalHits;
	private final boolean totalApproximate;
	private final List<SearchResult> results;

	public SearchResults(Builder b) {
		this.totalHits = b.totalHits;
		this.totalApproximate = b.totalApproximate;
		this.results = Collections.unmodifiableList(b.results);
	}

	/**
	 * The total results that matched the query, not the total returned. Note this
	 * may be an estimate, see {@link #isTotalApproximate()}.
	 * 
	 * @return total hits
	 */
	public long getTotalHits() {
		return totalHits;
	}

	/**
	 * if false, the total hits reported is an exact number. if true, the query
	 * matched at least as many documents as reported, possibly more, not less.
	 * 
	 * @return boolean
	 */
	public boolean isTotalApproximate() {
		return totalApproximate;
	}

	/**
	 * The returned documents with field-level data. Only as many as requested are
	 * returned, often less than the total hits.
	 * 
	 * @return the results
	 */
	public List<SearchResult> getResults() {
		return results;
	}

	public static class Builder {
		private long totalHits;
		private boolean totalApproximate;
		private List<SearchResult> results;

		public Builder totalHits(long totalHits) {
			this.totalHits = totalHits;
			return this;
		}

		public Builder totalApproximate(boolean totalApproximate) {
			this.totalApproximate = totalApproximate;
			return this;
		}

		public Builder results(List<SearchResult> results) {
			this.results = results;
			return this;
		}
	}
}

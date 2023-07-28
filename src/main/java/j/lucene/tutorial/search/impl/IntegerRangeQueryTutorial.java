package j.lucene.tutorial.search.impl;

import org.apache.lucene.document.IntPoint;

import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.SearchTutorial;
import j.lucene.tutorial.search.impl.IntegerRangeQueryTutorial.IntegerRange;

/**
 * Demonstrates searching an Integer field over an inclusive upper/lower range.
 */
public class IntegerRangeQueryTutorial extends QueryTutorialBase implements SearchTutorial<IntegerRange> {

	protected IntegerRangeQueryTutorial(IndexPhysicalLocation localDiskLocation) {
		super(localDiskLocation);
	}

	/**
	 * Note Lucene does not have a public top-level class for an integer range
	 * query. Instead, Lucene field types which may be searched by range have static
	 * methods that return the appropriate range queries.
	 */
	@Override
	public SearchResults query(IntegerRange queryFor, int maxResults) {
		return executeSearch(IntPoint.newRangeQuery(queryFor.field, queryFor.lower, queryFor.upper), 10);
	}

	public static class IntegerRange {
		private final String field;
		private final int lower;
		private final int upper;

		public IntegerRange(String field, int lower, int upper) {
			this.field = field;
			this.lower = lower;
			this.upper = upper;
		}

		public String getField() {
			return field;
		}

		public int getLower() {
			return lower;
		}

		public int getUpper() {
			return upper;
		}

	}
}

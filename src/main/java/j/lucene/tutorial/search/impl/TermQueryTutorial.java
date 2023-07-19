package j.lucene.tutorial.search.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.SearchTutorial;

/**
 * Demonstrates how to find documents matching single field and value
 * combination from Lucene.
 */
public class TermQueryTutorial extends QueryTutorialBase implements SearchTutorial<Term> {

	public TermQueryTutorial(IndexPhysicalLocation localDiskLocation) {
		super(localDiskLocation);
	}

	/**
	 * Takes a Lucene term which is a single indexed value on one field.
	 */
	@Override
	public SearchResults query(Term luceneTerm, int maxResults) {
		TermQuery q = new TermQuery(luceneTerm);
		return executeSearch(q, maxResults);
	}
}

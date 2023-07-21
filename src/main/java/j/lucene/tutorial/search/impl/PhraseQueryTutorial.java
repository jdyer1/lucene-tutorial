package j.lucene.tutorial.search.impl;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.PhraseQuery;

import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResults;
import j.lucene.tutorial.search.SearchTutorial;

/**
 * Demonstrates how to match on phrases against text fields that are analyzed and tokenized.  
 * 
 */
public class PhraseQueryTutorial extends QueryTutorialBase implements SearchTutorial<Term[]> {

	protected PhraseQueryTutorial(IndexPhysicalLocation localDiskLocation) {
		super(localDiskLocation);
	}

	/**
	 * The passed-in terms must occur in ajacent positions to match.
	 */
	@Override
	public SearchResults query(Term[] queryFor, int maxResults) {
		PhraseQuery.Builder b = new PhraseQuery.Builder();
		for(Term term : queryFor) {
			b.add(term);
		}
		return executeSearch(b.build(), maxResults);
	}

}

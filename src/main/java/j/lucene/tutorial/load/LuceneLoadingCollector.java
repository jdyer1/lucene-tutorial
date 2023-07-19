package j.lucene.tutorial.load;

import java.util.Optional;
import java.util.stream.Collector;

import j.lucene.tutorial.load.impl.DocumentLoaderSupplierObj;
import j.lucene.tutorial.load.impl.LuceneLoadingCollectorFailure;
import j.lucene.tutorial.transform.TransformedDocument;

/**
 * A Java stream Collector that loads TransformedDocument instances to a Lucene
 * index, returning the total documents that were successfully indexed.
 * 
 */
public interface LuceneLoadingCollector extends Collector<TransformedDocument, DocumentLoaderSupplierObj, Long> {

	/**
	 * Had any documents failed indexing, this will return information about the
	 * first failure.
	 * 
	 * @return an optional failure object
	 */
	public Optional<LuceneLoadingCollectorFailure> firstFailure();

}

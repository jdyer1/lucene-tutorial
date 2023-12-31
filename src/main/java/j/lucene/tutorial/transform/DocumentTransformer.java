package j.lucene.tutorial.transform;

import j.lucene.tutorial.extract.ExtractedDocument;

/**
 * Transforms raw document data to Lucene terms. This process may be lossy and
 * it is not guaranteed that the source data could be reconstructed from the
 * result.
 *
 */
public interface DocumentTransformer {

	/**
	 * Return a TransformedDocument from an ExtractedDocument.
	 * 
	 * @param in             the raw document extracted from a System of Record
	 * @return the transformed document
	 */
	public TransformedDocument transformExtractedDocument(ExtractedDocument in);
}

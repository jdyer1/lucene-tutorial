package j.lucene.tutorial.load.impl;

import j.lucene.tutorial.transform.TransformedDocument;

/**
 * If any failures were encountered while indexing documents, this holds
 * information about the first failure.
 * 
 */
public class LuceneLoadingCollectorFailure {
	private final TransformedDocument failedDocument;
	private final Exception exception;

	LuceneLoadingCollectorFailure(TransformedDocument failedDocument, Exception exception) {
		this.failedDocument = failedDocument;
		this.exception = exception;
	}

	/**
	 * The document that failed.
	 * 
	 * @return the document
	 */
	public TransformedDocument getFailedDocument() {
		return failedDocument;
	}

	/**
	 * The exception that was thrown on failure
	 * 
	 * @return the exception
	 */
	public Exception getException() {
		return exception;
	}

}
package j.lucene.tutorial.load.impl;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;

/**
 * Contains getters to the Lucene directory, index writer and index writer
 * configuration.
 */
public class DocumentLoaderSupplierObj {
	private final Directory dir;
	private final IndexWriterConfig iwc;
	protected final IndexWriter iw;

	public DocumentLoaderSupplierObj(Directory dir, IndexWriterConfig iwc, IndexWriter iw) {
		this.dir = dir;
		this.iwc = iwc;
		this.iw = iw;
	}

	/**
	 * The Lucene directory implementation for this index
	 * 
	 * @return the Directory
	 */
	public Directory dir() {
		return dir;
	}

	/**
	 * The Lucene Index Writer Configuration for this index
	 * 
	 * @return the Index Writer Configuration
	 */
	public IndexWriterConfig iwc() {
		return iwc;
	}

	/**
	 * The Lucene Index Writer for this index
	 * 
	 * @return the Index Writer
	 */
	public IndexWriter iw() {
		return iw;
	}
}
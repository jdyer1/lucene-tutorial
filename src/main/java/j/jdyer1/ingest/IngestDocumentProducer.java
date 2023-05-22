package j.jdyer1.ingest;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Extracts files from a zip archive and outputs a stream of documents. The
 * documents are raw data without any lossy transformations. The output of this
 * class represents a System Of Record extracted so that a Lucene index can be
 * generated from this stream.
 *
 */
public interface IngestDocumentProducer {

	/**
	 * create a stream of documents from a zip archive.
	 * 
	 * @param zipFilePath
	 */
	public Stream<IngestDocument> documents(Path zipFilePath);

}

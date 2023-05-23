package j.lucene.tutorial.extract;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Extracts files from a file path and outputs a stream of documents. The
 * documents are raw data without any lossy transformations. The output of this
 * class represents a System Of Record extracted so that a Lucene index can be
 * generated from this stream.
 *
 */
public interface DocumentExtractor {

	/**
	 * Extracts documents from a file
	 * 
	 * @param filePath the location
	 * @return the document stream
	 */
	public Stream<ExtractedDocument> documentsFromFilePath(Path filePath);

}

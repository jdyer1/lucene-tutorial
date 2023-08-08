package j.lucene.tutorial.search.impl;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

import org.apache.lucene.index.IndexWriterConfig;

import j.lucene.tutorial.extract.impl.DocumentExtractorBibleZipImpl;
import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.load.impl.LuceneLoadingCollectorImpl;
import j.lucene.tutorial.transform.impl.DocumentTransformerHtmlBibleImpl;

public class SearchTutorialTestBase {

	public static LuceneLoadingCollectorImpl setup(IndexWriterConfig iwc) throws Exception {
		Path tempDir = Files.createTempDirectory(QueryTutorialsTest.class.getSimpleName());
		LuceneLoadingCollectorImpl loader = new LuceneLoadingCollectorImpl(new IndexPhysicalLocation(tempDir));
		loader.iwc = iwc;
		loader.postConstruct();

		try (DocumentTransformerHtmlBibleImpl transformer = new DocumentTransformerHtmlBibleImpl()) {

			DocumentExtractorBibleZipImpl extractor = new DocumentExtractorBibleZipImpl();

			Path zipPath = Paths.get(".").toAbsolutePath().normalize().resolve("src").resolve("test")
					.resolve("resources").resolve("kj_new.zip");

			extractor.documentsFromFilePath(zipPath).map(transformer::transformExtractedDocument).collect(loader);
		}
		return loader;
	}

	public static void teardown(LuceneLoadingCollectorImpl loader) throws Exception {
		loader.preDestroy();
		Path tempDir = loader.localDiskLocation().getLocationPath();
		Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
	}

}

package j.lucene.tutorial.load.impl;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import j.lucene.tutorial.LuceneTutorialException;
import j.lucene.tutorial.load.impl.LuceneLoadingCollector.DocumentLoaderSupplierObj;
import j.lucene.tutorial.transform.TransformedDocument;

public class LuceneLoadingCollector implements Collector<TransformedDocument, DocumentLoaderSupplierObj, Long> {

	private final Path localDiskLocation;
	private final AtomicLong counter;
	private final AtomicReference<TransformedDocument> firstFailure;

	protected DocumentLoaderSupplierObj dlso;
	protected IndexWriterConfig iwc;

	public LuceneLoadingCollector(Path localDiskLocation) {
		this.localDiskLocation = localDiskLocation;
		this.counter = new AtomicLong();
		this.firstFailure = new AtomicReference<>();
	}

	public void postConstruct() {
		if (this.iwc == null) {
			this.iwc = new IndexWriterConfig();
		}
		iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		iwc.setUseCompoundFile(false);

		try {
			Directory dir = FSDirectory.open(localDiskLocation);
			IndexWriter iw = new IndexWriter(dir, iwc);
			this.dlso = new DocumentLoaderSupplierObj(dir, iwc, iw);
		} catch (Exception e) {
			throw new LuceneTutorialException("Could not open directory location:" + localDiskLocation, e);
		}
	}

	public void preDestroy() {
		check();
		try {
			dlso.iw().close();
			dlso.dir().close();
			dlso = null;
		} catch (Exception e) {
			throw new LuceneTutorialException("Could not close index writer and/or directory", e);
		}
	}

	private void check() {
		if (dlso == null) {
			throw new LuceneTutorialException("Must call 'postConstruct' before using.");
		}
	}

	@Override
	public Supplier<DocumentLoaderSupplierObj> supplier() {
		check();
		return new Supplier<DocumentLoaderSupplierObj>() {

			@Override
			public DocumentLoaderSupplierObj get() {
				return dlso;
			}
		};
	}

	@Override
	public BiConsumer<DocumentLoaderSupplierObj, TransformedDocument> accumulator() {
		return new BiConsumer<DocumentLoaderSupplierObj, TransformedDocument>() {

			@Override
			public void accept(DocumentLoaderSupplierObj t, TransformedDocument u) {
				try {
					t.iw.addDocument(u.getFields());
					counter.incrementAndGet();
				} catch (Exception e) {
					firstFailure.updateAndGet(f -> f == null ? u : f);
				}

			}

		};
	}

	@Override
	public BinaryOperator<DocumentLoaderSupplierObj> combiner() {
		return new BinaryOperator<DocumentLoaderSupplierObj>() {

			@Override
			public DocumentLoaderSupplierObj apply(DocumentLoaderSupplierObj t, DocumentLoaderSupplierObj u) {
				return t;
			}

		};
	}

	@Override
	public Function<DocumentLoaderSupplierObj, Long> finisher() {
		return new Function<DocumentLoaderSupplierObj, Long>() {

			@Override
			public Long apply(DocumentLoaderSupplierObj t) {
				try {
					t.iw.commit();
				} catch (Exception e) {
					throw new LuceneTutorialException("Could not commit.", e);
				}
				return counter.get();
			}

		};
	}

	@Override
	public Set<Characteristics> characteristics() {
		return Collections.emptySet();
	}

	public class DocumentLoaderSupplierObj {
		private final Directory dir;
		private final IndexWriterConfig iwc;
		private final IndexWriter iw;

		public DocumentLoaderSupplierObj(Directory dir, IndexWriterConfig iwc, IndexWriter iw) {
			this.dir = dir;
			this.iwc = iwc;
			this.iw = iw;
		}

		public Directory dir() {
			return dir;
		}

		public IndexWriterConfig iwc() {
			return iwc;
		}

		public IndexWriter iw() {
			return iw;
		}
	}

}

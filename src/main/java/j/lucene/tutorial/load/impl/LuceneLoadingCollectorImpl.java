package j.lucene.tutorial.load.impl;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import j.lucene.tutorial.LuceneTutorialException;
import j.lucene.tutorial.load.LuceneLoadingCollector;
import j.lucene.tutorial.transform.TransformedDocument;

/**
 * Loads documents by deferring to Lucene's IndexWriter. Because the Lucene
 * IndexWriter is threadsafe, we expect to be able to use parallel streams with
 * this collector as well.
 *
 */
public class LuceneLoadingCollectorImpl implements LuceneLoadingCollector {

	private final IndexPhysicalLocation localDiskLocation;
	private final AtomicLong counter;
	private final AtomicReference<LuceneLoadingCollectorFailure> firstFailure;

	protected DocumentLoaderSupplierObj dlso;
	public IndexWriterConfig iwc;

	public LuceneLoadingCollectorImpl(IndexPhysicalLocation localDiskLocation) {
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
			Directory dir = FSDirectory.open(localDiskLocation.getLocationPath());
			IndexWriter iw = new IndexWriter(dir, iwc);
			this.dlso = new DocumentLoaderSupplierObj(dir, iwc, iw);
		} catch (Exception e) {
			throw new LuceneTutorialException("Could not open directory location:" + localDiskLocation, e);
		}
	}

	public void preDestroy() {
		check();
		String msg = null;
		Exception e1 = null;
		try {
			dlso.iw().close();
		} catch (Exception e) {
			msg = "Could not close index writer";
			e1 = e;
		}
		try {
			dlso.dir().close();
		} catch (Exception e) {
			if (e1 != null) {
				msg = "Could not close directory";
				e1 = e;
			}
		}
		dlso = null;
		if (msg != null) {
			throw new LuceneTutorialException(msg, e1);
		}
	}
	
	public IndexPhysicalLocation localDiskLocation() {
		return localDiskLocation;
	}

	@Override
	public Optional<LuceneLoadingCollectorFailure> firstFailure() {
		LuceneLoadingCollectorFailure llcf = firstFailure.get();
		if (llcf == null) {
			return Optional.empty();
		}
		return Optional.of(llcf);
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
					firstFailure.updateAndGet(f -> f == null ? new LuceneLoadingCollectorFailure(u, e) : f);
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

}

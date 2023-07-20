package j.lucene.tutorial.search.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.StoredValue;
import org.apache.lucene.document.StoredValue.Type;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import j.lucene.tutorial.LuceneTutorialException;
import j.lucene.tutorial.load.impl.IndexPhysicalLocation;
import j.lucene.tutorial.search.SearchResult;
import j.lucene.tutorial.search.SearchResults;

/**
 * Each QueryTutorial does the essentialy the same thing but with a different
 * query input. This base class helps prevent duplication.
 * 
 */
public abstract class QueryTutorialBase {

	protected final IndexPhysicalLocation localDiskLocation;

	protected DirectoryReader dr;

	protected QueryTutorialBase(IndexPhysicalLocation localDiskLocation) {
		this.localDiskLocation = localDiskLocation;
	}

	public void postConstruct() {
		try {
			Directory dir = FSDirectory.open(localDiskLocation.getLocationPath());
			this.dr = DirectoryReader.open(dir);
		} catch (Exception e) {
			throw new LuceneTutorialException("Could not open directory location:" + localDiskLocation, e);
		}
	}

	public void preDestroy() {
		check();
		try {
			dr.close();
			dr = null;
		} catch (Exception e) {
			throw new LuceneTutorialException("Could not close index writer and/or directory", e);
		}
	}

	protected void check() {
		if (dr == null) {
			throw new LuceneTutorialException("Must call 'postConstruct' before using.");
		}
	}
	
	private Object storedValueToObject(StoredValue sv) {
		if(sv.getType() == Type.INTEGER) {
			return sv.getIntValue();
		}
		if(sv.getType() == Type.BINARY) {
			return sv.getBinaryValue().bytes;
		}
		if(sv.getType() == Type.DOUBLE) {
			return sv.getDoubleValue();
		}
		if(sv.getType() == Type.FLOAT) {
			return sv.getFloatValue();
		}
		if(sv.getType() == Type.LONG) {
			return sv.getLongValue();
		}
		return sv.getStringValue();
	}

	@SuppressWarnings("unchecked")
	protected SearchResults executeSearch(Query q, int maxResults) {
		check();
		IndexSearcher is = new IndexSearcher(dr);
		TopDocs docs;
		try {
			docs = is.search(q, maxResults);
			List<SearchResult> resultList = new ArrayList<>(maxResults);
			for (ScoreDoc sd : docs.scoreDocs) {
				Map<String, Object> fieldMap = new HashMap<>();
				for (IndexableField field : is.storedFields().document(sd.doc)) {
					Object existing = fieldMap.get(field.name());
					Object newValue = storedValueToObject(field.storedValue());
					if(existing == null) {
						fieldMap.put(field.name(), newValue);
					} else if(existing instanceof List) {
						((List<Object>) existing).add(newValue);
					} else {
						List<Object> l = new ArrayList<>(2);
						l.add(existing);
						l.add(newValue);
						fieldMap.put(field.name(), l);
					}
					
				}				
				resultList.add(new SearchResult(fieldMap));
			}
			return new SearchResults( //
					new SearchResults.Builder() //
							.results(resultList) //
							.totalHits(docs.totalHits.value) //
							.totalApproximate(docs.totalHits.relation != Relation.EQUAL_TO) //
			);
		} catch (Exception e) {
			throw new LuceneTutorialException("Could not execute query.", e);
		}
	}

}

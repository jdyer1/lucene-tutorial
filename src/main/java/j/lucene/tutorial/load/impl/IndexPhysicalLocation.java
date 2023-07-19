package j.lucene.tutorial.load.impl;

import java.nio.file.Path;

/**
 * Contains a getter for the Path where the Lucene index is located
 */
public class IndexPhysicalLocation {

	private final Path locationPath;
	
	public Path getLocationPath() {
		return locationPath;
	}

	public IndexPhysicalLocation(Path locationPath) {
		this.locationPath = locationPath;
	}
}

package j.jdyer1.lucenetutorial;

/**
 * This is an unchecked exception with which this application can wrap checked
 * esceptions.
 *
 */
public class LuceneTutorialException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public LuceneTutorialException() {
		super();
	}

	public LuceneTutorialException(String message) {
		super(message);
	}

	public LuceneTutorialException(String message, Throwable cause) {
		super(message, cause);
	}

	public LuceneTutorialException(Throwable cause) {
		super(cause);
	}
}

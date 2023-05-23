package j.lucene.tutorial;

/**
 * This is an unchecked exception with which this application can wrap checked
 * esceptions.
 *
 */
public class LuceneTutorialException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new exception
	 */
	public LuceneTutorialException() {
		super();
	}

	/**
	 * Create a new exception
	 * 
	 * @param message text
	 */
	public LuceneTutorialException(String message) {
		super(message);
	}

	/**
	 * Create a new exception
	 * 
	 * @param message text
	 * @param cause re-throw
	 */
	public LuceneTutorialException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create a new exception
	 * 
	 * @param cause re-throw
	 */
	public LuceneTutorialException(Throwable cause) {
		super(cause);
	}
}

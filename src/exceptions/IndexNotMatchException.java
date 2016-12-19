package exceptions;

public class IndexNotMatchException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IndexNotMatchException() {
		super();
	}

	public IndexNotMatchException(String message) {
		super(message);
	}
}

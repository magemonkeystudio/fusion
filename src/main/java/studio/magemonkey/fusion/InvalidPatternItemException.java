package studio.magemonkey.fusion;

public class InvalidPatternItemException extends Exception {

    public InvalidPatternItemException(String message) {
        super(message);
    }

    public InvalidPatternItemException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidPatternItemException(Throwable cause) {
        super(cause);
    }

    public InvalidPatternItemException() {
        super();
    }
}

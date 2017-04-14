package info.modoff.votingserver;

public class VoteCodeException extends Exception {
    public VoteCodeException(String message) {
        super(message);
    }

    public VoteCodeException(String message, Throwable cause) {
        super(message, cause);
    }
}

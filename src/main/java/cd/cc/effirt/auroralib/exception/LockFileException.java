package cd.cc.effirt.auroralib.exception;

public class LockFileException extends RuntimeException {
    public LockFileException(String message) {
        super(message);
    }

    public LockFileException(String message,Throwable cause) {
        super(message,cause);
    }
}

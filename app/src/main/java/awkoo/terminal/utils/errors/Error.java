package awkoo.terminal.utils.errors;

import java.io.Serializable;

public class Error implements Serializable {

    /**
     * The error code.
     */
    private int code = Errno.ERRNO_SUCCESS.code();
    /**
     * The error message.
     */
    private String message;


    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }


    public synchronized Error setStateFailed(int code, String message) {
        this.message = message;

        if (code > Errno.ERRNO_SUCCESS.code()) {
            this.code = code;
        } else {
            this.code = Errno.ERRNO_FAILED.code();
        }
        return this;
    }

    public boolean isStateFailed() {
        return code > Errno.ERRNO_SUCCESS.code();
    }


}

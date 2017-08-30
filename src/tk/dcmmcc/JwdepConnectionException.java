package tk.dcmmcc;

/**
 * 封装测试网络的时候遇到的所有异常
 * Created by DCMMC on 2017/8/28.
 */
public class JwdepConnectionException extends Exception {
    public JwdepConnectionException() {
        super();
    }

    public JwdepConnectionException(String message) {
        super(message);
    }

    public JwdepConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public JwdepConnectionException(Throwable cause) {
        super(cause);
    }
}

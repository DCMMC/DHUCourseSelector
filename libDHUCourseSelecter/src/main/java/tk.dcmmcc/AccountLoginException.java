package tk.dcmmcc;

/**
 * 封装所有因为登录出错引起的异常
 * Created by DCMMC on 2017/8/28.
 */
public class AccountLoginException extends Exception {
    public AccountLoginException() {
        super();
    }

    public AccountLoginException(String message) {
        super(message);
    }

    public AccountLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountLoginException(Throwable cause) {
        super(cause);
    }
}///~

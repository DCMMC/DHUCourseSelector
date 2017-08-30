package tk.dcmmcc;

/**
 * 封装所有因为登录出错引起的异常
 * Created by DCMMC on 2017/8/28.
 */
public class AccountLoginException extends Exception {
    AccountLoginException() {
        super();
    }

    AccountLoginException(String message) {
        super(message);
    }

    AccountLoginException(String message, Throwable cause) {
        super(message, cause);
    }

    AccountLoginException(Throwable cause) {
        super(cause);
    }
}///~

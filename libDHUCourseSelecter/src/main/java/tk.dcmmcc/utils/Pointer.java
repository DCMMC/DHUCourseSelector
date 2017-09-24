package tk.dcmmcc.utils;


/**
 * 因为Java没有指针, 所有参数都是pass-by-value, 为了方便搞了个类似于C的指针的东西
 * Created by DCMMC on 2017/8/30.
 */
public class Pointer<ObjType> {
    public ObjType p;

    //空指针
    public Pointer() {
        this(null);
    }

    public Pointer(ObjType p) {
        this.p = p;
    }

    public ObjType getP() {
        return p;
    }

    public void setP(ObjType p) {
        this.p = p;
    }
}

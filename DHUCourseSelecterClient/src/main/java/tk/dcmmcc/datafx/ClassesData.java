package tk.dcmmcc.datafx;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import tk.dcmmcc.Course;
import tk.dcmmcc.IllegalCourseException;
import tk.dcmmcc.SelectCourseThread;
import tk.dcmmcc.utils.LoggerUtil;

import java.util.logging.Logger;

/**
 * 里面封装班级和班级选课线程
 * Created by DCMMC on 2017/9/9.
 */
public class ClassesData {
    private Course course;
    private SelectCourseThread selectCourseThread;
    private static Logger logger = Logger.getLogger("DHUCourseSelecter");
    /**
     * Status Property
     * 总有4个数值:
     * 1: 还没开始
     * 2: 选课中
     * 3: 被中止
     * 4: 选课完成
     */
    private IntegerProperty status = new SimpleIntegerProperty(1);

    //private Property<Node> statusProperty = new SimpleObjectProperty<>();

    static {
        LoggerUtil.initLogger(logger);
    }

    /*
    {
        SVGGlyph icon = new SVGGlyph(0, "wait", "M512 1024a512 " +
                "512 0 1 1 512-512 512 512 0 0 1-512 512z m189.6704-362.7264L486.4 556.928V320a38.4 38.4" +
                " 0 0 0-76.8 0v256a37.9136 37.9136 0 0 0 13.1328 28.416 37.1456 37.1456 0 0 0 15.0784 " +
                "14.3104l230.4 111.6416a38.4 38.4 0 1 0 33.4592-69.0944z",Color.web("#d4237a"));
        icon.setPrefSize(32, 32);
        statusProperty.setValue(icon);
    }
    */

    //default constructor
    ClassesData() {

    }

    public ClassesData(Course course) {
        this.course = course;

        try {
            this.selectCourseThread = new SelectCourseThread(course, SettingData.getRequestInterval(),
                    SettingData.getDhuCurrentUser().getUserCookie(), SettingData.getMaxTryCount());
            this.selectCourseThread.getSuccessProperty().addListener(((observable, oldValue, newValue) -> {
                if (newValue) {
                    status.setValue(4);
                    /*
                    this.statusProperty.setValue(new SVGGlyph(0, "success", "M512 1024C229.451852" +
                            " 1024 0 794.548148 0 512S229.451852 0 512 0s512 229.451852 512 512-229.451852 " +
                            "512-512 512z m-66.37037-310.992593c17.066667 0 34.133333-5.688889 47.407407-18.9" +
                            "62963l254.103704-254.103703c26.548148-26.548148 26.548148-68.266667 0-94.814815s-" +
                            "68.266667-26.548148-94.814815 0l-208.592593 208.592593-73.955555-73.955556c-26.54" +
                            "8148-26.548148-68.266667-26.548148-92.918519 0-26.548148 26.548148-26.548148 68." +
                            "266667 0 92.918518l121.362963 121.362963c13.274074 13.274074 30.340741 18.962963 " +
                            "47.407408 18.962963z", Color.web("#0AC380")));
                    ((SVGGlyph)this.statusProperty.getValue()).setPrefSize(32, 32);
                    */
                }
            }));
        } catch (IllegalCourseException ie) {
            //Course连courseId和courseNo都没有
            logger.warning("Course连courseId和courseNo都没有\n");
        }
    }

    public SelectCourseThread getSelectCourseThread() {
        return selectCourseThread;
    }

    public Course getCourse() {
        return course;
    }

    public void setStatus(int status) {
        this.status.set(status);
    }

    /**
     * 非正常关闭
     */
    public void cancel() {
        getSelectCourseThread().stopCurrentSelectCourse();
        setStatus(3);
    }

    public IntegerProperty getStatus() {
        return status;
    }

    /*
    public void setStatus(Node status) {
        this.statusProperty.setValue(status);
    }

    public Property<Node> getStatusProperty() {
        return statusProperty;
    }
    */
}///~

package tk.dcmmcc;

/**
 * Course对象缺少必要的信息
 * Created by DCMMC on 2017/8/30.
 */
public class IllegalCourseException extends Exception {
    //问题Course
    private Course causeCourse;

    IllegalCourseException() {
        super();
    }

    IllegalCourseException(String message) {
        super(message);
    }

    IllegalCourseException(String message, Throwable cause) {
        super(message, cause);
    }


    IllegalCourseException(Throwable cause) {
        super(cause);
    }

    IllegalCourseException(String message, Course causeCourse) {
        super(message);
        this.causeCourse = causeCourse;
    }

    IllegalCourseException(Throwable cause, Course causeCourse) {
        this.causeCourse = causeCourse;
    }

    IllegalCourseException(String message, Throwable cause, Course causeCourse) {
        super(message, cause);
        this.causeCourse = causeCourse;
    }

    public Course getCauseCourse() {
        return causeCourse;
    }
}///~

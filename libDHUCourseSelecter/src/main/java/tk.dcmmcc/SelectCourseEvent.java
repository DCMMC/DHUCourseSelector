package tk.dcmmcc;

import java.util.EventObject;

/**
 * 选课成功的事件
 */
class SelectCourseEvent extends EventObject {
    private Course course;
    //抢课是否成功?
    private boolean success = false;

    SelectCourseEvent(Object source, Course course, boolean success) {
        super(source);
        this.course = course;
        this.success = success;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public Course getCourse() {
        return course;
    }

    public boolean isSuccess() {
        return success;
    }
}///~
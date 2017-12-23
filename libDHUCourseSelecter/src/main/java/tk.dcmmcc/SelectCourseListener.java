package tk.dcmmcc;

import java.util.EventListener;

/**
 * 选课事件的监听器
 * Created by DCMMC on 2017/8/30.
 */
public class SelectCourseListener implements EventListener {
    public void selectSuccessEvent(SelectCourseEvent selectEvent) {
        Course successCourse = selectEvent.getCourse();
        System.out.println(successCourse.getCourse().getTextTitle() + "(courseId=" + successCourse.getCourseId()
                + ", courseNo=" + successCourse.getCourseNo() + ")" + (selectEvent.isSuccess() ? "选课成功" :
                "选课未成功(被用户强行中止)") + "!");
        //do something....
    }
}///~

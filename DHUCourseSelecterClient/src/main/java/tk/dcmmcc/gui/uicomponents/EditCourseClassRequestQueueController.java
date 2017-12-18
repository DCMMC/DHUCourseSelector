package tk.dcmmcc.gui.uicomponents;

import io.datafx.controller.ViewController;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javax.annotation.PostConstruct;

/**
 * TODO 完成智能导入 手动导入功能
 * 修改课程选课队列的界面的Controller
 * Created by DCMMC on 2017/9/10.
 */
@ViewController(value = "/fxml/ui/EditQueue.fxml", title = "Select Courses Session")
public class EditCourseClassRequestQueueController {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    @FXML
    private StackPane root;
    @FXML
    private ScrollPane scrollPane;
    @FXML
    private VBox vboxRoot;

    /**
     * init fxml when loaded.
     */
    @PostConstruct
    public void init() throws Exception {
        //debug
        //System.out.println("debug");


    }
}

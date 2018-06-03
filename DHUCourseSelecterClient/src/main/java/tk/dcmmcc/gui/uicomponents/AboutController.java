package tk.dcmmcc.gui.uicomponents;

import com.jfoenix.controls.JFXListView;
import com.jfoenix.controls.JFXScrollPane;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javax.annotation.PostConstruct;
import java.util.Objects;

/**
 * TODO 完善关于信息
 * 关于界面
 * Created by DCMMC on 2017/9/5.
 */
@ViewController(value = "/fxml/ui/About.fxml", title = "About")
public class AboutController {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    @FXML
    private JFXListView<String> contentList;
    @FXML
    private JFXScrollPane scroll;
    @FXML
    private VBox aboutVBox;

    /**
     * init fxml when loaded.
     */
    @PostConstruct
    public void init() {
        Objects.requireNonNull(context, "context");
        ((BorderPane) context.getRegisteredObject("borderPane")).setBottom(null);

        //设置页面最大高度
        aboutVBox.setMaxHeight(3000);

        JFXScrollPane.smoothScrolling((ScrollPane) scroll.getChildren().get(0));
    }
}///~

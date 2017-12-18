package tk.dcmmcc.gui.sidemenu;

import com.jfoenix.controls.JFXDrawer;
import com.jfoenix.controls.JFXHamburger;
import com.jfoenix.controls.JFXListView;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.Flow;
import io.datafx.controller.flow.FlowException;
import io.datafx.controller.flow.FlowHandler;
import io.datafx.controller.flow.action.ActionTrigger;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import io.datafx.controller.util.VetoException;
import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import tk.dcmmcc.gui.uicomponents.*;

import javax.annotation.PostConstruct;
import java.util.Objects;

@ViewController(value = "/fxml/SideMenu.fxml", title = "DHU Course Selecter Client")
public class SideMenuController {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    //选课界面
    @FXML
    @ActionTrigger("session")
    private Label session;
    //设置
    @FXML
    @ActionTrigger("setting")
    private Label setting;
    //关于
    @FXML
    @ActionTrigger("about")
    private Label about;
    //退出
    @FXML
    @ActionTrigger("exit")
    private Label exit;
    @FXML
    private JFXListView<Label> sideList;

    /**
     * init fxml when loaded.
     */
    @PostConstruct
    public void init() {
        Objects.requireNonNull(context, "context");
        FlowHandler contentFlowHandler = (FlowHandler) context.getRegisteredObject("ContentFlowHandler");
        sideList.propagateMouseEventsToParent();
        //sideList.setPrefHeight(50);
        sideList.getSelectionModel().selectedItemProperty().addListener((o, oldVal, newVal) -> {
            if (newVal != null) {
                try {
                    //如果是exit, 就退出整个程序
                    if (newVal.getId().equals("exit"))
                        Platform.exit();
                    contentFlowHandler.handle(newVal.getId());
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ie) {
                        //...不管他
                    }
                    ((JFXDrawer) context.getRegisteredObject("drawer")).close();
                } catch (VetoException | FlowException exc) {
                    exc.printStackTrace();
                }
            }
        });

        Flow contentFlow = (Flow) context.getRegisteredObject("ContentFlow");
        //drawer中的那一堆控件
        bindNodeToController(session, SessionController.class, contentFlow, contentFlowHandler);
        bindNodeToController(setting, SettingController.class, contentFlow, contentFlowHandler);
        bindNodeToController(about, AboutController.class, contentFlow, contentFlowHandler);
        bindNodeToController(exit, AboutController.class, contentFlow, contentFlowHandler);

    }

    private void bindNodeToController(Node node, Class<?> controllerClass, Flow flow, FlowHandler flowHandler) {
        flow.withGlobalLink(node.getId(), controllerClass);
    }

}

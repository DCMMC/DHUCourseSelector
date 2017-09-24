package tk.dcmmcc.gui.uicomponents;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXSnackbar;
import com.jfoenix.controls.JFXTextField;
import com.jfoenix.validation.base.ValidatorBase;
import io.datafx.controller.ViewController;
import io.datafx.controller.flow.context.FXMLViewFlowContext;
import io.datafx.controller.flow.context.ViewFlowContext;
import javafx.fxml.FXML;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import tk.dcmmcc.datafx.SettingData;
import javax.annotation.PostConstruct;

/**
 * 设置界面
 * Created by DCMMC on 2017/9/5.
 */
@ViewController(value = "/fxml/ui/Setting.fxml", title = "Setting")
public class SettingController {
    @FXMLViewFlowContext
    private ViewFlowContext context;
    @FXML
    private StackPane root;
    @FXML
    private JFXButton confirmButton;
    @FXML
    private JFXButton resetButton;
    @FXML
    private JFXTextField requestInterval;
    @FXML
    private JFXTextField maxTryCount;

    /**
     * init fxml when loaded.
     */
    @PostConstruct
    public void init() {
        ((BorderPane) ((StackPane) context.getRegisteredObject("MainRoot")).getChildren().get(0))
                .setBottom(null);

        //输入只能是-1或者大于0的整数
        maxTryCount.setValidators(maxTryCount.getValidators().get(0), new ValidatorBase() {
            @Override
            protected void eval() {
                if (srcControl.get() instanceof TextInputControl) {
                    TextInputControl textField = (TextInputControl) srcControl.get();
                    if (!textField.getText().isEmpty() && textField.getText() != null) {
                        try {
                            Long maxTryCount = Long.valueOf(textField.getText());

                            if (maxTryCount < -1 || maxTryCount == 0) {
                                hasErrors.set(true);
                                setMessage("输入的值范围错误! 只能是-1或者大于0的整数");
                            } else {
                                hasErrors.set(false);
                            }
                        } catch (NumberFormatException ne) {
                            hasErrors.set(true);
                            setMessage("输入格式错误! 只能含有数字");
                        }
                    } else {
                        hasErrors.set(true);
                    }
                }
            }
        });
        //验证
        maxTryCount.focusedProperty().addListener((o, oldVal, newVal) -> {
            if (!newVal) {
                maxTryCount.validate();
            }
        });

        //输入只能是大于5的整数
        requestInterval.setValidators(requestInterval.getValidators().get(0), new ValidatorBase() {
            @Override
            protected void eval() {
                if (srcControl.get() instanceof TextInputControl) {
                    TextInputControl textField = (TextInputControl) srcControl.get();
                    if (!textField.getText().isEmpty() && textField.getText() != null) {
                        try {
                            Integer interval = Integer.valueOf(textField.getText());

                            if (interval < 5) {
                                hasErrors.set(true);
                                setMessage("输入的值范围错误! 只能是大于5的整数");
                            } else {
                                hasErrors.set(false);
                            }
                        } catch (NumberFormatException ne) {
                            hasErrors.set(true);
                            setMessage("输入格式错误! 只能含有数字");
                        }
                    } else {
                        hasErrors.set(true);
                    }
                }
            }
        });
        //验证
        requestInterval.focusedProperty().addListener((o, oldVal, newVal) -> {
            if (!newVal) {
                requestInterval.validate();
            }
        });

        JFXSnackbar info = new JFXSnackbar();
        //设置提示snackBar的Container
        info.registerSnackbarContainer(root);

        resetButton.setOnMouseClicked(event -> {
            SettingData.reset();
            info.fireEvent(new JFXSnackbar.SnackbarEvent("重置设置成功",
                    "了解", 2000, false,
                    b -> info.close()));
        });

        confirmButton.setOnMouseClicked(event -> {
            if (requestInterval.validate() && maxTryCount.validate()) {
                SettingData.setMaxTryCount(Long.valueOf(maxTryCount.getText()));
                SettingData.setRequestInterval(Integer.valueOf(requestInterval.getText()));
                info.fireEvent(new JFXSnackbar.SnackbarEvent("应用成功",
                        "了解", 2000, false,
                        b -> info.close()));

            } else {
                info.fireEvent(new JFXSnackbar.SnackbarEvent("应用失败, 请检查设置!",
                        "了解", 2000, false,
                        b -> info.close()));
            }
        });
    }
}///~

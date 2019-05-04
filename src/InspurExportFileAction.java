import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

/**
 * @author zhangchao03
 * @date 2019-05-01
 */
public class InspurExportFileAction extends AnAction {
    /**
     * action入口
     * */
    @Override
    public void actionPerformed(AnActionEvent e) {
        InspurExportFileDialog dialog = new InspurExportFileDialog(e);
        //设置弹窗宽度和高度
        dialog.setSize(720, 415);
        //设置窗口的初始位置
        dialog.setLocationRelativeTo(null);
        //设置窗口可见与否
        dialog.setVisible(true);
        dialog.requestFocus();
    }
}

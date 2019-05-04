import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

public class InspurExportFileDialog extends JDialog {

    public static final String JAVA_SUFFIX = ".java";
    //确定取消操作区面板区
    private JPanel contentPane;
    //确定按钮
    private JButton buttonOK;
    //取消按钮
    private JButton buttonCancel;
    //保存路径文本框
    private JTextField textField;
    //保存文件名称
    private JTextField fileRootName;
    //选择路径按钮
    private JButton fileChooseBtn;
    //已选择文件的操作区
    private JPanel filePanel;
    //java类型选择框
    private JCheckBox javaCheckBox;
    //class类型选择框
    private JCheckBox classCheckBox;
    //导出文件后打开根目录开关
    private JCheckBox openFileFlag;
    //导出进度条创建
    private JProgressBar progressBar1;
    private JScrollPane jScrollPane;
    //响应事件
    private AnActionEvent event;
    //已选择文件的列表数组
    private JBList fieldList;

    //3.0版本的java路径
    private static final String SRC_PATH_30 = "/src";
    //3.7版本的java路径
    private static final String SRC_PATH_37 = "/src/main";

    //组件属性(propertiesComponent)中用来写入导出路径的key值
    private static final String SAVE_PATH_KEY = "inspur_export_path";
    //组件属性(propertiesComponent)中用来存储是否默认打开文件管理器的key值
    private static final String OPEN_FILE_FLAG = "inspur_export_open_flag";
    //组件属性对象
    private static PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();

    /**
     * 导出窗口进行操作的设置
     */
    InspurExportFileDialog(final AnActionEvent event) {
        //捕获事件
        this.event = event;
        //设置对话框名称
        setTitle("文件导出");

        //加载确定取消操作区的面板
        setContentPane(contentPane);
        //指定此对话框是否应为模态
        setModal(true);
        //设置确定按钮为此对话框的对象下的默认操作按钮（响应键盘enter事件）
        getRootPane().setDefaultButton(buttonOK);
        //保存路径的文本框里面默认加载组件属性中存储的上次保留的导出路径(读取成功即加载，否则置空)
        textField.setText(StringUtils.isNoneBlank(propertiesComponent.getValue(SAVE_PATH_KEY)) ? propertiesComponent.getValue(SAVE_PATH_KEY) : "");
        //是否打开导出的根目录设置存储属性
        openFileFlag.setSelected(StringUtils.isNoneBlank(propertiesComponent.getValue(OPEN_FILE_FLAG)) ? Boolean.parseBoolean(propertiesComponent.getValue(OPEN_FILE_FLAG)) : false);
        //确定按钮添加事件监听事件[onOK()]
        buttonOK.addActionListener(e -> onOK());
        //取消按钮添加事件监听事件[onCancel()]
        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        //增加对话框右上角关闭按钮的监听事件（这里可以看到有4个属性，其中0是关闭）
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        //右上角关闭按钮增加响应事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        //onCancel()方法绑定键盘操作（不太懂，好像是默认响应ESC按键是取消和关闭）
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // 选择保存路径按钮（打开按钮）响应事件
        fileChooseBtn.addActionListener(e -> {
            //获取当前windows用户home路径
            String userDir = System.getProperty("user.home");
            //创建JFileChooser对象（文件路径选择对象），并且设置默认保存路径为桌面（当前windows用户home路径+/Desktop）
            JFileChooser fileChooser = new JFileChooser(userDir + "/Desktop");
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int flag = fileChooser.showOpenDialog(null);
            //JFileChooser对话框的操作按钮响应
            if (flag == JFileChooser.APPROVE_OPTION) {
                //获得选择的文件夹的全部路径用来进行保存和设置属性
                String absolutePath = fileChooser.getSelectedFile().getAbsolutePath();
                //保存路径文本框中填充选择的文件路径
                textField.setText(absolutePath);
                //组件属性中的保存路径属性设置为刚才选择的路径
                propertiesComponent.setValue(SAVE_PATH_KEY, absolutePath);
            }
        });

    }

    /**
     * 点击确定按钮响应事件
     */
    private void onOK() {
        // 条件校验
        //检查保存路径是否为空
        if (null == textField.getText() || "".equals(textField.getText())) {
            Messages.showErrorDialog(this, "请先选择保存路径!", "Error");
            return;
        }
        //检查保存文件根目录名称是否为空
        if (null == fileRootName.getText() || "".equals(fileRootName.getText())) {
            Messages.showErrorDialog(this, "请先填写保存名称!", "Error");
            return;
        } else {//判断保存文件名称中不能包含特殊字符
            String regEx = "[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t";
            if ((fileRootName.getText()).matches(regEx)) {
                Messages.showErrorDialog(this, "填写的保存名称中不能包含特殊字符!", "Error");
                return;
            }
        }
        //检查已选择文件列表是否为空
        ListModel<VirtualFile> model = fieldList.getModel();
        if (model.getSize() == 0) {
            Messages.showErrorDialog(this, "导出文件目录不能为空,请先选择导出文件!", "Error");
            return;
        }

        try {
            //导出之前设置进度条可见
            progressBar1.setVisible(true);
            //立即响应进度条设置的属性
            progressBar1.paintImmediately(progressBar1.getBounds());
            //获取当前事件所在的项目Project的组对象
            ModuleManager moduleManager = ModuleManager.getInstance(event.getProject());
            //模块组对象转为模块对象组
            Module[] modules = moduleManager.getModules();
            //模块名称
            String moduleName;
            String srcPath_30;
            String srcPath_37;
            //获取导出文件名并且过滤掉特殊字符
            String exportFileName = fileRootName.getText().replaceAll("[ _`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？]|\n|\r|\t", "");
            // 导出目录（选择的保存路径文件夹）
            String exportPath = textField.getText() + "/" + exportFileName;
            // 模块对象
            for (Module module : modules) {
                moduleName = module.getName();
                srcPath_30 = moduleName + SRC_PATH_30;
                srcPath_37 = moduleName + SRC_PATH_37;
                //检查java勾选框是否选择
                boolean javaCheckBoxSelected = javaCheckBox.isSelected();
                //检查class勾选框是否选择
                boolean classCheckBoxSelected = classCheckBox.isSelected();
                //对选择的文件（model）进行遍历执行后面的导出规则
                for (int i = 0; i < model.getSize(); i++) {
                    VirtualFile element = model.getElementAt(i);
                    //获取当前导出文件的路径
                    String elementPath = element.getPath();
                    //判断导出文件的路径中是否包含着项目的名称
                    if (elementPath.indexOf(moduleName) != -1) {
                        /*具体判断规则：为了兼容3.0版本和3.7maven的版本,两个版本的有些路径是不同的*/
                        //根据路径自动判断是3.0的还是3.7maven的
                        //要先判断37的再判断30的，因为有包含关系37的包含30的

                        //是3.7的版本的java格式文件或者resources因为带着/main
                        if (elementPath.indexOf(srcPath_37) != -1) {
                            //判断是java类文件还是resources文件
                            if (elementPath.indexOf(srcPath_37 + "/java") != -1) {
                                String packPath = StringUtils.substring(elementPath, elementPath.indexOf(srcPath_37 + "/java") + (srcPath_37 + "/java").length() + 1);
                                //导出java文件
                                if (javaCheckBoxSelected) {
                                    exportJavaSource(elementPath, exportPath + "/java/" + packPath);
                                }
                                //导出class文件
                                if (classCheckBoxSelected) {
                                    exportJavaClass("/src/main/webapp/WEB-INF/classes", SRC_PATH_37 + "/java", elementPath, exportPath + "/webapp/WEB-INF/classes/" + packPath);
                                }
                            } else if (elementPath.indexOf(srcPath_37 + "/resources") != -1) {
                                String packPath = StringUtils.substring(elementPath, elementPath.indexOf(srcPath_37 + "/resources") + (srcPath_37 + "/resources").length() + 1);
                                if (javaCheckBoxSelected) {
                                    exportJavaSource(elementPath, exportPath + "/resources/" + packPath);
                                }
                                //导出class文件
                                if (classCheckBoxSelected) {
                                    exportJavaClass("/src/main/webapp/WEB-INF/classes", SRC_PATH_37 + "/resources", elementPath, exportPath + "/webapp/WEB-INF/classes/" + packPath);
                                }
                            } else if (elementPath.indexOf(srcPath_37 + "/webapp") != -1) {
                                String packPath = StringUtils.substring(elementPath, elementPath.indexOf(srcPath_37 + "/webapp") + srcPath_37.length() + 1);
                                exportNormal(elementPath, exportPath + "/" + packPath);
                            }
                        }
                        //是3.0的版本的java格式文件或者resources,因为不带着/main
                        else if (elementPath.indexOf(srcPath_30) != -1) {
                            //判断是resources文件
                            if (elementPath.indexOf(srcPath_30 + "/resources") != -1) {
                                String packPath = StringUtils.substring(elementPath, elementPath.indexOf(srcPath_30) + srcPath_30.length() + 1);
                                if (javaCheckBoxSelected) {
                                    exportJavaSource(elementPath, exportPath + "/resources/" + packPath);
                                }
                                //导出class文件
                                if (classCheckBoxSelected) {
                                    exportJavaClass("/WebRoot/WEB-INF/classes", SRC_PATH_30, elementPath, exportPath + "/WebRoot/WEB-INF/classes/" + packPath);
                                }
                            }
                            //判断是java类文件
                            else {
                                String packPath = StringUtils.substring(elementPath, elementPath.indexOf(srcPath_30) + srcPath_30.length() + 1);
                                if (javaCheckBoxSelected) {
                                    exportJavaSource(elementPath, exportPath + "/java/" + packPath);
                                }
                                //导出class文件
                                if (classCheckBoxSelected) {
                                    exportJavaClass("/WebRoot/WEB-INF/classes", SRC_PATH_30, elementPath, exportPath + "/WebRoot/WEB-INF/classes/" + packPath);
                                }
                            }
                        }
                        //既不是java格式文件又不是resources里的文件就执行按照原路径的导出
                        else {
                            String packPath = StringUtils.substring(elementPath, elementPath.indexOf(moduleName) + moduleName.length() + 1);
                            exportNormal(elementPath, exportPath + "/" + packPath);
                        }

                    } else {
                        Messages.showErrorDialog(this, "项目Modules的Name与项目根路径名不一致,请修改或者联系张超!", "Error");
                        return;
                    }
                }
            }
            //导出成功之后设置进度条不可见属性
            progressBar1.setVisible(false);
            //立即响应进度条设置的属性
            progressBar1.paintImmediately(progressBar1.getBounds());
            //导出成功提示对话框
            Messages.showInfoMessage(this, "文件导出成功,请查看!", "Info");

            //获得是否默认打开根目录选择内容
            boolean openFileFlagSelected = openFileFlag.isSelected();
            //存储是否默认打开根目录选择属性
            propertiesComponent.setValue(OPEN_FILE_FLAG, String.valueOf(openFileFlagSelected));
            if (openFileFlagSelected) {
                //打开导出文件根目录
                Desktop.getDesktop().open(new File(exportPath));
            }
        } catch (Exception e) {
            e.printStackTrace();
            //导出失败之后设置进度条不可见属性
            progressBar1.setVisible(false);
            //立即响应进度条设置的属性
            progressBar1.paintImmediately(progressBar1.getBounds());
            //导出失败提示对话框
            Messages.showErrorDialog(this, "系统报错,文件导出失败!", "Error");
        }

        dispose();
    }

    /**
     * 导出非java非class非Resources文件的方法
     *
     * @param elementPath 要导出的文件
     * @param packPath    要导出的目录的文件夹
     * @throws IOException
     */
    private void exportNormal(String elementPath, String packPath) throws IOException {
        //按照elementPath路径找到要导出的文件
        File srcFrom = new File(elementPath);
        //按照packPath找到要导出的目录的文件夹
        File srcTo = new File(packPath);
        //复制文件到导出路径
        FileUtil.copyFileOrDir(srcFrom, srcTo);
    }

    /**
     * 导出class格式文件的方法
     *
     * @param targetPath
     * @param elementPath
     * @param packPath
     * @throws IOException
     */
    private void exportJavaClass(String targetPath, String src_path, String elementPath, String packPath) throws IOException {
        String toWebInfoPath = StringUtils.replace(elementPath, src_path, targetPath);
        if (toWebInfoPath.endsWith(JAVA_SUFFIX)) {
            String classPath = StringUtils.substring(toWebInfoPath, 0, toWebInfoPath.lastIndexOf("/") + 1);
            //获得java对应的文件class文件名称
            final String className = StringUtils.substring(toWebInfoPath, toWebInfoPath.lastIndexOf("/") + 1, toWebInfoPath.lastIndexOf("."));
            /*1.导出文件名称完全相符的文件*/
            String[] list = new File(classPath).list((dir, name) -> name.equals(className + ".class"));
            String localPath;
            for (String cname : list) {
                localPath = StringUtils.substring(packPath, 0, packPath.lastIndexOf("/") + 1) + cname;
                FileUtil.copyFileOrDir(new File(classPath + cname), new File(localPath));
            }
            /*2.导出编译的扩展文件，即带$字符的文件*/
            String[] listExt = new File(classPath).list((dir, name) -> name.startsWith(className + "$"));
            String localPathExt;
            for (String cnameExt : listExt) {
                localPathExt = StringUtils.substring(packPath, 0, packPath.lastIndexOf("/") + 1) + cnameExt;
                FileUtil.copyFileOrDir(new File(classPath + cnameExt), new File(localPathExt));
            }
        } else {
            FileUtil.copyFileOrDir(new File(toWebInfoPath), new File(packPath));
        }
    }

    /**
     * 导出java文件的方法
     *
     * @param elementPath 要导出的文件
     * @param packPath    要导出的目录的文件夹
     * @throws IOException
     */
    private void exportJavaSource(String elementPath, String packPath) throws IOException {
        //按照elementPath路径找到要导出的文件
        File srcFrom = new File(elementPath);
        //按照packPath找到要导出的目录的文件夹
        File srcTo = new File(packPath);
        //复制文件到导出路径
        FileUtil.copyFileOrDir(srcFrom, srcTo);
    }

    /**
     * 关闭对话框操作方法
     */
    private void onCancel() {
        //释放此使用的所有本机屏幕资源,关闭对话框
        dispose();
    }

    /**
     * 已选择文件 后面的文本框的创建
     */
    private void createUIComponents() {
        //捕获event时间中的所有文件路径
        VirtualFile[] data = event.getData(DataKeys.VIRTUAL_FILE_ARRAY);
        //fieldList赋值为JBList数组
        fieldList = new JBList(data);
        //JBList的空值是显示内容，这些都是这个文本框的一些属性
        fieldList.setEmptyText("没有任何文件可供导出!");
        //以下两句是创建一个已选择文件操作区域
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(fieldList);
        filePanel = decorator.createPanel();
    }
}

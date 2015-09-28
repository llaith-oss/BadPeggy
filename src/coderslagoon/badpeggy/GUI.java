package coderslagoon.badpeggy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.Logger;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;

import coderslagoon.badpeggy.scanner.ImageFormat;
import coderslagoon.badpeggy.scanner.ImageScanner;
import coderslagoon.badpeggy.scanner.ImageScanner.Result;
import coderslagoon.baselib.io.FileNode;
import coderslagoon.baselib.io.FileRegistrar;
import coderslagoon.baselib.io.FileSystem;
import coderslagoon.baselib.io.LocalFileSystem;
import coderslagoon.baselib.io.FileRegistrar.InMemory.DefCmp;
import coderslagoon.baselib.swt.dialogs.About;
import coderslagoon.baselib.swt.dialogs.LangDialog;
import coderslagoon.baselib.swt.dialogs.MessageBox2;
import coderslagoon.baselib.swt.util.ImageViewer;
import coderslagoon.baselib.swt.util.SWTUtil;
import coderslagoon.baselib.swt.util.Safe;
import coderslagoon.baselib.swt.util.ShellProps;
import coderslagoon.baselib.swt.util.Splitter;
import coderslagoon.baselib.util.BaseLibException;
import coderslagoon.baselib.util.Log;
import coderslagoon.baselib.util.MiscUtils;
import coderslagoon.baselib.util.Prp;
import coderslagoon.baselib.util.VarRef;

public class GUI implements Runnable, NLS.Reg.Listener {
    final static String PROPERTIES = "badpeggy";

    final static String VERSION = "2.1";

    final static int DLG_GAP = 10;

    final static int IO_BUF_SZ = 0x10000;

    static Log _log = new Log("GUI");

    boolean           fatalErr;
    boolean           scanning;
    int               scanned;
    int               unreadable;
    int               damaged;
    int               maxActiveScanRuns;
    int               activeScanRuns;
    long              numOfFiles;
    double            lastPercentage;
    FileSystem.Filter searchFilter;
    Map<String, File> manualFiles = new HashMap<>();
    ExecutorService   exec;
    AtomicBoolean     esc   = new AtomicBoolean();
    AtomicBoolean     close = new AtomicBoolean();

    List<ImageScanner.Result> results  = new LinkedList<>();

    Display     display;
    Shell       shell;
    Canvas      img;
    Table       badLst;
    TableColumn colFile;
    TableColumn colReason;
    Label       info;
    DropTarget  drop;
    MenuItem    mniFile;
    MenuItem    mniScan;
    MenuItem    mniExit;
    MenuItem    mniOpts;
    MenuItem    mniIncSubFolders;
    MenuItem    mniDifferentiate;
    MenuItem    mniUseAllCPUCores;
    MenuItem    mniFileExts;
    MenuItem    mniLanguage;
    MenuItem    mniHelp;
    MenuItem    mniDocumentation;
    MenuItem    mniWebsite;
    MenuItem    mniAbout;
    MenuItem    mniDelete;
    MenuItem    mniMove;
    MenuItem    mniExportList;
    MenuItem    mniSelectAll;
    MenuItem    mniClear;
    Menu        popupMenu;
    Menu        mainMenu;

    Composite    splitterLand;
    Splitter     splitter;
    ImageViewer  imgViewer;

    DragSource   dragSource;
    boolean      draggingOut;

    ShellProps   shellProps;

    static File _propertiesFile;
    static {
        _propertiesFile = MiscUtils.determinePropFile(GUI.class, PROPERTIES, true);
    }

    final static String PRODUCT_NAME = "Bad Peggy";
    public final static String PRODUCT_SITE = "https://www.coderslagoon.com/home.php";
    
    final static int EXITCODE_UNRECERR = 1;

    private void initLang() throws BaseLibException {
        NLS.Reg.instance().load(DEFAULT_LANG_ID);
        Prp.loadFromFile(Prp.global(), _propertiesFile);
        String lang = GUIProps.SET_LANG.get();
        if (null == lang) {
            LangDialog ldlg = new LangDialog(this.shell, Prp.global(), LANGS, PRODUCT_NAME, true);
            ldlg.open();
            ldlg.waitForClose();
            GUIProps.SET_LANG.set(Prp.global(), lang = ldlg.id());
        }
        NLS.Reg.instance().load(lang);
    }

    public GUI() throws BaseLibException {

        Display.setAppName(PRODUCT_NAME);
        Display.setAppVersion(VERSION);

        this.display = new Display();
        this.display.addListener(SWT.Dispose, new Safe.Listener() {
            @Override
            protected void unsafeHandleEvent(Event event) {
                exit();
            }
        });

        this.shell = new Shell(this.display, SWT.BORDER | SWT.SHELL_TRIM | SWT.TITLE);
        this.shell.setText(PRODUCT_NAME);

        setProgramIcon();
        initLang();

        GridLayout gl = new GridLayout();
        gl.numColumns        = 1;
        gl.horizontalSpacing = 0;
        gl.verticalSpacing   = 0;
        gl.marginHeight      = 0;
        gl.marginWidth       = 0;
        this.shell.setLayout(gl);

        this.shellProps = new ShellProps(
                this.shell,
                GUIProps.GUI_PFX,
                Prp.global(),
                new Point(600, 400),
                false);

        Menu mn0, mn1;

        mn0 = SWTUtil.getMainMenu(this.display, this.shell);

        this.mniFile = new MenuItem(mn0, SWT.CASCADE);
        this.mniFile.setMenu(mn1 = new Menu(this.shell, SWT.DROP_DOWN));
        this.mniScan = new MenuItem(mn1, SWT.NONE);
        this.mniScan.setAccelerator(SWT.MOD1 | 'S');
        this.mniScan.addListener(SWT.Selection, this.onScan);
        new MenuItem(mn1, SWT.SEPARATOR);
        this.mniExit = new MenuItem(mn1, SWT.NONE);
        this.mniExit.setAccelerator(SWT.MOD1 | (MiscUtils.underOSX() ? 'B' : 'Q'));
        this.mniExit.addListener(SWT.Selection, this.onExit);

        this.mniOpts = new MenuItem(mn0, SWT.CASCADE);
        this.mniOpts.setMenu(mn1 = new Menu(this.shell, SWT.DROP_DOWN));

        this.mniUseAllCPUCores = new MenuItem(mn1, SWT.CHECK);
        this.mniUseAllCPUCores.setSelection(GUIProps.OPTS_USEALLCPUCORES.get());
        if (1 == Runtime.getRuntime().availableProcessors()) {
            this.mniUseAllCPUCores.setEnabled(false);
        }

        this.mniIncSubFolders = new MenuItem(mn1, SWT.CHECK);
        this.mniIncSubFolders.setSelection(GUIProps.OPTS_INCSUBFOLDERS.get());

        this.mniDifferentiate = new MenuItem(mn1, SWT.CHECK);
        this.mniDifferentiate.setSelection(GUIProps.OPTS_DIFFERENTIATE.get());
        this.mniDifferentiate.addListener(SWT.Selection, this.onDifferentiate);

        this.mniFileExts = new MenuItem(mn1, SWT.NONE);
        this.mniFileExts.addListener(SWT.Selection, this.onFileExtensions);

        this.mniLanguage = new MenuItem(mn1, SWT.NONE);
        this.mniLanguage.addListener(SWT.Selection, this.onLanguage);

        this.mniHelp = new MenuItem(mn0, SWT.CASCADE);
        this.mniHelp.setMenu(mn1 = new Menu(this.shell, SWT.DROP_DOWN));
        this.mniDocumentation = new MenuItem(mn1, SWT.NONE);
        this.mniDocumentation.setAccelerator(SWT.F1);
        this.mniDocumentation.addListener(SWT.Selection, this.onDocumentation);
        this.mniWebsite = new MenuItem(mn1, SWT.NONE);
        this.mniWebsite.addListener(SWT.Selection, this.onWebsite);
        new MenuItem(mn1, SWT.SEPARATOR);
        this.mniAbout = new MenuItem(mn1, SWT.NONE);
        this.mniAbout.addListener(SWT.Selection, this.onAbout);

        this.splitterLand = new Composite(this.shell, SWT.FILL);

        this.splitter = new Splitter(this.splitterLand, true);

        this.img = new Canvas(this.splitterLand, SWT.PUSH | SWT.NO_BACKGROUND);
        this.img.setBounds(0, 0, 300, 300);
        this.img.addMouseListener(this.onImageClick);
        this.imgViewer = new ImageViewer(this.img, true,
                GUIProps.OPTS_IMGVIEWACTIVE.get());

        this.splitter.sash();

        this.popupMenu = new Menu(this.shell, SWT.POP_UP);
        this.mniDelete = new MenuItem(this.popupMenu, SWT.NONE);
        this.mniDelete.addListener(SWT.Selection, this.onDelete);
        this.mniMove = new MenuItem(this.popupMenu, SWT.NONE);
        this.mniMove.addListener(SWT.Selection, this.onMove);
        new MenuItem(this.popupMenu, SWT.SEPARATOR);
        this.mniSelectAll = new MenuItem(this.popupMenu, SWT.NONE);
        this.mniSelectAll.addListener(SWT.Selection, this.onSelectAll);
        this.mniClear = new MenuItem(this.popupMenu, SWT.NONE);
        this.mniClear.addListener(SWT.Selection, this.onClear);
        new MenuItem(this.popupMenu, SWT.SEPARATOR);
        this.mniExportList = new MenuItem(this.popupMenu, SWT.NONE);
        this.mniExportList.addListener(SWT.Selection, this.onExportList);

        this.badLst = new Table(this.splitterLand,
                SWT.MULTI    |
                SWT.H_SCROLL |
                SWT.V_SCROLL |
                SWT.VIRTUAL  |
                SWT.FULL_SELECTION);
        this.badLst.setLinesVisible(true);
        this.badLst.setHeaderVisible(true);
        this.badLst.pack();
        this.badLst.addListener(SWT.SetData  , this.onSetData);
        this.badLst.addListener(SWT.Selection, this.onSelected);
        this.badLst.addListener(SWT.KeyUp    , this.onBadLstKey);
        this.badLst.addMouseListener(          this.onFileOpen);
        this.badLst.setMenu(this.popupMenu);
        this.badLst.getMenu().addMenuListener(this.onPopupMenu);

        this.dragSource = new DragSource(this.badLst, DND.DROP_MOVE | DND.DROP_COPY);
        Transfer[] ttypes = new Transfer[] { FileTransfer.getInstance() };
        this.dragSource.setTransfer(ttypes);
        this.dragSource.addDragListener(this.onDrag);

        int splitterPercent = GUIProps.SPLITTER_PERCENT.get();
        this.splitter.initialize(this.img, this.badLst, splitterPercent);

        this.info = new Label(this.shell, SWT.BORDER);
        this.info.setBackground(this.display.getSystemColor(SWT.COLOR_DARK_GRAY));
        this.info.setForeground(this.display.getSystemColor(SWT.COLOR_WHITE));
        this.info.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

        this.splitterLand.setLayoutData(new GridData(
                GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL |
                GridData.VERTICAL_ALIGN_FILL   | GridData.GRAB_VERTICAL));

        this.drop = new DropTarget(this.shell, DND.DROP_MOVE | DND.DROP_DEFAULT);
        this.drop.setTransfer(new Transfer[] { FileTransfer.getInstance() });
        this.drop.addDropListener(this.onDrop);

        this.shell.layout(true, true);

        int w = GUIProps.COL_FILE_WIDTH.get();
        this.colFile = new TableColumn(this.badLst, SWT.NONE);
        this.colFile.setWidth(-1 == w ? this.badLst.getClientArea().width >> 1 : w);
        this.colFile.addListener(SWT.Selection, this.onSortByFile);

        w = -1 == w ? this.colFile.getWidth() : GUIProps.COL_REASON_WIDTH.get();
        this.colReason = new TableColumn(this.badLst, SWT.NONE);
        this.colReason.setWidth(w);
        this.colReason.addListener(SWT.Selection, this.onSortByReason);

        NLS.Reg.instance().addListener(this);
        onLoaded();

        this.shell.addShellListener(new ShellListener() {
            public void shellActivated  (ShellEvent e) { }
            public void shellDeactivated(ShellEvent e) { }
            public void shellDeiconified(ShellEvent e) { }
            public void shellIconified  (ShellEvent e) { }
            public void shellClosed(ShellEvent e) {
                GUI.this.storeProperties();
            }
        });

        this.shell.addListener(SWT.Close, new Safe.Listener() {
            protected void unsafeHandleEvent(Event evt) {
                evt.doit = !GUI.this.scanning;
            }
        });

        this.shell.addListener(SWT.Close, new Safe.Listener() {
            @Override
            protected void unsafeHandleEvent(Event evt) {
                if (GUI.this.scanning) {
                    scheduleStop(true);
                    evt.doit = false;
                }
            }
        });
    }

    void exit() {
        GUI.this.writeConfigurationFile();
        for (File fl : GUI.this.manualFiles.values()) {
            fl.delete();
        }
    }

    @Override
    public void onLoaded() {
        this.mniFile          .setText(NLS.GUI_MN_FILE               .s());
        this.mniScan          .setText(NLS.GUI_MN_FILE_SCAN          .s());
        this.mniExit          .setText(NLS.GUI_MN_FILE_EXIT          .s());
        this.mniOpts          .setText(NLS.GUI_MN_OPTS               .s());
        this.mniIncSubFolders .setText(NLS.GUI_MN_OPTS_INCSUBFOLDERS .s());
        this.mniDifferentiate .setText(NLS.GUI_MN_OPTS_DIFFERENTIATE .s());
        this.mniUseAllCPUCores.setText(NLS.GUI_MN_OPTS_USEALLCPUCORES.s());
        this.mniFileExts      .setText(NLS.GUI_MN_OPTS_FILEEXTS      .s());
        this.mniLanguage      .setText(NLS.GUI_MN_OPTS_LANGUAGE      .s());
        this.mniHelp          .setText(NLS.GUI_MN_HELP               .s());
        this.mniDocumentation .setText(NLS.GUI_MN_HELP_DOCUMENTATION .s());
        this.mniWebsite       .setText(NLS.GUI_MN_HELP_WEBSITE       .s());
        this.mniAbout         .setText(NLS.GUI_MN_HELP_ABOUT         .s());
        this.mniDelete        .setText(NLS.GUI_PMN_DELETE            .s());
        this.mniMove          .setText(NLS.GUI_PMN_MOVE              .s());
        this.mniClear         .setText(NLS.GUI_PMN_CLEAR             .s());
        this.mniSelectAll     .setText(NLS.GUI_PMN_SELECTALL         .s());
        this.mniExportList    .setText(NLS.GUI_PMN_EXPORTLIST        .s());
        this.info             .setText(NLS.GUI_MSG_WELCOME           .s());
        this.colFile          .setText(NLS.GUI_COL_FILE              .s());
        this.colReason        .setText(NLS.GUI_COL_REASON            .s());
    }

    public void run() {
        if (!ImageScanner.selfTest()) {
            MessageBox2.standard(GUI.this.shell,
                SWT.ICON_ERROR | SWT.OK,
                NLS.GUI_STARTUP_NOSCANNER.s(),
                NLS.GUI_STARTUP_ERROR.s());
            return;
        }
        this.shell.open();
        while (!this.shell.isDisposed()) {
            try {
                if (!this.display.readAndDispatch()) {
                    this.display.sleep();
                }
            }
            catch (Throwable err) {
                SWTUtil.msgboxError(err, this.shell);
            }
        }
        this.shell.dispose();
        this.display.dispose();
        System.exit(0);
    }

    void writeConfigurationFile() {
        FileOutputStream faos = null;
        try {
            faos = new FileOutputStream(_propertiesFile);
            Prp.global().store(faos, "");
        }
        catch (IOException ioe) {
            MessageBox2.standard(GUI.this.shell,
                SWT.ICON_WARNING | SWT.OK,
                NLS.GUI_MSG_CFGSAVEERR_2.fmt(_propertiesFile.getAbsolutePath(), ioe.getMessage()),
                NLS.GUI_DLG_GENERIC_WARNING.s());
        }
        finally {
            if (null != faos) try { faos.close(); } catch (IOException ignored) { }
        }
    }

    void storeProperties() {
        int splitterPercent = this.splitter.percentage();
        GUIProps.SPLITTER_PERCENT   .set(Prp.global(), splitterPercent);
        GUIProps.COL_FILE_WIDTH     .set(Prp.global(), this.colFile  .getWidth());
        GUIProps.COL_REASON_WIDTH   .set(Prp.global(), this.colReason.getWidth());
        GUIProps.OPTS_INCSUBFOLDERS .set(Prp.global(), this.mniIncSubFolders .getSelection());
        GUIProps.OPTS_DIFFERENTIATE .set(Prp.global(), this.mniDifferentiate .getSelection());
        GUIProps.OPTS_USEALLCPUCORES.set(Prp.global(), this.mniUseAllCPUCores.getSelection());
    }

    void setProgramIcon() {
        this.shell.setImages(new Image[] {
            new Image(this.display, getClass().getResourceAsStream("resources/icon48x48.png")),
            new Image(this.display, getClass().getResourceAsStream("resources/icon256x256.png"))
        });
    }

    public static void main(String[] args) {
        try {
            Logger rootLog = Logger.getLogger("");
            for (Handler h : rootLog.getHandlers()) {
                rootLog.removeHandler(h);
            }
            GUI gui = new GUI();
            gui.run();
        }
        catch (Throwable err) {
            MiscUtils.dumpUncaughtError(err, PROPERTIES);
        }
    }

    ///////////////////////////////////////////////////////////////////////////

    Listener onExit = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            GUI.this.shell.close();
        }
    };

    Listener onAbout = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            About dlg = new About(GUI.this.shell, Prp.global(),
                    NLS.GUI_ABOUT_CAPTION.s(),
                    String.format(NLS.GUI_ABOUT_PRODUCT_1.s(), VERSION),
                    NLS.GUI_ABOUT_INTRO.s(),
                    String.format(NLS.GUI_ABOUT_COPYRIGHT_1.s(),
                        MiscUtils.copyrightYear(2005, Calendar.getInstance())),
                    GUI.class.getResourceAsStream("resources/icon48x48.png"),
                    GUI.this.display.getSystemColor(SWT.COLOR_DARK_GRAY));
                dlg.open();
        }
    };

    Listener onDocumentation = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            String name = String.format("badpeggy_%s.html",
                coderslagoon.baselib.util.NLS.Reg.instance().id().toUpperCase());
            File fl = SWTUtil.openFileFromResource(
                getClass(),
                "resources/" + name,
                name,
                GUI.this.manualFiles.containsKey(name));
            if (null != fl) {
                GUI.this.manualFiles.put(name, fl);
            }
        }
    };

    Listener onWebsite = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            try {
                String pname = PRODUCT_NAME.toLowerCase().replace(" ", "");
                String url = PRODUCT_SITE + 
                    "?version=" + URLEncoder.encode(VERSION, "UTF-8") +
                    "&name=" + pname +
                    "&lang=" + coderslagoon.baselib.util.NLS.Reg.instance().id().toLowerCase() +
                    "#" + pname;
                if (Program.launch(url)) {
                    return;
                }
            }
            catch (Throwable ignored) {
            }
            MessageBox2.standard(GUI.this.shell,
                    SWT.ICON_WARNING | SWT.OK,
                    NLS.GUI_MSGBOX_WEBSITE_1.fmt(PRODUCT_SITE),
                    PRODUCT_NAME);
        }
    };

    Listener onDifferentiate = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            GUI.this.badLst.clearAll();
        }
    };

    Listener onFileExtensions = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            final Shell dlg = new Shell(GUI.this.shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);

            dlg.setText(NLS.GUI_DLG_FILEEXTS_CAPTION.s());
            FormLayout flo = new FormLayout ();
            flo.marginWidth  =
            flo.marginHeight =
            flo.spacing      = DLG_GAP;
            dlg.setLayout(flo);

            Label lbl = new Label(dlg, SWT.NONE);
            lbl.setText(NLS.GUI_DLG_FILEEXTS_LABEL.s());
            FormData fd = new FormData();
            lbl.setLayoutData(fd);

            Button btn = new Button(dlg, SWT.PUSH);
            btn.setText(NLS.GUI_DLG_FILEEXTS_BTN_CANCEL.s());
            fd = new FormData();
            fd.right  = new FormAttachment(100, 0);
            fd.bottom = new FormAttachment(100, 0);
            btn.setLayoutData(fd);
            btn.addSelectionListener (new SelectionAdapter () {
                public void widgetSelected (SelectionEvent sevt) {
                    dlg.close();
                }
            });

            final Text txt = new Text(dlg, SWT.BORDER);
            fd = new FormData();
            fd.left   = new FormAttachment(lbl, 0, SWT.DEFAULT);
            fd.right  = new FormAttachment(100, 0);
            fd.top    = new FormAttachment(lbl, 0, SWT.CENTER);
            fd.bottom = new FormAttachment(btn, 0, SWT.DEFAULT);
            txt.setLayoutData(fd);
            txt.setText(GUIProps.OPTS_FILEEXTS.get());

            Button btn2 = new Button(dlg, SWT.PUSH);
            btn2.setText(NLS.GUI_DLG_FILEEXTS_BTN_OK.s());
            fd = new FormData();
            fd.right  = new FormAttachment(btn, 0, SWT.DEFAULT);
            fd.bottom = new FormAttachment(100, 0);
            btn2.setLayoutData(fd);
            btn2.addSelectionListener (new SelectionAdapter () {
                public void widgetSelected (SelectionEvent e) {
                    GUIProps.OPTS_FILEEXTS.set(Prp.global(),
                            MiscUtils.csvSave(
                            MiscUtils.csvLoad(txt.getText(), true)));
                    dlg.close();
                }
            });

            dlg.setDefaultButton(btn2);
            dlg.pack();
            dlg.layout();

            new ShellProps(dlg, GUIProps.GUI_DLG_FILEEXTS_PFX,
                           Prp.global(), null, false);

            dlg.open();
        }
    };

    final static String DEFAULT_LANG_ID = "de";
    final static String[][] LANGS = new String[][] {
        { DEFAULT_LANG_ID, "Deutsch" },
        { "en"           , "English" }
    };

    Listener onLanguage = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            LangDialog ldlg = new LangDialog(GUI.this.shell, Prp.global(), LANGS, PRODUCT_NAME, false);
            ldlg.open();
            ldlg.waitForClose();
            String newLangID = ldlg.id();
            if (null != newLangID) {
                clear(false);
                GUIProps.SET_LANG.set(Prp.global(), newLangID);
            }
        }
    };

    Listener onScan = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            if (GUI.this.scanning) {
                scheduleStop(false);
            }
            else {
                DirectoryDialog ddlg = new DirectoryDialog(GUI.this.shell);
                ddlg.setMessage(NLS.GUI_SCAN_MESSAGE.s());
                ddlg.setText(NLS.GUI_SCAN_TEXT.s());
                ddlg.setFilterPath(GUIProps.SET_LASTFOLDER.get());
                String path = ddlg.open();
                if (null == path) {
                    return;
                }
                GUIProps.SET_LASTFOLDER.set(Prp.global(), path);
                scan(new String[] { path });
                reset();
            }
        }
    };

    void scheduleStop(boolean close) {
        if (this.scanning) {
            if (SWT.YES == MessageBox2.standard(
                GUI.this.shell,
                SWT.ICON_QUESTION | SWT.YES | SWT.NO,
                NLS.GUI_DLG_ABORT_MSG.s(),
                NLS.GUI_DLG_GENERIC_CONFIRM.s())) {
                GUI.this.esc.set(true);
                if (close) {
                    GUI.this.close.set(true);
                }
            }
        }
    }

    static String normalizeMessage(String  msg) {
        return msg.replaceAll("\\ 0x[a-fA-F0-9]+" , "")
                  .replaceAll("\\ [0-9]+\\.[0-9]+", "")
                  .replaceAll("\\ [0-9]+"         , "");
    }

    void differentiate(TableItem ti, String msg) {
        String msg2 = normalizeMessage(msg);
        int v = msg2.toString().hashCode();
        int r =  v & 0x00000ff;
        int g = (v & 0x000ff00) >>  8;
        int b = (v & 0x0ff0000) >> 16;
        int f = (r + g*2 + b) > (127 * 4) ? 0 : 255;
        int c = (int)(r * .299 + g * .587 + b * .114);
        ti.setBackground(new Color(GUI.this.display, c, c, c));
        ti.setForeground(new Color(GUI.this.display, f, f, f));
    }

    Listener onSetData = new Safe.Listener() {
        protected void unsafeHandleEvent(Event e) {
            TableItem ti = (TableItem)e.item;
            int idx = GUI.this.badLst.indexOf(ti);
            ImageScanner.Result res = GUI.this.results.get(idx);
            ti.setData(res);
            ti.setText(0, res.tag.toString());
            String msg0 = res.messages().iterator().next();
            ti.setText(1, msg0);
            if (GUI.this.mniDifferentiate.getSelection()) {
                differentiate(ti, msg0);
            }
        }
    };

    Listener onSelected = new Safe.Listener() {
        protected void unsafeHandleEvent(Event e) {
            if (GUI.this.draggingOut) {
                return;
            }
            if (GUI.this.imgViewer.active()) {
                if (null == e.item)
                    return;
                TableItem ti = (TableItem)e.item;
                ImageScanner.Result res = (ImageScanner.Result)ti.getData();
                File fl = new File(res.tag.toString());
                if (fl.exists() &&
                    fl.canRead()) {
                    GUI.this.imgViewer.setFile(fl);
                }
                else {
                    GUI.this.imgViewer.setFile(null);
                }
            }
        }
    };

    MouseListener onFileOpen = new MouseListener() {
        public void mouseDoubleClick(MouseEvent me) {
            TableItem ti = GUI.this.badLst.getItem(new Point(me.x, me.y));
            if (null == ti) {
                return;
            }
            ImageScanner.Result res = (ImageScanner.Result)ti.getData();
            SWTUtil.shellExecute(res.tag.toString());
        }
        public void mouseDown(MouseEvent ignored) { }
        public void mouseUp  (MouseEvent ignored) { }
    };


    MenuAdapter onPopupMenu = new MenuAdapter() {
        public void menuShown(MenuEvent me) {
            boolean enable = 0 < GUI.this.badLst.getSelectionCount();
            for (int i : new int[] { 0, 1, 6 }) {
                GUI.this.popupMenu.getItem(i).setEnabled(enable);
            }
            enable = 0 < GUI.this.results.size();
            for (int i : new int[] { 3, 4 }) {
                GUI.this.popupMenu.getItem(i).setEnabled(enable);
            }
        }
    };

    Listener onBadLstKey = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            if ((evt.stateMask & SWT.CONTROL) == SWT.CONTROL &&
                 evt.keyCode == 'a') {
                GUI.this.badLst.selectAll();
                return;
            }
            else if (evt.keyCode == SWT.DEL) {
                int c = GUI.this.badLst.getSelectionCount();
                if (0 == c) {
                    return;
                }
                if (SWT.YES == MessageBox2.standard(GUI.this.shell,
                    SWT.ICON_QUESTION | SWT.YES | SWT.NO,
                    NLS.GUI_DLG_REM_MSG_1.fmt(c),
                    NLS.GUI_DLG_GENERIC_CONFIRM.s())) {
                    int[] idxs = GUI.this.badLst.getSelectionIndices();
                    Arrays.sort(idxs);
                    int ofs = 0;
                    for (int idx : idxs) {
                        GUI.this.results.remove(idx - ofs);
                        ofs++;
                    }
                    reset();
                }
            }
            return;
        }
    };

    Listener onDelete = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            int c = GUI.this.badLst.getSelectionCount();
            if (0 == c) {
                return;
            }
            if (SWT.YES != MessageBox2.standard(GUI.this.shell,
                SWT.ICON_WARNING | SWT.YES | SWT.NO,
                NLS.GUI_DLG_DELETE_MSG_1.fmt(c),
                NLS.GUI_DLG_GENERIC_CONFIRM.s())) {
                return;
            }
            int[] idxs = GUI.this.badLst.getSelectionIndices();
            Arrays.sort(idxs);
            List<ImageScanner.Result> deleted = new ArrayList<>();
            int errors = 0;
            for (int idx : idxs) {
                ImageScanner.Result res = GUI.this.results.get(idx);
                File fl = new File(res.tag.toString());
                if (!fl.delete() && fl.exists()) {
                    errors++;
                }
                else {
                    deleted.add(res);
                }
            }
            if (0 < errors) {
                MessageBox2.standard(GUI.this.shell,
                    SWT.ICON_WARNING | SWT.OK,
                    NLS.GUI_DLG_DELETE_NODELS_1.fmt(errors),
                    NLS.GUI_DLG_GENERIC_WARNING.s());
            }
            finalizeRemoval(deleted);
        }
    };

    Listener onMove = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            int c = GUI.this.badLst.getSelectionCount();
            if (0 == c) {
                return;
            }
            DirectoryDialog ddlg = new DirectoryDialog(GUI.this.shell);
            ddlg.setMessage(NLS.GUI_DLG_MOVE_DEST_MSG_1.fmt(c));
            ddlg.setText(NLS.GUI_DLG_MOVE_DEST_TEXT.s());
            ddlg.setFilterPath(GUIProps.SET_LASTMOVEDEST.get());
            String path = ddlg.open();
            if (null == path) {
                return;
            }
            File dst = new File(path);
            GUIProps.SET_LASTMOVEDEST.set(Prp.global(), dst.getAbsolutePath());
            int[] idxs = GUI.this.badLst.getSelectionIndices();
            Arrays.sort(idxs);
            List<ImageScanner.Result> moved = new ArrayList<>();
            int errors = 0;
            for (int idx : idxs) {
                ImageScanner.Result res = GUI.this.results.get(idx);
                File fl = new File(res.tag.toString());
                if (!fl.renameTo(new File(dst, fl.getName()))) {
                    errors++;
                }
                else {
                    moved.add(res);
                }
            }
            if (0 < errors) {
                MessageBox2.standard(GUI.this.shell,
                    SWT.ICON_WARNING | SWT.OK,
                    NLS.GUI_DLG_MOVE_NOMOVES_1.fmt(errors),
                    NLS.GUI_DLG_GENERIC_WARNING.s());
            }
            finalizeRemoval(moved);
        }
    };

    Listener onClear = new Safe.Listener() {
        @Override
        protected void unsafeHandleEvent(Event event) {
            int c = GUI.this.badLst.getItemCount();
            if (0 == c) {
                return;
            }
            if (SWT.YES == MessageBox2.standard(GUI.this.shell,
                SWT.ICON_QUESTION | SWT.YES | SWT.NO,
                NLS.GUI_DLG_REM_MSG_1.fmt(c),
                NLS.GUI_DLG_GENERIC_CONFIRM.s())) {
                clear(true);
            }
        }
    };
    void clear(boolean clearInfo) {
        if (clearInfo) {
            GUI.this.info.setText("");
        }
        GUI.this.results.clear();
        reset();
    }

    Listener onSelectAll = new Safe.Listener() {
        @Override
        protected void unsafeHandleEvent(Event event) {
            GUI.this.badLst.selectAll();
        }
    };

    Listener onExportList = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            FileDialog fd = new FileDialog(GUI.this.shell, SWT.SAVE);
            File last = new File(GUIProps.SET_LASTEXPORTFILE.get()).getAbsoluteFile();
            fd.setFilterNames     (new String[] { NLS.GUI_DLG_EXPLST_FILTERNAMES.s() });
            fd.setFilterExtensions(new String[] { NLS.GUI_DLG_EXPLST_FILTEREXTS.s() });
            fd.setFilterPath      (last.getParentFile().getAbsolutePath());
            fd.setFileName        (last.getName());
            fd.setText            (NLS.GUI_DLG_EXPLST_TEXT.s());
            String fname = fd.open();
            if (null == fname) {
                return;
            }
            GUIProps.SET_LASTEXPORTFILE.set(Prp.global(), fname);
            PrintStream ps = null;
            try {
                ps = new PrintStream(
                     new BufferedOutputStream(
                     new FileOutputStream(fname)));

                int[] idxs = GUI.this.badLst.getSelectionIndices();
                for (int idx : idxs) {
                    fname = GUI.this.results.get(idx).tag.toString();
                    System.out.println(fname);
                    ps.println(fname);
                }
            }
            catch (IOException ioe) {
                MessageBox2.standard(GUI.this.shell,
                    SWT.ICON_ERROR | SWT.OK,
                    NLS.GUI_DLG_EXPLST_ERROR_1.fmt(ioe.getLocalizedMessage()),
                    NLS.GUI_DLG_GENERIC_WARNING.s());
            }
            finally {
                if (null != ps) {
                    ps.flush();
                    ps.close();
                }
            }
        }
    };

    MouseListener onImageClick = new MouseListener() {
        public void mouseDoubleClick(MouseEvent me) {
            boolean active = GUIProps.OPTS_IMGVIEWACTIVE.get();
            GUI.this.imgViewer.setActive(active = !active);
            GUIProps.OPTS_IMGVIEWACTIVE.set(Prp.global(), active);
        }
        public void mouseDown(MouseEvent ignored) { }
        public void mouseUp  (MouseEvent ignored) { }
    };

    Listener onSortByFile = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            Collections.sort(GUI.this.results,
                new Comparator<ImageScanner.Result>() {
                    public int compare(Result o1, Result o2) {
                        return o1.tag.toString().compareToIgnoreCase(
                               o2.tag.toString());
                    }
                });
            GUI.this.reset();
        }
    };

    Listener onSortByReason = new Safe.Listener() {
        protected void unsafeHandleEvent(Event evt) {
            Collections.sort(GUI.this.results,
                new Comparator<ImageScanner.Result>() {
                    public int compare(Result o1, Result o2) {
                        return o1.messages().iterator().next().compareToIgnoreCase(
                               o2.messages().iterator().next());
                    }
                });
            GUI.this.reset();
        }
    };

    DropTargetAdapter onDrop = new DropTargetAdapter() {
        public void drop(DropTargetEvent evt) {
            FileTransfer ft = FileTransfer.getInstance();
            if (!ft.isSupportedType(evt.currentDataType)) {
                return;
            }
            // TODO: kind of a hack, there must be a better way...
            GUI.this.scanning = true;
            final String[] items = (String[])evt.data;
            Thread thrd = new Thread(new Runnable() {
                public void run() {
                    GUI.this.display.syncExec(new Runnable() {
                        public void run() {
                            scan(items);
                        }
                    });
                }
            });
            thrd.start();
        }
        public void dragOver(DropTargetEvent evt) {
            if (GUI.this.scanning ||
                GUI.this.draggingOut) {
                evt.detail = DND.DROP_NONE;
            }
        }
    };

    DragSourceListener onDrag = new DragSourceListener() {
        @Override
        public void dragStart(DragSourceEvent event) {
            if (0 < GUI.this.badLst.getSelectionCount()) {
                event.doit = true;
                GUI.this.draggingOut = true;
            }
        }
        @Override
        public void dragSetData(DragSourceEvent event) {
            if (FileTransfer.getInstance().isSupportedType(event.dataType)) {
                int[] idxs = GUI.this.badLst.getSelectionIndices();
                String[] flst = new String[idxs.length];
                for (int i = 0; i < idxs.length; i++) {
                    flst[i] = GUI.this.results.get(idxs[i]).tag.toString();
                }
                event.data = flst;
            }
        }
        @Override
        public void dragFinished(DragSourceEvent event) {
              GUI.this.draggingOut = false;
              if (event.detail == DND.DROP_MOVE) {
                  // TODO: remove the moved items? maybe not, can't tell ...
              }
        }
    };

    ///////////////////////////////////////////////////////////////////////////

    void reset() {
        this.imgViewer.setFile(null);
        this.badLst.setItemCount(GUI.this.results.size());
        this.badLst.deselectAll();
        this.badLst.clearAll();
    }

    void finalizeRemoval(List<ImageScanner.Result> todel) {
        for (ImageScanner.Result res : todel) {
            this.results.remove(res);
            File viewedFile = this.imgViewer.file();
            if (null != viewedFile &&
                res.tag.equals(viewedFile.getAbsolutePath())) {
                this.imgViewer.setFile(null);
            }
        }
        reset();
    }

    ///////////////////////////////////////////////////////////////////////////

    void handleFatalError(Throwable err) {
        if (this.fatalErr) {
            return;
        }
        this.fatalErr = true;
        Log.exception(Log.Level.FATAL, GUI.class.getName(), err);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        err.printStackTrace(ps);
        ps.flush();
        StringBuilder sb = new StringBuilder();
        sb.append(err.getClass().getName() + '\n' + err.getMessage() + '\n');
        sb.append(baos.toString());
        MessageBox2.standard(GUI.this.shell,
            SWT.ICON_ERROR | SWT.OK,
            sb.toString(),
            NLS.GUI_DLG_GENERIC_UNRECERR.s());
        System.exit(EXITCODE_UNRECERR);
    }

    ///////////////////////////////////////////////////////////////////////////

    void endExec() {
        if (null == this.exec) {
            return;
        }
        try {
            this.exec.shutdown();
            while (this.activeScanRuns > 0) {
                if (!this.display.readAndDispatch()) {
                    this.display.sleep();
                }
            }
            this.exec.awaitTermination(1, TimeUnit.DAYS);
        }
        catch (InterruptedException ignored) {
        }
        this.exec = null;
    }

    void scan(String[] items) {
        this.scanning = true;
        enableControls(false);
        this.activeScanRuns = 0;
        this.maxActiveScanRuns = this.mniUseAllCPUCores.getSelection() ?
                Runtime.getRuntime().availableProcessors() : 1;
        this.exec = Executors.newFixedThreadPool(this.maxActiveScanRuns);
        try {
            scan1(items);
        }
        catch (Throwable err) {
            handleFatalError(err);
        }
        finally {
            endExec();
            this.scanning = false;
            if (this.close.get()) {
                this.shell.close();
                return;
            }
            this.shell.setText(PRODUCT_NAME);
            this.lastPercentage = 0.0;
            enableControls(true);
        }
    }

    void scan1(String[] items) {
        this.esc.set(false);
        this.fatalErr = false;
        this.info.setText(NLS.GUI_MSG_PRESSESC.s());
        this.shell.setText(String.format("%s - %s", PRODUCT_NAME,
                NLS.GUI_CAPTION_SEARCHING.s()));
        FileRegistrar freg = new FileRegistrar.InMemory(new DefCmp(true));
        final String[] exts = MiscUtils.csvLoad(GUIProps.OPTS_FILEEXTS.get(), true);
        for (int i = 0; i < exts.length; i++) {
            exts[i] = "." + exts[i].toLowerCase();
        }
        this.searchFilter = new FileSystem.Filter() {
            public boolean matches(FileNode fn) {
                if (fn.hasAttributes(FileNode.ATTR_DIRECTORY)) {
                    return true;
                }
                final String nm = fn.name().toLowerCase();
                for(String ext : exts) {
                    if (nm.endsWith(ext)) {
                        return true;
                    }
                }
                return false;
            }
        };
        FileSystem fs = new LocalFileSystem(false);
        this.numOfFiles = 0;
        try {
            FileRegistrar.Callback frcb = new FileRegistrar.Callback() {
                public Merge onMerge(FileNode[] nd0, FileNode nd1) {
                    return Merge.IGNORE;
                }
            };
            List<FileNode> files = new ArrayList<>();
            for (String item : items) {
                FileNode fn = fs.nodeFromString(item);
                if (fn.hasAttributes(FileNode.ATTR_DIRECTORY)) {
                    boolean recursive = GUI.this.mniIncSubFolders.getSelection();
                    this.numOfFiles += search(freg, fs, fn, fn, recursive);
                }
                else {
                    files.clear();
                    files.add(fn);
                    freg.add(files, fn, freg.root(), frcb);
                    this.numOfFiles++;
                }
            }
        }
        catch (IOException ioe) {
            if (this.esc.get()) {
                this.info.setText(NLS.GUI_MSG_ABORTED_SEARCH.s());
            }
            else {
                this.info.setText(NLS.GUI_MSG_FAILED_SEARCH.s());
                MessageBox2.standard(GUI.this.shell,
                    SWT.ICON_ERROR | SWT.OK,
                    NLS.GUI_MSG_SEARCH_FAILED_1.fmt(ioe.getMessage()),
                    NLS.GUI_DLG_GENERIC_WARNING.s());
            }
            return;
        }
        //FileRegistrar.dump(freg.root(), 0, System.out);
        this.scanned    = 0;
        this.unreadable = 0;
        this.damaged    = 0;
        boolean done = scanDirectory(freg.root());
        endExec();
        this.info.setText((done ?
            NLS.GUI_MSG_DONE        .s() :
            NLS.GUI_MSG_ABORTED_SCAN.s()) + ' ' +
            NLS.GUI_MSG_RESULT_3.fmt(this.scanned, this.damaged, this.unreadable));
    }

    boolean scanDirectory(FileRegistrar.Directory dir) {
        Iterator<FileNode> itf = dir.files();
        while (itf.hasNext()) {
            if (this.esc.get()) {
                return false;
            }
            executeScan(new ScanRun(itf.next()));
        }
        Iterator<FileRegistrar.Directory> itd = dir.dirs();
        while (itd.hasNext()) {
            if (this.esc.get() || !scanDirectory(itd.next())) {
                return false;
            }
        }
        return true;
    }

    void executeScan(ScanRun srun) {
        while (this.activeScanRuns >= this.maxActiveScanRuns) {
            if (!this.display.readAndDispatch()) {
                this.display.sleep();
            }
        }
        this.activeScanRuns++;
        this.exec.execute(srun);
    }

    ///////////////////////////////////////////////////////////////////////////

    void updateProgress(final String fpath) {
        this.display.syncExec(new Runnable() {
            public void run() {
                GUI.this.info.setText(fpath);
                GUI.this.scanned++;
                double prct = (GUI.this.scanned * 100.0) /
                               GUI.this.numOfFiles;
                if (GUI.this.lastPercentage < prct) {
                    GUI.this.shell.setText(String.format(
                            "%s - %.1f%%", PRODUCT_NAME,
                            GUI.this.lastPercentage = prct));
                }
            }
        });
    }

    void updateResult(final String fpath, final ImageScanner.Result res,
                      final boolean aborted) {
        this.display.syncExec(new Runnable() {
            public void run() {
                switch(res.type()) {
                    case OK: {
                        break;
                    }
                    case WARNING:
                    case ERROR: {
                        res.tag = fpath;
                        GUI.this.damaged++;
                        if (addResult(res)) {
                            GUI.this.badLst.setItemCount(GUI.this.results.size());
                        }
                        break;
                    }
                    case UNEXPECTED_ERROR: {
                        GUI.this.unreadable += aborted ? 0 : 1;
                        break;
                    }
                    default: {
                        throw new Error();
                    }
                }
            }
        });
    }

    void updateError(final Throwable err) {
        this.display.syncExec(new Runnable() {
            public void run() {
                if (err instanceof IOException) {
                    GUI.this.unreadable++;
                }
                else {
                    Log.exception(Log.Level.FATAL, GUI.class.getName(), err);
                }
            }
        });
    }

    void updateFatal(final Throwable err) {
        Log.exception(Log.Level.FATAL, GUI.class.getName(), err);
        this.display.syncExec(new Runnable() {
            public void run() {
                handleFatalError(err);
            }
        });
    }

    class ScanRun implements Runnable {
        final FileNode fnode;
        public ScanRun(FileNode fnode) {
            this.fnode = fnode;
        }
        public void run() {
            try {
                run2();
            }
            catch (Throwable uncaught) {
                updateFatal(uncaught);
            }
        }
        public void run2() {
            Thread.currentThread().setPriority(Thread.MIN_PRIORITY);
            InputStream ins = null;
            try {
                GUI.this.updateProgress(this.fnode.path(true));
                ins = this.fnode.fileSystem().openRead(this.fnode);
                ImageScanner scanner = new ImageScanner();
                final VarRef<Boolean> aborted = new VarRef<>(false);
                InputStream bins = new BufferedInputStream(ins, IO_BUF_SZ);
                if (null == scanner.scan(bins,
                    ImageFormat.fromFileName(this.fnode.name()),
                    new ImageScanner.Callback() {
                        public boolean onProgress(double percent) {
                            return !(aborted.v = GUI.this.esc.get());
                        }
                    })) {
                    throw new Exception("missing JPEG reader");
                }
                GUI.this.updateResult(this.fnode.path(true), scanner.lastResult(), aborted.v);
            }
            catch (Throwable err) {
                GUI.this.updateError(err);
            }
            finally {
                if (null != ins) {
                    try { ins.close(); } catch (IOException ignored) { }
                }
            }
            GUI.this.display.syncExec(new Runnable() {
                public void run() {
                    GUI.this.activeScanRuns--;
                }
            });
        }
    }

    int search(FileRegistrar freg, FileSystem fs, FileNode dir, FileNode bottom,
               boolean recursive) throws IOException {
        this.info.setText(NLS.GUI_MSG_SEARCHING_1.fmt(dir.name()));
        while (this.display.readAndDispatch()) {
            if (this.esc.get()) {
                throw new IOException();
            }
        }
        Iterator<FileNode> ifn = fs.list(dir, this.searchFilter);
        List<FileNode> files = new ArrayList<>();
        int result = 0;
        while (ifn.hasNext()) {
            FileNode fn = ifn.next();
            if (fn.hasAttributes(FileNode.ATTR_DIRECTORY)) {
                if (recursive) {
                    result += search(freg, fs, fn, bottom, recursive);
                }
            }
            else {
                files.add(fn);
                result++;
            }
        }
        freg.add(files, bottom, null, new FileRegistrar.Callback() {
            public Merge onMerge(FileNode[] nd0, FileNode nd1) {
                return Merge.IGNORE;
            }
        });
        return result;
    }

    boolean addResult(ImageScanner.Result res) {
        final Object tag = res.tag;
        for (ImageScanner.Result res2 : this.results) {
            if (res2.tag.equals(tag)) {
                return false;
            }
        }
        this.results.add(res);
        return true;
    }

    public void enableControls(boolean flag) {
        this.mniScan.setText((flag ? NLS.GUI_MN_FILE_SCAN :
                                     NLS.GUI_MN_FILE_SCANABORT).s());
        this.mniExit          .setEnabled(flag);
        this.mniIncSubFolders .setEnabled(flag);
        this.mniUseAllCPUCores.setEnabled(flag);
        this.mniFileExts      .setEnabled(flag);
        this.mniLanguage      .setEnabled(flag);
    }
}

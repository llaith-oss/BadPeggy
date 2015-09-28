package coderslagoon.badpeggy;

import coderslagoon.badpeggy.scanner.ImageFormat;
import coderslagoon.baselib.util.Prp;

public class GUIProps {
    public final static String GUI_PFX              = "gui.";
    public final static String GUI_DLG_FILEEXTS_PFX = GUI_PFX + "fileexts.";
    public final static String GUI_STATUSBAR_PFX    = GUI_PFX + "statusbar.";
    public final static String GUI_COL_PFX          = GUI_PFX + "col.";
    public final static String GUI_OPTS_PFX         = GUI_PFX + "opts.";
    public final static String GUI_SET_PFX          = GUI_PFX + "set.";

    public final static Prp.Int  SPLITTER_PERCENT    = new Prp.Int (GUI_PFX      + "splpct"        , 50);
    public final static Prp.Int  COL_FILE_WIDTH      = new Prp.Int (GUI_COL_PFX  + "file.width"    , -1);
    public final static Prp.Int  COL_REASON_WIDTH    = new Prp.Int (GUI_COL_PFX  + "reason.width"  , -1);
    public final static Prp.Bool OPTS_LOWPRIO        = new Prp.Bool(GUI_OPTS_PFX + "lowprio"       , true);
    public final static Prp.Bool OPTS_USEALLCPUCORES = new Prp.Bool(GUI_OPTS_PFX + "useallcpucores", true);
    public final static Prp.Bool OPTS_INCSUBFOLDERS  = new Prp.Bool(GUI_OPTS_PFX + "incsubfolders" , true);
    public final static Prp.Str  OPTS_FILEEXTS       = new Prp.Str (GUI_OPTS_PFX + "fileexts"      , makeFileExtensionList());
    public final static Prp.Bool OPTS_IMGVIEWACTIVE  = new Prp.Bool(GUI_OPTS_PFX + "imgviewactive" , true);
    public final static Prp.Bool OPTS_DIFFERENTIATE  = new Prp.Bool(GUI_OPTS_PFX + "differentiate" , true);
    public final static Prp.Str  SET_LASTFOLDER      = new Prp.Str (GUI_SET_PFX  + "lastfolder"    , ".");
    public final static Prp.Str  SET_LASTMOVEDEST    = new Prp.Str (GUI_SET_PFX  + "lastmovedest"  , ".");
    public final static Prp.Str  SET_LASTEXPORTFILE  = new Prp.Str (GUI_SET_PFX  + "lastexportfile", "badpeggy_list.txt");
    public final static Prp.Str  SET_LANG            = new Prp.Str (GUI_SET_PFX  + "lang", null);

    private static String makeFileExtensionList() {
        String result = "";
        String comma = "";
        for (ImageFormat ifmt : ImageFormat.values()) {
            for (String ext : ifmt.extensions) {
                result += comma + ext;
                comma = ",";
            }
        }
        return result;
    }
}

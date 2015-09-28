package coderslagoon.badpeggy.scanner;

import java.util.HashSet;
import java.util.Set;

public enum ImageFormat {

    GIF ("gif" , new String[] { "gif" } , false, true),
    PNG ("png" , new String[] { "png" } , false, false),
    BMP ("bmp" , new String[] { "bmp" } , false, true),
    JPEG("jpeg", new String[] { "jpg", "jpeg", "jpe", "jfif", "jfi", "jif" }, true, false);


    ImageFormat(String name, String[] exts, boolean lossy, boolean indexed) {
        this.name = name;
        this.lossy = lossy;
        this.indexed = indexed;
        this.defaultExtension = exts[0].toLowerCase();
        this.extensions = new HashSet<>();
        for (String ext: exts) {
            this.extensions.add(ext.toLowerCase());
        }
    }
    public final String name;
    public final boolean lossy;
    public final boolean indexed;
    public final String defaultExtension;
    public final Set<String> extensions;
    public static ImageFormat fromFileName(String fname) {
        int lastDot = fname.lastIndexOf('.');
        if (-1 == lastDot) {
            return null;
        }
        String ext = fname.substring(lastDot + 1).toLowerCase();
        for (ImageFormat ifmt : values()) {
            if (ifmt.extensions.contains(ext)) {
                return ifmt;
            }
        }
        return null;
    }
}

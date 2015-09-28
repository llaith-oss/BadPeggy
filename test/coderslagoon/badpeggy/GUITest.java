package coderslagoon.badpeggy;

import org.junit.Assert;
import org.junit.Test;

import coderslagoon.badpeggy.GUI;

public class GUITest {

    @Test
    public void testNormalizeMessage() {
        Assert.assertEquals("", GUI.normalizeMessage(""));
        Assert.assertEquals("a", GUI.normalizeMessage("a"));
        Assert.assertEquals("", GUI.normalizeMessage(" 1"));
        Assert.assertEquals("", GUI.normalizeMessage(" 123"));
        Assert.assertEquals("a", GUI.normalizeMessage(" 0a"));
        Assert.assertEquals("a", GUI.normalizeMessage("a 9"));
        Assert.assertEquals("0x", GUI.normalizeMessage("0x"));
        Assert.assertEquals("", GUI.normalizeMessage(" 0x1"));
        Assert.assertEquals("1.0", GUI.normalizeMessage("1.0"));
        Assert.assertEquals("", GUI.normalizeMessage(" 21.8899"));
        Assert.assertEquals(" X ", GUI.normalizeMessage("  0x1aX "));
        Assert.assertEquals("this is ", GUI.normalizeMessage("this 1 is 0x18461a "));
        Assert.assertEquals("", GUI.normalizeMessage(" 0x1999 99"));
        Assert.assertEquals("Unsupported JPEG: SOF type", GUI.normalizeMessage("Unsupported JPEG: SOF type 0xcb"));
    }
}

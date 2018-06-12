package com.ggx.editor.options;

import com.ggx.editor.utils.PrefsIntegerProperty;
import com.ggx.editor.utils.PrefsStringProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.text.Font;

import java.util.List;
import java.util.prefs.Preferences;


public class Options
{
	public static final String[] DEF_FONT_FAMILIES = {
		"Consolas",
		"DejaVu Sans Mono",
		"Lucida Sans Typewriter",
		"Lucida Console",
	};

	public static final int DEF_FONT_SIZE = 12;
	public static final int MIN_FONT_SIZE = 8;
	public static final int MAX_FONT_SIZE = 36;


	public static void load(Preferences options) {
		fontFamily.init(options, "fontFamily", null, Options::safeFontFamily);
		fontSize.init(options, "fontSize", DEF_FONT_SIZE);
		encoding.init(options, "encoding", "UTF-8");
		lastFilePathProperty().init(options,"lastFilePath",null);
	}

	/**
	 * 搜索并检查一个在系统中可用的字体
	 */
	private static String safeFontFamily(String fontFamily) {
		List<String> fontFamilies = Font.getFamilies();
		if (fontFamily != null && fontFamilies.contains(fontFamily))
			return fontFamily;

		for (String family : DEF_FONT_FAMILIES) {
			if (fontFamilies.contains(family))
				return family;
		}
		return "Monospaced";
	}

	// 字体属性
	private static final PrefsStringProperty fontFamily = new PrefsStringProperty();
	public static String getFontFamily() { return fontFamily.get(); }
	public static void setFontFamily(String fontFamily) { Options.fontFamily.set(fontFamily); }
	public static StringProperty fontFamilyProperty() { return fontFamily; }

	// 字体大小属性
	private static final PrefsIntegerProperty fontSize = new PrefsIntegerProperty();
	public static int getFontSize() { return fontSize.get(); }
	public static void setFontSize(int fontSize) { Options.fontSize.set(fontSize); }
	public static IntegerProperty fontSizeProperty() { return fontSize; }

	// 编码格式属性
	private static final PrefsStringProperty encoding = new PrefsStringProperty();
	public static String getEncoding() { return encoding.get(); }
	public static void setEncoding(String encoding) { Options.encoding.set(encoding); }
	public static StringProperty encodingProperty() { return encoding; }

	//上一次文件打开位置属性
	private static final PrefsStringProperty lastFilePath=new PrefsStringProperty();
	public static String getLastFilePath() {
		return lastFilePath.get();
	}
	public static PrefsStringProperty lastFilePathProperty() {
		return lastFilePath;
	}
	public static void setLastFilePath(String lastFilePath) {
		Options.lastFilePath.set(lastFilePath);
	}
}

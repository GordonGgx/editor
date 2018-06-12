package com.ggx.editor.utils;

import javafx.beans.property.SimpleIntegerProperty;

import java.util.prefs.Preferences;

/**
 * 数值存储键值对信息
 *
 */
public class PrefsIntegerProperty
	extends SimpleIntegerProperty
{
	public PrefsIntegerProperty() {
	}

	public PrefsIntegerProperty(Preferences prefs, String key, int def) {
		init(prefs, key, def);
	}

	public void init(Preferences prefs, String key, int def) {
		set(prefs.getInt(key, def));
		addListener((ob, o, n) -> {
			Utils.putPrefsInt(prefs, key, get(), def);
		});
	}
}

package com.ggx.editor.utils;

import javafx.beans.property.SimpleStringProperty;

import java.util.function.Function;
import java.util.prefs.Preferences;

/**
 * 字符串存储键值对信息
 *
 */
public class PrefsStringProperty
	extends SimpleStringProperty
{
	public PrefsStringProperty() {
	}

	public PrefsStringProperty(Preferences prefs, String key, String def) {
		init(prefs, key, def);
	}

	public void init(Preferences prefs, String key, String def) {
		init(prefs, key, def, value -> value);
	}

	public void init(Preferences prefs, String key, String def, Function<String, String> loadConverter) {
		set(loadConverter.apply(prefs.get(key, def)));
		addListener((ob, o, n) -> {
			Utils.putPrefs(prefs, key, get(), def);
		});
	}
}

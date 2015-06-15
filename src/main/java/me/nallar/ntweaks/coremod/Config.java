package me.nallar.ntweaks.coremod;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Our own config implementation. Can't use Forge's as this loads before a lot of forge stuff
 */
public class Config {
	final Properties properties;
	private boolean modified = false;
	private final File file;
	private final Set<String> allowedProperties = new HashSet<String>();
	private static final String commentSeparator = "$$";

	public Config(File file) {
		this.file = file;
		properties = new Properties();
		try {
			properties.load(new FileInputStream(file));
		} catch (IOException e) {
			modified = true;
			CoreMod.log.info("Couldn't load properties file", e);
			// File not found/invalid, don't load old properties.
		}
	}

	private String addComment(String value, String comment) {
		if (value.contains(commentSeparator)) {
			throw new IllegalArgumentException("Value must not contain comment separator " + value);
		}
		return value + " " + commentSeparator + comment;
	}

	private String stripComment(String value) {
		if (value.contains(commentSeparator)) {
			String[] parts = value.split(Pattern.quote(commentSeparator));
			return parts[0].trim();
		}
		return value.trim();
	}

	public void add(String name, String description, String def) {
		if (def == null) {
			throw null;
		}
		allowedProperties.add(name);
		String old = properties.getProperty(name);
		def = old == null ? def : stripComment(old);
		String result = addComment(def, description);
		if (!result.equals(old)) {
			modified = true;
			properties.setProperty(name, result);
		}
	}

	public boolean getBool(String name) {
		return Boolean.parseBoolean(getString(name));
	}

	private String getString(String name) {
		if (!allowedProperties.contains(name)) {
			throw new IllegalArgumentException("Property " + name + " does not exist");
		}
		String value = properties.getProperty(name);
		if (value == null) {
			throw null;
		}
		return stripComment(value);
	}

	public void save() {
		if (modified) {
			try {
				properties.store(new FileOutputStream(file), "NTweaks config");
			} catch (IOException e) {
				CoreMod.log.error("Failed to save log file", e);
			}
		}
	}
}

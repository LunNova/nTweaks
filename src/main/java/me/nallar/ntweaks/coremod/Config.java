package me.nallar.ntweaks.coremod;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * Our own config implementation. Can't use Forge's as this loads before a lot of forge stuff
 */
public class Config {
	final Properties properties;
	private final Set<String> allowedProperties = new HashSet<String>();
	private static final String commentSeparator = "$$";

	public Config(File name) {
		properties = new Properties();
		try {
			properties.load(new FileInputStream(name));
		} catch (IOException e) {
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
		properties.setProperty(name, addComment(def, description));
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
}

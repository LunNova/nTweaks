package me.nallar.ntweaks;

import me.nallar.ntweaks.coremod.CoreMod;
import org.apache.logging.log4j.Level;

import java.lang.ref.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public class MemoryLeakDetector {
	private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(1);
	private final long waitTimeSeconds;
	private final Map<Long, LeakCheckEntry> scheduledObjects = new ConcurrentHashMap<Long, LeakCheckEntry>();

	public MemoryLeakDetector(final long waitTimeSeconds) {
		if (waitTimeSeconds < 120) {
			throw new IllegalArgumentException("Wait time unreasonably low, will only get false positives");
		}
		this.waitTimeSeconds = waitTimeSeconds;
	}

	public synchronized void scheduleLeakCheck(Object o, String oDescription_, final boolean clean) {
		try {
			if (clean) {
				// Schedule cleaning of the checked object after a short delay.
				// CleanerTask uses weak reference - ideally the object will already be GCd by the time this runs and no
				// cleaning will be needed.
				scheduledThreadPoolExecutor.schedule(new CleanerTask(o), 40, TimeUnit.SECONDS);
			}
			final long id = System.identityHashCode(o);
			final String oDescription = (oDescription_ == null ? "" : oDescription_ + " : ") + description(o);
			scheduledObjects.put(id, new LeakCheckEntry(o, oDescription));
			scheduledThreadPoolExecutor.schedule(new Runnable() {
				@Override
				public void run() {
					LeakCheckEntry leakCheckEntry = scheduledObjects.remove(id);
					Object o = leakCheckEntry.o.get();
					if (o == null) {
						CoreMod.log.trace("Object likely to be leaked " + leakCheckEntry.description + " has been removed normally.");
					} else {
						CoreMod.log.log(clean ? Level.TRACE : Level.INFO, "Probable memory leak detected. \"" + leakCheckEntry.description + "\" has not been garbage collected after " + waitTimeSeconds + "s.");
					}
				}
			}, waitTimeSeconds, TimeUnit.SECONDS);
		} catch (Throwable t) {
			CoreMod.log.error("Failed to schedule leak check for " + oDescription_, t);
		}
	}

	private static String description(Object o) {
		return String.valueOf(o.getClass()) + '@' + System.identityHashCode(o);
	}

	/**
	 * Sets all non-primitive/array fields of o to null. For use when you know some stupid mod/plugin is going to leak this object,
	 * and want to leak only the size of the object, not everything it references.
	 *
	 * @param o Object to clean.
	 */
	public static void clean(Object o) {
		CoreMod.log.debug("Cleaning object " + description(o));
		Class c = o.getClass();
		while (c != null) {
			for (Field field : c.getDeclaredFields()) {
				if ((!field.getType().isArray() && field.getType().isPrimitive()) || (field.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
					continue;
				}
				try {
					field.setAccessible(true);
					field.set(o, null);
				} catch (IllegalAccessException e) {
					CoreMod.log.warn("Exception cleaning " + o.getClass() + '@' + System.identityHashCode(o), e);
				}
			}
			c = c.getSuperclass();
		}
	}

	private static class CleanerTask extends TimerTask {
		final WeakReference<Object> toClean;

		CleanerTask(final Object toClean) {
			this.toClean = new WeakReference<Object>(toClean);
		}

		@Override
		public void run() {
			Object toClean = this.toClean.get();
			if (toClean != null) {
				clean(toClean);
			}
		}
	}

	private static class LeakCheckEntry {
		public final WeakReference<Object> o;
		public final String description;

		LeakCheckEntry(final Object o, final String description) {
			this.o = new WeakReference<Object>(o);
			this.description = description;
		}
	}
}

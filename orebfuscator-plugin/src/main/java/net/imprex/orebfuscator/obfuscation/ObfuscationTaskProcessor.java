package net.imprex.orebfuscator.obfuscation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.Bukkit;

import net.imprex.orebfuscator.Orebfuscator;

public class ObfuscationTaskProcessor {

	private static final AtomicInteger COUNT = new AtomicInteger(-2500);

	private static final AtomicLong TIME_OLD = new AtomicLong();
	private static final AtomicLong TIME_NEW = new AtomicLong();

	private static final AtomicLong SIZE_OLD = new AtomicLong();
	private static final AtomicLong SIZE_NEW = new AtomicLong();

	private final ObfuscationTaskProcessorOld oldProcessor;
	private final ObfuscationTaskProcessorNew newProcessor;

	public ObfuscationTaskProcessor(Orebfuscator orebfuscator) {
		this.oldProcessor = new ObfuscationTaskProcessorOld(orebfuscator);
		this.newProcessor = new ObfuscationTaskProcessorNew(orebfuscator);
	}

	public void process(ObfuscationTask task) {
		long processTimeNew, processTimeOld;
		int sizeNew, sizeOld;

		long time = System.currentTimeMillis();
		try {
			sizeNew = newProcessor.process(task);
		} finally {
			processTimeNew = System.currentTimeMillis() - time;
		}

		time = System.currentTimeMillis();
		try {
			sizeOld = oldProcessor.process(task);
		} finally {
			processTimeOld = System.currentTimeMillis() - time;
		}

		int count = COUNT.incrementAndGet();
		if (count > 0) {
			double totalTimeNew = (double) TIME_NEW.addAndGet(processTimeNew);
			double totalTimeOld = (double) TIME_OLD.addAndGet(processTimeOld);

			double totalSizeNew = (double) SIZE_NEW.addAndGet(sizeNew);
			double totalSizeOld = (double) SIZE_OLD.addAndGet(sizeOld);

			if (count % 100 == 0) {
				Bukkit.broadcastMessage(String.format("ms/chunk: %.3f - %.3f", (totalTimeNew / count), (totalTimeOld / count)));
				Bukkit.broadcastMessage(String.format("kb/chunk: %.3f - %.3f", (totalSizeNew / 1024 / count), (totalSizeOld / 1024 / count)));
			}
		}
	}
}

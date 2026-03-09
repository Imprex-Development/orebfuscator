package dev.imprex.orebfuscator;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.util.RollingAverage;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.jspecify.annotations.Nullable;

public class SystemMonitor implements Runnable {

  private static @Nullable OperatingSystemMXBeanProxy tryCreateOsBeanProxy() {
    try {
      MBeanServer beanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName beanName = ObjectName.getInstance(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
      return JMX.newMXBeanProxy(beanServer, beanName, OperatingSystemMXBeanProxy.class);
    } catch (Exception | LinkageError e) {
      return null;
    }
  }

  private final @Nullable OperatingSystemMXBeanProxy osBean;

  private final RollingAverage system10sec = new RollingAverage(10);
  private final RollingAverage system1min = new RollingAverage(60);
  private final RollingAverage system15min = new RollingAverage(60 * 15);
  private final RollingAverage process10sec = new RollingAverage(10);
  private final RollingAverage process1min = new RollingAverage(60);
  private final RollingAverage process15min = new RollingAverage(60 * 15);

  private final RollingAverage[] system = new RollingAverage[]{system10sec, system1min, system15min};
  private final RollingAverage[] process = new RollingAverage[]{process10sec, process1min, process15min};

  public SystemMonitor(OrebfuscatorCore orebfuscator) {
    this.osBean = tryCreateOsBeanProxy();
    if (this.osBean != null) {
      orebfuscator.executor().scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
    }
  }

  public void dump(ConfigurationSection section) {
    if (this.osBean != null) {
      section.set("cpu.system", "%.1f%% / %.1f%% / %.1f%%".formatted(
          system10sec.average() * 100, system1min.average() * 100, system15min.average() * 100));
      section.set("cpu.process", "%.1f%% / %.1f%% / %.1f%%".formatted(
          process10sec.average() * 100, process1min.average() * 100, process15min.average() * 100));

      section.set("memory.physical.total", this.osBean.getTotalMemorySize());
      section.set("memory.physical.free", this.osBean.getFreeMemorySize());
      section.set("memory.swap.total", this.osBean.getTotalSwapSpaceSize());
      section.set("memory.swap.free", this.osBean.getFreeSwapSpaceSize());
    }

    var runtime = Runtime.getRuntime();
    section.set("memory.process.total", runtime.totalMemory());
    section.set("memory.process.free", runtime.freeMemory());

    section.set("java.version", System.getProperty("java.version", "unknown"));
    section.set("java.vendor.name", System.getProperty("java.vendor", "unknown"));
    section.set("java.vendor.version", System.getProperty("java.vendor.version", "unknown"));

    RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    section.set("jvm.name", System.getProperty("java.vm.name", "unknown"));
    section.set("jvm.vendor", System.getProperty("java.vm.vendor", "unknown"));
    section.set("jvm.version", System.getProperty("java.vm.version", "unknown"));
    section.set("jvm.args", String.join(" ", runtimeBean.getInputArguments()));
  }

  @Override
  public void run() {
    assert this.osBean != null;

    double cpuLoad = this.osBean.getCpuLoad();
    if (cpuLoad >= 0) {
      for (RollingAverage average : this.system) {
        average.add(cpuLoad);
      }
    }

    double processCpuLoad = this.osBean.getProcessCpuLoad();
    if (processCpuLoad >= 0) {
      for (RollingAverage average : this.process) {
        average.add(cpuLoad);
      }
    }
  }


  public interface OperatingSystemMXBeanProxy {
    long getTotalSwapSpaceSize();
    long getFreeSwapSpaceSize();
    long getTotalMemorySize();
    long getFreeMemorySize();
    double getCpuLoad();
    double getProcessCpuLoad();
  }
}

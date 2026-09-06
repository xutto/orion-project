package com.mac.orion.infrastructure.util;

import com.mac.orion.OrionApplication;
import com.mac.orion.application.out.ProcessRelauncherUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Paths;

/**
 * Relaunches the application as a detached child process (same classpath, working dir and JVM).
 * Used by the port-change flow: the port lives in the DB (NODE_CONFIG), so the new process
 * picks it up on boot without any extra parameter.
 */
@Component
@Slf4j
public class ProcessRelauncher implements ProcessRelauncherUseCase {

  private static final long CHILD_LIVENESS_CHECK_MS = 1000L;

  /**
   * Relaunches the application with the same classpath and reports whether the child
   * survived the liveness check.
   *
   * todo ATTENTION: NEEDS improvement this method ,in the future the application launcher will be a .exe process...
   */
  @Override
  public boolean relaunch() {
    try {
      final String javaExe = Paths.get(System.getProperty("java.home"), "bin", "java").toString();
      final ProcessBuilder pb = new ProcessBuilder(
          javaExe,
          "-cp", System.getProperty("java.class.path"),
          OrionApplication.class.getName(),
          "--enabled-ui=true");
      pb.directory(new File(System.getProperty("user.dir")));
      pb.inheritIO();
      final Process child = pb.start();
      Thread.sleep(CHILD_LIVENESS_CHECK_MS);
      final boolean alive = child.isAlive();
      log.info("Relaunch process started: pid={}, alive={}", child.pid(), alive);
      return alive;
    } catch (Exception e) {
      log.error("Failed to relaunch the application", e);
      return false;
    }
  }
}

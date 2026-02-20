package com.gmavrommatis.config;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.event.StartupEvent;
import io.micronaut.http.server.HttpServerConfiguration;
import io.micronaut.scheduling.executor.ThreadSelection;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * Application listener that logs the configured Micronaut server thread-selection mode at startup.
 *
 * <p>Runs once on application startup and retrieves the current {@link ThreadSelection} setting
 * from the server configuration. Available thread-selection modes:
 *
 * <ul>
 *   <li>{@link ThreadSelection#AUTO AUTO} – automatically balances between event-loop and I/O
 *       threads
 *   <li>{@link ThreadSelection#IO IO} – always use the I/O thread pool
 *   <li>{@link ThreadSelection#MANUAL MANUAL} – user-managed selection of threads
 * </ul>
 *
 * @author GewrgiosMmavrommatis
 * @version 1.0
 */
@Singleton
@Slf4j
public class ThreadSelectionLogger implements ApplicationEventListener<StartupEvent> {

  private final HttpServerConfiguration serverConfiguration;

  public ThreadSelectionLogger(HttpServerConfiguration serverConfiguration) {
    this.serverConfiguration = serverConfiguration;
  }

  /**
   * Handles the {@link StartupEvent}, logging the configured thread-selection mode.
   *
   * <p>Invoked automatically by the Micronaut runtime when the application starts. Retrieves the
   * {@link ThreadSelection} from {@link HttpServerConfiguration} and writes it to the application
   * log.
   *
   * @param event the startup event triggering this listener
   */
  @Override
  public void onApplicationEvent(StartupEvent event) {
    ThreadSelection selection = serverConfiguration.getThreadSelection();
    log.info("Micronaut server thread-selection is: {}", selection);
  }
}

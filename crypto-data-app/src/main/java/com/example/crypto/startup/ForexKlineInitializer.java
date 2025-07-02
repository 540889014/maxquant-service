package com.example.crypto.startup;

import com.example.crypto.scheduler.ForexDataScheduler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test") // Do not run this initializer during tests
@RequiredArgsConstructor
@Slf4j
public class ForexKlineInitializer implements CommandLineRunner {

    private final ForexDataScheduler forexDataScheduler;

    @Value("${fcsapi.sync-on-startup:true}")
    private boolean syncOnStartup;

    @Override
    public void run(String... args) throws Exception {
        if (syncOnStartup) {
            log.info("sync-on-startup is true. Triggering initial forex data synchronization...");
            // Trigger the synchronization on application startup
            forexDataScheduler.syncAllForexData();
        } else {
            log.info("sync-on-startup is false. Skipping initial forex data synchronization.");
        }
    }
} 
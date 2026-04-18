package br.com.mmgabri.application.services;

import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class MetricsService {
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);

    private final StatsDClient statsDClient;

    public MetricsService(StatsDClient statsDClient) {
        this.statsDClient = statsDClient;
    }

    public void incrementMetric(String name, OffsetDateTime startTime, String... tags) {
        var duration = 0L;
        duration = Duration.between(startTime, OffsetDateTime.now()).toMillis();
        try {
            statsDClient.recordExecutionTime(name, duration, buildTags(tags));
        } catch (Exception e) {
            logger.error("Error sending metric", e);
            throw new RuntimeException(e);
        }
    }

    public void incrementMetricCounter(String name) {
        try {
            statsDClient.incrementCounter(name, "app:formatador");
        } catch (Exception e) {
            logger.error("Error sending metric", e);
            throw new RuntimeException(e);
        }
    }

    private String[] buildTags(String... extraTags) {
        if (extraTags == null || extraTags.length == 0) {
            return new String[] { "app:formatador" };
        }

        String[] all = new String[1 + extraTags.length];
        all[0] = "app:formatador";
        System.arraycopy(extraTags, 0, all, 1, extraTags.length);
        return all;
    }
}

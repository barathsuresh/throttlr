package com.desertrider.throttlr.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.desertrider.throttlr.dto.response.AnalyticsResponse;
import com.desertrider.throttlr.model.App;
import com.desertrider.throttlr.repository.AppRepository;
import com.desertrider.throttlr.service.analytics.AnalyticsReader;

class AnalyticsReadServiceTest {
    @Test
    void getCurrentHourAnalyticsReturnsCountersForOwnedApp() {
        App app = App.builder()
                .id("app-1")
                .accountId("account-1")
                .build();
        RecordingAppRepository appRepository = new RecordingAppRepository(app);
        RecordingAnalyticsReader analyticsReader = new RecordingAnalyticsReader(
                Map.of("total", 7L, "allowed", 5L, "blocked", 2L),
                1_770_000_000_000L);

        AnalyticsReadService service = new AnalyticsReadService(appRepository.proxy(), analyticsReader);

        AnalyticsResponse response = service.getCurrentHourAnalytics("account-1", "app-1");

        assertEquals("app-1", response.appId());
        assertEquals(1_770_000_000_000L, response.hour());
        assertEquals(7, response.total());
        assertEquals(5, response.allowed());
        assertEquals(2, response.blocked());
        assertEquals("app-1", analyticsReader.appId);
    }

    private static final class RecordingAppRepository {
        private final App app;

        private RecordingAppRepository(App app) {
            this.app = app;
        }

        private AppRepository proxy() {
            return (AppRepository) Proxy.newProxyInstance(
                    AppRepository.class.getClassLoader(),
                    new Class<?>[] { AppRepository.class },
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByIdAndAccountId" -> Optional.of(app);
                        case "toString" -> "RecordingAppRepository";
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    private static final class RecordingAnalyticsReader implements AnalyticsReader {
        private final Map<String, Long> counters;
        private final long currentHour;
        private String appId;

        private RecordingAnalyticsReader(Map<String, Long> counters, long currentHour) {
            this.counters = counters;
            this.currentHour = currentHour;
        }

        @Override
        public Map<String, Long> getCurrentHourCounters(String appId) {
            this.appId = appId;
            return counters;
        }

        @Override
        public long currentHour() {
            return currentHour;
        }
    }
}

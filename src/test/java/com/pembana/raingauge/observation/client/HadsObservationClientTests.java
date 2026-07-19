package com.pembana.raingauge.observation.client;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.pembana.raingauge.config.ProviderRestClientFactory;
import com.pembana.raingauge.config.RainfallProperties;
import com.pembana.raingauge.observation.HadsObservationParser;
import com.pembana.raingauge.observation.ObservationBatch;
import com.pembana.raingauge.station.client.ProviderException;
import com.pembana.raingauge.support.ProviderStatusRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HadsObservationClientTests {

	private static final Instant FROM = Instant.parse("2026-07-01T00:00:00Z");

	private static final Instant TO = Instant.parse("2026-07-01T01:00:00Z");

	private static final String VALID_BODY = """
			station,utc_valid,PCIRGZZ,PPHRGZZ
			WIHH1,2026-07-01 00:00:00,41.99,0.0
			WIHH1,2026-07-01 00:15:00,42.01,
			""";

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (this.server != null) {
			this.server.stop(0);
		}
	}

	@Test
	void sendsUtcBoundariesAndParsesSuccessfulResponse() throws Exception {
		AtomicInteger requests = new AtomicInteger();
		startServer((exchange) -> {
			requests.incrementAndGet();
			assertThat(exchange.getRequestURI().getRawQuery())
					.contains("stations=WIHH1", "network=HI_DCP", "sts=2026-07-01T00:00:00Z",
							"ets=2026-07-01T01:00:00Z", "what=txt", "delim=comma");
			respond(exchange, 200, VALID_BODY);
		});

		ObservationBatch batch = client(0, Duration.ofSeconds(1)).fetch(
				List.of("WIHH1"), "HI_DCP", "PCIRG", FROM, TO);

		assertThat(requests).hasValue(1);
		assertThat(batch.observations()).hasSize(2);
		assertThat(batch.rejectedRows()).isZero();
	}

	@Test
	void retriesRateLimitThenSucceeds() throws Exception {
		AtomicInteger requests = new AtomicInteger();
		startServer((exchange) -> {
			if (requests.incrementAndGet() == 1) {
				respond(exchange, 429, "rate limited");
			} else {
				respond(exchange, 200, VALID_BODY);
			}
		});

		ObservationBatch batch = client(1, Duration.ofSeconds(1)).fetch(
				List.of("WIHH1"), "HI_DCP", "PCIRG", FROM, TO);

		assertThat(requests).hasValue(2);
		assertThat(batch.observations()).hasSize(2);
	}

	@Test
	void retriesTransientServerErrorThenSucceeds() throws Exception {
		AtomicInteger requests = new AtomicInteger();
		startServer((exchange) -> {
			if (requests.incrementAndGet() == 1) {
				respond(exchange, 503, "temporarily unavailable");
			} else {
				respond(exchange, 200, VALID_BODY);
			}
		});

		client(1, Duration.ofSeconds(1)).fetch(List.of("WIHH1"), "HI_DCP", "PCIRG", FROM, TO);

		assertThat(requests).hasValue(2);
	}

	@Test
	void doesNotRetryPermanentClientError() throws Exception {
		AtomicInteger requests = new AtomicInteger();
		startServer((exchange) -> {
			requests.incrementAndGet();
			respond(exchange, 400, "invalid request");
		});

		assertThatThrownBy(() -> client(2, Duration.ofSeconds(1)).fetch(
				List.of("WIHH1"), "HI_DCP", "PCIRG", FROM, TO))
				.isInstanceOf(ProviderException.class)
				.hasMessageContaining("Unable to retrieve HADS observations");
		assertThat(requests).hasValue(1);
	}

	@Test
	void reportsReadTimeout() throws Exception {
		startServer((exchange) -> {
			try {
				Thread.sleep(150);
				respond(exchange, 200, VALID_BODY);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			} catch (IOException ex) {
				// The client closes the exchange when its configured read timeout wins.
			}
		});

		assertThatThrownBy(() -> client(0, Duration.ofMillis(25)).fetch(
				List.of("WIHH1"), "HI_DCP", "PCIRG", FROM, TO))
				.isInstanceOf(ProviderException.class);
	}

	@Test
	void emptyResponseIsNotPresentedAsACompleteDataset() throws Exception {
		startServer((exchange) -> respond(exchange, 200, ""));

		assertThatThrownBy(() -> client(0, Duration.ofSeconds(1)).fetch(
				List.of("WIHH1"), "HI_DCP", "PCIRG", FROM, TO))
				.isInstanceOf(ProviderException.class)
				.hasMessageContaining("no response body");
	}

	private void startServer(HttpHandler handler) throws IOException {
		this.server = HttpServer.create(
				new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		this.server.createContext("/cgi-bin/request/hads.py", handler);
		this.server.start();
	}

	private HadsObservationClient client(int retries, Duration readTimeout) {
		RainfallProperties properties = new RainfallProperties();
		properties.getProviders().setHadsBaseUrl("http://localhost:" + this.server.getAddress().getPort());
		properties.getProviders().setRetries(retries);
		properties.getProviders().setRetryInitialBackoff(Duration.ofMillis(1));
		properties.getProviders().setReadTimeout(readTimeout);
		ProviderRestClientFactory factory = new ProviderRestClientFactory(RestClient.builder(), properties);
		return new HadsObservationClient(factory, new HadsObservationParser(), properties,
				new ProviderStatusRegistry(), Clock.fixed(TO, ZoneOffset.UTC));
	}

	private static void respond(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "text/csv; charset=UTF-8");
		exchange.sendResponseHeaders(status, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

}

package io.openems.edge.bridge.http.api;

import static io.openems.edge.bridge.http.time.DelayTimeProviderChain.immediate;
import static java.util.Collections.emptyMap;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.google.gson.JsonElement;

import io.openems.common.function.ThrowingConsumer;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;

/**
 * BridgeHttpTime to handle request to a endpoint based on a time delay.
 * 
 * <p>
 * The calculation when an endpoint gets called is provided in the
 * {@link DelayTimeProvider}. The
 * {@link DelayTimeProvider#nextRun(boolean, boolean)} gets called instantly
 * when the initial method to add the endpoint gets called and then every time
 * after the last endpoint handle was finished.
 * 
 * <p>
 * So for e. g. if a fixed delay of 1 minute gets provided the time will shift
 * into the back a little bit every time an endpoint gets called because
 * fetching the endpoint and handling it also takes some time.
 * 
 * <p>
 * A simple example to subscribe to an endpoint with 1 minute delay in between
 * would be:
 * 
 * <pre>
 * final var delayProvider = DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(1));
 * this.httpBridge.subscribeTime(delayProvider, "http://127.0.0.1/status", t -> {
 * 	// process data
 * }, t -> {
 * 	// handle error
 * });
 * </pre>
 */
public interface BridgeHttpTime {

	public record TimeEndpoint(//
			/**
			 * The delay time provider. Gives the time from the current time to the next
			 * time when the endpoint should be fetched.
			 */
			DelayTimeProvider delayTimeProvider, //
			/**
			 * The url which should be fetched.
			 */
			Endpoint endpoint, //
			/**
			 * The callback to execute on every successful result.
			 */
			Consumer<String> onResult, //
			/**
			 * The callback to execute on every error.
			 */
			Consumer<Throwable> onError //
	) {

	}

	/**
	 * Subscribes to an {@link TimeEndpoint}. The {@link TimeEndpoint#endpoint} gets
	 * fetched based on the delayed time provided by the
	 * {@link TimeEndpoint#delayTimeProvider}. After the endpoint gets fetched
	 * either the {@link TimeEndpoint#onResult} or the {@link TimeEndpoint#onError}
	 * gets executed depending on the result.
	 * 
	 * @param endpoint the {@link TimeEndpoint} to add a subscription
	 */
	public void subscribeTime(TimeEndpoint endpoint);

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link Endpoint} to fetch
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 */
	public default void subscribeTime(//
			DelayTimeProvider delayTimeProvider, //
			Endpoint endpoint, //
			ThrowingConsumer<String, Exception> onResult, //
			Consumer<Throwable> onError //
	) {
		this.subscribeTime(new TimeEndpoint(delayTimeProvider, endpoint, t -> {
			try {
				onResult.accept(t);
			} catch (Exception e) {
				onError.accept(e);
			}
		}, onError));
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link Endpoint} to fetch
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 */
	public default void subscribeTime(//
			DelayTimeProvider delayTimeProvider, //
			Endpoint endpoint, //
			BiConsumer<String, Throwable> action //
	) {
		this.subscribeTime(new TimeEndpoint(delayTimeProvider, endpoint, r -> action.accept(r, null),
				t -> action.accept(null, t)));
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProviderChain} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * <p>
	 * Note: the first fetch gets triggered immediately
	 * 
	 * @param onErrorDelay   the {@link DelayTimeProviderChain} when the last fetch
	 *                       was not successful
	 * @param onSuccessDelay the {@link DelayTimeProviderChain} when the last fetch
	 *                       was successful
	 * @param url            the url to fetch
	 * @param onResult       the method to call on successful fetch
	 * @param onError        the method to call if an error happens during fetching
	 *                       or handling the result
	 */
	public default void subscribeTime(//
			DelayTimeProviderChain onErrorDelay, //
			DelayTimeProviderChain onSuccessDelay, //
			String url, //
			ThrowingConsumer<String, Exception> onResult, //
			Consumer<Throwable> onError //
	) {
		this.subscribeTime(new DefaultDelayTimeProvider(immediate(), onErrorDelay, onSuccessDelay), new Endpoint(url, //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap() //
		), onResult, onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProviderChain} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * <p>
	 * Note: the first fetch gets triggered immediately
	 * 
	 * @param delay    the {@link DelayTimeProviderChain} between each fetch
	 * @param url      the url to fetch
	 * @param onResult the method to call on successful fetch
	 * @param onError  the method to call if an error happens during fetching or
	 *                 handling the result
	 */
	public default void subscribeTime(//
			DelayTimeProviderChain delay, //
			String url, //
			ThrowingConsumer<String, Exception> onResult, //
			Consumer<Throwable> onError //
	) {
		this.subscribeTime(delay, delay, url, onResult, onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProviderChain} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * <p>
	 * Note: the first fetch gets triggered immediately
	 * 
	 * @param delay    the {@link DelayTimeProviderChain} between each fetch
	 * @param url      the url to fetch
	 * @param onResult the method to call on successful fetch
	 */
	public default void subscribeTime(//
			DelayTimeProviderChain delay, //
			String url, //
			ThrowingConsumer<String, Exception> onResult //
	) {
		this.subscribeTime(delay, delay, url, onResult, BridgeHttp.EMPTY_ERROR_HANDLER);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch either the
	 * <code>onResult</code> or the <code>onError</code> method gets called.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link Endpoint} to fetch
	 * @param onResult          the method to call on successful fetch
	 * @param onError           the method to call if an error happens during
	 *                          fetching or handling the result
	 */
	public default void subscribeJsonTime(//
			DelayTimeProvider delayTimeProvider, //
			Endpoint endpoint, //
			ThrowingConsumer<JsonElement, Exception> onResult, //
			Consumer<Throwable> onError //
	) {
		this.subscribeTime(delayTimeProvider, endpoint, t -> onResult.accept(JsonUtils.parse(t)), onError);
	}

	/**
	 * Subscribes to an {@link Endpoint} with the delay provided by the
	 * {@link DelayTimeProvider} and after every endpoint fetch the
	 * <code>action</code> gets called either with the result or the error at least
	 * one is not null.
	 * 
	 * @param delayTimeProvider the {@link DelayTimeProvider} to provided the delay
	 *                          between the fetches
	 * @param endpoint          the {@link Endpoint} to fetch
	 * @param action            the action to perform; the first is the result of
	 *                          the endpoint if existing and the second argument is
	 *                          passed if an error happend. One of the params is
	 *                          always null and one not
	 */
	public default void subscribeJsonTime(//
			DelayTimeProvider delayTimeProvider, //
			Endpoint endpoint, //
			BiConsumer<JsonElement, Throwable> action //
	) {
		this.subscribeTime(delayTimeProvider, endpoint, t -> action.accept(JsonUtils.parse(t), null),
				e -> action.accept(null, e));
	}

}

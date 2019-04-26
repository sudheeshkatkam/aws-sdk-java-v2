package software.amazon.awssdk.services.transcribestreaming;

import static software.amazon.awssdk.core.http.HttpResponseHandler.X_AMZN_REQUEST_ID_HEADER;

import software.amazon.awssdk.core.async.SdkPublisher;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponse;
import software.amazon.awssdk.services.transcribestreaming.model.StartStreamTranscriptionResponseHandler;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptEvent;
import software.amazon.awssdk.services.transcribestreaming.model.TranscriptResultStream;

public class TestResponseHandlers {

    /**
     * A simple consumer of events to subscribe
     */
    public static StartStreamTranscriptionResponseHandler responseHandlerBuilder_Consumer() {
        return StartStreamTranscriptionResponseHandler.builder()
                                                      .onResponse(r -> {
                                                          String idFromHeader = r.sdkHttpResponse()
                                                                                 .firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                                                                                 .orElse(null);
                                                          System.out.println("Received Initial response: " + idFromHeader);
                                                      })
                                                      .onError(e -> {
                                                          System.out.println("Error message: " + e.getMessage());
                                                      })
                                                      .onComplete(() -> {
                                                          System.out.println("All records stream successfully");
                                                      })
                                                      .subscriber(event -> {
                                                          System.out.println(((TranscriptEvent) event).transcript().results());
                                                      })
                                                      .build();
    }


    /**
     * A classic way by implementing the interface and using helper method in {@link SdkPublisher}.
     */
    public static StartStreamTranscriptionResponseHandler responseHandlerBuilder_Classic() {
        return new StartStreamTranscriptionResponseHandler() {
            @Override
            public void responseReceived(StartStreamTranscriptionResponse response) {
                String idFromHeader = response.sdkHttpResponse()
                                              .firstMatchingHeader(X_AMZN_REQUEST_ID_HEADER)
                                              .orElse(null);
                System.out.println("Received Initial response: " + idFromHeader);
            }

            @Override
            public void onEventStream(SdkPublisher<TranscriptResultStream> publisher) {
                publisher
                    // Filter to only SubscribeToShardEvents
                    .filter(TranscriptEvent.class)
                    // Flat map into a publisher of just records
                    // Using
                    .flatMapIterable(event -> event.transcript().results())
                    // TODO limit is broken. After limit is reached, app fails with SdkCancellationException
                    // instead of gracefully exiting. Limit to 1000 total records
                    //.limit(5)
                    // Batch records into lists of 25
                    .buffer(25)
                    // Print out each record batch
                    // You won't see any data printed as the audio files we use have no voice
                    .subscribe(batch ->  {
                        System.out.println("Record Batch - " + batch);
                    });
            }

            @Override
            public void exceptionOccurred(Throwable throwable) {
                System.out.println("Error message: " + throwable.getMessage());
            }

            @Override
            public void complete() {
                System.out.println("All records stream successfully");
            }
        };
    }
}

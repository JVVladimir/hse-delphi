package ru.hse.delphi.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.hse.protobuf.ClientRequest;
import ru.hse.protobuf.HandlerServiceGrpc;
import ru.hse.protobuf.ServerResponse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
public class GrpcVoteService extends HandlerServiceGrpc.HandlerServiceImplBase {

    private static Logger log = LoggerFactory.getLogger(GrpcVoteService.class);

    static Set<StreamObserver<ServerResponse>> observers = ConcurrentHashMap.newKeySet();

    private static final int n = 4;
    private static final String QUESTION = "Действительно ли, Вова гранд пиг?";

    @Override
    public StreamObserver<ClientRequest> handleRequest(StreamObserver<ServerResponse> responseObserver) {

        return new StreamObserver<>() {

            @Override
            public void onNext(ClientRequest value) {
                log.info("Client request: {}", value);
                // Generate (1)
                if (value.getDescription().equals("-1") && value.getMark().equals("-1")) {
                    if (observers.size() < n) {
                        observers.add(responseObserver);
                        log.info("Observers size: {}", observers.size());
                        ServerResponse message = ServerResponse.newBuilder()
                                .setAction("Wait:" + n)
                                .setComments(QUESTION)
                                .build();
                        observers.stream()
                                .filter(e -> e.equals(responseObserver))
                                .forEach(observer -> {
                                    observer.onNext(message);
                                    log.info("Send only to {} - {}", observer, message);
                                });
                        if (observers.size() == n) {
                            log.info("Vote mode");
                            ServerResponse message2 = ServerResponse.newBuilder()
                                    .setAction("Vote")
                                    .build();
                            observers.forEach(observer -> {
                                observer.onNext(message2);
                                log.info("Broadcast to {} - {}", observer, message2);
                            });
                        }
                    } else {
                        responseObserver.onCompleted();
                    }
                    // Vote (2)
                } else if (!value.getMark().equals("-1") && value.getDescription().equals("-1")) {
                    log.info("Mark got");
                    ServerResponse message = ServerResponse.newBuilder()
                            .setId(value.getId())
                            .setMark(value.getMark())
                            .setAction("Mark")
                            .build();
                    observers.forEach(observer -> {
                        observer.onNext(message);
                        log.info("Broadcast to {} - {}", observer, message);
                    });
                    // Comment(3)
                } else if (value.getMark().equals("-1") && !value.getDescription().equals("-1")) {
                    log.info("Comment got");
                    ServerResponse message = ServerResponse.newBuilder()
                            .setId(value.getId())
                            .setMark(value.getMark())
                            .setComments(value.getDescription())
                            .setAction("Comment")
                            .build();
                    observers.forEach(observer -> {
                        observer.onNext(message);
                        log.info("Broadcast to {} - {}", observer, message);
                    });
                }
            }

            @Override
            public void onError(Throwable t) {
                observers.forEach(observer -> observer.onError(t));
                log.info("Error happened!");
            }

            @Override
            public void onCompleted() {
                log.info("On completed!");
                observers.stream().filter(e -> e.equals(responseObserver))
                        .forEach(StreamObserver::onCompleted);
                // observers.forEach(StreamObserver::onCompleted);
                observers.remove(responseObserver);
            }
        };
    }
}

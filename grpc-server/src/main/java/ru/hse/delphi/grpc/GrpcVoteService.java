package ru.hse.delphi.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import ru.hse.delphi.entity.Config;
import ru.hse.delphi.service.ConfigReader;
import ru.hse.delphi.service.ConfigWriter;
import ru.hse.protobuf.ClientRequest;
import ru.hse.protobuf.HandlerServiceGrpc;
import ru.hse.protobuf.ServerResponse;

import javax.annotation.PostConstruct;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@GrpcService
@EnableScheduling
public class GrpcVoteService extends HandlerServiceGrpc.HandlerServiceImplBase {

    private static Logger log = LoggerFactory.getLogger(GrpcVoteService.class);

    private static Set<StreamObserver<ServerResponse>> observers = ConcurrentHashMap.newKeySet();

    private volatile static int n;
    private volatile static String question;
    private volatile static boolean isVote;

    @Autowired
    private ConfigReader configReader;
    @Autowired
    private ConfigWriter configWriter;

    @PostConstruct
    public void sendIpToConfig() {
        configWriter.writeConfig();
    }

    @Override
    public StreamObserver<ClientRequest> handleRequest(StreamObserver<ServerResponse> responseObserver) {

        return new StreamObserver<>() {

            @Override
            public void onNext(ClientRequest value) {
                log.info("Client request: {}", value);
                if (n < 4 || question == null || question.trim().equals(""))
                    responseObserver.onCompleted();
                // Generate (1)
                if (value.getDescription().equals("-1") && value.getMark().equals("-1")) {
                    isVote = true;
                    if (observers.size() < n) {
                        observers.add(responseObserver);
                        log.info("Observers size: {}", observers.size());
                        ServerResponse message = ServerResponse.newBuilder()
                                .setAction("Wait:" + n)
                                .setComments(question)
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
                isVote = false;
                observers.forEach(observer -> observer.onError(t));
                log.info("Error happened!");
            }

            @Override
            public void onCompleted() {
                isVote = false;
                log.info("On completed!");
                observers.stream().filter(e -> e.equals(responseObserver))
                        .forEach(StreamObserver::onCompleted);
                // Тут подумать, как их лучше отключать!!!
                // observers.forEach(StreamObserver::onCompleted);
                observers.remove(responseObserver);
            }
        };
    }

    @Scheduled(fixedDelay = 5000)
    public void checkConfigEnable() {
        if (!isVote) {
            Config config = configReader.readConfig();
            if (config != null && !(config.getNumberOfExperts() == 0 && config.getQuestion() == null)) {
                n = config.getNumberOfExperts();
                question = config.getQuestion();
                log.info("Данные, полученные из ГУГЛ-док: {}, {}", question, n);
            }
        }
    }
}
// 141
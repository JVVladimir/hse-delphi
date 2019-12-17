package ru.hse.delphi.grpc;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.hse.delphi.algo.DelphiMethod;
import ru.hse.delphi.algo.MedianCounter;
import ru.hse.protobuf.ClientRequest;
import ru.hse.protobuf.HandlerServiceGrpc;
import ru.hse.protobuf.ServerResponse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GrpcClientService {

    private static Logger log = LoggerFactory.getLogger(GrpcClientService.class);

    @GrpcClient("CreateHandleChannel")
    private HandlerServiceGrpc.HandlerServiceStub handlerService;

    private static int n;
    private static final String ID = UUID.randomUUID().toString();
    private static ConcurrentHashMap<String, ServerResponse> map = new ConcurrentHashMap<>();

    // 1 - vote, 2 - comment, 3 - done
    private static int status;

    private static Comparator<ServerResponse> comparator = (a, b) -> {
        double a1 = Double.parseDouble(a.getMark());
        double b1 = Double.parseDouble(b.getMark());
        if (a1 > b1) {
            return 1;
        } else if (a1 < b1) {
            return -1;
        }
        return 0;
    };


    public void process() {

        StreamObserver<ClientRequest> chat = handlerService.handleRequest(new StreamObserver<>() {
            @Override
            public void onNext(ServerResponse value) {
                log.info("Message from server: {}", value);
                String action = value.getAction();
                if (action.startsWith("Wait:")) {
                    n = Integer.parseInt(action.substring(action.indexOf(":") + 1)) + 2; //// + 2
                    map.put("1", ServerResponse.newBuilder()
                            .setMark("5")
                            .build());
                    map.put("2", ServerResponse.newBuilder()
                            .setMark("6")
                            .build());
                    log.info("Question for experts: {}", value.getComments());
                } else if (action.equals("Vote")) {
                    log.info("Vote: {}", value);
                    status = 1;
                } else if (action.equals("Mark")) {
                    log.info("Mark received. Value: {}", value);
                    map.put(value.getId(), value);

                    // Мапа накполнена и можно делать анализ
                    if (map.size() == n) {
                        log.info("Vote done. Result map: {}", map);
                        double[] marks = getMarksFromMap();
                        if (DelphiMethod.getK0() == -1) {
                            DelphiMethod.setInitQuantile(n, marks);
                            defineCommentGroup();
                            map.clear(); ///////
                            map.put("1", ServerResponse.newBuilder()
                                    .setMark("5")
                                    .build());
                            map.put("2", ServerResponse.newBuilder()
                                    .setMark("6")
                                    .build());
                        } else {
                            double k = DelphiMethod.getQuantile(n, marks);
                            if (DelphiMethod.quantileIsNotThreshold(k)) {
                                defineCommentGroup();
                                map.clear(); /////
                                map.put("1", ServerResponse.newBuilder()
                                        .setMark("5")
                                        .build());
                                map.put("2", ServerResponse.newBuilder()
                                        .setMark("6")
                                        .build());
                            } else {
                                log.info("**** MEDIAN MARK IS: {} *****", MedianCounter.countMedian(marks));
                                status = 3;
                            }
                        }
                    }
                } else if(action.equals("Comment")) {
                    log.info("Comment received. Value: {}", value);
                    if(status == 1)
                        log.info("Vote again");
                }
            }

            @Override
            public void onError(Throwable t) {
                System.err.println("Some error occured!");
            }

            @Override
            public void onCompleted() {
                System.err.println("done");
                status = 0;
            }
        });

        while (true) {
            var s = new Scanner(System.in).nextLine();
            if (s.equals("q")) {
                chat.onCompleted();
                break;
            }
            // Запускаем программу
            if (s.equals("s")) {
                status = 0;
                ClientRequest requestGrpc = getClientRequest("-1", "-1");
                chat.onNext(requestGrpc);
                // Пишем оценку
            } else if (status == 1) {
                ClientRequest requestGrpc = getClientRequest("-1", s);
                chat.onNext(requestGrpc);
                // Пишем свой комментарий
            } else if (status == 2) {
                ClientRequest requestGrpc = getClientRequest(s, "-1");
                chat.onNext(requestGrpc);
                status = 1;
                // Успешно закончили выполнение программы
            } else if (status == 3) {
                status = 0;
                n = -1;
                DelphiMethod.setK0(-1);
                chat.onCompleted();
            }
        }
    }

    private ClientRequest getClientRequest(String description, String mark) {
        return ClientRequest.newBuilder()
                .setId(ID)
                .setMark(mark)
                .setDescription(description)
                .build();
    }

    private void defineCommentGroup() {
        List<ServerResponse> list = new ArrayList<>(map.values());
        list.sort(comparator);
        int groupSize = n / 4;
        for (int i = 0; i < groupSize; i++) {
            if (list.get(i).getId().equals(ID)) {
                status = 2;
                log.info("Please enter your comment -> ");
                break;
            }
        }

        for (int i = 3 * groupSize; i < n; i++) {
            if (list.get(i).getId().equals(ID)) {
                status = 2;
                log.info("Please enter your comment -> ");
                break;
            }
        }
    }

    private double[] getMarksFromMap() {
        double[] marks = new double[n];
        int i = 0;
        for (ServerResponse response : map.values()) {
            marks[i] = Double.parseDouble(response.getMark());
            i++;
        }
        return marks;
    }
}

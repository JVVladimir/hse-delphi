package ru.hse.delphi;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.hse.delphi.grpc.GrpcClientService;

@SpringBootApplication
public class ApplicationRunner {

    private static GrpcClientService service;

    @Autowired
    public void setService(GrpcClientService service) {
        ApplicationRunner.service = service;
    }

    public static void main(String[] args) {
       // service = new GrpcClientService();
        SpringApplication.run(ApplicationRunner.class, args);
       // ManagedChannel channel = NettyChannelBuilder.forAddress("localhost", 9091).usePlaintext().build();
       // HandlerServiceGrpc.HandlerServiceStub ru.hse.client = HandlerServiceGrpc.newStub(channel);
            service.process();
    }

}

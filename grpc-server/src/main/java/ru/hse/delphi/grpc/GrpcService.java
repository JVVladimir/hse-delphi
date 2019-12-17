package ru.hse.delphi.grpc;

import com.google.protobuf.AbstractMessage;

public interface GrpcService<T extends AbstractMessage, R extends AbstractMessage> {
    R handleRequest(T request);
}

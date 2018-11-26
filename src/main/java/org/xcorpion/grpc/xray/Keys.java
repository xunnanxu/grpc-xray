package org.xcorpion.grpc.xray;

import io.grpc.Metadata;

public class Keys {

    public static final Metadata.Key<String> TRACE_ID_HEADER = Metadata.Key.of("traceId", Metadata.ASCII_STRING_MARSHALLER);
    public static final Metadata.Key<String> PARENT_ID_HEADER = Metadata.Key.of("parentId", Metadata.ASCII_STRING_MARSHALLER);

}

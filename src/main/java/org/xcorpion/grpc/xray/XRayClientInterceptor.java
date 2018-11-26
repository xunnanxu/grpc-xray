package org.xcorpion.grpc.xray;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.Subsegment;
import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.ForwardingClientCallListener;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class XRayClientInterceptor implements ClientInterceptor {

    private final AWSXRayRecorder recorder = AWSXRayRecorderBuilder.defaultRecorder();

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        final Segment segment = recorder.getCurrentSegmentOptional().orElseGet(() -> {
            //noinspection CodeBlock2Expr
            return recorder.beginSegment(method.getFullMethodName());
        });
        final String segmentId = segment.getId();
        final String traceId = segment.getTraceId().toString();
        ClientCall<ReqT, RespT> call = next.newCall(method, callOptions);
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(call) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                Subsegment callSegment = recorder.beginSubsegment(method.getFullMethodName());
                final Entity context = recorder.getTraceEntity();
                headers.discardAll(Keys.PARENT_ID_HEADER);
                headers.put(Keys.PARENT_ID_HEADER, segmentId);
                headers.put(Keys.TRACE_ID_HEADER, traceId);
                delegate().start(
                        new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                            @Override
                            public void onClose(io.grpc.Status status, Metadata trailers) {
                                if (status.getCause() != null) {
                                    callSegment.addException(status.getCause());
                                } else if (!status.isOk()) {
                                    callSegment.setError(true);
                                }
                                try {
                                    super.onClose(status, trailers);
                                } finally {
                                    Entity originalContext = recorder.getTraceEntity();
                                    recorder.setTraceEntity(context);
                                    try {
                                        callSegment.close();
                                    } finally {
                                        recorder.setTraceEntity(originalContext);
                                    }
                                }
                            }
                        },
                        headers);
            }
        };
    }
}

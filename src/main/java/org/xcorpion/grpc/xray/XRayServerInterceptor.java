package org.xcorpion.grpc.xray;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.AWSXRayRecorderBuilder;
import com.amazonaws.xray.entities.Segment;
import com.amazonaws.xray.entities.TraceID;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@GRpcGlobalInterceptor
public class XRayServerInterceptor implements ServerInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(XRayServerInterceptor.class);

    @Value("${spring.application.name}")
    private String appName;

    private AWSXRayRecorder recorder = AWSXRayRecorderBuilder.defaultRecorder();

    public XRayServerInterceptor() {
        LOG.info("XRay Interceptor Enabled");
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String traceId = headers.get(Keys.TRACE_ID_HEADER);
        String parentId = headers.get(Keys.PARENT_ID_HEADER);
        TraceID tId = new TraceID();
        if (traceId != null) {
            tId = TraceID.fromString(traceId);
        }
        Segment segment = recorder.beginSegment(appName, tId, parentId);
        headers.discardAll(Keys.PARENT_ID_HEADER);
        headers.discardAll(Keys.TRACE_ID_HEADER);
        headers.put(Keys.PARENT_ID_HEADER, segment.getId());
        headers.put(Keys.TRACE_ID_HEADER, tId.toString());
        ServerCall.Listener<ReqT> listener = next.startCall(call, headers);

        return new ForwardingListener<>(listener, call, recorder, recorder.getTraceEntity(), segment);
    }
}

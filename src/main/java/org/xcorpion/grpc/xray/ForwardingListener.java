package org.xcorpion.grpc.xray;

import com.amazonaws.xray.AWSXRayRecorder;
import com.amazonaws.xray.entities.Entity;
import com.amazonaws.xray.entities.Segment;
import io.grpc.ForwardingServerCallListener;
import io.grpc.ServerCall;

public class ForwardingListener<T, R>
        extends ForwardingServerCallListener.SimpleForwardingServerCallListener<T> {

    private ServerCall<T, R> call;
    private AWSXRayRecorder recorder;
    private Entity entity;
    private Segment segment;

    public ForwardingListener(ServerCall.Listener<T> delegate,
            ServerCall<T, R> call,
            AWSXRayRecorder recorder,
            Entity entity,
            Segment segment
    ) {
        super(delegate);
        this.call = call;
        this.recorder = recorder;
        this.entity = entity;
        this.segment = segment;
    }

    @Override
    public void onCancel() {
        recorder.setTraceEntity(entity);
        if (call.isCancelled()) {
            return;
        }
        segment.setFault(true);
        try {
            super.onCancel();
        }
        finally {
            segment.close();
        }
    }

    @Override
    public void onComplete() {
        recorder.setTraceEntity(entity);
        try {
            super.onComplete();
        }
        catch (Throwable e) {
            segment.setError(true);
        }
        finally {
            segment.close();
        }
    }

}

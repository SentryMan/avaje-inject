package org.example.myapp.generic;

import javax.inject.Singleton;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

@Singleton
public class GenericImpl implements GenericInterfaceObject<Flow.Publisher<Object>> {
    public Flow.Publisher<Object> get() {
        return new SubmissionPublisher<>();
    }
}

package com.mdt.integration.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: integration.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class IntegrationServiceGrpc {

  private IntegrationServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "mdt.integration.IntegrationService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.mdt.integration.grpc.PullStudyRequest,
      com.mdt.integration.grpc.PullStudyResponse> getPullStudyFromPacsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PullStudyFromPacs",
      requestType = com.mdt.integration.grpc.PullStudyRequest.class,
      responseType = com.mdt.integration.grpc.PullStudyResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.integration.grpc.PullStudyRequest,
      com.mdt.integration.grpc.PullStudyResponse> getPullStudyFromPacsMethod() {
    io.grpc.MethodDescriptor<com.mdt.integration.grpc.PullStudyRequest, com.mdt.integration.grpc.PullStudyResponse> getPullStudyFromPacsMethod;
    if ((getPullStudyFromPacsMethod = IntegrationServiceGrpc.getPullStudyFromPacsMethod) == null) {
      synchronized (IntegrationServiceGrpc.class) {
        if ((getPullStudyFromPacsMethod = IntegrationServiceGrpc.getPullStudyFromPacsMethod) == null) {
          IntegrationServiceGrpc.getPullStudyFromPacsMethod = getPullStudyFromPacsMethod =
              io.grpc.MethodDescriptor.<com.mdt.integration.grpc.PullStudyRequest, com.mdt.integration.grpc.PullStudyResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PullStudyFromPacs"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.integration.grpc.PullStudyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.integration.grpc.PullStudyResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IntegrationServiceMethodDescriptorSupplier("PullStudyFromPacs"))
              .build();
        }
      }
    }
    return getPullStudyFromPacsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mdt.integration.grpc.IngestStudyRequest,
      com.mdt.integration.grpc.Ack> getIngestStudyMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "IngestStudy",
      requestType = com.mdt.integration.grpc.IngestStudyRequest.class,
      responseType = com.mdt.integration.grpc.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.integration.grpc.IngestStudyRequest,
      com.mdt.integration.grpc.Ack> getIngestStudyMethod() {
    io.grpc.MethodDescriptor<com.mdt.integration.grpc.IngestStudyRequest, com.mdt.integration.grpc.Ack> getIngestStudyMethod;
    if ((getIngestStudyMethod = IntegrationServiceGrpc.getIngestStudyMethod) == null) {
      synchronized (IntegrationServiceGrpc.class) {
        if ((getIngestStudyMethod = IntegrationServiceGrpc.getIngestStudyMethod) == null) {
          IntegrationServiceGrpc.getIngestStudyMethod = getIngestStudyMethod =
              io.grpc.MethodDescriptor.<com.mdt.integration.grpc.IngestStudyRequest, com.mdt.integration.grpc.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "IngestStudy"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.integration.grpc.IngestStudyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.integration.grpc.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new IntegrationServiceMethodDescriptorSupplier("IngestStudy"))
              .build();
        }
      }
    }
    return getIngestStudyMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mdt.integration.grpc.StudyReceivedEvent,
      com.mdt.integration.grpc.Ack> getOnStudyReceivedMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OnStudyReceived",
      requestType = com.mdt.integration.grpc.StudyReceivedEvent.class,
      responseType = com.mdt.integration.grpc.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.integration.grpc.StudyReceivedEvent,
      com.mdt.integration.grpc.Ack> getOnStudyReceivedMethod() {
    io.grpc.MethodDescriptor<com.mdt.integration.grpc.StudyReceivedEvent, com.mdt.integration.grpc.Ack> getOnStudyReceivedMethod;
    if ((getOnStudyReceivedMethod = IntegrationServiceGrpc.getOnStudyReceivedMethod) == null) {
      synchronized (IntegrationServiceGrpc.class) {
        if ((getOnStudyReceivedMethod = IntegrationServiceGrpc.getOnStudyReceivedMethod) == null) {
          IntegrationServiceGrpc.getOnStudyReceivedMethod = getOnStudyReceivedMethod =
              io.grpc.MethodDescriptor.<com.mdt.integration.grpc.StudyReceivedEvent, com.mdt.integration.grpc.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OnStudyReceived"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.integration.grpc.StudyReceivedEvent.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.integration.grpc.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new IntegrationServiceMethodDescriptorSupplier("OnStudyReceived"))
              .build();
        }
      }
    }
    return getOnStudyReceivedMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mdt.integration.grpc.ClinicalDataRequest,
      com.mdt.integration.grpc.ClinicalDataResponse> getFetchClinicalDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "FetchClinicalData",
      requestType = com.mdt.integration.grpc.ClinicalDataRequest.class,
      responseType = com.mdt.integration.grpc.ClinicalDataResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.integration.grpc.ClinicalDataRequest,
      com.mdt.integration.grpc.ClinicalDataResponse> getFetchClinicalDataMethod() {
    io.grpc.MethodDescriptor<com.mdt.integration.grpc.ClinicalDataRequest, com.mdt.integration.grpc.ClinicalDataResponse> getFetchClinicalDataMethod;
    if ((getFetchClinicalDataMethod = IntegrationServiceGrpc.getFetchClinicalDataMethod) == null) {
      synchronized (IntegrationServiceGrpc.class) {
        if ((getFetchClinicalDataMethod = IntegrationServiceGrpc.getFetchClinicalDataMethod) == null) {
          IntegrationServiceGrpc.getFetchClinicalDataMethod = getFetchClinicalDataMethod =
              io.grpc.MethodDescriptor.<com.mdt.integration.grpc.ClinicalDataRequest, com.mdt.integration.grpc.ClinicalDataResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "FetchClinicalData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.integration.grpc.ClinicalDataRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.integration.grpc.ClinicalDataResponse.getDefaultInstance()))
              .setSchemaDescriptor(new IntegrationServiceMethodDescriptorSupplier("FetchClinicalData"))
              .build();
        }
      }
    }
    return getFetchClinicalDataMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static IntegrationServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IntegrationServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IntegrationServiceStub>() {
        @java.lang.Override
        public IntegrationServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IntegrationServiceStub(channel, callOptions);
        }
      };
    return IntegrationServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static IntegrationServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IntegrationServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IntegrationServiceBlockingStub>() {
        @java.lang.Override
        public IntegrationServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IntegrationServiceBlockingStub(channel, callOptions);
        }
      };
    return IntegrationServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static IntegrationServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<IntegrationServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<IntegrationServiceFutureStub>() {
        @java.lang.Override
        public IntegrationServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new IntegrationServiceFutureStub(channel, callOptions);
        }
      };
    return IntegrationServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void pullStudyFromPacs(com.mdt.integration.grpc.PullStudyRequest request,
        io.grpc.stub.StreamObserver<com.mdt.integration.grpc.PullStudyResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPullStudyFromPacsMethod(), responseObserver);
    }

    /**
     */
    default void ingestStudy(com.mdt.integration.grpc.IngestStudyRequest request,
        io.grpc.stub.StreamObserver<com.mdt.integration.grpc.Ack> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getIngestStudyMethod(), responseObserver);
    }

    /**
     */
    default void onStudyReceived(com.mdt.integration.grpc.StudyReceivedEvent request,
        io.grpc.stub.StreamObserver<com.mdt.integration.grpc.Ack> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getOnStudyReceivedMethod(), responseObserver);
    }

    /**
     */
    default void fetchClinicalData(com.mdt.integration.grpc.ClinicalDataRequest request,
        io.grpc.stub.StreamObserver<com.mdt.integration.grpc.ClinicalDataResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getFetchClinicalDataMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service IntegrationService.
   */
  public static abstract class IntegrationServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return IntegrationServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service IntegrationService.
   */
  public static final class IntegrationServiceStub
      extends io.grpc.stub.AbstractAsyncStub<IntegrationServiceStub> {
    private IntegrationServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IntegrationServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IntegrationServiceStub(channel, callOptions);
    }

    /**
     */
    public void pullStudyFromPacs(com.mdt.integration.grpc.PullStudyRequest request,
        io.grpc.stub.StreamObserver<com.mdt.integration.grpc.PullStudyResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPullStudyFromPacsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void ingestStudy(com.mdt.integration.grpc.IngestStudyRequest request,
        io.grpc.stub.StreamObserver<com.mdt.integration.grpc.Ack> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getIngestStudyMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void onStudyReceived(com.mdt.integration.grpc.StudyReceivedEvent request,
        io.grpc.stub.StreamObserver<com.mdt.integration.grpc.Ack> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getOnStudyReceivedMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void fetchClinicalData(com.mdt.integration.grpc.ClinicalDataRequest request,
        io.grpc.stub.StreamObserver<com.mdt.integration.grpc.ClinicalDataResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getFetchClinicalDataMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service IntegrationService.
   */
  public static final class IntegrationServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<IntegrationServiceBlockingStub> {
    private IntegrationServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IntegrationServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IntegrationServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.mdt.integration.grpc.PullStudyResponse pullStudyFromPacs(com.mdt.integration.grpc.PullStudyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPullStudyFromPacsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mdt.integration.grpc.Ack ingestStudy(com.mdt.integration.grpc.IngestStudyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getIngestStudyMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mdt.integration.grpc.Ack onStudyReceived(com.mdt.integration.grpc.StudyReceivedEvent request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getOnStudyReceivedMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mdt.integration.grpc.ClinicalDataResponse fetchClinicalData(com.mdt.integration.grpc.ClinicalDataRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getFetchClinicalDataMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service IntegrationService.
   */
  public static final class IntegrationServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<IntegrationServiceFutureStub> {
    private IntegrationServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected IntegrationServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new IntegrationServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.integration.grpc.PullStudyResponse> pullStudyFromPacs(
        com.mdt.integration.grpc.PullStudyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPullStudyFromPacsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.integration.grpc.Ack> ingestStudy(
        com.mdt.integration.grpc.IngestStudyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getIngestStudyMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.integration.grpc.Ack> onStudyReceived(
        com.mdt.integration.grpc.StudyReceivedEvent request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getOnStudyReceivedMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.integration.grpc.ClinicalDataResponse> fetchClinicalData(
        com.mdt.integration.grpc.ClinicalDataRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getFetchClinicalDataMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PULL_STUDY_FROM_PACS = 0;
  private static final int METHODID_INGEST_STUDY = 1;
  private static final int METHODID_ON_STUDY_RECEIVED = 2;
  private static final int METHODID_FETCH_CLINICAL_DATA = 3;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PULL_STUDY_FROM_PACS:
          serviceImpl.pullStudyFromPacs((com.mdt.integration.grpc.PullStudyRequest) request,
              (io.grpc.stub.StreamObserver<com.mdt.integration.grpc.PullStudyResponse>) responseObserver);
          break;
        case METHODID_INGEST_STUDY:
          serviceImpl.ingestStudy((com.mdt.integration.grpc.IngestStudyRequest) request,
              (io.grpc.stub.StreamObserver<com.mdt.integration.grpc.Ack>) responseObserver);
          break;
        case METHODID_ON_STUDY_RECEIVED:
          serviceImpl.onStudyReceived((com.mdt.integration.grpc.StudyReceivedEvent) request,
              (io.grpc.stub.StreamObserver<com.mdt.integration.grpc.Ack>) responseObserver);
          break;
        case METHODID_FETCH_CLINICAL_DATA:
          serviceImpl.fetchClinicalData((com.mdt.integration.grpc.ClinicalDataRequest) request,
              (io.grpc.stub.StreamObserver<com.mdt.integration.grpc.ClinicalDataResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getPullStudyFromPacsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.integration.grpc.PullStudyRequest,
              com.mdt.integration.grpc.PullStudyResponse>(
                service, METHODID_PULL_STUDY_FROM_PACS)))
        .addMethod(
          getIngestStudyMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.integration.grpc.IngestStudyRequest,
              com.mdt.integration.grpc.Ack>(
                service, METHODID_INGEST_STUDY)))
        .addMethod(
          getOnStudyReceivedMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.integration.grpc.StudyReceivedEvent,
              com.mdt.integration.grpc.Ack>(
                service, METHODID_ON_STUDY_RECEIVED)))
        .addMethod(
          getFetchClinicalDataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.integration.grpc.ClinicalDataRequest,
              com.mdt.integration.grpc.ClinicalDataResponse>(
                service, METHODID_FETCH_CLINICAL_DATA)))
        .build();
  }

  private static abstract class IntegrationServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    IntegrationServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.mdt.integration.grpc.Integration.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("IntegrationService");
    }
  }

  private static final class IntegrationServiceFileDescriptorSupplier
      extends IntegrationServiceBaseDescriptorSupplier {
    IntegrationServiceFileDescriptorSupplier() {}
  }

  private static final class IntegrationServiceMethodDescriptorSupplier
      extends IntegrationServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    IntegrationServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (IntegrationServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new IntegrationServiceFileDescriptorSupplier())
              .addMethod(getPullStudyFromPacsMethod())
              .addMethod(getIngestStudyMethod())
              .addMethod(getOnStudyReceivedMethod())
              .addMethod(getFetchClinicalDataMethod())
              .build();
        }
      }
    }
    return result;
  }
}

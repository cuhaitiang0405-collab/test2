package com.mdt.collab.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: collab.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class CollabServiceGrpc {

  private CollabServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "mdt.collab.CollabService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.mdt.collab.grpc.JoinRoomRequest,
      com.mdt.collab.grpc.JoinRoomResponse> getJoinRoomMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "JoinRoom",
      requestType = com.mdt.collab.grpc.JoinRoomRequest.class,
      responseType = com.mdt.collab.grpc.JoinRoomResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.collab.grpc.JoinRoomRequest,
      com.mdt.collab.grpc.JoinRoomResponse> getJoinRoomMethod() {
    io.grpc.MethodDescriptor<com.mdt.collab.grpc.JoinRoomRequest, com.mdt.collab.grpc.JoinRoomResponse> getJoinRoomMethod;
    if ((getJoinRoomMethod = CollabServiceGrpc.getJoinRoomMethod) == null) {
      synchronized (CollabServiceGrpc.class) {
        if ((getJoinRoomMethod = CollabServiceGrpc.getJoinRoomMethod) == null) {
          CollabServiceGrpc.getJoinRoomMethod = getJoinRoomMethod =
              io.grpc.MethodDescriptor.<com.mdt.collab.grpc.JoinRoomRequest, com.mdt.collab.grpc.JoinRoomResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "JoinRoom"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.collab.grpc.JoinRoomRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.collab.grpc.JoinRoomResponse.getDefaultInstance()))
              .setSchemaDescriptor(new CollabServiceMethodDescriptorSupplier("JoinRoom"))
              .build();
        }
      }
    }
    return getJoinRoomMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mdt.collab.grpc.AnnotationMessage,
      com.mdt.collab.grpc.Ack> getPushAnnotationMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PushAnnotation",
      requestType = com.mdt.collab.grpc.AnnotationMessage.class,
      responseType = com.mdt.collab.grpc.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.collab.grpc.AnnotationMessage,
      com.mdt.collab.grpc.Ack> getPushAnnotationMethod() {
    io.grpc.MethodDescriptor<com.mdt.collab.grpc.AnnotationMessage, com.mdt.collab.grpc.Ack> getPushAnnotationMethod;
    if ((getPushAnnotationMethod = CollabServiceGrpc.getPushAnnotationMethod) == null) {
      synchronized (CollabServiceGrpc.class) {
        if ((getPushAnnotationMethod = CollabServiceGrpc.getPushAnnotationMethod) == null) {
          CollabServiceGrpc.getPushAnnotationMethod = getPushAnnotationMethod =
              io.grpc.MethodDescriptor.<com.mdt.collab.grpc.AnnotationMessage, com.mdt.collab.grpc.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PushAnnotation"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.collab.grpc.AnnotationMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.collab.grpc.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new CollabServiceMethodDescriptorSupplier("PushAnnotation"))
              .build();
        }
      }
    }
    return getPushAnnotationMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mdt.collab.grpc.QualityReport,
      com.mdt.collab.grpc.Ack> getReportQualityMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReportQuality",
      requestType = com.mdt.collab.grpc.QualityReport.class,
      responseType = com.mdt.collab.grpc.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.collab.grpc.QualityReport,
      com.mdt.collab.grpc.Ack> getReportQualityMethod() {
    io.grpc.MethodDescriptor<com.mdt.collab.grpc.QualityReport, com.mdt.collab.grpc.Ack> getReportQualityMethod;
    if ((getReportQualityMethod = CollabServiceGrpc.getReportQualityMethod) == null) {
      synchronized (CollabServiceGrpc.class) {
        if ((getReportQualityMethod = CollabServiceGrpc.getReportQualityMethod) == null) {
          CollabServiceGrpc.getReportQualityMethod = getReportQualityMethod =
              io.grpc.MethodDescriptor.<com.mdt.collab.grpc.QualityReport, com.mdt.collab.grpc.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReportQuality"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.collab.grpc.QualityReport.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.collab.grpc.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new CollabServiceMethodDescriptorSupplier("ReportQuality"))
              .build();
        }
      }
    }
    return getReportQualityMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static CollabServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollabServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollabServiceStub>() {
        @java.lang.Override
        public CollabServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollabServiceStub(channel, callOptions);
        }
      };
    return CollabServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static CollabServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollabServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollabServiceBlockingStub>() {
        @java.lang.Override
        public CollabServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollabServiceBlockingStub(channel, callOptions);
        }
      };
    return CollabServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static CollabServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<CollabServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<CollabServiceFutureStub>() {
        @java.lang.Override
        public CollabServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new CollabServiceFutureStub(channel, callOptions);
        }
      };
    return CollabServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void joinRoom(com.mdt.collab.grpc.JoinRoomRequest request,
        io.grpc.stub.StreamObserver<com.mdt.collab.grpc.JoinRoomResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getJoinRoomMethod(), responseObserver);
    }

    /**
     */
    default void pushAnnotation(com.mdt.collab.grpc.AnnotationMessage request,
        io.grpc.stub.StreamObserver<com.mdt.collab.grpc.Ack> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getPushAnnotationMethod(), responseObserver);
    }

    /**
     */
    default void reportQuality(com.mdt.collab.grpc.QualityReport request,
        io.grpc.stub.StreamObserver<com.mdt.collab.grpc.Ack> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getReportQualityMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service CollabService.
   */
  public static abstract class CollabServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return CollabServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service CollabService.
   */
  public static final class CollabServiceStub
      extends io.grpc.stub.AbstractAsyncStub<CollabServiceStub> {
    private CollabServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollabServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollabServiceStub(channel, callOptions);
    }

    /**
     */
    public void joinRoom(com.mdt.collab.grpc.JoinRoomRequest request,
        io.grpc.stub.StreamObserver<com.mdt.collab.grpc.JoinRoomResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getJoinRoomMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void pushAnnotation(com.mdt.collab.grpc.AnnotationMessage request,
        io.grpc.stub.StreamObserver<com.mdt.collab.grpc.Ack> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getPushAnnotationMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void reportQuality(com.mdt.collab.grpc.QualityReport request,
        io.grpc.stub.StreamObserver<com.mdt.collab.grpc.Ack> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getReportQualityMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service CollabService.
   */
  public static final class CollabServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<CollabServiceBlockingStub> {
    private CollabServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollabServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollabServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.mdt.collab.grpc.JoinRoomResponse joinRoom(com.mdt.collab.grpc.JoinRoomRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getJoinRoomMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mdt.collab.grpc.Ack pushAnnotation(com.mdt.collab.grpc.AnnotationMessage request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getPushAnnotationMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mdt.collab.grpc.Ack reportQuality(com.mdt.collab.grpc.QualityReport request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getReportQualityMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service CollabService.
   */
  public static final class CollabServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<CollabServiceFutureStub> {
    private CollabServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected CollabServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new CollabServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.collab.grpc.JoinRoomResponse> joinRoom(
        com.mdt.collab.grpc.JoinRoomRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getJoinRoomMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.collab.grpc.Ack> pushAnnotation(
        com.mdt.collab.grpc.AnnotationMessage request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getPushAnnotationMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.collab.grpc.Ack> reportQuality(
        com.mdt.collab.grpc.QualityReport request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getReportQualityMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_JOIN_ROOM = 0;
  private static final int METHODID_PUSH_ANNOTATION = 1;
  private static final int METHODID_REPORT_QUALITY = 2;

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
        case METHODID_JOIN_ROOM:
          serviceImpl.joinRoom((com.mdt.collab.grpc.JoinRoomRequest) request,
              (io.grpc.stub.StreamObserver<com.mdt.collab.grpc.JoinRoomResponse>) responseObserver);
          break;
        case METHODID_PUSH_ANNOTATION:
          serviceImpl.pushAnnotation((com.mdt.collab.grpc.AnnotationMessage) request,
              (io.grpc.stub.StreamObserver<com.mdt.collab.grpc.Ack>) responseObserver);
          break;
        case METHODID_REPORT_QUALITY:
          serviceImpl.reportQuality((com.mdt.collab.grpc.QualityReport) request,
              (io.grpc.stub.StreamObserver<com.mdt.collab.grpc.Ack>) responseObserver);
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
          getJoinRoomMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.collab.grpc.JoinRoomRequest,
              com.mdt.collab.grpc.JoinRoomResponse>(
                service, METHODID_JOIN_ROOM)))
        .addMethod(
          getPushAnnotationMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.collab.grpc.AnnotationMessage,
              com.mdt.collab.grpc.Ack>(
                service, METHODID_PUSH_ANNOTATION)))
        .addMethod(
          getReportQualityMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.collab.grpc.QualityReport,
              com.mdt.collab.grpc.Ack>(
                service, METHODID_REPORT_QUALITY)))
        .build();
  }

  private static abstract class CollabServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    CollabServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.mdt.collab.grpc.Collab.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("CollabService");
    }
  }

  private static final class CollabServiceFileDescriptorSupplier
      extends CollabServiceBaseDescriptorSupplier {
    CollabServiceFileDescriptorSupplier() {}
  }

  private static final class CollabServiceMethodDescriptorSupplier
      extends CollabServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    CollabServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (CollabServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new CollabServiceFileDescriptorSupplier())
              .addMethod(getJoinRoomMethod())
              .addMethod(getPushAnnotationMethod())
              .addMethod(getReportQualityMethod())
              .build();
        }
      }
    }
    return result;
  }
}

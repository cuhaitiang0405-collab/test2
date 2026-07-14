package com.mdt.auth.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: auth.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class AuthServiceGrpc {

  private AuthServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "mdt.auth.AuthService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.mdt.auth.grpc.FieldPermissionRequest,
      com.mdt.auth.grpc.FieldPermissionResponse> getCheckFieldPermissionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CheckFieldPermission",
      requestType = com.mdt.auth.grpc.FieldPermissionRequest.class,
      responseType = com.mdt.auth.grpc.FieldPermissionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.auth.grpc.FieldPermissionRequest,
      com.mdt.auth.grpc.FieldPermissionResponse> getCheckFieldPermissionMethod() {
    io.grpc.MethodDescriptor<com.mdt.auth.grpc.FieldPermissionRequest, com.mdt.auth.grpc.FieldPermissionResponse> getCheckFieldPermissionMethod;
    if ((getCheckFieldPermissionMethod = AuthServiceGrpc.getCheckFieldPermissionMethod) == null) {
      synchronized (AuthServiceGrpc.class) {
        if ((getCheckFieldPermissionMethod = AuthServiceGrpc.getCheckFieldPermissionMethod) == null) {
          AuthServiceGrpc.getCheckFieldPermissionMethod = getCheckFieldPermissionMethod =
              io.grpc.MethodDescriptor.<com.mdt.auth.grpc.FieldPermissionRequest, com.mdt.auth.grpc.FieldPermissionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CheckFieldPermission"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.auth.grpc.FieldPermissionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.auth.grpc.FieldPermissionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new AuthServiceMethodDescriptorSupplier("CheckFieldPermission"))
              .build();
        }
      }
    }
    return getCheckFieldPermissionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mdt.auth.grpc.AuditRecord,
      com.mdt.auth.grpc.Ack> getWriteAuditMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WriteAudit",
      requestType = com.mdt.auth.grpc.AuditRecord.class,
      responseType = com.mdt.auth.grpc.Ack.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mdt.auth.grpc.AuditRecord,
      com.mdt.auth.grpc.Ack> getWriteAuditMethod() {
    io.grpc.MethodDescriptor<com.mdt.auth.grpc.AuditRecord, com.mdt.auth.grpc.Ack> getWriteAuditMethod;
    if ((getWriteAuditMethod = AuthServiceGrpc.getWriteAuditMethod) == null) {
      synchronized (AuthServiceGrpc.class) {
        if ((getWriteAuditMethod = AuthServiceGrpc.getWriteAuditMethod) == null) {
          AuthServiceGrpc.getWriteAuditMethod = getWriteAuditMethod =
              io.grpc.MethodDescriptor.<com.mdt.auth.grpc.AuditRecord, com.mdt.auth.grpc.Ack>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WriteAudit"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.auth.grpc.AuditRecord.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mdt.auth.grpc.Ack.getDefaultInstance()))
              .setSchemaDescriptor(new AuthServiceMethodDescriptorSupplier("WriteAudit"))
              .build();
        }
      }
    }
    return getWriteAuditMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static AuthServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthServiceStub>() {
        @java.lang.Override
        public AuthServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthServiceStub(channel, callOptions);
        }
      };
    return AuthServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static AuthServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthServiceBlockingStub>() {
        @java.lang.Override
        public AuthServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthServiceBlockingStub(channel, callOptions);
        }
      };
    return AuthServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static AuthServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<AuthServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<AuthServiceFutureStub>() {
        @java.lang.Override
        public AuthServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new AuthServiceFutureStub(channel, callOptions);
        }
      };
    return AuthServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void checkFieldPermission(com.mdt.auth.grpc.FieldPermissionRequest request,
        io.grpc.stub.StreamObserver<com.mdt.auth.grpc.FieldPermissionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCheckFieldPermissionMethod(), responseObserver);
    }

    /**
     */
    default void writeAudit(com.mdt.auth.grpc.AuditRecord request,
        io.grpc.stub.StreamObserver<com.mdt.auth.grpc.Ack> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getWriteAuditMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service AuthService.
   */
  public static abstract class AuthServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return AuthServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service AuthService.
   */
  public static final class AuthServiceStub
      extends io.grpc.stub.AbstractAsyncStub<AuthServiceStub> {
    private AuthServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthServiceStub(channel, callOptions);
    }

    /**
     */
    public void checkFieldPermission(com.mdt.auth.grpc.FieldPermissionRequest request,
        io.grpc.stub.StreamObserver<com.mdt.auth.grpc.FieldPermissionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCheckFieldPermissionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void writeAudit(com.mdt.auth.grpc.AuditRecord request,
        io.grpc.stub.StreamObserver<com.mdt.auth.grpc.Ack> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getWriteAuditMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service AuthService.
   */
  public static final class AuthServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<AuthServiceBlockingStub> {
    private AuthServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.mdt.auth.grpc.FieldPermissionResponse checkFieldPermission(com.mdt.auth.grpc.FieldPermissionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCheckFieldPermissionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mdt.auth.grpc.Ack writeAudit(com.mdt.auth.grpc.AuditRecord request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getWriteAuditMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service AuthService.
   */
  public static final class AuthServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<AuthServiceFutureStub> {
    private AuthServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected AuthServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new AuthServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.auth.grpc.FieldPermissionResponse> checkFieldPermission(
        com.mdt.auth.grpc.FieldPermissionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCheckFieldPermissionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mdt.auth.grpc.Ack> writeAudit(
        com.mdt.auth.grpc.AuditRecord request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getWriteAuditMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_CHECK_FIELD_PERMISSION = 0;
  private static final int METHODID_WRITE_AUDIT = 1;

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
        case METHODID_CHECK_FIELD_PERMISSION:
          serviceImpl.checkFieldPermission((com.mdt.auth.grpc.FieldPermissionRequest) request,
              (io.grpc.stub.StreamObserver<com.mdt.auth.grpc.FieldPermissionResponse>) responseObserver);
          break;
        case METHODID_WRITE_AUDIT:
          serviceImpl.writeAudit((com.mdt.auth.grpc.AuditRecord) request,
              (io.grpc.stub.StreamObserver<com.mdt.auth.grpc.Ack>) responseObserver);
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
          getCheckFieldPermissionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.auth.grpc.FieldPermissionRequest,
              com.mdt.auth.grpc.FieldPermissionResponse>(
                service, METHODID_CHECK_FIELD_PERMISSION)))
        .addMethod(
          getWriteAuditMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mdt.auth.grpc.AuditRecord,
              com.mdt.auth.grpc.Ack>(
                service, METHODID_WRITE_AUDIT)))
        .build();
  }

  private static abstract class AuthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    AuthServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.mdt.auth.grpc.Auth.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("AuthService");
    }
  }

  private static final class AuthServiceFileDescriptorSupplier
      extends AuthServiceBaseDescriptorSupplier {
    AuthServiceFileDescriptorSupplier() {}
  }

  private static final class AuthServiceMethodDescriptorSupplier
      extends AuthServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    AuthServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (AuthServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new AuthServiceFileDescriptorSupplier())
              .addMethod(getCheckFieldPermissionMethod())
              .addMethod(getWriteAuditMethod())
              .build();
        }
      }
    }
    return result;
  }
}

$version: "2"

namespace com.cloud.provider

resource WorkerNode {
    identifiers: {
        workerId: WorkerId
    }

    put: RegisterWorker
    read: GetWorkerNode
    list: ListWorkerNodes

    operations: [
        HeartbeatWorker
        ConfigureWorkerNetworking
        GetWorkerMessageBindings
    ]
}

@idempotent
@internal
@http(method: "PUT", uri: "/workers/{workerId}", code: 200)
operation RegisterWorker {
    input: RegisterWorkerInput
    output: RegisterWorkerOutput
    errors: [BadRequestError HostExecutionError]
}

structure RegisterWorkerInput {
    /// Stable worker identifier. A simple implementation can use hostname.
    @httpLabel
    @required
    workerId: WorkerId

    @required
    hostname: Hostname

    rabbitMqHost: Hostname

    capacity: WorkerCapacity

    network: NetworkConfiguration

    baseImages: BaseImageList
}

structure RegisterWorkerOutput {
    @required
    workerId: WorkerId

    @required
    worker: WorkerNodeDescription
}

@readonly
@internal
@http(method: "GET", uri: "/workers/{workerId}", code: 200)
operation GetWorkerNode {
    input: GetWorkerNodeInput
    output: GetWorkerNodeOutput
    errors: [WorkerNotFoundError HostExecutionError]
}

structure GetWorkerNodeInput {
    @required
    @httpLabel
    workerId: WorkerId
}

structure GetWorkerNodeOutput {
    @required
    workerId: WorkerId

    @required
    worker: WorkerNodeDescription
}

@readonly
@internal
@http(method: "GET", uri: "/workers", code: 200)
operation ListWorkerNodes {
    input: ListWorkerNodesInput
    output: ListWorkerNodesOutput
    errors: [HostExecutionError]
}

structure ListWorkerNodesInput {}

structure ListWorkerNodesOutput {
    @required
    workers: WorkerNodeList
}

@idempotent
@internal
@http(method: "POST", uri: "/workers/{workerId}/heartbeat", code: 200)
operation HeartbeatWorker {
    input: HeartbeatWorkerInput
    output: HeartbeatWorkerOutput
    errors: [WorkerNotFoundError HostExecutionError]
}

structure HeartbeatWorkerInput {
    @required
    @httpLabel
    workerId: WorkerId

    @required
    status: WorkerStatus

    activeVmIds: VmIdList

    availableCapacity: WorkerCapacity

    observedAt: Timestamp
}

structure HeartbeatWorkerOutput {
    @required
    workerId: WorkerId

    @required
    accepted: Boolean
}

@idempotent
@internal
@http(method: "PUT", uri: "/workers/{workerId}/networking", code: 200)
operation ConfigureWorkerNetworking {
    input: ConfigureWorkerNetworkingInput
    output: ConfigureWorkerNetworkingOutput
    errors: [WorkerNotFoundError BadRequestError HostExecutionError]
}

structure ConfigureWorkerNetworkingInput {
    @required
    @httpLabel
    workerId: WorkerId

    @required
    network: NetworkConfiguration
}

structure ConfigureWorkerNetworkingOutput {
    @required
    workerId: WorkerId

    @required
    network: NetworkConfiguration
}

@readonly
@internal
@http(method: "GET", uri: "/workers/{workerId}/message-bindings", code: 200)
operation GetWorkerMessageBindings {
    input: GetWorkerMessageBindingsInput
    output: GetWorkerMessageBindingsOutput
    errors: [WorkerNotFoundError MessageBrokerUnavailableError]
}

structure GetWorkerMessageBindingsInput {
    @required
    @httpLabel
    workerId: WorkerId
}

structure GetWorkerMessageBindingsOutput {
    @required
    workerId: WorkerId

    @required
    bindings: MessageBindingList
}

structure WorkerNodeDescription {
    @required
    workerId: WorkerId

    @required
    hostname: Hostname

    @required
    status: WorkerStatus

    rabbitMqHost: Hostname

    capacity: WorkerCapacity

    availableCapacity: WorkerCapacity

    network: NetworkConfiguration

    queues: WorkerQueueConfiguration

    baseImages: BaseImageList

    activeVmIds: VmIdList

    lastHeartbeatAt: Timestamp
}

list WorkerNodeList {
    member: WorkerNodeDescription
}

enum WorkerStatus {
    STARTING = "starting"
    READY = "ready"
    DRAINING = "draining"
    OFFLINE = "offline"
    ERROR = "error"
}

structure WorkerCapacity {
    maxVms: NonNegativeInteger

    runningVms: NonNegativeInteger

    memoryMiB: MemoryMiB

    vcpuCount: VcpuCount
}

structure NetworkConfiguration {
    @required
    mode: NetworkMode

    /// Linux bridge used by TAP/VDE networking, for example `br0`.
    bridgeDevice: String

    /// Physical device enslaved to the bridge.
    physicalDevice: String

    /// TAP device used by a VDE switch, for example `tap-vde`.
    vdeTapDevice: String

    /// VDE control socket, for example `/tmp/vde.ctl`.
    vdeSocket: UnixPath

    /// Host CIDR address assigned to the bridge.
    hostAddressCidr: String

    /// Default gateway configured on the bridge.
    gateway: IpAddress

    /// User that QEMU is allowed to connect as when TAP mode is used.
    qemuUser: String
}

structure WorkerQueueConfiguration {
    /// Direct exchange used by the tutorial after message routing cleanup.
    @required
    exchange: String

    /// Shared queue used by competing consumers for `create-vm`.
    @required
    competingQueue: String

    /// Exclusive queue generated for this worker. It receives broadcast and
    /// owner-targeted commands.
    exclusiveQueue: String

    @required
    bindings: MessageBindingList
}

structure MessageBinding {
    @required
    command: CommandName

    @required
    routingKey: String

    @required
    delivery: MessageDelivery

    queue: String
}

list MessageBindingList {
    member: MessageBinding
}

enum MessageDelivery {
    COMPETING = "competing"
    BROADCAST = "broadcast"
    OWNER_WORKER = "owner-worker"
}

/// Tutorial command names used as both JSON `command` values and RabbitMQ
/// direct-exchange routing keys.
enum CommandName {
    CREATE_VM = "create-vm"
    START_VM = "start-vm"
    STOP_VM = "stop-vm"
    DELETE_VM = "delete-vm"
    LIST_VMS = "list-vms"
}

/// Raw AMQP command envelope from the tutorial. Generated code can prefer the
/// typed API operations; this shape documents the wire contract used by the
/// Python clients and workers.
structure CommandEnvelope {
    @required
    command: CommandName

    options: CommandOptions

    /// RabbitMQ correlation id when Direct Reply-To is used.
    correlationId: CorrelationId

    /// Usually `amq.rabbitmq.reply-to` for request/reply commands.
    replyTo: String
}

union CommandOptions {
    createVm: CreateVmOptions
    vm: VmIdOptions
    stopVm: StopVmOptions
    listVms: ListVmsOptions
}

structure CreateVmOptions {
    @required
    image: ImageName
}

@references(
    [{
       resource : VirtualMachine
       }
    ]
)

@references(
    [{
       resource : VirtualMachine
       }
    ]
)
structure VmIdOptions {
    @required
    @jsonName("vm-id")
    vmId: VmId
}

@references(
    [{
       resource : VirtualMachine
       }
    ]
)
structure StopVmOptions {
    @required
    @jsonName("vm-id")
    vmId: VmId

    kill: Boolean
}

structure ListVmsOptions {}

/// Raw response wrapper used by workers. `response` is deliberately modeled as
/// a document because the tutorial returns different JSON shapes depending on
/// the command: a VM action object, a list of VM summaries, or no payload.
structure CommandResponseEnvelope {
    @required
    @jsonName("execution-info")
    executionInfo: ExecutionInfo

    response: CommandResponsePayload
}

document CommandResponsePayload

enum ExecutionStatus {
    SUCCESS = "success"
    ERROR = "error"
}

structure ExecutionInfo {
    @required
    status: ExecutionStatus

    reason: HumanMessage
}

/// Typed version of the possible response bodies, useful for non-Python
/// clients that do not want to work with `document`.
union TypedCommandResponse {
    vmAction: VmActionResult
    vmList: VmSummaryList
    vm: Vm
    ipAddresses: IpAddressList
    empty: Empty
}

structure Empty {}

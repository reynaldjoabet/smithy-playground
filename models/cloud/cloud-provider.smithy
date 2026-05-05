$version: "2"

namespace com.cloud.provider
use com.cloud.provider.amqp#amqp

resource VirtualMachine {
    identifiers: {
        vmId: VmId
    }

    create: CreateVm
    read: GetVm
    list: ListVms
    delete: DeleteVm

    operations: [
        StartVm
        StopVm
        GetVmIpAddresses
    ]
}

/// Base images are qcow2 backing images used to create per-VM copy-on-write
/// user images.
resource BaseImage {
    identifiers: {
        imageName: ImageName
    }

    read: GetBaseImage
    list: ListBaseImages
}

@http(method: "POST", uri: "/vms", code: 202)
@amqp(
    exchange: "computing"
    routingKey: "create-vm"
    target: "competing"
    queue: "computing-competing"
    reply: {
        directReplyTo: true
        required: true
        timeoutSeconds: 30
        multipleResponses: false
    }
)
operation CreateVm {
    input: CreateVmInput
    output: CreateVmOutput
    errors: [
        BadRequestError
        ImageNotFoundError
        CapacityUnavailableError
        HostExecutionError
        MessageBrokerUnavailableError
    ]
}

structure CreateVmInput {
    /// Name of the base qcow2 image, for example `ubuntu_base` or `rabbitmq`.
    @required
    image: ImageName

    /// Optional idempotency token for API frontends. The tutorial does not
    /// persist request tokens, but production frontends should.
    idempotencyToken: IdempotencyToken

    /// Resource shape extensions that map to QEMU flags. The tutorial uses
    /// 4096 MiB and 4 vCPUs in examples, but the model keeps these configurable.
    memoryMiB: MemoryMiB

    vcpuCount: VcpuCount

    /// Public networking is implemented via TAP/VDE bridged networking.
    publicNetwork: Boolean

    /// Whether the base image is expected to run the QEMU Guest Agent. When it
    /// does, list/get operations can return IP addresses.
    guestAgent: Boolean

    tags: Tags
}

structure CreateVmOutput {
    @required
    vmId: VmId

    @required
    vm: Vm
}

@readonly
@http(method: "GET", uri: "/vms/{vmId}", code: 200)
@amqp(
    exchange: "computing"
    routingKey: "list-vms"
    target: "derived"
    reply: {
        directReplyTo: true
        required: true
        timeoutSeconds: 5
        multipleResponses: true
    }
)
operation GetVm {
    input: GetVmInput
    output: GetVmOutput
    errors: [
        VmNotFoundError
        MessageBrokerUnavailableError
        HostExecutionError
        GuestAgentUnavailableError
    ]
}

structure GetVmInput {
    @required
    @httpLabel
    vmId: VmId

    /// Include addresses fetched through the QEMU Guest Agent when available.
    @httpQuery("includeIpAddresses")
    includeIpAddresses: Boolean
}

structure GetVmOutput {
    @required
    vmId: VmId

    @required
    vm: Vm

    partialFailures: WorkerFailureList
}

@readonly
@http(method: "GET", uri: "/vms", code: 200)
@amqp(
    exchange: "computing"
    routingKey: "list-vms"
    target: "broadcast"
    reply: {
        directReplyTo: true
        required: true
        timeoutSeconds: 5
        multipleResponses: true
    }
)
operation ListVms {
    input: ListVmsInput
    output: ListVmsOutput
    errors: [
        MessageBrokerUnavailableError
        HostExecutionError
    ]
}

structure ListVmsInput {
    /// The tutorial only lists running processes. A production implementation
    /// can set this to true and also inspect installed systemd units.
    @httpQuery("includeStopped")
    includeStopped: Boolean

    /// Include addresses fetched through the QEMU Guest Agent when available.
    @httpQuery("includeIpAddresses")
    includeIpAddresses: Boolean
}

structure ListVmsOutput {
    @required
    vms: VmSummaryList

    /// Errors returned by workers that participated in a broadcast command.
    partialFailures: WorkerFailureList
}

@idempotent
@http(method: "POST", uri: "/vms/{vmId}/start", code: 202)
@amqp(
    exchange: "computing"
    routingKey: "start-vm"
    target: "owner-worker"
    reply: {
        directReplyTo: true
        required: true
        timeoutSeconds: 10
        multipleResponses: true
    }
)
operation StartVm {
    input: StartVmInput
    output: StartVmOutput
    errors: [
        VmNotFoundError
        VmStateConflictError
        MessageBrokerUnavailableError
        HostExecutionError
    ]
}

structure StartVmInput {
    @required
    @httpLabel
    vmId: VmId
}

structure StartVmOutput {
    @required
    vmId: VmId

    @required
    result: VmActionResult

    partialFailures: WorkerFailureList
}

@idempotent
@http(method: "POST", uri: "/vms/{vmId}/stop", code: 202)
@amqp(
    exchange: "computing"
    routingKey: "stop-vm"
    target: "owner-worker"
    reply: {
        directReplyTo: true
        required: true
        timeoutSeconds: 10
        multipleResponses: true
    }
)
operation StopVm {
    input: StopVmInput
    output: StopVmOutput
    errors: [
        VmNotFoundError
        VmStateConflictError
        MessageBrokerUnavailableError
        HostExecutionError
        GuestAgentUnavailableError
    ]
}

structure StopVmInput {
    @required
    @httpLabel
    vmId: VmId

    /// True maps to the tutorial's `kill` flag. False means graceful shutdown
    /// using QMP `system_powerdown`.
    kill: Boolean

    /// Optional production extension: after this many seconds a graceful stop
    /// can fall back to a kill.
    gracefulTimeoutSeconds: Seconds
}

structure StopVmOutput {
    @required
    vmId: VmId

    @required
    result: VmActionResult

    partialFailures: WorkerFailureList
}

@idempotent
@http(method: "DELETE", uri: "/vms/{vmId}", code: 200)
@amqp(
    exchange: "computing"
    routingKey: "delete-vm"
    target: "owner-worker"
    reply: {
        directReplyTo: true
        required: true
        timeoutSeconds: 10
        multipleResponses: true
    }
)
operation DeleteVm {
    input: DeleteVmInput
    output: DeleteVmOutput
    errors: [
        VmNotFoundError
        MessageBrokerUnavailableError
        HostExecutionError
    ]
}

structure DeleteVmInput {
    @required
    @httpLabel
    vmId: VmId
}

structure DeleteVmOutput {
    @required
    vmId: VmId

    @required
    result: VmActionResult

    partialFailures: WorkerFailureList
}

@readonly
@http(method: "GET", uri: "/vms/{vmId}/ip-addresses", code: 200)
@amqp(
    exchange: "computing"
    routingKey: "list-vms"
    target: "derived"
    reply: {
        directReplyTo: true
        required: true
        timeoutSeconds: 5
        multipleResponses: true
    }
)
operation GetVmIpAddresses {
    input: GetVmIpAddressesInput
    output: GetVmIpAddressesOutput
    errors: [
        VmNotFoundError
        GuestAgentUnavailableError
        MessageBrokerUnavailableError
        HostExecutionError
    ]
}

structure GetVmIpAddressesInput {
    @required
    @httpLabel
    vmId: VmId
}

structure GetVmIpAddressesOutput {
    @required
    vmId: VmId

    @required
    ipAddresses: IpAddressList

    interfaces: VmNetworkInterfaceList

    hint: HumanMessage
}

@readonly
@http(method: "GET", uri: "/images", code: 200)
operation ListBaseImages {
    input: ListBaseImagesInput
    output: ListBaseImagesOutput
    errors: [HostExecutionError]
}

structure ListBaseImagesInput {}

structure ListBaseImagesOutput {
    @required
    images: BaseImageList
}

@readonly
@http(method: "GET", uri: "/images/{imageName}", code: 200)
operation GetBaseImage {
    input: GetBaseImageInput
    output: GetBaseImageOutput
    errors: [ImageNotFoundError HostExecutionError]
}

structure GetBaseImageInput {
    @required
    @httpLabel
    imageName: ImageName
}

structure GetBaseImageOutput {
    @required
    imageName: ImageName

    @required
    image: BaseImageDescription
}

@references(
    [{
       resource : WorkerNode
       }
    ]
)
structure Vm {
    @required
    vmId: VmId

    @required
    image: ImageName

    @required
    state: VmState

    workerId: WorkerId

    processName: String

    systemdUnit: String

    qmpSocket: UnixPath

    guestAgentSocket: UnixPath

    macAddress: MacAddress

    ipAddresses: IpAddressList

    interfaces: VmNetworkInterfaceList

    createdAt: Timestamp

    updatedAt: Timestamp

    tags: Tags

    hint: HumanMessage
}

@references(
    [{
       resource : WorkerNode
       }
    ]
)
structure VmSummary {
    @required
    vmId: VmId

    @required
    state: VmState

    image: ImageName

    workerId: WorkerId

    ipAddresses: IpAddressList

    hint: HumanMessage,
    pid: Integer
}

list VmSummaryList {
    member: VmSummary
}

structure BaseImageDescription {
    @required
    imageName: ImageName

    /// Path to the qcow2 backing image on the worker.
    path: UnixPath

    osFamily: String

    osVersion: String

    supportsGuestAgent: Boolean

    description: HumanMessage

    tags: Tags
}

list BaseImageList {
    member: BaseImageDescription
}

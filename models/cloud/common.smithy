$version: "2"

namespace com.cloud.provider

/// VM identifiers are eight lower-case characters in the tutorial so that
/// derived Linux interface and process names stay below kernel name limits.
@length(min: 8, max: 8)
@pattern("^[a-z]{8}$")
string VmId

@length(min: 1, max: 128)
@pattern("^[A-Za-z0-9._-]+$")
string ImageName

@length(min: 1, max: 128)
@pattern("^[A-Za-z0-9._-]+$")
string WorkerId

@length(min: 1, max: 128)
@pattern("^[A-Za-z0-9._-]+$")
string BrokerId

@length(min: 1, max: 128)
@pattern("^[A-Za-z0-9._-]+$")
string Hostname

@length(min: 1, max: 4096)
string UnixPath

@length(min: 1, max: 512)
string Url

@length(min: 1, max: 512)
string Uri

@length(min: 1, max: 2048)
string HumanMessage

@length(min: 1, max: 128)
string IdempotencyToken

@length(min: 1, max: 128)
string CorrelationId

@length(min: 1, max: 64)
@pattern("^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$")
string MacAddress

@length(min: 2, max: 64)
string IpAddress

@range(min: 0, max: 128)
integer IpPrefixLength

@range(min: 1, max: 65535)
integer Port

@range(min: 1, max: 1048576)
integer MemoryMiB

@range(min: 1, max: 512)
integer VcpuCount

@range(min: 0, max: 31536000)
integer Seconds

@range(min: 0, max: 1000000)
integer NonNegativeInteger

@length(min: 1, max: 128)
string TagKey

@length(min: 0, max: 256)
string TagValue

map Tags {
    key: TagKey
    value: TagValue
}

list IpAddressList {
    member: IpAddress
}

list UrlList {
    member: Url
}

list VmIdList {
    member: VmId
}

/// Lifecycle states exposed to clients. The tutorial initially returns
/// `starting`, `killed`, `stopped`, and `deleted`; the extra values allow the
/// API to grow without changing the state member type.
enum VmState {
    STARTING = "starting"
    RUNNING = "running"
    STOPPING = "stopping"
    STOPPED = "stopped"
    KILLED = "killed"
    DELETED = "deleted"
    CRASHED = "crashed"
    UNKNOWN = "unknown"
}

enum IpAddressType {
    IPV4 = "ipv4"
    IPV6 = "ipv6"
    UNKNOWN = "unknown"
}

enum NetworkMode {
    /// QEMU TAP per VM, attached to a bridge.
    TAP = "tap"

    /// VDE switch backed by a TAP device and shared by QEMU guests.
    VDE = "vde"

    /// QEMU slirp/user networking; useful for local tests but not externally
    /// reachable.
    SLIRP = "slirp"
}

enum ShutdownMode {
    /// Send `system_powerdown` through QMP and let the guest OS shut down.
    GRACEFUL = "graceful"

    /// Stop the systemd unit/QEMU process immediately.
    KILL = "kill"
}

/// Common success payload for VM lifecycle mutations.
structure VmActionResult {
    @required
    vmId: VmId

    @required
    state: VmState

    hint: HumanMessage
}

structure VmNetworkAddress {
    @required
    address: IpAddress

    type: IpAddressType

    prefixLength: IpPrefixLength
}

list VmNetworkAddressList {
    member: VmNetworkAddress
}

structure VmNetworkInterface {
    @required
    name: String

    hardwareAddress: MacAddress

    addresses: VmNetworkAddressList
}

list VmNetworkInterfaceList {
    member: VmNetworkInterface
}

/// Partial worker failures are expected for broadcast commands in the tutorial:
/// for example, only the owner worker can start/stop/delete a specific VM.
@references(
    [{
       resource : WorkerNode
       }
    ]
)
structure WorkerFailure {
    workerId: WorkerId

    @required
    message: HumanMessage
}

list WorkerFailureList {
    member: WorkerFailure
}

@error("client")
@httpError(400)
structure BadRequestError {
    @required
    message: HumanMessage

    field: String
}

@error("client")
@httpError(404)
structure VmNotFoundError {
    @required
    message: HumanMessage

    vmId: VmId
}

@error("client")
@httpError(404)
structure ImageNotFoundError {
    @required
    message: HumanMessage

    image: ImageName
}

@error("client")
@httpError(404)
structure WorkerNotFoundError {
    @required
    message: HumanMessage

    workerId: WorkerId
}

@error("client")
@httpError(404)
structure BrokerNotFoundError {
    @required
    message: HumanMessage

    brokerId: BrokerId
}

@error("client")
@httpError(409)
structure VmStateConflictError {
    @required
    message: HumanMessage

    vmId: VmId

    currentState: VmState
}

@error("server")
@httpError(500)
structure HostExecutionError {
    @required
    message: HumanMessage

    workerId: WorkerId
}

@error("server")
@httpError(503)
structure CapacityUnavailableError {
    @required
    message: HumanMessage
}

@error("server")
@httpError(503)
structure MessageBrokerUnavailableError {
    @required
    message: HumanMessage
}

@error("server")
@httpError(504)
structure GuestAgentUnavailableError {
    @required
    message: HumanMessage

    vmId: VmId
}

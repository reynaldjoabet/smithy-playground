$version: "2"

namespace com.cloud.provider

resource RabbitMqBroker {
    identifiers: {
        brokerId: BrokerId
    }

    create: ProvisionRabbitMqBroker
    read: GetRabbitMqBroker
    delete: DeleteRabbitMqBroker

    operations: [
        GetRabbitMqBrokerConnection
    ]
}

/// Create a VM from the `rabbitmq` base image, wait for it to acquire an IP
/// address via DHCP/Guest Agent, and return customer-facing connection URLs.
@http(method: "POST", uri: "/providers/rabbitmq/brokers", code: 202)
operation ProvisionRabbitMqBroker {
    input: ProvisionRabbitMqBrokerInput
    output: ProvisionRabbitMqBrokerOutput
    errors: [
        BadRequestError
        ImageNotFoundError
        CapacityUnavailableError
        MessageBrokerUnavailableError
        GuestAgentUnavailableError
        HostExecutionError
    ]
}

structure ProvisionRabbitMqBrokerInput {
    /// Base image to use. The tutorial example uses `rabbitmq`.
    image: ImageName

    idempotencyToken: IdempotencyToken

    /// Number of seconds the API frontend should poll for guest IP addresses
    /// before returning a provisioning response without URLs.
    waitForReadySeconds: Seconds

    tags: Tags
}

structure ProvisionRabbitMqBrokerOutput {
    @required
    brokerId: BrokerId

    @required
    broker: RabbitMqBrokerDescription
}

@readonly
@http(method: "GET", uri: "/providers/rabbitmq/brokers/{brokerId}", code: 200)
operation GetRabbitMqBroker {
    input: GetRabbitMqBrokerInput
    output: GetRabbitMqBrokerOutput
    errors: [BrokerNotFoundError HostExecutionError]
}

structure GetRabbitMqBrokerInput {
    @required
    @httpLabel
    brokerId: BrokerId
}

structure GetRabbitMqBrokerOutput {
    @required
    brokerId: BrokerId

    @required
    broker: RabbitMqBrokerDescription
}

@readonly
@http(method: "GET", uri: "/providers/rabbitmq/brokers/{brokerId}/connection", code: 200)
operation GetRabbitMqBrokerConnection {
    input: GetRabbitMqBrokerConnectionInput
    output: GetRabbitMqBrokerConnectionOutput
    errors: [BrokerNotFoundError GuestAgentUnavailableError HostExecutionError]
}

structure GetRabbitMqBrokerConnectionInput {
    @required
    @httpLabel
    brokerId: BrokerId
}

structure GetRabbitMqBrokerConnectionOutput {
    @required
    brokerId: BrokerId

    @required
    connection: RabbitMqConnection
}

@idempotent
@http(method: "DELETE", uri: "/providers/rabbitmq/brokers/{brokerId}", code: 200)
operation DeleteRabbitMqBroker {
    input: DeleteRabbitMqBrokerInput
    output: DeleteRabbitMqBrokerOutput
    errors: [BrokerNotFoundError HostExecutionError MessageBrokerUnavailableError]
}

structure DeleteRabbitMqBrokerInput {
    @required
    @httpLabel
    brokerId: BrokerId
}

structure DeleteRabbitMqBrokerOutput {
    @required
    brokerId: BrokerId

    @required
    vmId: VmId

    @required
    result: VmActionResult
}

structure RabbitMqBrokerDescription {
    @required
    brokerId: BrokerId

    @required
    vmId: VmId

    @required
    state: RabbitMqBrokerState

    connection: RabbitMqConnection

    createdAt: Timestamp

    updatedAt: Timestamp

    tags: Tags

    hint: HumanMessage
}

enum RabbitMqBrokerState {
    PROVISIONING = "provisioning"
    READY = "ready"
    STOPPED = "stopped"
    DELETED = "deleted"
    ERROR = "error"
}

structure RabbitMqConnection {
    /// AMQP endpoint URLs, usually port 5672.
    @required
    amqpUrls: UrlList

    /// HTTP management URLs, usually port 15672.
    @required
    managementUrls: UrlList

    /// Public addresses after filtering out loopback and link-local addresses.
    @required
    publicIpAddresses: IpAddressList

    username: String

    virtualHost: String
}

$version: "2"

namespace com.cloud.provider

use aws.protocols#restJson1

/// A small VM hosting control plane service that provisions RabbitMQ brokers and exposes their connection details to customers.
@restJson1
/// Python/RabbitMQ/QEMU tutorial series.
@title("Aetherscale Tutorial Control Plane")
@restJson1
service Aetherscale {
    version: "2020-12-25"

    resources: [
        VirtualMachine
        BaseImage
        WorkerNode
        RabbitMqBroker
    ]
}

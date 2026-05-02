$version: "2.0"

namespace unison.traits

// kafkaEvent

@trait(selector: "operation")
structure kafkaEvent {
    /// The name of the Kafka topic to publish to
    topic: String
    /// Optional key to partition the event by (e.g., employeeId)
    partitionKey: String
    /// Whether the event should be published synchronously (wait for confirmation)
    synchronous: Boolean = false
    /// Additional metadata to include with the event
    metadata: Metadata
}

map Metadata {
    key: String
    value: String
}

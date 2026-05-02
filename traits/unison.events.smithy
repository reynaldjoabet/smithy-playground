$version: "2.0"
namespace unison.events

/// Declares the domain events an operation emits upon success,
/// enabling code generators to produce event publishing code
/// and allowing architects to trace data flow from the model.
@trait(selector: "operation")
structure emitsEvents {
    events: EventDeclarationList
}

list EventDeclarationList {
    member: EventDeclaration
}

list StringList {
    member: String
}

structure EventDeclaration {
    /// Fully qualified event name
    eventType: String

    /// Event bus / topic this is published to
    destination: String

    /// Downstream consumers (for documentation and impact analysis)
    knownConsumers: StringList

    /// Whether the event must be delivered exactly-once (transactional outbox)
    deliveryGuarantee: DeliveryGuarantee

    /// Maximum acceptable latency for consumer processing
    slaMs: Integer
}

enum DeliveryGuarantee {
    AT_LEAST_ONCE = "AT_LEAST_ONCE"
    EXACTLY_ONCE = "EXACTLY_ONCE"
    BEST_EFFORT = "BEST_EFFORT"
}
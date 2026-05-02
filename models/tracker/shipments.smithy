$version: "2"
namespace unison.tracker.shipments

resource Shipment {
    identifiers: { trackingId: TrackingId }
    properties: {
        status: ShipmentStatus
        origin: Address
        destination: Address
        carrier: String
        estimatedDelivery: Timestamp
    }
    create: CreateShipment
    read: GetShipment
    update: UpdateShipment
    list: ListShipments
    operations: [CancelShipment]
    resources: [ShipmentEvent]
}

resource ShipmentEvent {
    identifiers: {
        trackingId: TrackingId
        eventId: EventId
    }
    read: GetShipmentEvent
    list: ListShipmentEvents
}

resource Customer {
    identifiers: { customerId: CustomerId }
    properties: {
        name: String
        email: Email
    }
    create: CreateCustomer
    read: GetCustomer
    update: UpdateCustomer
    delete: DeleteCustomer
    list: ListCustomers
}

@pattern("^1Z[A-Z0-9]{16}$")
string TrackingId

@pattern("^evt_[a-zA-Z0-9]{24}$")
string EventId

@pattern("^cust_[a-zA-Z0-9]{20}$")
string CustomerId

@pattern("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
@length(max: 254)
string Email

enum ShipmentStatus {
    PENDING
    IN_TRANSIT
    OUT_FOR_DELIVERY
    DELIVERED
    EXCEPTION
    CANCELLED
}

structure Address {
    @required
    line1: String

    line2: String

    @required
    city: String

    @required
    @length(min: 2, max: 2)
    stateCode: String

    @required
    @pattern("^[0-9]{5}(-[0-9]{4})?$")
    postalCode: String

    @required
    @length(min: 2, max: 2)
    countryCode: String
}

operation CreateShipment {
    input: CreateShipmentInput
    output: CreateShipmentOutput
}

structure CreateShipmentInput {
    @required
    origin: Address

    @required
    destination: Address

    @required
    carrier: String
}

structure CreateShipmentOutput {
    @required
    trackingId: TrackingId

    @required
    status: ShipmentStatus
}

@readonly
operation GetShipment {
    input: GetShipmentInput
    output: GetShipmentOutput
}

structure GetShipmentInput {
    @required
    trackingId: TrackingId
}

structure GetShipmentOutput {
    @required
    trackingId: TrackingId

    @required
    status: ShipmentStatus

    @required
    origin: Address

    @required
    destination: Address

    @required
    carrier: String

    estimatedDelivery: Timestamp
}

operation UpdateShipment {
    input: UpdateShipmentInput
    output: UpdateShipmentOutput
}

structure UpdateShipmentInput {
    @required
    trackingId: TrackingId

    status: ShipmentStatus

    estimatedDelivery: Timestamp
}

structure UpdateShipmentOutput {
    @required
    trackingId: TrackingId

    @required
    status: ShipmentStatus
}

@readonly
operation ListShipments {
    input: ListShipmentsInput
    output: ListShipmentsOutput
}

structure ListShipmentsInput {
}

structure ListShipmentsOutput {
    @required
    shipments: ShipmentList
}

list ShipmentList {
    member: GetShipmentOutput
}

operation CancelShipment {
    input: CancelShipmentInput
    output: CancelShipmentOutput
}

structure CancelShipmentInput {
    @required
    trackingId: TrackingId
}

structure CancelShipmentOutput {
    @required
    trackingId: TrackingId

    @required
    status: ShipmentStatus
}

@readonly
operation GetShipmentEvent {
    input: GetShipmentEventInput
    output: GetShipmentEventOutput
}

@references([
    { resource: ShipmentEvent }
])
structure GetShipmentEventInput {
    @required
    trackingId: TrackingId

    @required
    eventId: EventId
}

@references([
    { resource: ShipmentEvent }
])
structure GetShipmentEventOutput {
    @required
    trackingId: TrackingId

    @required
    eventId: EventId
}

@readonly
operation ListShipmentEvents {
    input: ListShipmentEventsInput
    output: ListShipmentEventsOutput
}

structure ListShipmentEventsInput {
    @required
    trackingId: TrackingId
}

structure ListShipmentEventsOutput {
    @required
    events: ShipmentEventList
}

list ShipmentEventList {
    member: GetShipmentEventOutput
}

operation CreateCustomer {
    input: CreateCustomerInput
    output: CreateCustomerOutput
}

structure CreateCustomerInput {
    @required
    name: String

    @required
    email: Email
}

structure CreateCustomerOutput {
    @required
    customerId: CustomerId

    @required
    name: String

    @required
    email: Email
}

@readonly
operation GetCustomer {
    input: GetCustomerInput
    output: GetCustomerOutput
}

structure GetCustomerInput {
    @required
    customerId: CustomerId
}

structure GetCustomerOutput {
    @required
    customerId: CustomerId

    @required
    name: String

    @required
    email: Email
}

operation UpdateCustomer {
    input: UpdateCustomerInput
    output: UpdateCustomerOutput
}

structure UpdateCustomerInput {
    @required
    customerId: CustomerId

    name: String

    email: Email
}

structure UpdateCustomerOutput {
    @required
    customerId: CustomerId

    @required
    name: String

    @required
    email: Email
}

@idempotent
operation DeleteCustomer {
    input: DeleteCustomerInput
    output: DeleteCustomerOutput
}

structure DeleteCustomerInput {
    @required
    customerId: CustomerId
}

structure DeleteCustomerOutput {
}

@readonly
operation ListCustomers {
    input: ListCustomersInput
    output: ListCustomersOutput
}

structure ListCustomersInput {
}

structure ListCustomersOutput {
    @required
    customers: CustomerList
}

list CustomerList {
    member: GetCustomerOutput
}

apply GetShipmentOutput @sensitive
apply GetShipment @examples([
    {
        title: "Get a shipment by tracking ID"
        input: { trackingId: "1Z999AA10123456784" }
        output: { trackingId: "1Z999AA10123456784", status: "IN_TRANSIT", origin: { line1: "123 Main St", city: "New York", stateCode: "NY", postalCode: "10001", countryCode: "US" }, destination: { line1: "456 Oak Ave", city: "Los Angeles", stateCode: "CA", postalCode: "90001", countryCode: "US" }, carrier: "UPS" }
    }
])
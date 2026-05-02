$version: "2.0"

namespace unison.tracker

use unison.tracker.shipments#Address
use unison.tracker.shipments#Customer
use unison.tracker.shipments#CustomerId
use unison.tracker.shipments#Shipment
use unison.tracker.shipments#ShipmentStatus
use unison.tracker.shipments#TrackingId
use unison.tracker.shipments#EventId
use unison.tracker.shipments#ShipmentEvent

@http(method: "POST", uri: "/shipments", code: 201)
operation CreateShipment {

    input :=
        @references([{ resource: Customer }])
        {
            @required
            origin: Address

            @required
            destination: Address

            @required
            @length(min: 1, max: 64)
            carrier: String

            customerId: CustomerId

            idempotencyKey: String
        }

    output := {
        @required
        status: ShipmentStatus

        @required
        estimatedDelivery: Timestamp
    }

    errors: [
        ValidationError
        CustomerNotFoundError
    ]
}

@readonly
@http(method: "GET", uri: "/shipments/{trackingId}")
operation GetShipment {
    input :=
        @references([{ resource: Shipment,
        
        ids: {trackingId: "trackingId"} },
        { resource: ShipmentEvent,
        ids: {trackingId: "trackingId", eventId: "eventId"} }
        ])
        {
            @httpLabel
            @required
            trackingId: TrackingId

            @httpQuery("eventId")
            eventId: EventId
        }

    output :=
        @references([{ resource: Shipment },{ resource: ShipmentEvent, ids: {trackingId: "trackingId", eventId: "eventId"} }])
        {
            @required
            trackingId: TrackingId
            
            @required
            eventId: EventId

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

    errors: [
        ShipmentNotFoundError
    ]
}

@readonly
@paginated(inputToken: "nextToken", outputToken: "nextToken", pageSize: "maxResults", items: "shipments")
@http(method: "GET", uri: "/shipments")
operation ListShipments {
    input :=
        @references([{ resource: Customer }])
        {
            @httpQuery("maxResults")
            @range(min: 1, max: 100)
            maxResults: Integer

            @httpQuery("nextToken")
            nextToken: String

            @httpQuery("status")
            status: ShipmentStatus

            @httpQuery("customerId")
            customerId: CustomerId
        }

    output := {
        @required
        shipments: ShipmentSummaryList

        nextToken: String
    }
}

list ShipmentSummaryList {
    member: ShipmentSummary
}

@references([{ resource: Shipment },{ resource: ShipmentEvent, ids: {trackingId: "trackingId", eventId: "eventId"} }])
structure ShipmentSummary {
    @required
    trackingId: TrackingId

    @required
    eventId: EventId

    @required
    status: ShipmentStatus

    @required
    carrier: String

    @required
    estimatedDelivery: Timestamp
}

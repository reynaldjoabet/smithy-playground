$version: "2.0"

namespace unison.tracker

use unison.tracker.shipments#Shipment
use unison.tracker.shipments#TrackingId
use unison.tracker.shipments#EventId
use unison.tracker.shipments#ShipmentEvent

@error("client")
@httpError(404)
@references([
    {
        resource: Shipment
        ids: {
            trackingId: "trackingId"
        }
    },
    {
        resource: ShipmentEvent
        ids: {
            trackingId: "trackingId",
            eventId: "eventId"
        }
    }
])
structure ShipmentNotFoundError {
    @required
    message: String

    @required
    trackingId: TrackingId,

    @required
    eventId: EventId
}

@error("client")
@httpError(400)
structure ValidationError {
    @required
    message: String

    fieldErrors: FieldErrorList
}

list FieldErrorList {
    member: FieldError
}

structure FieldError {
    @required
    field: String

    @required
    reason: String
}

@error("client")
@httpError(404)
structure CustomerNotFoundError {
    @required
    message: String
}

@error("client")
@httpError(401)
structure UnauthorizedError {
    @required
    message: String
}

@error("client")
@httpError(429)
@retryable(throttling: true)
structure ThrottlingError {
    @required
    message: String

    retryAfterSeconds: Integer
}

@error("server")
@httpError(500)
@retryable
structure InternalServerError {
    @required
    message: String
}

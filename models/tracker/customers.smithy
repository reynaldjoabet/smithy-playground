$version: "2.0"
namespace unison.tracker.customers
use unison.tracker.shipments#CustomerId
use unison.tracker.shipments#Address
use unison.tracker.shipments#Email
structure Customer {
    @required
    //@pii(level: "MEDIUM", retentionDays: 2555, purpose: "Account identification")
    name: String

    @required
   // @pii(level: "HIGH", retentionDays: 2555, purpose: "Communication")
    email: Email
}
$version: "2.0"

namespace unison.timeattendance.operations

use unison.identity#authenticationRequired

// ============================================================================
// TIME & ATTENDANCE OPERATIONS - Clocking, Shifts, Attendance
// ============================================================================
/// Clock in to start work.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["attendance:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/timeattendance/clock/in")
operation ClockIn {
    input: ClockInInput
    output: ClockInOutput
    errors: [
        Unauthorized
        AlreadyClockedIn
        InvalidLocation
    ]
}

structure ClockInInput {
    latitude: Double
    longitude: Double
    notes: String
}

structure ClockInOutput {
    @required
    clockId: String

    @required
    clockInTime: Long

    @required
    location: String
}

/// Clock out to end work.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["attendance:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/timeattendance/clock/out")
operation ClockOut {
    input: ClockOutInput
    output: ClockOutOutput
    errors: [
        Unauthorized
        NotClockedIn
        InvalidState
    ]
}

structure ClockOutInput {
    latitude: Double
    longitude: Double
    notes: String
}

structure ClockOutOutput {
    @required
    clockId: String

    @required
    clockOutTime: Long

    @required
    hoursWorked: Double
}

/// Get attendance history for employee.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["attendance:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/timeattendance/attendance")
operation GetAttendanceHistory {
    input: GetAttendanceHistoryInput
    output: GetAttendanceHistoryOutput
    errors: [
        Unauthorized
        InvalidDateRange
    ]
}

structure GetAttendanceHistoryInput {
    @httpQuery("startDate")
    startDate: Long

    @httpQuery("endDate")
    endDate: Long

    @httpQuery("limit")
    limit: Integer = 100

    @httpQuery("offset")
    offset: Integer = 0
}

structure GetAttendanceHistoryOutput {
    @required
    records: AttendanceRecordList

    @required
    totalCount: Integer
}

list AttendanceRecordList {
    member: AttendanceRecord
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure AttendanceRecord {
    @required
    recordId: String

    @required
    employeeId: String

    @required
    date: Long

    @required
    hoursWorked: Double

    @required
    status: String

    clockInTime: Long

    clockOutTime: Long
}

/// Mark attendance (manual entry by HR/Manager).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["attendance:write", "hr:admin"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/timeattendance/mark-attendance")
operation MarkAttendance {
    input: MarkAttendanceInput
    output: MarkAttendanceOutput
    errors: [
        Unauthorized
        EmployeeNotFound
        DuplicateEntry
    ]
}

@references([
    {
        resource: unison.operations#EmployeeRecord
        ids: { employeeId: "employeeId" }
    }
])
structure MarkAttendanceInput {
    @required
    employeeId: String

    @required
    date: Long

    @required
    status: String

    hoursWorked: Double

    notes: String
}

structure MarkAttendanceOutput {
    @required
    recordId: String

    @required
    createdAt: Long
}

/// Request time off (vacation, sick, etc).
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["timeoff:write"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/timeattendance/timeoff/request")
operation RequestTimeOff {
    input: RequestTimeOffInput
    output: RequestTimeOffOutput
    errors: [
        Unauthorized
        InvalidDateRange
        InsufficientBalance
    ]
}

structure RequestTimeOffInput {
    @required
    startDate: Long

    @required
    endDate: Long

    @required
    timeOffType: String

    reason: String
}

structure RequestTimeOffOutput {
    @required
    requestId: String

    @required
    createdAt: Long

    status: String
}

/// Get employee time off balance.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["timeoff:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/timeattendance/timeoff/balance")
operation GetTimeOffBalance {
    input: GetTimeOffBalanceInput
    output: GetTimeOffBalanceOutput
    errors: [
        Unauthorized
        EmployeeNotFound
    ]
}

structure GetTimeOffBalanceInput {
    @httpQuery("year")
    year: Integer
}

structure GetTimeOffBalanceOutput {
    @required
    balances: TimeOffBalanceList
}

list TimeOffBalanceList {
    member: TimeOffBalance
}

structure TimeOffBalance {
    @required
    timeOffType: String

    @required
    totalDays: Double

    @required
    usedDays: Double

    @required
    remainingDays: Double

    @required
    expiryDate: Long
}

/// Approve or reject time off request.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["timeoff:approve"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 1800
)
@http(method: "POST", uri: "/timeattendance/timeoff/requests/{requestId}/respond")
operation RespondToTimeOffRequest {
    input: RespondToTimeOffRequestInput
    output: RespondToTimeOffRequestOutput
    errors: [
        Unauthorized
        RequestNotFound
        InvalidState
    ]
}

structure RespondToTimeOffRequestInput {
    @required
    @httpLabel
    requestId: String

    @required
    approved: Boolean

    reason: String
}

structure RespondToTimeOffRequestOutput {
    @required
    updated: Boolean

    @required
    updatedAt: Long
}

/// Get shift schedule.
@authenticationRequired(
    allowedGrantTypes: ["AUTHORIZATION_CODE_PKCE", "REFRESH_TOKEN"]
    mfaPolicy: { required: false }
    ssoRequired: false
    requiredScopes: ["schedule:read"]
    sessionAffinity: "DISTRIBUTED_SESSION"
    maxTokenAgeSec: 3600
)
@readonly
@http(method: "GET", uri: "/timeattendance/shifts")
operation GetShiftSchedule {
    input: GetShiftScheduleInput
    output: GetShiftScheduleOutput
    errors: [
        Unauthorized
        InvalidDateRange
    ]
}

structure GetShiftScheduleInput {
    @httpQuery("startDate")
    startDate: Long

    @httpQuery("endDate")
    endDate: Long
}

structure GetShiftScheduleOutput {
    @required
    shifts: ShiftList
}

list ShiftList {
    member: Shift
}

structure Shift {
    @required
    shiftId: String

    @required
    startTime: Long

    @required
    endTime: Long

    @required
    location: String

    shiftType: String

    breakMinutes: Integer
}

@error("client")
structure Unauthorized {
    @required
    message: String
}

@error("client")
structure AlreadyClockedIn {
    @required
    message: String
}

@error("client")
structure InvalidLocation {
    @required
    message: String
}

@error("client")
structure NotClockedIn {
    @required
    message: String
}

@error("client")
structure InvalidState {
    @required
    message: String
}

@error("client")
structure InvalidDateRange {
    @required
    message: String
}

@error("client")
structure EmployeeNotFound {
    @required
    message: String
}

@error("client")
structure DuplicateEntry {
    @required
    message: String
}

@error("client")
structure InsufficientBalance {
    @required
    message: String

    availableDays: Double
}

@error("client")
structure RequestNotFound {
    @required
    message: String
}

list StringList {
    member: String
}

import Foundation

struct AdminService {
    private let api = APIClient.shared

    // MARK: - Users
    func fetchUsers() async throws -> [User] {
        try await api.get("/users")
    }

    func createUser(_ body: [String: String]) async throws -> User {
        try await api.post("/users", body: body)
    }

    func updateUser(id: Int, _ body: [String: String]) async throws -> User {
        try await api.patch("/users/\(id)", body: body)
    }

    func deleteUser(id: Int) async throws {
        let _: EmptyResponse = try await api.delete("/users/\(id)")
    }

    func updateUserStatus(id: Int, status: String) async throws {
        let _: EmptyResponse = try await api.patch("/users/\(id)/status", body: ["userStatus": status])
    }

    func resetPassword(id: Int) async throws {
        let _: EmptyResponse = try await api.post("/users/\(id)/reset-password")
    }

    func fetchUserRoles(id: Int) async throws -> [Role] {
        try await api.get("/users/\(id)/roles")
    }

    func assignUserRoles(userId: Int, roleIds: [Int]) async throws {
        let _: EmptyResponse = try await api.post("/users/\(userId)/roles", body: ["roleIds": roleIds])
    }

    // MARK: - Clubs
    func fetchClubs() async throws -> [Club] {
        try await api.get("/clubs")
    }

    func createClub(_ body: [String: String]) async throws -> Club {
        try await api.post("/clubs", body: body)
    }

    func updateClub(id: Int, _ body: [String: String]) async throws -> Club {
        try await api.patch("/clubs/\(id)", body: body)
    }

    func updateClubStatus(id: Int, status: String) async throws {
        let _: EmptyResponse = try await api.patch("/clubs/\(id)/status", body: ["status": status])
    }

    func fetchClubDepartments(clubId: Int) async throws -> [ClubDepartment] {
        try await api.get("/clubs/\(clubId)/departments", authenticated: false)
    }

    func fetchClubPositions(clubId: Int) async throws -> [ClubPosition] {
        try await api.get("/clubs/\(clubId)/positions")
    }

    // MARK: - Applications
    func fetchApplications() async throws -> [MembershipApplication] {
        try await api.get("/applications")
    }

    func fetchApplication(id: Int) async throws -> MembershipApplication {
        try await api.get("/applications/\(id)")
    }

    func approveApplication(id: Int, action: String, comment: String?) async throws {
        var body: [String: String] = ["action": action]
        if let comment { body["comment"] = comment }
        let _: EmptyResponse = try await api.post("/applications/\(id)/approvals", body: body)
    }

    func batchApprove(ids: [Int], action: String, comment: String?) async throws {
        var body: [String: any Encodable] = ["applicationIds": ids, "action": action]
        if let comment { body["comment"] = comment }
        let _: EmptyResponse = try await api.post("/applications/batch-approvals", body: JSONBody(body))
    }

    func finalDecision(id: Int, action: String) async throws {
        let _: EmptyResponse = try await api.post("/applications/\(id)/final-decisions", body: ["action": action])
    }

    // MARK: - Interviews
    func fetchInterviews() async throws -> [Interview] {
        try await api.get("/interviews")
    }

    func createInterview(_ body: [String: String]) async throws -> Interview {
        try await api.post("/interviews", body: body)
    }

    func updateInterview(id: Int, _ body: [String: String]) async throws -> Interview {
        try await api.patch("/interviews/\(id)", body: body)
    }

    func completeInterview(id: Int) async throws {
        let _: EmptyResponse = try await api.post("/interviews/\(id)/complete")
    }

    // MARK: - Memberships
    func fetchMemberships() async throws -> [ClubMembership] {
        try await api.get("/memberships")
    }

    func changeMembershipStatus(id: Int, status: String) async throws {
        let _: EmptyResponse = try await api.post("/memberships/\(id)/change-status", body: ["status": status])
    }

    func forceExit(membershipId: Int) async throws {
        let _: EmptyResponse = try await api.post("/memberships/\(membershipId)/force-exit")
    }

    // MARK: - Roles & Permissions
    func fetchRoles() async throws -> [Role] {
        try await api.get("/roles")
    }

    func fetchPermissions() async throws -> [Permission] {
        try await api.get("/permissions")
    }

    // MARK: - Notifications
    func fetchNotifications() async throws -> [NotificationItem] {
        try await api.get("/notifications")
    }

    func fetchUnreadCount() async throws -> Int {
        let r: UnreadCount = try await api.get("/notifications/unread-count")
        return r.count
    }

    func markRead(notificationId: Int) async throws {
        let _: EmptyResponse = try await api.post("/notifications/\(notificationId)/read")
    }

    // MARK: - Recruitment Campaigns
    func fetchCampaigns(clubId: Int) async throws -> [RecruitmentCampaign] {
        try await api.get("/clubs/\(clubId)/recruitment-campaigns")
    }

    func createCampaign(clubId: Int, _ body: [String: String]) async throws -> RecruitmentCampaign {
        try await api.post("/clubs/\(clubId)/recruitment-campaigns", body: body)
    }

    func publishCampaign(id: Int) async throws {
        let _: EmptyResponse = try await api.post("/recruitment-campaigns/\(id)/publish")
    }

    func closeCampaign(id: Int) async throws {
        let _: EmptyResponse = try await api.post("/recruitment-campaigns/\(id)/close")
    }
}

struct JSONBody: Encodable {
    private let storage: [String: any Encodable]

    init(_ dict: [String: any Encodable]) {
        self.storage = dict
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: DynamicCodingKey.self)
        for (key, value) in storage {
            guard let codingKey = DynamicCodingKey(stringValue: key) else { continue }
            try container.encode(AnyEncodable(value), forKey: codingKey)
        }
    }
}

struct DynamicCodingKey: CodingKey {
    var stringValue: String
    var intValue: Int?

    init?(stringValue: String) { self.stringValue = stringValue }
    init?(intValue: Int) { return nil }
}

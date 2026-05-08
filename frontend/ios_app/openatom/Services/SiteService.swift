import Foundation

struct SiteService {
    private let api = APIClient.shared

    func fetchHome() async throws -> SiteHome {
        try await api.get("/site/club-home", authenticated: false)
    }

    func fetchActivities() async throws -> [Activity] {
        try await api.get("/site/activities", authenticated: false)
    }

    func fetchActivity(id: Int) async throws -> Activity {
        try await api.get("/site/activities/\(id)", authenticated: false)
    }

    func fetchRecruitment() async throws -> ResponseRecruitmentDTO {
        try await api.get("/site/recruitment", authenticated: false)
    }

    func fetchRecruitmentDetail(id: Int) async throws -> ResponseRecruitmentDetailDTO {
        try await api.get("/site/recruitment/\(id)", authenticated: false)
    }

    func fetchProgress() async throws -> [ApplicationProgress] {
        try await api.get("/site/progress")
    }

    func fetchRegisterEnabled() async throws -> Bool {
        struct RegEnabled: Decodable { let enabled: Bool? }
        let r: RegEnabled = try await api.get("/site/register-enabled", authenticated: false)
        return r.enabled ?? true
    }

    func registerActivity(activityId: Int) async throws {
        let _: EmptyResponse = try await api.post("/activities/\(activityId)/registrations")
    }
}

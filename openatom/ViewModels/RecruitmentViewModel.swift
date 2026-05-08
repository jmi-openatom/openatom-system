import Foundation
import Observation

@MainActor
@Observable
final class RecruitmentViewModel {
    var club: Club?
    var campaigns: [RecruitmentCampaign] = []
    var departments: [ClubDepartment] = []
    var isLoading = false
    var error: String?

    private let service = SiteService()

    func load() async {
        isLoading = true
        error = nil
        do {
            let dto = try await service.fetchRecruitment()
            club = dto.club
            campaigns = dto.campaigns ?? []
            departments = dto.departments ?? []
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

@MainActor
@Observable
final class ApplicationFormViewModel {
    var club: Club?
    var campaign: RecruitmentCampaign?
    var departments: [ClubDepartment] = []
    var isLoading = false
    var error: String?
    var submitSuccess = false
    var formData: [String: String] = [:]

    private let service = SiteService()
    private let api = APIClient.shared

    func loadCampaign(id: Int) async {
        isLoading = true
        error = nil
        do {
            let dto = try await service.fetchRecruitmentDetail(id: id)
            club = dto.club
            campaign = dto.campaign
            departments = dto.departments ?? []
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func submit() async {
        guard let campaignId = campaign?.id else { return }
        do {
            let body: [String: any Encodable] = [
                "campaignId": campaignId,
                "profile": formData
            ]
            let _: EmptyResponse = try await api.post("/applications", body: JSONBody(body))
            submitSuccess = true
        } catch {
            self.error = error.localizedDescription
        }
    }
}

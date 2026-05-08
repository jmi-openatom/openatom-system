import Foundation
import Observation

@MainActor
@Observable
final class ActivitiesViewModel {
    var activities: [Activity] = []
    var isLoading = false
    var error: String?

    private let service = SiteService()

    func load() async {
        isLoading = true
        error = nil
        do {
            activities = try await service.fetchActivities()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

@MainActor
@Observable
final class ActivityDetailViewModel {
    var activity: Activity?
    var isLoading = false
    var error: String?
    var registerSuccess = false

    private let service = SiteService()

    func load(id: Int) async {
        isLoading = true
        error = nil
        do {
            activity = try await service.fetchActivity(id: id)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func register() async {
        guard let id = activity?.id else { return }
        do {
            try await service.registerActivity(activityId: id)
            registerSuccess = true
        } catch {
            self.error = error.localizedDescription
        }
    }
}

import Foundation
import Observation

@MainActor
@Observable
final class ApplicationProgressViewModel {
    var applications: [ApplicationProgress] = []
    var isLoading = false
    var error: String?

    private let service = SiteService()

    func load() async {
        isLoading = true
        error = nil
        do {
            applications = try await service.fetchProgress()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

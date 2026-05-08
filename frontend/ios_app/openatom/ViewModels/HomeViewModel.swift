import Foundation
import Observation

@MainActor
@Observable
final class HomeViewModel {
    var home: SiteHome?
    var isLoading = false
    var error: String?

    private let service = SiteService()

    func load() async {
        isLoading = true
        error = nil
        do {
            home = try await service.fetchHome()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

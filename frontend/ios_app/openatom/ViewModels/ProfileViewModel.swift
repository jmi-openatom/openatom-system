import Foundation
import Observation

@MainActor
@Observable
final class ProfileViewModel {
    var user: User?
    var isLoading = false
    var error: String?
    var passwordChangeSuccess = false
    var logoutCompleted = false

    private let auth = AuthService()

    func load() async {
        isLoading = true
        error = nil
        do {
            user = try await auth.fetchMe()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func changePassword(old: String, new: String) async {
        do {
            try await auth.changePassword(oldPassword: old, newPassword: new)
            passwordChangeSuccess = true
        } catch {
            self.error = error.localizedDescription
        }
    }

    func logout() async {
        try? await auth.logout()
        logoutCompleted = true
    }
}

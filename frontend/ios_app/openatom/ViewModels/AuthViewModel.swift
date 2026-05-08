import Foundation
import Observation

@MainActor
@Observable
final class AuthViewModel {
    var isLoading = false
    var error: String?
    var loginSuccess = false

    private let auth = AuthService()
    private let site = SiteService()
    var registerEnabled = true

    func checkRegisterEnabled() async {
        registerEnabled = (try? await site.fetchRegisterEnabled()) ?? true
    }

    func login(username: String, password: String) async {
        isLoading = true
        error = nil
        do {
            let _ = try await auth.login(username: username, password: password)
            loginSuccess = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func register(userName: String, realName: String, phone: String, email: String, password: String) async {
        isLoading = true
        error = nil
        do {
            let _ = try await auth.register(userName: userName, realName: realName, phone: phone, email: email, password: password)
            loginSuccess = true
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

import Foundation
import Observation

@MainActor
@Observable
final class Session {
    static let shared = Session()

    var token: String?
    var refreshToken: String?
    var currentUser: User?
    var roles: [Role] = []
    var permissions: [Permission] = []
    var unreadCount: Int = 0

    var isLoggedIn: Bool { token != nil }

    private enum Keys {
        static let token = "openatom_token"
        static let refreshToken = "openatom_refresh_token"
        static let user = "openatom_user"
        static let roles = "openatom_roles"
        static let permissions = "openatom_permissions"
    }

    private init() {
        loadFromKeychain()
    }

    func saveLogin(_ response: LoginResponse) {
        token = response.token ?? response.accessToken
        refreshToken = response.refreshToken
        currentUser = response.user
        roles = response.roles ?? []
        permissions = response.permissions ?? []

        if let t = token { KeychainHelper.save(key: Keys.token, value: t) }
        if let rt = refreshToken { KeychainHelper.save(key: Keys.refreshToken, value: rt) }
        if let user = currentUser, let data = try? JSONEncoder().encode(user) {
            KeychainHelper.save(key: Keys.user, value: String(data: data, encoding: .utf8) ?? "")
        }
        if let rData = try? JSONEncoder().encode(roles) {
            KeychainHelper.save(key: Keys.roles, value: String(data: rData, encoding: .utf8) ?? "")
        }
        if let pData = try? JSONEncoder().encode(permissions) {
            KeychainHelper.save(key: Keys.permissions, value: String(data: pData, encoding: .utf8) ?? "")
        }
    }

    func clear() {
        token = nil
        refreshToken = nil
        currentUser = nil
        roles = []
        permissions = []
        KeychainHelper.clearAll()
    }

    private func loadFromKeychain() {
        token = KeychainHelper.load(key: Keys.token)
        refreshToken = KeychainHelper.load(key: Keys.refreshToken)

        if let json = KeychainHelper.load(key: Keys.user),
           let data = json.data(using: .utf8) {
            currentUser = try? JSONDecoder().decode(User.self, from: data)
        }
        if let json = KeychainHelper.load(key: Keys.roles),
           let data = json.data(using: .utf8) {
            roles = (try? JSONDecoder().decode([Role].self, from: data)) ?? []
        }
        if let json = KeychainHelper.load(key: Keys.permissions),
           let data = json.data(using: .utf8) {
            permissions = (try? JSONDecoder().decode([Permission].self, from: data)) ?? []
        }
    }

    var isAdmin: Bool {
        let adminRoles = Set(["super_admin", "club_admin", "department_head"])
        return roles.contains { adminRoles.contains($0.code ?? "") }
    }

    func hasPermission(_ code: String) -> Bool {
        if roles.contains(where: { $0.code == "super_admin" }) { return true }
        return permissions.contains { $0.code == code }
    }

    func hasAnyPermission(_ codes: [String]) -> Bool {
        if codes.isEmpty { return true }
        if roles.contains(where: { $0.code == "super_admin" }) { return true }
        return permissions.contains { p in codes.contains(p.code ?? "") }
    }
}

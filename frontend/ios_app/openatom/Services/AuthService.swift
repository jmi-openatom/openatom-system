import Foundation

struct AuthService {
    private let api = APIClient.shared

    func login(username: String, password: String) async throws -> LoginResponse {
        let body = ["username": username, "password": password]
        let response: LoginResponse = try await api.post("/auth/login", body: body, authenticated: false)
        await Session.shared.saveLogin(response)
        return response
    }

    func register(userName: String, realName: String, phone: String, email: String, password: String) async throws -> LoginResponse {
        let body: [String: String] = [
            "userName": userName,
            "realName": realName,
            "phone": phone,
            "email": email,
            "password": password
        ]
        let _: EmptyResponse = try await api.post("/auth/register", body: body, authenticated: false)
        return try await login(username: userName, password: password)
    }

    func logout() async throws {
        let _: EmptyResponse = try await api.post("/auth/logout")
        await Session.shared.clear()
    }

    func fetchMe() async throws -> User {
        try await api.get("/auth/me")
    }

    func changePassword(oldPassword: String, newPassword: String) async throws {
        let body = ["oldPassword": oldPassword, "newPassword": newPassword]
        let _: EmptyResponse = try await api.patch("/auth/password", body: body)
    }
}

import Foundation

struct User: Codable, Identifiable, Equatable {
    let id: Int
    let userName: String?
    let realName: String?
    let gender: String?
    let phone: String?
    let email: String?
    let studentId: String?
    let college: String?
    let major: String?
    let grade: String?
    let className: String?
    let avatar: String?
    let userStatus: String?
    let createTime: String?
    let lastLoginAt: String?

    var displayName: String { realName ?? userName ?? "未知用户" }

    static func == (lhs: User, rhs: User) -> Bool { lhs.id == rhs.id }
}

struct Role: Codable, Identifiable {
    let id: Int
    let name: String?
    let code: String?
    let dataScope: String?
    let description: String?
}

struct Permission: Codable, Identifiable {
    let id: Int
    let name: String?
    let code: String?
    let type: String?
    let path: String?
    let method: String?
}

struct UserRole: Codable {
    let userId: Int
    let roleId: Int
}

struct LoginResponse: Codable {
    let token: String?
    let accessToken: String?
    let refreshToken: String?
    let user: User?
    let roles: [Role]?
    let permissions: [Permission]?
}

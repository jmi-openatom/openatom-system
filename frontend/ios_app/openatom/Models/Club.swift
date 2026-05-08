import Foundation

struct Club: Codable, Identifiable {
    let id: Int
    let name: String?
    let code: String?
    let category: String?
    let description: String?
    let logoUrl: String?
    let presidentUserId: Int?
    let status: String?
    let recruitmentStatus: String?
    let createdAt: String?
    let updatedAt: String?
}

struct ClubDepartment: Codable, Identifiable {
    let id: Int
    let clubId: Int?
    let name: String?
    let description: String?
    let managerUserId: Int?
    let createdAt: String?
    let updatedAt: String?
}

struct ClubPosition: Codable, Identifiable {
    let id: Int
    let clubId: Int?
    let departmentId: Int?
    let name: String?
    let maxCount: Int?
}

struct ClubMembership: Codable, Identifiable {
    let id: Int
    let userId: Int?
    let clubId: Int?
    let departmentId: Int?
    let positionId: Int?
    let status: String?
    let featured: Bool?
    let sortOrder: Int?
    let joinedAt: String?
    let leftAt: String?
    let user: User?
    let department: ClubDepartment?
    let position: ClubPosition?
}

import Foundation

struct MembershipApplication: Codable, Identifiable {
    let id: Int
    let campaignId: Int?
    let clubId: Int?
    let userId: Int?
    let firstChoiceDepartmentId: Int?
    let secondChoiceDepartmentId: Int?
    let status: String?
    let profile: String?
    let club: Club?
    let user: User?
    let firstChoiceDepartment: ClubDepartment?
    let secondChoiceDepartment: ClubDepartment?
    let approvalRecords: [ApprovalRecord]?
}

struct ApprovalRecord: Codable, Identifiable {
    let id: Int
    let applicationId: Int?
    let node: Int?
    let action: String?
    let operatorId: Int?
    let comment: String?
    let createdAt: String?
    let operatorUser: User?
}

struct ApplicationProgress: Codable, Identifiable {
    let id: Int
    let campaignId: Int?
    let clubId: Int?
    let status: String?
    let club: Club?
    let campaign: RecruitmentCampaign?
    let firstChoiceDepartment: ClubDepartment?
    let approvalRecords: [ApprovalRecord]?
}

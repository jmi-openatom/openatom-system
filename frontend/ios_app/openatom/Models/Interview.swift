import Foundation

struct Interview: Codable, Identifiable {
    let id: Int
    let applicationId: Int?
    let round: Int?
    let scheduledStartAt: String?
    let scheduledEndAt: String?
    let location: String?
    let mode: String?
    let status: String?
    let application: MembershipApplication?
    let interviewers: [User]?
    let feedbacks: [InterviewFeedback]?
}

struct InterviewFeedback: Codable, Identifiable {
    let id: Int
    let interviewId: Int?
    let interviewerId: Int?
    let scores: String?
    let suggestion: String?
    let comment: String?
    let interviewer: User?
}

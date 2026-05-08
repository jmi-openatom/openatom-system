import Foundation

struct NotificationItem: Codable, Identifiable {
    let id: Int
    let title: String?
    let content: String?
    let type: String?
    let createdAt: String?
    let readFlag: Bool?
    let readAt: String?
}

struct UnreadCount: Codable {
    let count: Int
}

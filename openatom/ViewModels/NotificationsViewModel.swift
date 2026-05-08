import Foundation
import Observation

@MainActor
@Observable
final class NotificationsViewModel {
    var notifications: [NotificationItem] = []
    var unreadCount: Int = 0
    var isLoading = false
    var error: String?

    private let admin = AdminService()

    func load() async {
        isLoading = true
        error = nil
        do {
            notifications = try await admin.fetchNotifications()
            let count = try? await admin.fetchUnreadCount()
            unreadCount = count ?? 0
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }

    func markRead(id: Int) async {
        try? await admin.markRead(notificationId: id)
        if let idx = notifications.firstIndex(where: { $0.id == id }) {
            var item = notifications[idx]
            item = NotificationItem(
                id: item.id,
                title: item.title,
                content: item.content,
                type: item.type,
                createdAt: item.createdAt,
                readFlag: true,
                readAt: item.readAt
            )
            notifications[idx] = item
        }
        unreadCount = max(0, unreadCount - 1)
    }
}

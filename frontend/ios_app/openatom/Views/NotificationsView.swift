import SwiftUI

struct NotificationsView: View {
    @Environment(Session.self) private var session
    @State private var vm = NotificationsViewModel()

    var body: some View {
        Group {
            if !session.isLoggedIn {
                VStack(spacing: 16) {
                    Image(systemName: "bell.slash")
                        .font(.system(size: 48))
                        .foregroundColor(.secondary)
                    Text("请先登录")
                        .font(.headline)
                    Text("登录后查看消息通知")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            } else if vm.isLoading {
                LoadingView("加载中...")
            } else if let error = vm.error {
                ErrorView(message: error) { Task { await vm.load() } }
            } else if vm.notifications.isEmpty {
                EmptyStateView(icon: "bell", title: "暂无通知")
            } else {
                listView
            }
        }
        .navigationTitle("消息通知")
        .navigationBarTitleDisplayMode(.large)
        .refreshable { await vm.load() }
        .task { await vm.load() }
    }

    private var listView: some View {
        List {
            ForEach(vm.notifications) { notification in
                Button {
                    Task { await vm.markRead(id: notification.id) }
                } label: {
                    NotificationRow(notification: notification)
                }
                .buttonStyle(.plain)
            }
        }
        .listStyle(.plain)
    }
}

struct NotificationRow: View {
    let notification: NotificationItem

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(notification.readFlag == true ? Color.clear : Color.accentColor)
                .frame(width: 8, height: 8)

            VStack(alignment: .leading, spacing: 4) {
                Text(notification.title ?? "")
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(notification.readFlag == true ? .secondary : .primary)
                if let content = notification.content {
                    Text(content)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
            }

            Spacer()

            if let date = notification.createdAt {
                Text(date)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

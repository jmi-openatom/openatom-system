import SwiftUI

struct ApplicationProgressView: View {
    @Environment(Session.self) private var session
    @State private var vm = ApplicationProgressViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if !session.isLoggedIn {
                    notLoggedInView
                } else if vm.isLoading {
                    LoadingView("加载中...")
                } else if let error = vm.error {
                    ErrorView(message: error) { Task { await vm.load() } }
                } else if vm.applications.isEmpty {
                    EmptyStateView(icon: "doc.text", title: "暂无申请记录", message: "前往入会申请页面提交申请")
                } else {
                    listView
                }
            }
            .navigationTitle("我的申请")
            .navigationBarTitleDisplayMode(.large)
            .refreshable { await vm.load() }
            .task { await vm.load() }
        }
    }

    private var notLoggedInView: some View {
        VStack(spacing: 16) {
            Image(systemName: "person.crop.circle")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
            Text("请先登录")
                .font(.headline)
            Text("登录后查看您的申请进度")
                .font(.subheadline)
                .foregroundColor(.secondary)
            NavigationLink("去登录") {
                LoginView()
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private var listView: some View {
        List {
            ForEach(vm.applications) { app in
                NavigationLink {
                    ApplicationDetailView(application: app)
                } label: {
                    ProgressRow(application: app)
                }
            }
        }
        .listStyle(.plain)
    }
}

struct ProgressRow: View {
    let application: ApplicationProgress

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(application.club?.name ?? "社团")
                    .font(.headline)
                Spacer()
                if let status = application.status {
                    StatusBadge(status: status)
                }
            }
            if let campaignName = application.campaign?.name {
                Text(campaignName)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            if let deptName = application.firstChoiceDepartment?.name {
                Label(deptName, systemImage: "building")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ApplicationDetailView: View {
    let application: ApplicationProgress

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("申请信息")
                        .font(.headline)
                    infoRow(label: "社团", value: application.club?.name ?? "-")
                    infoRow(label: "招募", value: application.campaign?.name ?? "-")
                    infoRow(label: "意向部门", value: application.firstChoiceDepartment?.name ?? "-")
                    infoRow(label: "状态", badge: application.status)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(Color(.systemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                if let records = application.approvalRecords, !records.isEmpty {
                    VStack(alignment: .leading, spacing: 12) {
                        Text("审核进度")
                            .font(.headline)

                        ForEach(records.sorted(by: { ($0.node ?? 0) < ($1.node ?? 0) })) { record in
                            HStack(spacing: 12) {
                                Circle()
                                    .fill(approvalColor(record.action))
                                    .frame(width: 10, height: 10)

                                VStack(alignment: .leading, spacing: 2) {
                                    Text(record.operatorUser?.displayName ?? "审核人")
                                        .font(.subheadline.weight(.medium))
                                    if let action = record.action {
                                        Text(approvalActionText(action))
                                            .font(.caption)
                                            .foregroundColor(.secondary)
                                    }
                                    if let comment = record.comment, !comment.isEmpty {
                                        Text("备注: \(comment)")
                                            .font(.caption2)
                                            .foregroundColor(.secondary)
                                    }
                                }

                                Spacer()

                                if let date = record.createdAt {
                                    Text(date)
                                        .font(.caption2)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color(.systemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("申请详情")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func infoRow(label: String, value: String? = nil, badge: String? = nil) -> some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
            if let badge = badge {
                StatusBadge(status: badge)
            } else if let value = value {
                Text(value)
                    .font(.subheadline)
            }
        }
    }

    private func approvalColor(_ action: String?) -> Color {
        switch action {
        case "APPROVED": return .green
        case "REJECTED": return .red
        default: return .orange
        }
    }

    private func approvalActionText(_ action: String) -> String {
        switch action {
        case "APPROVED": return "已通过"
        case "REJECTED": return "已拒绝"
        case "PENDING": return "待审核"
        default: return action
        }
    }
}

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
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(vm.applications) { app in
                    NavigationLink {
                        ApplicationDetailView(application: app)
                    } label: {
                        ProgressRow(application: app)
                            .premiumCard()
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(16)
        }
    }
}

struct ProgressRow: View {
    let application: ApplicationProgress

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(application.club?.name ?? "社团")
                    .font(.headline)
                Spacer()
                if let status = application.status {
                    StatusBadge(status: status)
                }
            }
            
            VStack(alignment: .leading, spacing: 4) {
                if let campaignName = application.campaign?.name {
                    Text(campaignName)
                        .font(.subheadline.bold())
                }
                if let deptName = application.firstChoiceDepartment?.name {
                    Label(deptName, systemImage: "building.2")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            

        }
    }
}

struct ApplicationDetailView: View {
    let application: ApplicationProgress

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                VStack(alignment: .leading, spacing: 16) {
                    Text("申请信息")
                        .font(.headline)
                    
                    VStack(spacing: 12) {
                        infoRow(label: "申请社团", value: application.club?.name)
                        Divider()
                        infoRow(label: "招募计划", value: application.campaign?.name)
                        Divider()
                        infoRow(label: "意向部门", value: application.firstChoiceDepartment?.name)
                        Divider()
                        infoRow(label: "当前状态", badge: application.status)
                    }
                }
                .premiumCard()

                if let records = application.approvalRecords, !records.isEmpty {
                    VStack(alignment: .leading, spacing: 20) {
                        Text("审核进度")
                            .font(.headline)

                        VStack(alignment: .leading, spacing: 0) {
                            ForEach(Array(records.sorted(by: { ($0.node ?? 0) < ($1.node ?? 0) }).enumerated()), id: \.offset) { index, record in
                                HStack(alignment: .top, spacing: 16) {
                                    VStack(spacing: 0) {
                                        Circle()
                                            .fill(approvalColor(record.action))
                                            .frame(width: 12, height: 12)
                                        
                                        if index < records.count - 1 {
                                            Rectangle()
                                                .fill(Color(.systemGray4))
                                                .frame(width: 2)
                                                .frame(minHeight: 40)
                                        }
                                    }
                                    
                                    VStack(alignment: .leading, spacing: 4) {
                                        HStack {
                                            Text(record.operatorUser?.displayName ?? "审核人")
                                                .font(.subheadline.bold())
                                            Spacer()
                                            if let date = record.createdAt {
                                                Text(date)
                                                    .font(.system(size: 10))
                                                    .foregroundColor(.secondary)
                                            }
                                        }
                                        
                                        if let action = record.action {
                                            Text(approvalActionText(action))
                                                .font(.caption)
                                                .foregroundColor(approvalColor(action))
                                                .padding(.horizontal, 8)
                                                .padding(.vertical, 2)
                                                .background(approvalColor(action).opacity(0.1))
                                                .clipShape(Capsule())
                                        }
                                        
                                        if let comment = record.comment, !comment.isEmpty {
                                            Text(comment)
                                                .font(.caption)
                                                .foregroundColor(.secondary)
                                                .padding(8)
                                                .frame(maxWidth: .infinity, alignment: .leading)
                                                .background(Color(.systemGray6))
                                                .clipShape(RoundedRectangle(cornerRadius: 8))
                                        }
                                    }
                                    .padding(.bottom, index < records.count - 1 ? 16 : 0)
                                }
                            }
                        }
                    }
                    .premiumCard()
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
                    .font(.subheadline.bold())
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
        case "APPROVED": return "审核通过"
        case "REJECTED": return "审核驳回"
        case "PENDING": return "等待审核"
        default: return action
        }
    }
}

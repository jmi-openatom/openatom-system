import SwiftUI

struct AdminApplicationsView: View {
    @State private var applications: [MembershipApplication] = []
    @State private var isLoading = false
    @State private var error: String?
    @State private var selectedFilter = "全部"

    private let filters = ["全部", "待审核", "已通过", "已拒绝", "面试中", "已录取"]
    private let admin = AdminService()

    var filteredApplications: [MembershipApplication] {
        switch selectedFilter {
        case "待审核": return applications.filter { $0.status == "SUBMITTED" || $0.status == "PENDING" }
        case "已通过": return applications.filter { $0.status == "APPROVED" }
        case "已拒绝": return applications.filter { $0.status == "REJECTED" }
        case "面试中": return applications.filter { $0.status == "INTERVIEWING" }
        case "已录取": return applications.filter { $0.status == "ACCEPTED" }
        default: return applications
        }
    }

    var body: some View {
        Group {
            if isLoading {
                LoadingView("加载中...")
            } else if let error = error {
                ErrorView(message: error) { Task { await load() } }
            } else {
                contentView
            }
        }
        .navigationTitle("申请管理")
        .refreshable { await load() }
        .task { await load() }
    }

    private var contentView: some View {
        VStack(spacing: 0) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(filters, id: \.self) { filter in
                        Button(filter) { selectedFilter = filter }
                            .font(.subheadline.weight(selectedFilter == filter ? .semibold : .regular))
                            .padding(.horizontal, 14)
                            .padding(.vertical, 8)
                            .background(selectedFilter == filter ? Color.accentColor : Color(.systemGray6))
                            .foregroundColor(selectedFilter == filter ? .white : .primary)
                            .clipShape(Capsule())
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
            }

            if filteredApplications.isEmpty {
                EmptyStateView(icon: "doc.text", title: "暂无申请")
            } else {
                List {
                    ForEach(filteredApplications) { app in
                        NavigationLink {
                            ApplicationAdminDetailView(application: app, onAction: { Task { await load() } })
                        } label: {
                            ApplicationAdminRow(application: app)
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
    }

    private func load() async {
        isLoading = true
        error = nil
        do {
            applications = try await admin.fetchApplications()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

struct ApplicationAdminRow: View {
    let application: MembershipApplication

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(application.user?.displayName ?? "申请人")
                    .font(.subheadline.weight(.medium))
                Spacer()
                StatusBadge(status: application.status)
            }
            if let clubName = application.club?.name {
                Text(clubName)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            if let deptName = application.firstChoiceDepartment?.name {
                Text("意向: \(deptName)")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct ApplicationAdminDetailView: View {
    let application: MembershipApplication
    let onAction: () -> Void

    @State private var selectedAction = "APPROVED"
    @State private var comment = ""
    @State private var showActionSheet = false

    private let admin = AdminService()

    var body: some View {
        List {
            Section("申请人信息") {
                infoRow("姓名", application.user?.displayName)
                infoRow("用户名", application.user?.userName)
                infoRow("学号", application.user?.studentId)
                infoRow("学院", application.user?.college)
            }

            Section("申请信息") {
                infoRow("社团", application.club?.name)
                infoRow("第一志愿部门", application.firstChoiceDepartment?.name)
                infoRow("第二志愿部门", application.secondChoiceDepartment?.name)
                infoRow("状态", badge: application.status)
            }

            Section("操作") {
                Picker("审核决定", selection: $selectedAction) {
                    Text("通过").tag("APPROVED")
                    Text("拒绝").tag("REJECTED")
                }

                TextField("审核意见（可选）", text: $comment, axis: .vertical)
                    .lineLimit(2...4)

                Button("提交审核") {
                    showActionSheet = true
                }
                .buttonStyle(.borderedProminent)
                .disabled(selectedAction.isEmpty)
            }
        }
        .navigationTitle("申请详情")
        .navigationBarTitleDisplayMode(.inline)
        .alert("确认操作", isPresented: $showActionSheet) {
            Button("取消", role: .cancel) {}
            Button("确认") {
                Task {
                    try? await admin.approveApplication(id: application.id, action: selectedAction, comment: comment.isEmpty ? nil : comment)
                    onAction()
                }
            }
        } message: {
            Text("确定要\(selectedAction == "APPROVED" ? "通过" : "拒绝")此申请吗？")
        }
    }

    private func infoRow(_ label: String, _ value: String? = nil, badge: String? = nil) -> some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
            if let b = badge {
                StatusBadge(status: b)
            } else {
                Text(value ?? "-")
                    .font(.subheadline)
            }
        }
    }
}

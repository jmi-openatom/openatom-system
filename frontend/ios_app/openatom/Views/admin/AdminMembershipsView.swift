import SwiftUI

struct AdminMembershipsView: View {
    @State private var memberships: [ClubMembership] = []
    @State private var isLoading = false
    @State private var error: String?

    private let admin = AdminService()

    var body: some View {
        Group {
            if isLoading {
                LoadingView("加载中...")
            } else if let error = error {
                ErrorView(message: error) { Task { await load() } }
            } else if memberships.isEmpty {
                EmptyStateView(icon: "person.3", title: "暂无成员记录")
            } else {
                listView
            }
        }
        .navigationTitle("成员管理")
        .refreshable { await load() }
        .task { await load() }
    }

    private var listView: some View {
        List {
            ForEach(memberships) { membership in
                NavigationLink {
                    MembershipDetailView(membership: membership, onAction: { Task { await load() } })
                } label: {
                    MembershipRow(membership: membership)
                }
            }
        }
        .listStyle(.plain)
    }

    private func load() async {
        isLoading = true
        error = nil
        do {
            memberships = try await admin.fetchMemberships()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

struct MembershipRow: View {
    let membership: ClubMembership

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(url: membership.user?.avatar, name: membership.user?.displayName ?? "?", size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(membership.user?.displayName ?? "成员")
                    .font(.subheadline.weight(.medium))
                if let deptName = membership.department?.name {
                    Text(deptName)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                if let posName = membership.position?.name {
                    Text(posName)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            Spacer()
            StatusBadge(status: membership.status)
        }
        .padding(.vertical, 4)
    }
}

struct MembershipDetailView: View {
    let membership: ClubMembership
    let onAction: () -> Void

    @State private var selectedStatus: String
    @State private var showForceExitAlert = false

    private let admin = AdminService()

    init(membership: ClubMembership, onAction: @escaping () -> Void) {
        self.membership = membership
        self.onAction = onAction
        _selectedStatus = State(initialValue: membership.status ?? "ACTIVE")
    }

    var body: some View {
        List {
            Section("成员信息") {
                HStack(spacing: 16) {
                    AvatarView(url: membership.user?.avatar, name: membership.user?.displayName ?? "?", size: 60)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(membership.user?.displayName ?? "成员")
                            .font(.title3.bold())
                        if let studentId = membership.user?.studentId {
                            Text(studentId)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(.vertical, 8)

                infoRow("用户名", membership.user?.userName)
                infoRow("部门", membership.department?.name)
                infoRow("岗位", membership.position?.name)
                infoRow("加入时间", membership.joinedAt)
            }

            Section("状态管理") {
                Picker("成员状态", selection: $selectedStatus) {
                    Text("正常").tag("ACTIVE")
                    Text("停用").tag("DISABLED")
                    Text("退出").tag("EXITED")
                }
                .onChange(of: selectedStatus) { _, newValue in
                    Task {
                        try? await admin.changeMembershipStatus(id: membership.id, status: newValue)
                        onAction()
                    }
                }

                Button("强制退出", role: .destructive) {
                    showForceExitAlert = true
                }
            }
        }
        .navigationTitle("成员详情")
        .navigationBarTitleDisplayMode(.inline)
        .alert("强制退出", isPresented: $showForceExitAlert) {
            Button("取消", role: .cancel) {}
            Button("确认", role: .destructive) {
                Task {
                    try? await admin.forceExit(membershipId: membership.id)
                    onAction()
                }
            }
        } message: {
            Text("确定要强制移除该成员吗？此操作不可撤销。")
        }
    }

    private func infoRow(_ label: String, _ value: String?) -> some View {
        HStack {
            Text(label)
                .font(.subheadline)
                .foregroundColor(.secondary)
            Spacer()
            Text(value ?? "-")
                .font(.subheadline)
        }
    }
}

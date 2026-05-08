import SwiftUI

struct AdminUsersView: View {
    @State private var users: [User] = []
    @State private var isLoading = false
    @State private var error: String?
    @State private var searchText = ""

    private let admin = AdminService()

    var filteredUsers: [User] {
        if searchText.isEmpty { return users }
        return users.filter {
            ($0.userName ?? "").localizedCaseInsensitiveContains(searchText) ||
            ($0.realName ?? "").localizedCaseInsensitiveContains(searchText) ||
            ($0.studentId ?? "").localizedCaseInsensitiveContains(searchText)
        }
    }

    var body: some View {
        Group {
            if isLoading {
                LoadingView("加载中...")
            } else if let error = error {
                ErrorView(message: error) { Task { await load() } }
            } else {
                listView
            }
        }
        .navigationTitle("用户管理")
        .searchable(text: $searchText, prompt: "搜索用户名/姓名/学号")
        .refreshable { await load() }
        .task { await load() }
    }

    private var listView: some View {
        List {
            ForEach(filteredUsers) { user in
                NavigationLink {
                    UserDetailView(user: user)
                } label: {
                    UserRow(user: user)
                }
            }
        }
        .listStyle(.plain)
    }

    private func load() async {
        isLoading = true
        error = nil
        do {
            users = try await admin.fetchUsers()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

struct UserRow: View {
    let user: User

    var body: some View {
        HStack(spacing: 12) {
            AvatarView(url: user.avatar, name: user.displayName, size: 40)
            VStack(alignment: .leading, spacing: 2) {
                Text(user.displayName)
                    .font(.subheadline.weight(.medium))
                if let userName = user.userName {
                    Text("@\(userName)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            Spacer()
            StatusBadge(status: user.userStatus)
        }
        .padding(.vertical, 4)
    }
}

struct UserDetailView: View {
    let user: User
    @State private var selectedStatus: String
    @State private var showStatusPicker = false
    @State private var showResetAlert = false
    @State private var error: String?

    private let admin = AdminService()

    init(user: User) {
        self.user = user
        _selectedStatus = State(initialValue: user.userStatus ?? "ACTIVE")
    }

    var body: some View {
        List {
            Section("基本信息") {
                HStack(spacing: 16) {
                    AvatarView(url: user.avatar, name: user.displayName, size: 60)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(user.displayName)
                            .font(.title3.bold())
                        if let userName = user.userName {
                            Text("@\(userName)")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(.vertical, 8)

                infoRow("学号", user.studentId)
                infoRow("性别", user.gender)
                infoRow("手机", user.phone)
                infoRow("邮箱", user.email)
                infoRow("学院", user.college)
                infoRow("专业", user.major)
                infoRow("年级", user.grade)
                infoRow("班级", user.className)
                infoRow("注册时间", user.createTime)
            }

            Section("状态管理") {
                Picker("状态", selection: $selectedStatus) {
                    Text("正常").tag("ACTIVE")
                    Text("停用").tag("DISABLED")
                    Text("锁定").tag("LOCKED")
                }
                .onChange(of: selectedStatus) { _, newValue in
                    Task {
                        try? await admin.updateUserStatus(id: user.id, status: newValue)
                    }
                }

                Button("重置密码", role: .destructive) {
                    showResetAlert = true
                }
            }
        }
        .navigationTitle("用户详情")
        .navigationBarTitleDisplayMode(.inline)
        .alert("重置密码", isPresented: $showResetAlert) {
            Button("取消", role: .cancel) {}
            Button("确认") {
                Task { try? await admin.resetPassword(id: user.id) }
            }
        } message: {
            Text("确定要重置此用户的密码吗？")
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

import SwiftUI

struct ProfileView: View {
    @Environment(Session.self) private var session
    @State private var vm = ProfileViewModel()

    @State private var showPasswordSheet = false
    @State private var oldPassword = ""
    @State private var newPassword = ""
    @State private var showLogoutAlert = false

    var body: some View {
        NavigationStack {
            Group {
                if !session.isLoggedIn {
                    notLoggedInView
                } else {
                    profileContent
                }
            }
            .navigationTitle("我的")
            .navigationBarTitleDisplayMode(.large)
        }
    }

    private var notLoggedInView: some View {
        VStack(spacing: 16) {
            Spacer()
            Image(systemName: "person.crop.circle")
                .font(.system(size: 64))
                .foregroundColor(.secondary)
            Text("未登录")
                .font(.title3)
            Text("登录后查看个人信息")
                .font(.subheadline)
                .foregroundColor(.secondary)
            NavigationLink {
                LoginView()
            } label: {
                Text("登录 / 注册")
                    .fontWeight(.semibold)
                    .frame(maxWidth: 200)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
            Spacer()
        }
    }

    private var profileContent: some View {
        List {
            Section {
                HStack(spacing: 16) {
                    AvatarView(url: session.currentUser?.avatar, name: session.currentUser?.displayName ?? "?", size: 56)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(session.currentUser?.displayName ?? "用户")
                            .font(.title3.bold())
                        if let studentId = session.currentUser?.studentId {
                            Text(studentId)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding(.vertical, 8)
            }

            Section("基本信息") {
                infoCell(label: "用户名", value: session.currentUser?.userName)
                infoCell(label: "真实姓名", value: session.currentUser?.realName)
                infoCell(label: "手机", value: session.currentUser?.phone)
                infoCell(label: "邮箱", value: session.currentUser?.email)
                infoCell(label: "学院", value: session.currentUser?.college)
                infoCell(label: "专业", value: session.currentUser?.major)
                infoCell(label: "年级", value: session.currentUser?.grade)
                infoCell(label: "班级", value: session.currentUser?.className)
                infoCell(label: "状态", badge: session.currentUser?.userStatus)
            }

            Section("操作") {
                Button("修改密码") { showPasswordSheet = true }

                NavigationLink {
                    NotificationsView()
                } label: {
                    Label("消息通知", systemImage: "bell")
                }

                Button(role: .destructive) { showLogoutAlert = true } label: {
                    Label("退出登录", systemImage: "rectangle.portrait.and.arrow.right")
                        .foregroundColor(.red)
                }
            }
        }
        .sheet(isPresented: $showPasswordSheet) {
            passwordChangeSheet
        }
        .alert("确认退出", isPresented: $showLogoutAlert) {
            Button("取消", role: .cancel) {}
            Button("退出", role: .destructive) { Task { await vm.logout() } }
        } message: {
            Text("确定要退出登录吗？")
        }
        .task { await vm.load() }
    }

    private func infoCell(label: String, value: String? = nil, badge: String? = nil) -> some View {
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

    private var passwordChangeSheet: some View {
        NavigationStack {
            Form {
                Section("修改密码") {
                    SecureField("原密码", text: $oldPassword)
                    SecureField("新密码", text: $newPassword)
                }

                if let error = vm.error {
                    Section {
                        Text(error)
                            .foregroundColor(.red)
                    }
                }

                Section {
                    Button("确认修改") {
                        Task {
                            await vm.changePassword(old: oldPassword, new: newPassword)
                            if vm.passwordChangeSuccess {
                                showPasswordSheet = false
                                oldPassword = ""
                                newPassword = ""
                                vm.passwordChangeSuccess = false
                            }
                        }
                    }
                    .disabled(oldPassword.isEmpty || newPassword.isEmpty)
                }
            }
            .navigationTitle("修改密码")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { showPasswordSheet = false }
                }
            }
        }
    }
}

import SwiftUI

struct ProfileView: View {
    @Environment (Session.self) private var session
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
        ScrollView {
            VStack(spacing: 20) {
                // Modern Header
                VStack(spacing: 16) {
                    AvatarView(url: session.currentUser?.avatar, name: session.currentUser?.displayName ?? "?", size: 90)
                        .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)

                    VStack(spacing: 4) {
                        Text(session.currentUser?.displayName ?? "用户")
                            .font(.title2.bold())
                        if let studentId = session.currentUser?.studentId {
                            Text(studentId)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 32)
                //                .background(Color(.systemBackground))
                    .clipShape(UnevenRoundedRectangle(bottomLeadingRadius: 32, bottomTrailingRadius: 32))
                    .shadow(color: .black.opacity(0.03), radius: 10, x: 0, y: 5)

                VStack(spacing: 20) {
                    // Application Section
                    VStack(alignment: .leading, spacing: 12) {
                        Text("我的申请")
                            .font(.headline)

                        NavigationLink {
                            ApplicationProgressView()
                        } label: {
                            HStack {
                                Label("查看申请进度", systemImage: "doc.text.fill")
                                    .foregroundColor(.primary)
                                Spacer()
                                Image(systemName: "chevron.right")
                                    .font(.caption.bold())
                                    .foregroundColor(.secondary)
                            }
                                .padding()
                                .background(Color.accentColor.opacity(0.05))
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                        .premiumCard()

                    // Info Section
                    VStack(alignment: .leading, spacing: 16) {
                        Text("基本信息")
                            .font(.headline)

                        VStack(spacing: 12) {
                            infoCell(label: "用户名", value: session.currentUser?.userName)
                            Divider()
                            infoCell(label: "真实姓名", value: session.currentUser?.realName)
                            Divider()
                            infoCell(label: "学院", value: session.currentUser?.college)
                            Divider()
                            infoCell(label: "专业", value: session.currentUser?.major)
                            Divider()
                            infoCell(label: "年级", value: session.currentUser?.grade)
                            Divider()
                            infoCell(label: "状态", badge: session.currentUser?.userStatus)
                        }
                    }
                        .premiumCard()

                    // Action Section
                    VStack(alignment: .leading, spacing: 12) {
                        Text("账户设置")
                            .font(.headline)

                        VStack(spacing: 0) {
                            actionRow(label: "修改密码", icon: "lock.fill") { showPasswordSheet = true }
                            Divider()
                            NavigationLink {
                                NotificationsView()
                            } label: {
                                HStack {
                                    Label("消息通知", systemImage: "bell.fill")
                                        .foregroundColor(.primary)
                                    Spacer()
                                    if session.unreadCount > 0 {
                                        Text("\(session.unreadCount)")
                                            .font(.caption2.bold())
                                            .foregroundColor(.white)
                                            .padding(.horizontal, 6)
                                            .padding(.vertical, 2)
                                            .background(Color.red)
                                            .clipShape(Capsule())
                                    }
                                    Image(systemName: "chevron.right")
                                        .font(.caption.bold())
                                        .foregroundColor(.secondary)
                                }
                                    .padding(.vertical, 12)
                            }
                            Divider()
                            actionRow(label: "退出登录", icon: "rectangle.portrait.and.arrow.right", color: .red) { showLogoutAlert = true }
                        }
                    }
                        .premiumCard()
                }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
            }
        }
        //        .background(Color(.systemGroupedBackground))
            .ignoresSafeArea(edges: .top)
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
                    .font(.subheadline.bold())
            }
        }
    }

    private func actionRow(
            label: String,
            icon: String,
            color: Color =.primary,
            action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack {
                Label(label, systemImage: icon)
                    .foregroundColor(color)
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption.bold())
                    .foregroundColor(.secondary)
            }
                .padding(.vertical, 12)
        }
    }

    /*private var passwordChangeSheet: some View {
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
    }*/
}

import SwiftUI

struct SettingsView: View {
    @Environment(Session.self) private var session

    var body: some View {
        NavigationStack {
            Group {
                if session.isLoggedIn && session.isAdmin {
                    adminLinks
                } else if session.isLoggedIn {
                    regularUserView
                } else {
                    notLoggedInView
                }
            }
            .navigationTitle("管理")
            .navigationBarTitleDisplayMode(.large)
        }
    }

    private var adminLinks: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Text("管理功能")
                    .font(.headline)
                    .padding(.horizontal, 4)
                
                VStack(spacing: 0) {
                    adminRow(label: "控制台", icon: "rectangle.3.group.fill", color: .blue) { AdminDashboardView() }
                    Divider()
                    adminRow(label: "用户管理", icon: "person.2.fill", color: .purple) { AdminUsersView() }
                    Divider()
                    adminRow(label: "社团管理", icon: "building.2.fill", color: .orange) { AdminClubsView() }
                    Divider()
                    adminRow(label: "申请管理", icon: "doc.text.fill", color: .green) { AdminApplicationsView() }
                    Divider()
                    adminRow(label: "面试管理", icon: "person.bubble.fill", color: .red) { AdminInterviewsView() }
                    Divider()
                    adminRow(label: "成员管理", icon: "person.3.fill", color: .indigo) { AdminMembershipsView() }
                }
                .premiumCard()
            }
            .padding(16)
        }
    }
    
    private func adminRow<V: View>(label: String, icon: String, color: Color, @ViewBuilder destination: () -> V) -> some View {
        NavigationLink {
            destination()
        } label: {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .foregroundColor(.white)
                    .frame(width: 32, height: 32)
                    .background(color)
                    .clipShape(RoundedRectangle(cornerRadius: 8))
                
                Text(label)
                    .foregroundColor(.primary)
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption.bold())
                    .foregroundColor(.secondary)
            }
            .padding(.vertical, 12)
        }
    }

    private var regularUserView: some View {
        VStack(spacing: 16) {
            Image(systemName: "lock.shield")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
            Text("无管理权限")
                .font(.headline)
            Text("您没有访问管理功能的权限")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }

    private var notLoggedInView: some View {
        VStack(spacing: 16) {
            Image(systemName: "lock.shield")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
            Text("请先登录")
                .font(.headline)
            NavigationLink("去登录") {
                LoginView()
            }
            .buttonStyle(.borderedProminent)
        }
    }
}

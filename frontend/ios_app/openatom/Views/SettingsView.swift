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
        List {
            Section("管理功能") {
                NavigationLink {
                    AdminDashboardView()
                } label: {
                    Label("控制台", systemImage: "rectangle.3.group")
                }

                NavigationLink {
                    AdminUsersView()
                } label: {
                    Label("用户管理", systemImage: "person.2")
                }

                NavigationLink {
                    AdminClubsView()
                } label: {
                    Label("社团管理", systemImage: "building.2")
                }

                NavigationLink {
                    AdminApplicationsView()
                } label: {
                    Label("申请管理", systemImage: "doc.text")
                }

                NavigationLink {
                    AdminInterviewsView()
                } label: {
                    Label("面试管理", systemImage: "person.bubble")
                }

                NavigationLink {
                    AdminMembershipsView()
                } label: {
                    Label("成员管理", systemImage: "person.3")
                }
            }
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

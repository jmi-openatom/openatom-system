import SwiftUI

struct AdminDashboardView: View {
    @Environment(Session.self) private var session
    @State private var users: [User] = []
    @State private var clubs: [Club] = []
    @State private var applications: [MembershipApplication] = []
    @State private var interviews: [Interview] = []
    @State private var isLoading = false
    @State private var error: String?

    private let admin = AdminService()

    var body: some View {
        Group {
            if isLoading {
                LoadingView("加载中...")
            } else if let error = error {
                ErrorView(message: error) { Task { await loadStats() } }
            } else {
                contentView
            }
        }
        .navigationTitle("控制台")
        .task { await loadStats() }
        .refreshable { await loadStats() }
    }

    private var contentView: some View {
        ScrollView {
            VStack(spacing: 16) {
                HStack(spacing: 12) {
                    DashboardCard(
                        title: "用户总数",
                        value: "\(users.count)",
                        icon: "person.fill",
                        color: .blue
                    )
                    DashboardCard(
                        title: "社团数量",
                        value: "\(clubs.count)",
                        icon: "building.2.fill",
                        color: .green
                    )
                }

                HStack(spacing: 12) {
                    DashboardCard(
                        title: "待审核申请",
                        value: "\(applications.filter { $0.status == "SUBMITTED" || $0.status == "PENDING" }.count)",
                        icon: "doc.text.fill",
                        color: .orange
                    )
                    DashboardCard(
                        title: "待面试",
                        value: "\(interviews.filter { $0.status == "SCHEDULED" }.count)",
                        icon: "person.bubble.fill",
                        color: .purple
                    )
                }

                pendingApplicationsSection
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
    }

    private var pendingApplicationsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Label("待处理申请", systemImage: "clock")
                .font(.headline)

            if applications.isEmpty {
                Text("暂无待处理申请")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding()
            } else {
                ForEach(applications.prefix(10)) { app in
                    VStack(alignment: .leading, spacing: 4) {
                        HStack {
                            Text(app.user?.displayName ?? "申请人")
                                .font(.subheadline.weight(.medium))
                            Spacer()
                            StatusBadge(status: app.status)
                        }
                        if let clubName = app.club?.name {
                            Text(clubName)
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.vertical, 4)

                    if app.id != applications.prefix(10).last?.id {
                        Divider()
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func loadStats() async {
        isLoading = true
        error = nil
        do {
            async let u = admin.fetchUsers()
            async let c = admin.fetchClubs()
            async let a = admin.fetchApplications()
            async let i = admin.fetchInterviews()
            (users, clubs, applications, interviews) = try await (u, c, a, i)
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

struct DashboardCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .font(.title3)
                    .foregroundColor(color)
                Spacer()
            }
            Text(value)
                .font(.title.bold())
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(color.opacity(0.08))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

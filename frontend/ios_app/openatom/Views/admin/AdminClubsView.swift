import SwiftUI

struct AdminClubsView: View {
    @State private var clubs: [Club] = []
    @State private var isLoading = false
    @State private var error: String?

    private let admin = AdminService()

    var body: some View {
        Group {
            if isLoading {
                LoadingView("加载中...")
            } else if let error = error {
                ErrorView(message: error) { Task { await load() } }
            } else if clubs.isEmpty {
                EmptyStateView(icon: "building.2", title: "暂无社团")
            } else {
                listView
            }
        }
        .navigationTitle("社团管理")
        .refreshable { await load() }
        .task { await load() }
    }

    private var listView: some View {
        List {
            ForEach(clubs) { club in
                NavigationLink {
                    ClubDetailView(club: club)
                } label: {
                    ClubRow(club: club)
                }
            }
        }
        .listStyle(.plain)
    }

    private func load() async {
        isLoading = true
        error = nil
        do {
            clubs = try await admin.fetchClubs()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

struct ClubRow: View {
    let club: Club

    var body: some View {
        HStack(spacing: 12) {
            if let logo = club.logoUrl {
                AsyncImageView(url: logo, size: 44)
            } else {
                Image(systemName: "building.2.fill")
                    .font(.title3)
                    .foregroundColor(.accentColor)
                    .frame(width: 44, height: 44)
                    .background(Color.accentColor.opacity(0.1))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(club.name ?? "")
                    .font(.subheadline.weight(.medium))
                if let code = club.code {
                    Text(code)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            Spacer()
            StatusBadge(status: club.status)
        }
        .padding(.vertical, 4)
    }
}

struct ClubDetailView: View {
    let club: Club
    @State private var selectedStatus: String
    @State private var selectedRecruitmentStatus: String
    @State private var departments: [ClubDepartment] = []
    @State private var positions: [ClubPosition] = []
    @State private var isLoading = false

    private let admin = AdminService()

    init(club: Club) {
        self.club = club
        _selectedStatus = State(initialValue: club.status ?? "ACTIVE")
        _selectedRecruitmentStatus = State(initialValue: club.recruitmentStatus ?? "CLOSED")
    }

    var body: some View {
        List {
            Section("基本信息") {
                infoRow("名称", club.name)
                infoRow("编码", club.code)
                infoRow("类别", club.category)
                if let desc = club.description {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("简介")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        Text(desc)
                            .font(.body)
                    }
                }
            }

            Section("状态管理") {
                Picker("社团状态", selection: $selectedStatus) {
                    Text("正常").tag("ACTIVE")
                    Text("停用").tag("DISABLED")
                }
                .onChange(of: selectedStatus) { _, newValue in
                    Task { try? await admin.updateClubStatus(id: club.id, status: newValue) }
                }

                Picker("招募状态", selection: $selectedRecruitmentStatus) {
                    Text("开放").tag("OPEN")
                    Text("关闭").tag("CLOSED")
                }
                .onChange(of: selectedRecruitmentStatus) { _, newValue in
                    Task {
                        let _: EmptyResponse = try await APIClient.shared.patch(
                            "/clubs/\(club.id)/recruitment-status",
                            body: ["recruitmentStatus": newValue]
                        )
                    }
                }
            }

            if !departments.isEmpty {
                Section("部门 (\(departments.count))") {
                    ForEach(departments) { dept in
                        Text(dept.name ?? "")
                    }
                }
            }

            if !positions.isEmpty {
                Section("岗位 (\(positions.count))") {
                    ForEach(positions) { pos in
                        HStack {
                            Text(pos.name ?? "")
                            Spacer()
                            if let max = pos.maxCount {
                                Text("最多\(max)人")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }
        }
        .navigationTitle(club.name ?? "社团详情")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            isLoading = true
            async let d = admin.fetchClubDepartments(clubId: club.id)
            async let p = admin.fetchClubPositions(clubId: club.id)
            do {
                let (deptResult, posResult) = try await (d, p)
                departments = deptResult
                positions = posResult
            } catch {}
            isLoading = false
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

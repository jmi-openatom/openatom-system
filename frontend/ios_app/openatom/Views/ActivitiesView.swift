import SwiftUI

struct ActivitiesView: View {
    @State private var vm = ActivitiesViewModel()

    var body: some View {
        NavigationStack {
            Group {
                if vm.isLoading {
                    LoadingView("加载中...")
                } else if let error = vm.error {
                    ErrorView(message: error) { Task { await vm.load() } }
                } else if vm.activities.isEmpty {
                    EmptyStateView(icon: "calendar.badge.exclamationmark", title: "暂无活动")
                } else {
                    listView
                }
            }
            .navigationTitle("活动")
            .navigationBarTitleDisplayMode(.large)
            .refreshable { await vm.load() }
            .task { await vm.load() }
        }
    }

    private var listView: some View {
        List {
            ForEach(vm.activities) { activity in
                NavigationLink {
                    ActivityDetailView(activityId: activity.id)
                } label: {
                    ActivityRow(activity: activity)
                }
            }
        }
        .listStyle(.plain)
    }
}

struct ActivityRow: View {
    let activity: Activity

    var body: some View {
        HStack(spacing: 12) {
            if let cover = activity.coverUrl, !cover.isEmpty {
                AsyncImageView(url: cover, size: 64)
            } else {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(.systemGray5))
                    .frame(width: 64, height: 64)
                    .overlay {
                        Image(systemName: "calendar")
                            .foregroundColor(.gray)
                    }
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(activity.title ?? "")
                    .font(.subheadline.weight(.medium))
                    .lineLimit(2)
                if let summary = activity.summary, !summary.isEmpty {
                    Text(summary)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                HStack(spacing: 8) {
                    let date = activity.formattedDate
                    if !date.isEmpty {
                        Label(date, systemImage: "calendar")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    if let location = activity.location {
                        Label(location, systemImage: "location")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
        .padding(.vertical, 4)
    }
}

struct ActivityDetailView: View {
    let activityId: Int
    @Environment(Session.self) private var session
    @State private var vm = ActivityDetailViewModel()

    var body: some View {
        Group {
            if vm.isLoading {
                LoadingView("加载中...")
            } else if let error = vm.error {
                ErrorView(message: error) { Task { await vm.load(id: activityId) } }
            } else if let activity = vm.activity {
                activityContent(activity)
            }
        }
        .navigationTitle("活动详情")
        .navigationBarTitleDisplayMode(.inline)
        .task { await vm.load(id: activityId) }
        .alert("报名成功", isPresented: $vm.registerSuccess) {
            Button("确定", role: .cancel) {}
        } message: {
            Text("您已成功报名此活动")
        }
    }

    private func activityContent(_ activity: Activity) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                if let cover = activity.coverUrl, !cover.isEmpty {
                    AsyncImageView(url: cover, size: 240)
                        .frame(maxWidth: .infinity)
                        .frame(height: 200)
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                VStack(alignment: .leading, spacing: 8) {
                    Text(activity.title ?? "")
                        .font(.title2.bold())

                    if let summary = activity.summary, !summary.isEmpty {
                        Text(summary)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    let dateRange = activity.formattedDateRange
                    if !dateRange.isEmpty {
                        Label(dateRange, systemImage: "calendar")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    if let location = activity.location {
                        Label(location, systemImage: "location")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }

                    if let status = activity.status {
                        StatusBadge(status: status)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(Color(.systemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))

                if let desc = activity.descriptionMarkdown, !desc.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("活动详情")
                            .font(.headline)
                        Text(desc)
                            .font(.body)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color(.systemBackground))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                if activity.registrationRequired == true && session.isLoggedIn {
                    Button(action: { Task { await vm.register() } }) {
                        Label("立即报名", systemImage: "person.badge.plus")
                            .fontWeight(.semibold)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                    }
                    .buttonStyle(.borderedProminent)
                    .padding(.horizontal)
                }
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
    }
}

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
        HStack(spacing: 16) {
            if let cover = activity.coverUrl, !cover.isEmpty {
                AsyncImageView(url: cover, size: 80)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            } else {
                RoundedRectangle(cornerRadius: 12)
                    .fill(Color(.systemGray6))
                    .frame(width: 80, height: 80)
                    .overlay {
                        Image(systemName: "calendar")
                            .font(.title2)
                            .foregroundColor(.gray)
                    }
            }
            
            VStack(alignment: .leading, spacing: 6) {
                Text(activity.title ?? "")
                    .font(.headline)
                    .lineLimit(2)
                    .foregroundColor(.primary)
                
                if let summary = activity.summary, !summary.isEmpty {
                    Text(summary)
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
                
                Spacer(minLength: 0)
                
                HStack(spacing: 12) {
                    let date = activity.formattedDate
                    if !date.isEmpty {
                        Label(date, systemImage: "clock")
                    }
                    if let location = activity.location {
                        Label(location, systemImage: "mappin")
                    }
                }
                .font(.system(size: 10, weight: .medium))
                .foregroundColor(.accentColor)
            }
        }
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
            VStack(spacing: 24) {
                if let cover = activity.coverUrl, !cover.isEmpty {
                    AsyncImageView(url: cover, size: 400)
                        .frame(maxWidth: .infinity)
                        .frame(height: 240)
                        .clipShape(RoundedRectangle(cornerRadius: 24))
                        .shadow(color: .black.opacity(0.1), radius: 10, x: 0, y: 5)
                }

                VStack(alignment: .leading, spacing: 20) {
                    VStack(alignment: .leading, spacing: 12) {
                        if let status = activity.status {
                            StatusBadge(status: status)
                        }
                        
                        Text(activity.title ?? "")
                            .font(.title.bold())
                        
                        if let summary = activity.summary, !summary.isEmpty {
                            Text(summary)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                    
                    Divider()
                    
                    VStack(spacing: 16) {
                        if !activity.formattedDateRange.isEmpty {
                            detailRow(icon: "calendar", label: "活动时间", value: activity.formattedDateRange)
                        }
                        detailRow(icon: "mappin.circle", label: "活动地点", value: activity.location ?? "未指定")
                    }
                    
                    if let desc = activity.descriptionMarkdown, !desc.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("详情介绍")
                                .font(.headline)
                            Text(desc)
                                .font(.body)
                                .foregroundColor(.secondary)
                                .lineSpacing(4)
                        }
                    }
                }
                .premiumCard()

                if activity.registrationRequired == true && session.isLoggedIn {
                    Button(action: { Task { await vm.register() } }) {
                        HStack {
                            Text("立即报名")
                                .fontWeight(.bold)
                            Image(systemName: "arrow.right")
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(
                            LinearGradient(colors: [.accentColor, .accentColor.opacity(0.8)], startPoint: .leading, endPoint: .trailing)
                        )
                        .foregroundColor(.white)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                        .shadow(color: .accentColor.opacity(0.3), radius: 10, x: 0, y: 5)
                    }
                }
            }
            .padding(16)
        }
        .background(Color(.systemGroupedBackground))
    }
    
    private func detailRow(icon: String, label: String, value: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.headline)
                .foregroundColor(.accentColor)
                .frame(width: 32, height: 32)
                .background(Color.accentColor.opacity(0.1))
                .clipShape(Circle())
            
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption2)
                    .foregroundColor(.secondary)
                Text(value)
                    .font(.subheadline.bold())
            }
        }
    }
}

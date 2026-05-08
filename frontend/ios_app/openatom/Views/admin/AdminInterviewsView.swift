import SwiftUI

struct AdminInterviewsView: View {
    @State private var interviews: [Interview] = []
    @State private var isLoading = false
    @State private var error: String?

    private let admin = AdminService()

    var body: some View {
        Group {
            if isLoading {
                LoadingView("加载中...")
            } else if let error = error {
                ErrorView(message: error) { Task { await load() } }
            } else if interviews.isEmpty {
                EmptyStateView(icon: "person.bubble", title: "暂无面试安排")
            } else {
                listView
            }
        }
        .navigationTitle("面试管理")
        .refreshable { await load() }
        .task { await load() }
    }

    private var listView: some View {
        List {
            ForEach(interviews) { interview in
                NavigationLink {
                    InterviewDetailView(interview: interview, onAction: { Task { await load() } })
                } label: {
                    InterviewRow(interview: interview)
                }
            }
        }
        .listStyle(.plain)
    }

    private func load() async {
        isLoading = true
        error = nil
        do {
            interviews = try await admin.fetchInterviews()
        } catch {
            self.error = error.localizedDescription
        }
        isLoading = false
    }
}

struct InterviewRow: View {
    let interview: Interview

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(interview.application?.user?.displayName ?? "面试者")
                    .font(.subheadline.weight(.medium))
                Spacer()
                StatusBadge(status: interview.status)
            }
            if let time = interview.scheduledStartAt {
                Label(time, systemImage: "clock")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            if let location = interview.location {
                Label(location, systemImage: "location")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            if let mode = interview.mode {
                Text(mode)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
}

struct InterviewDetailView: View {
    let interview: Interview
    let onAction: () -> Void

    @State private var showCompleteAlert = false

    private let admin = AdminService()

    var body: some View {
        List {
            Section("面试信息") {
                infoRow("面试者", interview.application?.user?.displayName)
                infoRow("社团", interview.application?.club?.name)
                infoRow("轮次", interview.round.map { "第\($0)轮" })
                infoRow("时间", interview.scheduledStartAt)
                infoRow("地点", interview.location)
                infoRow("方式", interview.mode)
                infoRow("状态", badge: interview.status)
            }

            if let interviewers = interview.interviewers, !interviewers.isEmpty {
                Section("面试官") {
                    ForEach(interviewers) { interviewer in
                        HStack {
                            AvatarView(url: interviewer.avatar, name: interviewer.displayName, size: 32)
                            Text(interviewer.displayName)
                        }
                    }
                }
            }

            if let feedbacks = interview.feedbacks, !feedbacks.isEmpty {
                Section("面试反馈") {
                    ForEach(feedbacks) { feedback in
                        VStack(alignment: .leading, spacing: 4) {
                            Text(feedback.interviewer?.displayName ?? "面试官")
                                .font(.subheadline.weight(.medium))
                            if let suggestion = feedback.suggestion {
                                Text(suggestion)
                                    .font(.body)
                            }
                            if let scores = feedback.scores {
                                Text("评分: \(scores)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }
            }

            if interview.status == "SCHEDULED" {
                Section("操作") {
                    Button("完成面试") {
                        showCompleteAlert = true
                    }
                    .buttonStyle(.borderedProminent)
                }
            }
        }
        .navigationTitle("面试详情")
        .navigationBarTitleDisplayMode(.inline)
        .alert("确认完成", isPresented: $showCompleteAlert) {
            Button("取消", role: .cancel) {}
            Button("确认") {
                Task {
                    try? await admin.completeInterview(id: interview.id)
                    onAction()
                }
            }
        } message: {
            Text("确定将此面试标记为已完成吗？")
        }
    }

    private func infoRow(_ label: String, _ value: String? = nil, badge: String? = nil) -> some View {
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
}

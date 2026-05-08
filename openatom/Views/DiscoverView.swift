import SwiftUI

struct DiscoverView: View {
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
